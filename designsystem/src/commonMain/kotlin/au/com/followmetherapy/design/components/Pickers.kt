package au.com.followmetherapy.design.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import au.com.followmetherapy.design.icons.FmtIcon
import au.com.followmetherapy.design.icons.FmtIcons
import au.com.followmetherapy.design.theme.FmtTheme
import au.com.followmetherapy.design.tokens.FmtBorder
import au.com.followmetherapy.design.tokens.FmtPickerTokens
import au.com.followmetherapy.design.tokens.FmtRadius
import au.com.followmetherapy.design.tokens.FmtShadow
import au.com.followmetherapy.design.tokens.FmtSize
import au.com.followmetherapy.design.tokens.FmtSpace
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** "07:30" → "7:30am", "16:00" → "4pm": times as people say them. */
fun fmtShowTime(t: String): String {
    val h = t.substringBefore(':').toIntOrNull() ?: return t
    val m = t.substringAfter(':').toIntOrNull() ?: 0
    return "${if (h % 12 == 0) 12 else h % 12}${if (m == 0) "" else ":" + m.toString().padStart(2, '0')}${if (h < 12) "am" else "pm"}"
}

/** The picker's card (the shared Picker): paper, a purple-line outline, the title, then [content], Cancel and Done. */
@Composable
private fun PickerCard(title: String, onCancel: () -> Unit, onDone: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val t = FmtPickerTokens
    val m = FmtTheme.metrics
    val shape = RoundedCornerShape(FmtRadius.card)
    Dialog(onDismissRequest = onCancel) {
        Column(
            Modifier.widthIn(max = FmtSize.target * 7 + m.cardPad * 2 + FmtBorder.outline * 2)
                .fmtShadow(FmtShadow.overlay, shape).background(t.fill, shape).border(FmtBorder.outline, t.line, shape)
                .padding(m.cardPad),
            verticalArrangement = Arrangement.spacedBy(m.groupGap),
        ) {
            FmtText(title, FmtTheme.type.subheading.copy(fontSize = m.textMd, fontWeight = FontWeight.SemiBold), Modifier.semantics { heading() }, color = t.ink)
            content()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FmtSpace.s3, Alignment.End)) {
                FmtButton("Cancel", onCancel, kind = ButtonKind.Outline)
                FmtButton("Done", onDone)
            }
        }
    }
}

/**
 * Our clock (the shared Picker's TimePicker): the time large at the top (tap
 * the hour or the minutes), am / pm chips, and a dial of the 12 hours, then
 * the minutes in fives, each a 44dp target; the chosen one a purple circle on
 * the hand. [time] and [onDone] are 24-hour, "07:30".
 */
@Composable
fun FmtTimePicker(time: String, title: String, onDone: (String) -> Unit, onCancel: () -> Unit) {
    val t = FmtPickerTokens
    val h0 = time.substringBefore(':').toIntOrNull() ?: 7
    val m0 = time.substringAfter(':').toIntOrNull() ?: 30
    var hour by remember { mutableIntStateOf(h0 % 12) }
    var minute by remember { mutableIntStateOf(((m0 / 5f).roundToInt() * 5) % 60) }
    var pm by remember { mutableStateOf(h0 >= 12) }
    var minutes by remember { mutableStateOf(false) }
    val out = { "${(hour + if (pm) 12 else 0).toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}" }
    PickerCard(title, onCancel, { onDone(out()) }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FmtSpace.s1)) {
            val big = FmtTheme.type.title.copy(fontSize = FmtTheme.metrics.text3xl)
            TimePart(if (hour == 0) "12" else "$hour", !minutes, "Hour, ${if (hour == 0) 12 else hour}") { minutes = false }
            FmtText(":", big, color = t.ink)
            TimePart(minute.toString().padStart(2, '0'), minutes, "Minutes, $minute") { minutes = true }
            Spacer(Modifier.weight(1f))
            FmtToggleChip("am", !pm, { pm = false })
            FmtToggleChip("pm", pm, { pm = true })
        }
        // The dial: numbers round a circle, the hand from the centre to the chosen one.
        val chosen = if (minutes) minute / 5 else hour
        BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1f).widthIn(max = FmtSize.target * 6).align(Alignment.CenterHorizontally)) {
            val side = maxWidth
            val r = side * 0.4f
            Canvas(Modifier.size(side)) {
                drawCircle(t.dial)
                val a = (chosen * 30 - 90) * PI / 180
                val end = center + Offset((cos(a) * r.toPx()).toFloat(), (sin(a) * r.toPx()).toFloat())
                drawLine(t.selected, center, end, FmtBorder.outline.toPx())
                drawCircle(t.selected, FmtSpace.s1.toPx(), center)
            }
            for (i in 0 until 12) {
                val a = (i * 30 - 90) * PI / 180
                val on = i == chosen
                val words = if (minutes) "${i * 5} minutes" else "${if (i == 0) 12 else i} o'clock"
                Box(
                    Modifier.offset(side / 2 + r * cos(a).toFloat() - FmtSize.target / 2, side / 2 + r * sin(a).toFloat() - FmtSize.target / 2)
                        .size(FmtSize.target)
                        .background(if (on) t.selected else t.dial.copy(alpha = 0f), CircleShape)
                        .clickable(role = Role.Button, onClickLabel = "Choose") { if (minutes) minute = i * 5 else { hour = i; minutes = true } }
                        .semantics { contentDescription = words; selected = on },
                    contentAlignment = Alignment.Center,
                ) {
                    FmtText(if (minutes) (i * 5).toString().padStart(2, '0') else if (i == 0) "12" else "$i",
                        FmtTheme.type.body.copy(fontWeight = FontWeight.SemiBold), color = if (on) t.onSelected else t.ink)
                }
            }
        }
    }
}

