package au.com.followmetherapy.app

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * A family's plan from Simone: which activities, on which days, at what time.
 *
 * It travels inside a link (…/#plan=<code>): the plan is in the part of the
 * address after #, which browsers never send to a server, so it goes from
 * Simone's phone to the family's without passing through anyone else.
 * Days are 0 = Monday … 6 = Sunday; times are "HH:mm" (24 hours).
 */
data class PlanItem(val name: String, val days: Set<Int>, val time: String? = null)

data class Plan(
    /** Which of Simone's services this plan belongs to (5 Steps Forward …), if any. */
    val service: String? = null,
    /** A note from Simone to the family. */
    val note: String? = null,
    val routine: PlanItem? = null,
    /** The calm-down plan is part of this plan (used when needed, no days). */
    val calm: Boolean = false,
    /** Practice activities (up to four), shown with a star a day. */
    val practice: List<PlanItem> = emptyList(),
    /** The day Simone made it, yyyy-mm-dd. */
    val made: String = "",
) {
    fun routineOn(day: Int) = routine != null && day in routine.days
    fun practiceOn(day: Int) = practice.withIndex().filter { day in it.value.days }

    fun toJson(): String = buildString {
        append("{\"v\":1")
        service?.let { append(",\"s\":"); str(it) }
        note?.let { append(",\"n\":"); str(it) }
        routine?.let { append(",\"r\":"); item(it) }
        if (calm) append(",\"c\":1")
        if (practice.isNotEmpty()) { append(",\"p\":["); practice.forEachIndexed { i, p -> if (i > 0) append(','); item(p) }; append(']') }
        append(",\"m\":"); str(made)
        append('}')
    }

    private fun StringBuilder.item(p: PlanItem) {
        append("{\"n\":"); str(p.name)
        append(",\"d\":[").append(p.days.sorted().joinToString(",")).append(']')
        p.time?.let { append(",\"t\":"); str(it) }
        append('}')
    }

    private fun StringBuilder.str(s: String) {
        append('"')
        for (c in s) when (c) {
            '"' -> append("\\\""); '\\' -> append("\\\\"); '\n' -> append("\\n"); '\r' -> append("\\r"); '\t' -> append("\\t")
            else -> if (c < ' ') append("\\u").append(c.code.toString(16).padStart(4, '0')) else append(c)
        }
        append('"')
    }

    /** The link a family opens: the app's address with the plan after #plan=. */
    @OptIn(ExperimentalEncodingApi::class)
    fun link(appUrl: String): String = appUrl + "#plan=" + B64.encode(toJson().encodeToByteArray())

    companion object {
        @OptIn(ExperimentalEncodingApi::class)
        private val B64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

        fun parse(json: String): Plan? {
            val root = (try { Json(json).value() } catch (e: Throwable) { null }) as? Map<*, *> ?: return null
            if ((root["v"] as? Long)?.toInt() != 1) return null
            fun item(o: Any?): PlanItem? {
                val m = o as? Map<*, *> ?: return null
                val name = m["n"] as? String ?: return null
                val days = (m["d"] as? List<*>)?.mapNotNull { (it as? Long)?.toInt()?.takeIf { d -> d in 0..6 } }?.toSet() ?: emptySet()
                return PlanItem(name, days, (m["t"] as? String)?.let { normaliseTime(it) })
            }
            return Plan(
                service = root["s"] as? String,
                note = root["n"] as? String,
                routine = item(root["r"]),
                calm = root["c"] == 1L,
                practice = (root["p"] as? List<*>)?.mapNotNull { item(it) }?.take(4) ?: emptyList(),
                made = root["m"] as? String ?: "",
            )
        }

        /** A plan from a link, or from text holding one (pasted from a message). */
        @OptIn(ExperimentalEncodingApi::class)
        fun fromLink(text: String): Plan? {
            val at = text.indexOf("plan=")
            if (at < 0) return null
            val code = text.substring(at + 5).takeWhile { it.isLetterOrDigit() || it == '-' || it == '_' || it == '=' }
            val json = try { B64.decode(code.trimEnd('=')).decodeToString() } catch (e: Throwable) { return null }
            return parse(json)
        }

        /** "7:30", "7.30am", "4pm", "16:00" → "07:30" / "16:00"; null if it isn't a time. */
        fun normaliseTime(text: String): String? {
            val m = Regex("""^\s*(\d{1,2})(?:[:.](\d{2}))?\s*([aApP])?\.?\s*[mM]?\.?\s*$""").find(text) ?: return null
            var h = m.groupValues[1].toInt()
            val min = m.groupValues[2].ifEmpty { "0" }.toInt()
            val ampm = m.groupValues[3].lowercase()
            if (ampm == "p" && h < 12) h += 12
            if (ampm == "a" && h == 12) h = 0
            if (h !in 0..23 || min !in 0..59) return null
            return "${h.toString().padStart(2, '0')}:${min.toString().padStart(2, '0')}"
        }

        /** "07:30" → "7:30am", "16:00" → "4pm". */
        fun showTime(t: String): String {
            val h = t.substringBefore(':').toInt()
            val min = t.substringAfter(':')
            val h12 = if (h % 12 == 0) 12 else h % 12
            return "$h12${if (min == "00") "" else ":$min"}${if (h < 12) "am" else "pm"}"
        }

        val DAY_SHORT = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        /** "Mon, Wed, Fri", "Every day", "Weekdays". */
        fun showDays(days: Set<Int>): String = when (days) {
            (0..6).toSet() -> "Every day"
            (0..4).toSet() -> "Weekdays"
            setOf(5, 6) -> "Weekends"
            else -> days.sorted().joinToString(", ") { DAY_SHORT[it] }
        }

        fun showWhen(p: PlanItem): String = showDays(p.days) + (p.time?.let { " · " + showTime(it) } ?: "")
    }
}

/** Calendar dates as yyyy-mm-dd, with no library: days since 1970 (H. Hinnant's civil-date algorithm). */
internal object Dates {
    fun toDays(date: String): Long {
        val (y0, m, d) = date.split("-").map { it.toInt() }
        val y = if (m <= 2) y0 - 1 else y0
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val doy = (153 * (m + (if (m > 2) -3 else 9)) + 2) / 5 + d - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097L + doe - 719468
    }

    fun fromDays(days: Long): String {
        val z = days + 719468
        val era = (if (z >= 0) z else z - 146096) / 146097
        val doe = z - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = if (mp < 10) mp + 3 else mp - 9
        val y = yoe + era * 400 + (if (m <= 2) 1 else 0)
        return "$y-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
    }

    fun add(date: String, n: Int) = fromDays(toDays(date) + n)

    /** Monday = 0 … Sunday = 6 (1 January 1970 was a Thursday). */
    fun weekday(date: String) = (((toDays(date) + 3) % 7 + 7) % 7).toInt()

    private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    /** "Mon 26 Oct". */
    fun show(date: String): String {
        val (_, m, d) = date.split("-").map { it.toInt() }
        return "${Plan.DAY_SHORT[weekday(date)]} $d ${MONTHS[m - 1]}"
    }

    /** UTC time stamp for a calendar file: 20261030T014200Z. */
    fun utcStamp(ms: Long): String {
        val day = ms / 86_400_000L
        val sec = (ms - day * 86_400_000L) / 1000
        val date = fromDays(day).replace("-", "")
        fun two(n: Long) = n.toString().padStart(2, '0')
        return "${date}T${two(sec / 3600)}${two(sec / 60 % 60)}${two(sec % 60)}Z"
    }
}

/**
 * The plan's reminders as a calendar file (.ics): one weekly repeating event
 * for each timed activity, with an alert at its time. Adding it to the
 * phone's calendar gives real reminders with no server and no account.
 */
internal fun Plan.calendar(today: String, nowMs: Long, appUrl: String): String {
    val byDay = listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")
    val events = buildList {
        routine?.let { if (it.time != null && it.days.isNotEmpty()) add("routine" to it.copy(name = "Morning routine")) }
        practice.forEachIndexed { i, p -> if (p.time != null && p.days.isNotEmpty()) add("practice$i" to p) }
    }
    val lines = mutableListOf("BEGIN:VCALENDAR", "VERSION:2.0", "PRODID:-//Follow Me Therapy//Plan//EN", "CALSCALE:GREGORIAN", "METHOD:PUBLISH")
    val stamp = Dates.utcStamp(nowMs)
    val todayDay = Dates.weekday(today)
    for ((id, p) in events) {
        val first = (0..6).first { (todayDay + it) % 7 in p.days }
        val date = Dates.add(today, first).replace("-", "")
        val time = p.time!!.replace(":", "")
        lines += listOf(
            "BEGIN:VEVENT",
            "UID:fmt-$id-${made.ifEmpty { today }}@followmetherapy.com.au",
            "DTSTAMP:$stamp",
            "DTSTART:${date}T${time}00",
            "DURATION:PT15M",
            "RRULE:FREQ=WEEKLY;BYDAY=" + p.days.sorted().joinToString(",") { byDay[it] },
            "SUMMARY:" + ics("${p.name} (Follow Me)"),
            "DESCRIPTION:" + ics("Open Follow Me: $appUrl"),
            "BEGIN:VALARM", "ACTION:DISPLAY", "DESCRIPTION:" + ics(p.name), "TRIGGER:PT0M", "END:VALARM",
            "END:VEVENT",
        )
    }
    lines += "END:VCALENDAR"
    return lines.joinToString("\r\n") { fold(it) } + "\r\n"
}

/** Whether any activity in the plan has a time (so there's something to remind). */
internal val Plan.hasReminders: Boolean get() = (routine?.time != null && routine.days.isNotEmpty()) || practice.any { it.time != null && it.days.isNotEmpty() }

private fun ics(s: String) = s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n")

/** Calendar lines are at most 75 characters; longer ones continue on the next line after a space. */
private fun fold(line: String): String = if (line.length <= 74) line else line.chunked(74).joinToString("\r\n ")
