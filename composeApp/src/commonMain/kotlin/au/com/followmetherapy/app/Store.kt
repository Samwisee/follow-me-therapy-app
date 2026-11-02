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

    /** Hands the person a file to keep (a backup, a calendar of reminders). */
    fun saveFile(name: String, text: String, type: String = "application/json") {}
    /** Opens the share sheet with [text] (Messages, Mail…); false if there's none (then copy instead). */
    fun shareText(title: String, text: String, onResult: (Boolean) -> Unit) { onResult(false) }
    /** Copies [text]; true if it worked. */
    fun copyText(text: String, onResult: (Boolean) -> Unit) { onResult(false) }
    /** Reads what was copied (after a tap); null if the browser won't say. */
    fun readClipboard(onText: (String?) -> Unit) { onText(null) }
    /** A QR code of [text] (rows of dark modules), or null if it can't be made here. */
    fun qrCode(text: String): List<BooleanArray>? = null
    /** Opened from the Planner icon (Simone's planner), not the family app. */
    fun plannerEntry(): Boolean = false
    /** The reader asked for less motion (prefers-reduced-motion). */
    fun reducedMotion(): Boolean = false
    /** This app's address, for links (plans). */
    fun appUrl(): String = "https://samwisee.github.io/follow-me-therapy-app/"
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
    /** On an iPhone or iPad in a browser other than Safari (Chrome, Firefox, Edge…). */
    fun otherIosBrowser(): Boolean = false
    /** Opens this page in Safari (iOS 17 and later). */
    fun openInSafari() {}
    /** Opens [url] (a new tab, or the app that handles it). */
    fun openUrl(url: String) {}
    /** Goes to the Planner's own page (planner.html), in this window. */
    fun openPlanner() {}
    /** The app has drawn its first screen: the page's splash can go. */
    fun ready() {}

    // ── The Planner's lock (vault.js, loaded only by planner.html) ──
    /** "absent" (not the Planner page), "unsupported", "new", "locked" or "open". */
    fun vaultStatus(): String = "absent"
    /** Checks the setup code, makes the passkey (Face ID) and starts the vault with [seedJson]. */
    fun vaultSetup(code: String, seedJson: String, onResult: (String) -> Unit) { onResult("unsupported") }
    fun vaultUnlock(onResult: (String) -> Unit) { onResult("error") }
    fun vaultGet(key: String): String? = null
    fun vaultPut(key: String, value: String?) {}
    fun vaultKeys(prefix: String): List<String> = emptyList()
    fun vaultReset() {}
    /** Called when the vault locks itself (five minutes in the background). */
    fun onVaultLock(onLock: () -> Unit) {}
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

    /**
     * The first time a finished row is seen (back on Today or Activities after
     * finishing), its star bursts; after that it just shows. [key] names the
     * row and its period: "routine:<date>", "calm", "practice:<week>".
     */
    fun firstSeenDone(key: String, done: Boolean): Boolean {
        if (!done) return false
        val seen = get("seen.done")?.split(",")?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
        if (key in seen) return false
        put("seen.done", (seen + key).toList().takeLast(60).joinToString(","))
        return true
    }

    /** Letters and shapes traced (any day), for the ticks on the picker. */
    private val traced = mutableStateListOf<String>().apply { addAll(get("letters.traced")?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()) }
    fun letterTraced(name: String) = name in traced
    fun markLetterTraced(name: String) { if (name !in traced) { traced.add(name); put("letters.traced", traced.joinToString(",")) } }

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

    // ── The plan from Simone ──
    var plan by mutableStateOf<Plan?>(null)
        private set

    /** Takes a plan from Simone. Its practice activities become this week's practice. */
    fun setPlan(p: Plan) {
        plan = p
        put("plan", p.toJson())
        if (p.practice.isNotEmpty()) {
            for (i in weekNames.indices) weekNames[i] = p.practice.getOrNull(i)?.name ?: ""
            put("week.names", weekNames.joinToString(SEP))
            weekSetUp = true
            put("week.setup", "1")
        }
    }

    /** The calm-down plan was used today (counted for Simone's summary). */
    fun calmUsed() { put("calm.used.$today", ((get("calm.used.$today")?.toIntOrNull() ?: 0) + 1).toString()) }


    /** This week's Monday, yyyy-mm-dd. */
    val weekStart: String get() = week

    // ── Progress on any day (for the calendar) ──
    fun routineStepsOn(date: String): Int = ints(get("routine.done.$date")).size
    fun calmUsedOn(date: String): Int = get("calm.used.$date")?.toIntOrNull() ?: 0
    /** Practice activity [row] starred on weekday [day] of the week starting [monday]. */
    fun practisedOn(monday: String, row: Int, day: Int): Boolean =
        if (monday == week) (row to day) in weekDone
        else get("week.done.$monday")?.split(",")?.any { it == "$row:$day" } == true

    /** Making the calm-down plan: choose, an idea of your own, your body's signs, saved. */
    val calmProgress: Float get() = if (calmSaved) 1f else listOf(calmChosen.isNotEmpty(), calmIdea.isNotBlank(), calmBody.isNotBlank()).count { it } / 4f

    /** Stars this week against the stars planned (or a star a day for each activity without a plan). */
    val practiceTarget: Int get() = plan?.practice?.sumOf { it.days.size }?.takeIf { it > 0 } ?: (activities.size * 7)
    val practiceProgress: Float get() = if (practiceTarget == 0) 0f else (starsThisWeek.toFloat() / practiceTarget).coerceAtMost(1f)

    /** This week, Monday to Sunday, as yyyy-mm-dd. */
    val weekDates: List<String> get() = (0..6).map { Dates.add(week, it) }

    /** This week so far, in words, for the family to send to Simone before a session. */
    fun progressSummary(): String = buildString {
        val dates = weekDates.filter { it <= today }
        val p = plan
        appendLine("Follow Me: progress for the week of ${Dates.show(week)}")
        val routine = dates.mapNotNull { d ->
            val n = ints(get("routine.done.$d")).size
            if (n == 0) null else Plan.DAY_SHORT[Dates.weekday(d)] + (if (n == routineSteps.size) " done" else " $n of ${routineSteps.size} steps")
        }
        append("Morning routine: ").append(if (routine.isEmpty()) "not yet this week" else routine.joinToString(", "))
        p?.routine?.let { append(" (planned ${Plan.showDays(it.days).lowercase()})") }
        appendLine()
        val names = weekNames.withIndex().filter { it.value.isNotBlank() }
        if (names.isNotEmpty()) {
            appendLine("Practice:")
            for ((r, name) in names) {
                val done = weekDone.filter { it.first == r }.map { it.second }.sorted()
                append("· $name: ").append(if (done.isEmpty()) "not yet" else done.joinToString(", ") { Plan.DAY_SHORT[it] })
                val planned = p?.practice?.getOrNull(r)?.days.orEmpty()
                if (planned.isNotEmpty()) append(" (${done.count { it in planned }} of ${planned.size} planned days)")
                appendLine()
            }
        }
        val calm = dates.sumOf { get("calm.used.$it")?.toIntOrNull() ?: 0 }
        if (calmSaved || calm > 0) appendLine("Calm-down plan: used $calm ${if (calm == 1) "time" else "times"}")
        val tricky = dates.mapNotNull { d -> get("routine.tricky.$d")?.takeIf { it.isNotBlank() }?.let { "$it (${Plan.DAY_SHORT[Dates.weekday(d)]})" } }
        if (tricky.isNotEmpty()) appendLine("Tricky: " + tricky.joinToString("; "))
        if (weekProud.isNotBlank()) appendLine("Proud of: $weekProud")
    }.trimEnd()

    // ── Simone's planner: her families and templates, on her phone only, in the
    //    Planner's encrypted vault (see vault.js); open only after Face ID. ──
    private val FAMILY = "planner.family."
    fun families(): List<Family> = p.vaultKeys(FAMILY)
        .mapNotNull { k -> p.vaultGet(k)?.let { Family.parse(k.removePrefix(FAMILY), it) } }
        .sortedBy { it.name.lowercase() }
    fun saveFamily(f: Family) = p.vaultPut(FAMILY + f.id, f.toJson())
    fun deleteFamily(id: String) = p.vaultPut(FAMILY + id, null)
    fun newFamilyId(): String = p.now().toString(36)
    /** Her template for a service (or her own starting point without one): hers if she saved one, else the built-in. */
    fun template(service: String?): Plan = p.vaultGet("planner.template.${service ?: "none"}")?.let { Plan.parse(it) } ?: Templates.builtIn(service)
    fun saveTemplate(service: String?, plan: Plan) = p.vaultPut("planner.template.${service ?: "none"}", plan.copy(note = null, made = "").toJson())

    /** Planner entries saved before the lock (in the open record), to move into the vault at setup. */
    fun plannerSeedJson(): String = record.entries.filter { it.key.startsWith("planner.") && it.value.value != null }
        .entries.joinToString(",", "{", "}") { (k, v) -> Family.q(k) + ":" + Family.q(v.value!!) }
    /** Once they're in the vault, take them out of the open record. */
    fun clearOpenPlanner() = record.entries.keys.filter { it.startsWith("planner.") }.forEach { put(it, null) }

    // ── Install ──
    /** Chose to use the app in the browser instead of adding it to the Home Screen. */
    var installSkipped by mutableStateOf(false)
        private set
    fun skipInstall() { installSkipped = true; put("install.skipped", "1") }

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
        installSkipped = get("install.skipped") == "1"
        plan = get("plan")?.let { Plan.parse(it) }
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
