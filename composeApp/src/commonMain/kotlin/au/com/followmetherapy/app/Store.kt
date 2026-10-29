package au.com.followmetherapy.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * What the app needs from the device. On the web it's the browser's local
 * storage and clock; Android and iOS implement the same interface later.
 * Everything stays on the device: nothing is sent anywhere.
 */
interface Platform {
    fun load(key: String): String?
    fun save(key: String, value: String?)
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
}

/** A platform that keeps nothing (previews and tests). */
object MemoryPlatform : Platform {
    private val m = mutableMapOf<String, String>()
    override fun load(key: String) = m[key]
    override fun save(key: String, value: String?) { if (value == null) m.remove(key) else m[key] = value }
    override fun today() = "2026-10-30"
    override fun weekday() = 4
    override fun hour() = 8
    override fun weekStart() = "2026-10-26"
}

private const val SEP = "\u001f"

private fun ints(s: String?): List<Int> = s?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList()

/**
 * The app's saved state: today's routine, the calm-down plan and this week's
 * practice. Each change is written straight to the device.
 */
class AppStore(private val p: Platform) {
    val today: String = p.today()
    val weekday: Int = p.weekday()
    val hour: Int = p.hour()
    private val week: String = p.weekStart()
    val build: String = p.build()

    // ── Morning routine ──
    val routineSteps = listOf("Toilet and wash hands", "Get dressed", "Eat breakfast", "Brush teeth", "Shoes on", "Bag on, out the door")
    val routineDone = mutableStateListOf<Int>().apply { addAll(ints(p.load("routine.done.$today"))) }
    var routineTricky by mutableStateOf(p.load("routine.tricky.$today") ?: "")
        private set

    fun toggleRoutineStep(i: Int, done: Boolean) {
        if (done && i !in routineDone) routineDone.add(i)
        if (!done) routineDone.remove(i)
        p.save("routine.done.$today", routineDone.sorted().joinToString(","))
    }
    fun setTricky(v: String) { routineTricky = v; p.save("routine.tricky.$today", v) }
    val routineComplete: Boolean get() = routineDone.size == routineSteps.size

    // ── Calm-down plan ──
    val calmChosen = mutableStateListOf<Int>().apply { addAll(ints(p.load("calm.chosen"))) }
    var calmIdea by mutableStateOf(p.load("calm.idea") ?: "")
        private set
    var calmBody by mutableStateOf(p.load("calm.body") ?: "")
        private set
    var calmSaved by mutableStateOf(p.load("calm.saved") == "1")
        private set

    fun toggleCalm(i: Int) {
        if (i in calmChosen) calmChosen.remove(i) else calmChosen.add(i)
        p.save("calm.chosen", calmChosen.joinToString(","))
    }
    fun setCalmIdea(v: String) { calmIdea = v; p.save("calm.idea", v) }
    fun setCalmBody(v: String) { calmBody = v; p.save("calm.body", v) }
    fun saveCalmPlan() { calmSaved = true; p.save("calm.saved", "1") }

    // ── Practice week ──
    val weekNames = mutableStateListOf<String>().apply {
        val saved = p.load("week.names")?.split(SEP) ?: emptyList()
        addAll((saved + List(4) { "" }).take(4))
    }
    val weekDone = mutableStateListOf<Pair<Int, Int>>().apply {
        p.load("week.done.$week")?.split(",")?.forEach { cell ->
            val parts = cell.split(":")
            val r = parts.getOrNull(0)?.toIntOrNull()
            val d = parts.getOrNull(1)?.toIntOrNull()
            if (r != null && d != null) add(r to d)
        }
    }
    var weekSetUp by mutableStateOf(p.load("week.setup") == "1")
        private set
    var weekProud by mutableStateOf(p.load("week.proud.$week") ?: "")
        private set

    fun renameActivity(i: Int, v: String) { weekNames[i] = v; p.save("week.names", weekNames.joinToString(SEP)) }
    fun finishWeekSetup() { weekSetUp = true; p.save("week.setup", "1") }
    fun togglePractice(r: Int, d: Int) {
        val k = r to d
        if (k in weekDone) weekDone.remove(k) else weekDone.add(k)
        p.save("week.done.$week", weekDone.joinToString(",") { "${it.first}:${it.second}" })
    }
    fun setProud(v: String) { weekProud = v; p.save("week.proud.$week", v) }
    val activities: List<Pair<Int, String>> get() = weekNames.withIndex().filter { it.value.isNotBlank() }.map { it.index to it.value }
    val starsThisWeek: Int get() = weekDone.count { (r, _) -> weekNames.getOrNull(r)?.isNotBlank() == true }

    /** Clears everything this app has saved on this device. */
    fun forgetEverything() {
        listOf("routine.done.$today", "routine.tricky.$today", "calm.chosen", "calm.idea", "calm.body", "calm.saved",
            "week.names", "week.done.$week", "week.setup", "week.proud.$week").forEach { p.save(it, null) }
        routineDone.clear(); routineTricky = ""
        calmChosen.clear(); calmIdea = ""; calmBody = ""; calmSaved = false
        for (i in weekNames.indices) weekNames[i] = ""
        weekDone.clear(); weekSetUp = false; weekProud = ""
    }
}
