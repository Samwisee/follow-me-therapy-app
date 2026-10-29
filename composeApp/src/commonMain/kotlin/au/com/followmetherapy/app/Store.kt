package au.com.followmetherapy.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * What the app needs from the device. On the web it's the browser (local
 * storage, files, the clock); Android and iOS implement the same interface
 * later. Everything stays on the device: nothing is sent anywhere.
 */
interface Platform {
    /** The saved record (see [Record]), or null on first run. */
    fun loadRecord(): String?
    fun saveRecord(json: String)
    /** Data saved by older versions (one key per entry), to move into the record once. */
    fun legacyEntries(): Map<String, String> = emptyMap()
    fun clearLegacy() {}

    /** Now, in ms since 1970 (the device's clock): when each entry changed. */
    fun now(): Long
    /** Today's date on the device, as yyyy-mm-dd. */
    fun today(): String
    /** Today's weekday, Monday = 0 … Sunday = 6. */
    fun weekday(): Int
    /** The local hour, 0–23 (for the greeting). */
    fun hour(): Int
    /** The date of this week's Monday, as yyyy-mm-dd. */
    fun weekStart(): String
    /** Which build is running (shown under For grown-ups). */
    fun build(): String = "dev"

    /** Hands the person a file to keep (a backup). */
    fun saveFile(name: String, text: String) {}
    /** Lets the person pick a file, and gives back its text. */
    fun openFile(onText: (String) -> Unit) {}
    /** Asks the device to keep this app's data even when it's short of space: true kept, false not, null can't tell. */
    fun keepData(onResult: (Boolean?) -> Unit) { onResult(null) }
    /** Running from the Home Screen (installed), not in a browser tab. */
    fun installed(): Boolean = false
    /** Which install steps to show first. */
    fun device(): Device = Device.Computer
    /** The browser can install the app with one tap (Chrome, Edge on Android and computers). */
    fun canInstall(): Boolean = false
    fun install(onResult: (Boolean) -> Unit) { onResult(false) }
    /** Space the device keeps at the bottom edge (the home indicator), in dp. */
    fun safeBottom(): Float = 0f
}

enum class Device { IPhone, Android, Computer }

/** A platform that keeps nothing (previews and tests). */
object MemoryPlatform : Platform {
    private var record: String? = null
    override fun loadRecord() = record
    override fun saveRecord(json: String) { record = json }
    override fun now() = 1793315000000L
    override fun today() = "2026-10-30"
    override fun weekday() = 4
    override fun hour() = 8
    override fun weekStart() = "2026-10-26"
}

private const val SEP = "\u001f"

private fun ints(s: String?): List<Int> = s?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList()

/**
 * The app's saved state: today's routine, the calm-down plan and this week's
 * practice, kept in one [Record] and written to the device on every change.
 */
class AppStore(private val p: Platform) {
    val today: String = p.today()
    val weekday: Int = p.weekday()
    val hour: Int = p.hour()
    private val week: String = p.weekStart()
    val build: String = p.build()

    private val record: Record = (p.loadRecord()?.let { Record.parse(it) } ?: Record()).also { r ->
        // Move data from older versions (one key per entry) into the record, once.
        val old = p.legacyEntries()
        if (old.isNotEmpty()) {
            val at = p.now()
            for ((k, v) in old) if (r.entries[k] == null) r.put(k, v, at)
            p.saveRecord(r.toJson(at))
            p.clearLegacy()
        }
    }

    private fun get(key: String): String? = record.get(key)
    private fun put(key: String, value: String?) {
        val at = p.now()
        record.put(key, value, at)
        p.saveRecord(record.toJson(at))
    }

    // ── Morning routine ──
    val routineSteps = listOf("Toilet and wash hands", "Get dressed", "Eat breakfast", "Brush teeth", "Shoes on", "Bag on, out the door")
    val routineDone = mutableStateListOf<Int>()
    var routineTricky by mutableStateOf("")
        private set

    fun toggleRoutineStep(i: Int, done: Boolean) {
        if (done && i !in routineDone) routineDone.add(i)
        if (!done) routineDone.remove(i)
        put("routine.done.$today", routineDone.sorted().joinToString(","))
    }
    fun setTricky(v: String) { routineTricky = v; put("routine.tricky.$today", v) }
    val routineComplete: Boolean get() = routineDone.size == routineSteps.size