@Composable
private fun TimePart(text: String, on: Boolean, words: String, onClick: () -> Unit) {
    val t = FmtPickerTokens
    Box(
        Modifier.size(FmtSize.target * 1.6f, FmtSize.target * 1.4f)
            .background(if (on) t.dial else t.dial.copy(alpha = 0f), RoundedCornerShape(FmtRadius.sm))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = words; selected = on },
        contentAlignment = Alignment.Center,
    ) { FmtText(text, FmtTheme.type.title.copy(fontSize = FmtTheme.metrics.text3xl), color = t.ink, align = TextAlign.Center) }
}

private val MONTHS = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
private val WEEKDAYS = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

/** Days since 1970-01-01 for a civil date (H. Hinnant's algorithm), and back. */
private fun daysFrom(y0: Int, m: Int, d: Int): Int {
    val y = if (m <= 2) y0 - 1 else y0
    val era = (if (y >= 0) y else y - 399) / 400
    val yoe = y - era * 400
    val doy = (153 * (m + (if (m > 2) -3 else 9)) + 2) / 5 + d - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097 + doe - 719468
}
private fun monthDays(y: Int, m: Int) = daysFrom(if (m == 12) y + 1 else y, if (m == 12) 1 else m + 1, 1) - daysFrom(y, m, 1)
private fun iso(y: Int, m: Int, d: Int) = "$y-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"

/**
 * Our calendar (the shared Picker's DatePicker): the month and year with ‹ ›,
 * Monday first, each day a 44dp target; today ringed in purple, the chosen
 * day a purple circle. Dates are yyyy-mm-dd.
 */
@Composable
fun FmtDatePicker(date: String, today: String, title: String, onDone: (String) -> Unit, onCancel: () -> Unit) {
    val t = FmtPickerTokens
    var chosen by remember { mutableStateOf(date) }
    val parts = date.split('-').mapNotNull { it.toIntOrNull() }
    var year by remember { mutableIntStateOf(parts.getOrElse(0) { 2026 }) }
    var month by remember { mutableIntStateOf(parts.getOrElse(1) { 1 }) }
    PickerCard(title, onCancel, { onDone(chosen) }) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FmtIconButton(FmtIcons.back, "Last month", { if (month == 1) { month = 12; year-- } else month-- })
            FmtText("${MONTHS[month - 1]} $year", FmtTheme.type.body.copy(fontWeight = FontWeight.SemiBold),
                Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite }, color = t.ink, align = TextAlign.Center)
            FmtIconButton(FmtIcons.chevron, "Next month", { if (month == 12) { month = 1; year++ } else month++ })
        }
        val lead = ((daysFrom(year, month, 1) % 7 + 7 + 3) % 7) // 1970-01-01 was a Thursday; Monday = 0
        val days = monthDays(year, month)
        Column(Modifier.align(Alignment.CenterHorizontally)) {
            Row {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                    Box(Modifier.size(FmtSize.target).semantics { contentDescription = "" }, contentAlignment = Alignment.Center) {
                        FmtText(it, FmtTheme.type.small.copy(fontWeight = FontWeight.SemiBold), color = t.soft)
                    }
                }
            }
            for (week in 0 until (lead + days + 6) / 7) Row {
                for (col in 0 until 7) {
                    val d = week * 7 + col - lead + 1
                    if (d < 1 || d > days) { Spacer(Modifier.size(FmtSize.target)); continue }
                    val id = iso(year, month, d)
                    val on = id == chosen
                    val isToday = id == today
                    Box(
                        Modifier.size(FmtSize.target)
                            .background(if (on) t.selected else t.fill.copy(alpha = 0f), CircleShape)
                            .border(FmtBorder.outline, if (isToday && !on) t.today else t.today.copy(alpha = 0f), CircleShape)
                            .clickable(role = Role.Button, onClickLabel = "Choose") { chosen = id }
                            .semantics { contentDescription = "${WEEKDAYS[col]} $d ${MONTHS[month - 1]}${if (isToday) ", today" else ""}"; selected = on },
                        contentAlignment = Alignment.Center,
                    ) { FmtText("$d", FmtTheme.type.body, color = if (on) t.onSelected else t.ink) }
                }
            }
        }
    }
}
