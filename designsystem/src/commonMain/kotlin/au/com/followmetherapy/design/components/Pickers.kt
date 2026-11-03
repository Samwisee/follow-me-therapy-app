package au.com.followmetherapy.design.components

import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import au.com.followmetherapy.design.tokens.FmtMotion
import kotlin.math.abs
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
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.height
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
    // Not the platform's narrow dialog width: a week of 44dp days has to fit across a small phone.
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.padding(horizontal = FmtSpace.s2).widthIn(max = FmtSize.target * 7 + m.cardPad * 2 + FmtBorder.outline * 2)
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
 * the hour or the minutes), an am / pm switch, and a dial of the 12 hours, then
 * the minutes in fives, each a 44dp target; the chosen one a purple circle on
 * the hand, which turns the short way round to each new choice (not with
 * reduced motion). [time] and [onDone] are 24-hour, "07:30".
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
            AmPm(pm) { pm = it }
        }
        // The dial: numbers round a circle, the hand from the centre to the chosen one.
        val chosen = if (minutes) minute / 5 else hour
        val reduced = LocalFmtReducedMotion.current
        val angle = remember { Animatable(chosen * 30f) }
        LaunchedEffect(chosen) {
            // The short way round: 11 → 1 goes past 12, not back through 6.
            val turn = ((chosen * 30f - angle.value) % 360f + 540f) % 360f - 180f
            if (reduced) angle.snapTo(angle.value + turn)
            else angle.animateTo(angle.value + turn, tween(FmtMotion.durBase, easing = FmtMotion.easeStandard))
        }
        BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1f).widthIn(max = FmtSize.target * 6).align(Alignment.CenterHorizontally)) {
            val side = maxWidth
            val r = side * 0.4f
            Canvas(Modifier.size(side)) {
                drawCircle(t.dial)
                val a = (angle.value - 90) * PI / 180
                val end = center + Offset((cos(a) * r.toPx()).toFloat(), (sin(a) * r.toPx()).toFloat())
                drawLine(t.selected, center, end, FmtBorder.outline.toPx())
                drawCircle(t.selected, FmtSpace.s1.toPx(), center)
                drawCircle(t.selected, FmtSize.target.toPx() / 2, end)
            }
            for (i in 0 until 12) {
                val a = (i * 30 - 90) * PI / 180
                val on = i == chosen
                // The number is in paper while the hand's circle is over it.
                val under = abs(((angle.value - i * 30f) % 360f + 540f) % 360f - 180f) < 15f
                val words = if (minutes) "${i * 5} minutes" else "${if (i == 0) 12 else i} o'clock"
                Box(
                    Modifier.offset(side / 2 + r * cos(a).toFloat() - FmtSize.target / 2, side / 2 + r * sin(a).toFloat() - FmtSize.target / 2)
                        .size(FmtSize.target)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClickLabel = "Choose") { if (minutes) minute = i * 5 else { hour = i; minutes = true } }
                        .semantics { contentDescription = words; selected = on },
                    contentAlignment = Alignment.Center,
                ) {
                    FmtText(if (minutes) (i * 5).toString().padStart(2, '0') else if (i == 0) "12" else "$i",
                        FmtTheme.type.body.copy(fontWeight = FontWeight.SemiBold), color = if (under) t.onSelected else t.ink)
                }
            }
        }
    }
}

/**
 * am over pm, one switch: two equal pill segments in a purple-line pill, the
 * chosen one purple with paper words (and read out as selected).
 */
@Composable
private fun AmPm(pm: Boolean, onPm: (Boolean) -> Unit) {
    val t = FmtPickerTokens
    val pill = RoundedCornerShape(FmtRadius.pill)
    Column(Modifier.border(FmtBorder.outline, t.line, pill).padding(FmtBorder.outline).selectableGroup()) {
        listOf(false to "am", true to "pm").forEach { (isPm, word) ->
            val on = pm == isPm
            Box(
                Modifier.size(FmtSize.target * 1.4f, FmtSize.target)
                    .background(if (on) t.selected else t.fill, pill)
                    .clip(pill)
                    .selectable(on, role = Role.RadioButton) { onPm(isPm) },
                contentAlignment = Alignment.Center,
            ) { FmtText(word, FmtTheme.type.body.copy(fontWeight = FontWeight.SemiBold), color = if (on) t.onSelected else t.ink) }
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

private val DAY_SHORT = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

/** Days of the week (0 = Monday) as people say them: "Weekdays", "Mon, Wed, Fri". */
fun fmtShowDays(days: Set<Int>): String = when (days) {
    emptySet<Int>() -> "No days"
    (0..6).toSet() -> "Every day"
    (0..4).toSet() -> "Weekdays"
    setOf(5, 6) -> "Weekends"
    else -> days.sorted().joinToString(", ") { DAY_SHORT[it] }
}

/**
 * Our calendar's week (the shared Picker's DaysPicker): which days of the week,
 * Monday first, as the calendar's days: each a 44dp circle, the chosen ones
 * purple with paper words, as many as you like; Weekdays and Every day a tap
 * away. Days are 0 (Monday) to 6.
 */
@Composable
fun FmtDaysPicker(days: Set<Int>, title: String, onDone: (Set<Int>) -> Unit, onCancel: () -> Unit) {
    val t = FmtPickerTokens
    var chosen by remember { mutableStateOf(days) }
    PickerCard(title, onCancel, { onDone(chosen) }) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                    Box(Modifier.weight(1f).height(FmtSize.target).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
                        FmtText(it, FmtTheme.type.small.copy(fontWeight = FontWeight.SemiBold), color = t.soft)
                    }
                }
            }
            Row(Modifier.fillMaxWidth()) {
                for (d in 0 until 7) {
                    val on = d in chosen
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Box(
                        Modifier.size(FmtSize.target)
                            .background(if (on) t.selected else t.fill.copy(alpha = 0f), CircleShape)
                            .border(FmtBorder.outline, if (on) t.selected else t.line, CircleShape)
                            .clip(CircleShape)
                            .toggleable(on, role = Role.Checkbox) { chosen = if (it) chosen + d else chosen - d }
                            .semantics { contentDescription = WEEKDAYS[d] },
                        contentAlignment = Alignment.Center,
                    ) { FmtText(DAY_SHORT[d].take(2), FmtTheme.type.small.copy(fontWeight = FontWeight.SemiBold), color = if (on) t.onSelected else t.ink) } }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(FmtSpace.s2), modifier = Modifier.align(Alignment.CenterHorizontally)) {
            FmtToggleChip("Weekdays", chosen == (0..4).toSet(), { chosen = (0..4).toSet() })
            FmtToggleChip("Every day", chosen == (0..6).toSet(), { chosen = (0..6).toSet() })
        }
    }
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
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                    Box(Modifier.weight(1f).height(FmtSize.target).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
                        FmtText(it, FmtTheme.type.small.copy(fontWeight = FontWeight.SemiBold), color = t.soft)
                    }
                }
            }
            for (week in 0 until (lead + days + 6) / 7) Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    val d = week * 7 + col - lead + 1
                    if (d < 1 || d > days) Spacer(Modifier.size(FmtSize.target)) else {
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
}