    // ── Calm-down plan ──
    val calmChosen = mutableStateListOf<Int>()
    var calmIdea by mutableStateOf("")
        private set
    var calmBody by mutableStateOf("")
        private set
    var calmSaved by mutableStateOf(false)
        private set

    fun toggleCalm(i: Int) {
        if (i in calmChosen) calmChosen.remove(i) else calmChosen.add(i)
        put("calm.chosen", calmChosen.joinToString(","))
    }
    fun setCalmIdea(v: String) { calmIdea = v; put("calm.idea", v) }
    fun setCalmBody(v: String) { calmBody = v; put("calm.body", v) }
    fun saveCalmPlan() { calmSaved = true; put("calm.saved", "1") }

    // ── Practice week ──
    val weekNames = mutableStateListOf<String>()
    val weekDone = mutableStateListOf<Pair<Int, Int>>()
    var weekSetUp by mutableStateOf(false)
        private set
    var weekProud by mutableStateOf("")
        private set

    fun renameActivity(i: Int, v: String) { weekNames[i] = v; put("week.names", weekNames.joinToString(SEP)) }
    fun finishWeekSetup() { weekSetUp = true; put("week.setup", "1") }
    fun togglePractice(r: Int, d: Int) {
        val k = r to d
        if (k in weekDone) weekDone.remove(k) else weekDone.add(k)
        put("week.done.$week", weekDone.joinToString(",") { "${it.first}:${it.second}" })
    }
    fun setProud(v: String) { weekProud = v; put("week.proud.$week", v) }
    val activities: List<Pair<Int, String>> get() = weekNames.withIndex().filter { it.value.isNotBlank() }.map { it.index to it.value }
    val starsThisWeek: Int get() = weekDone.count { (r, _) -> weekNames.getOrNull(r)?.isNotBlank() == true }

    init { readAll() }

    /** Fills the on-screen state from the record (at start, and after a restore). */
    private fun readAll() {
        routineDone.clear(); routineDone.addAll(ints(get("routine.done.$today")))
        routineTricky = get("routine.tricky.$today") ?: ""
        calmChosen.clear(); calmChosen.addAll(ints(get("calm.chosen")))
        calmIdea = get("calm.idea") ?: ""
        calmBody = get("calm.body") ?: ""
        calmSaved = get("calm.saved") == "1"
        weekNames.clear(); weekNames.addAll(((get("week.names")?.split(SEP) ?: emptyList()) + List(4) { "" }).take(4))
        weekDone.clear()
        get("week.done.$week")?.split(",")?.forEach { cell ->
            val parts = cell.split(":")
            val r = parts.getOrNull(0)?.toIntOrNull()
            val d = parts.getOrNull(1)?.toIntOrNull()
            if (r != null && d != null) weekDone.add(r to d)
        }
        weekSetUp = get("week.setup") == "1"
        weekProud = get("week.proud.$week") ?: ""
    }

    // ── Backup and restore ──
    /** How many things are saved (for the backup line). */
    val savedCount: Int get() = record.size

    /** Hands over the whole record as a file to keep. */
    fun saveBackup() {
        val at = p.now()
        p.saveFile("follow-me-therapy-backup-$today.json", record.toJson(at))
    }

    /**
     * Restores from a backup file: for each entry, whichever changed last wins,
     * so restoring never loses something newer on this device.
     */
    fun restore(text: String): RestoreResult {
        val incoming = Record.parse(text) ?: return RestoreResult.NotABackup
        val changed = record.merge(incoming)
        p.saveRecord(record.toJson(p.now()))
        readAll()
        return RestoreResult.Restored(changed)
    }

    /** Clears everything this app has saved on this device. */
    fun forgetEverything() {
        val at = p.now()
        for (k in record.entries.keys.toList()) record.put(k, null, at)
        p.saveRecord(record.toJson(at))
        readAll()
    }
}

sealed interface RestoreResult {
    data object NotABackup : RestoreResult
    data class Restored(val changed: Int) : RestoreResult
}
