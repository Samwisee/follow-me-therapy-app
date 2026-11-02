package au.com.followmetherapy.design.components

import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.Role
import au.com.followmetherapy.design.icons.FmtIcon
import au.com.followmetherapy.design.icons.FmtIcons
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import au.com.followmetherapy.design.theme.FmtTheme
import au.com.followmetherapy.design.tokens.FmtBorder
import au.com.followmetherapy.design.tokens.FmtCalendarTokens
import au.com.followmetherapy.design.tokens.FmtRadius
import au.com.followmetherapy.design.tokens.FmtSize
import au.com.followmetherapy.design.tokens.FmtSpace

/** One day of an activity in a [FmtWeekCalendar]. */
enum class CalendarDay(val words: String) { None("not planned"), Planned("planned"), Partial("partly done"), Done("done") }

/**
 * An activity's week: its name and Monday-to-Sunday days. With [onToggle]
 * the row's stars can be tapped (practice); otherwise it's read-only. With
 * [onOpen] the name is a link (with a chevron) to the activity.
 */
data class CalendarRow(val label: String, val tone: Tone, val days: List<CalendarDay>, val onToggle: ((Int) -> Unit)? = null, val onOpen: (() -> Unit)? = null)

private val FULL_DAYS = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
private val LETTERS = listOf("M", "T", "W", "T", "F", "S", "S")

/**
 * A week of an activity plan (the shared WeekCalendar), as the practice
 * week's stars: the days across with their dates, today's column on lilac;
 * each activity's name, then its row of stars between dashed rules: not
 * planned (a dot), planned (a purple outline), partly done (a pale star),
 * done (gold), the stars icon-sized to keep the week compact. Each row's
 * name sits small above its stars, one dashed rule under the row; a row that
 * links to its activity is one tap target (a chevron after the name). A row with onToggle (44dp stars) is tapped like the practice week, and
 * adding a star bursts. Each day is read out: "Monday, Morning routine: done".
 */
@Composable
fun FmtWeekCalendar(rows: List<CalendarRow>, dates: List<String>, today: Int?, modifier: Modifier = Modifier) {
    val t = FmtCalendarTokens
    val cellShape = RoundedCornerShape(FmtRadius.tile)
    val todayBg = { i: Int -> if (i == today) t.today else t.today.copy(alpha = 0f) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(FmtSpace.s1)) {
        Row(Modifier.fillMaxWidth().clearAndSetSemantics { }) {
            LETTERS.forEachIndexed { i, d ->
                Column(
                    Modifier.weight(1f).background(todayBg(i), cellShape).padding(vertical = FmtSpace.s1),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    FmtText(d, FmtTheme.type.groupTitle.copy(fontSize = FmtTheme.metrics.textBase), color = t.ink, align = TextAlign.Center)
                    FmtText(dates.getOrElse(i) { "" }, FmtTheme.type.small.copy(fontSize = FmtTheme.metrics.textXs), color = t.dayInk, align = TextAlign.Center)
                }
            }
        }
        rows.forEach { r ->
            val open = r.onOpen
            val toggle = r.onToggle
            // Compact: the name small above its stars, one dashed rule under the row. A row
            // that links is one tap target, name and stars (each day still read out).
            val summary = r.days.mapIndexed { i, d -> "${FULL_DAYS[i]}: ${d.words}" }.joinToString(", ")
            Column(
                Modifier.fillMaxWidth().dashedBottom(t.rule).padding(top = FmtSpace.s2)
                    .then(
                        if (open != null && toggle == null) Modifier.clickable(role = Role.Button, onClickLabel = "Open", onClick = open)
                            .clearAndSetSemantics { contentDescription = "${r.label}. $summary"; role = Role.Button }
                        else Modifier,
                    ),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FmtSpace.s1)) {
                    FmtText(r.label, FmtTheme.type.small.copy(fontWeight = FontWeight.SemiBold), Modifier.weight(1f, fill = false), color = t.ink)
                    if (open != null) FmtIcon(FmtIcons.chevron, t.planned, FmtSize.iconSm)
                }
                Row(Modifier.fillMaxWidth()) {
                    r.days.forEachIndexed { i, day ->
                        val words = "${FULL_DAYS[i]}, ${r.label}"
                        Box(
                            Modifier.weight(1f).background(todayBg(i))
                                .then(if (toggle == null) Modifier.heightIn(min = FmtSize.icon + FmtSpace.s2).semantics(mergeDescendants = true) { contentDescription = "$words: ${day.words}" } else Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                toggle != null -> FmtStarToggle(day == CalendarDay.Done, words, iconSize = FmtSize.icon) { toggle(i) }
                                day == CalendarDay.None -> Box(Modifier.size(FmtSpace.s2).background(t.none, CircleShape))
                                else -> FmtStarMark(if (day == CalendarDay.Done) StarState.Done else if (day == CalendarDay.Partial) StarState.Partial else StarState.Planned, 0, Modifier.size(FmtSize.icon + FmtSpace.s2), iconSize = FmtSize.icon)
                            }
                        }
                    }
                }
            }
        }
    }
}

