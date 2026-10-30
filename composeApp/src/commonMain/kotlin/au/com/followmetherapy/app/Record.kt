package au.com.followmetherapy.app

/**
 * Everything the app saves, as ONE versioned record: each entry has a value
 * and the time it last changed (ms since 1970, the device's clock). A
 * deleted entry keeps its time with no value, so a merge knows it was cleared.
 *
 * The same JSON is the saved data, the backup file and (one day) what a sync
 * service would exchange, so moving to a backend means sending this record,
 * not rewriting the app:
 *
 *   {"app":"follow-me-therapy","schema":1,"saved":1793315000000,
 *    "entries":{"calm.idea":{"v":"Hug my dog","at":1793314000000}, ...}}
 *
 * Merging two records keeps, for each entry, whichever changed last.
 */
class Record(val entries: MutableMap<String, Entry> = mutableMapOf()) {
    data class Entry(val value: String?, val at: Long)

    fun get(key: String): String? = entries[key]?.value
    fun put(key: String, value: String?, at: Long) { entries[key] = Entry(value, at) }

    /** Takes every entry from [other] that changed after ours. Returns how many changed. */
    fun merge(other: Record): Int {
        var n = 0
        for ((k, e) in other.entries) {
            val mine = entries[k]
            if (mine == null || e.at > mine.at) { entries[k] = e; n++ }
        }
        return n
    }

    /** Entries with a value (not cleared ones). */
    val size: Int get() = entries.values.count { it.value != null }

    fun toJson(saved: Long): String = buildString {
        append("{\"app\":\"").append(APP).append("\",\"schema\":").append(SCHEMA).append(",\"saved\":").append(saved).append(",\"entries\":{")
        entries.entries.sortedBy { it.key }.forEachIndexed { i, (k, e) ->
            if (i > 0) append(',')
            str(k); append(":{\"v\":")
            if (e.value == null) append("null") else str(e.value)
            append(",\"at\":").append(e.at).append('}')
        }
        append("}}")
    }

    private fun StringBuilder.str(s: String) {
        append('"')
        for (c in s) when (c) {
            '"' -> append("\\\""); '\\' -> append("\\\\"); '\n' -> append("\\n"); '\r' -> append("\\r"); '\t' -> append("\\t")
            else -> if (c < ' ') append("\\u").append(c.code.toString(16).padStart(4, '0')) else append(c)
        }
        append('"')
    }

    companion object {
        const val APP = "follow-me-therapy"
        /** Bump when the shape of entries changes, and migrate in [parse]. */
        const val SCHEMA = 1

        /** Reads a record (saved data or a backup file). Null if it isn't one of ours. */
        fun parse(text: String): Record? {
            val parsed: Any? = try { Json(text).value() } catch (e: Throwable) { null }
            val root = parsed as? Map<*, *> ?: return null
            if (root["app"] != APP) return null
            val schema = (root["schema"] as? Long)?.toInt() ?: return null
            if (schema > SCHEMA) return null // made by a newer app
            val raw = root["entries"] as? Map<*, *> ?: return null
            val r = Record()
            for ((k, v) in raw) {
                val e = v as? Map<*, *> ?: continue
                r.entries[k as String] = Entry(e["v"] as? String, e["at"] as? Long ?: 0L)
            }
            return r
        }
    }
}

/** A small JSON reader (objects, arrays, strings, whole numbers, true/false/null): enough for records, with no library. */
internal class Json(private val s: String) {
    private var i = 0
    fun value(): Any? {
        ws()
        val c = s[i]
        return when {
            c == '{' -> obj()
            c == '[' -> arr()
            c == '"' -> str()
            c == 'n' -> word("null", null)
            c == 't' -> word("true", true)
            c == 'f' -> word("false", false)
            else -> num()
        }
    }
    private fun ws() { while (i < s.length && s[i].isWhitespace()) i++ }
    private fun expect(c: Char) { ws(); require(s[i] == c) { "expected $c" }; i++ }
    private fun word(w: String, v: Any?): Any? { require(s.startsWith(w, i)); i += w.length; return v }
    private fun obj(): Map<String, Any?> {
        val m = LinkedHashMap<String, Any?>()
        expect('{'); ws()
        if (s[i] == '}') { i++; return m }
        while (true) {
            ws(); val k = str(); expect(':'); m[k] = value(); ws()
            if (s[i] == ',') { i++; continue }
            expect('}'); return m
        }
    }
    private fun arr(): List<Any?> {
        val l = ArrayList<Any?>()
        expect('['); ws()
        if (s[i] == ']') { i++; return l }
        while (true) {
            l.add(value()); ws()
            if (s[i] == ',') { i++; continue }
            expect(']'); return l
        }
    }
    private fun str(): String {
        expect('"')
        val b = StringBuilder()
        while (true) {
            val c = s[i++]
            when (c) {
                '"' -> return b.toString()
                '\\' -> {
                    val e = s[i++]
                    when (e) {
                        'n' -> b.append('\n'); 'r' -> b.append('\r'); 't' -> b.append('\t'); 'b' -> b.append('\b'); 'f' -> b.append('\u000C')
                        'u' -> { b.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4 }
                        else -> b.append(e)
                    }
                }
                else -> b.append(c)
            }
        }
    }
    private fun num(): Any {
        val start = i
        while (i < s.length && (s[i].isDigit() || s[i] in "-+.eE")) i++
        val t = s.substring(start, i)
        return t.toLongOrNull() ?: t.toDouble()
    }
}
