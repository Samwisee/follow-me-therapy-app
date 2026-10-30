package au.com.followmetherapy.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import au.com.followmetherapy.design.icons.FmtIcon
import au.com.followmetherapy.design.icons.FmtIcons
import au.com.followmetherapy.design.theme.FmtTheme
import au.com.followmetherapy.design.tokens.FmtBorder
import au.com.followmetherapy.design.tokens.FmtCalendarTokens
import au.com.followmetherapy.design.tokens.FmtRadius
import au.com.followmetherapy.design.tokens.FmtSize
import au.com.followmetherapy.design.tokens.FmtSpace

/** One day of an activity in a [FmtWeekCalendar]. */
enum class CalendarDay(val words: String) { None("not planned"), Planned("planned"), Partial("partly done"), Done("done") }

/** An activity's week: its name, tone and Monday-to-Sunday days. */
data class CalendarRow(val label: String, val tone: Tone, val days: List<CalendarDay>)

private val FULL_DAYS = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
private val SHORT_DAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

/**
 * A week of an activity plan (the shared WeekCalendar): the days across with
 * their dates, today's column on lilac; each activity's days beneath it,
 * planned (a ring in its line colour), partly done (its tint, a solid ring)
 * or done (teal with a tick). Each day is read out: "Monday, Morning routine: done".
 */
@Composable
fun FmtWeekCalendar(rows: List<CalendarRow>, dates: List<String>, today: Int?, modifier: Modifier = Modifier) {
    val t = FmtCalendarTokens
    val cellShape = RoundedCornerShape(FmtRadius.tile)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(FmtSpace.s3)) {
        Row(Modifier.fillMaxWidth().clearAndSetSemantics { }) {
            SHORT_DAYS.forEachIndexed { i, d ->
                Column(
                    Modifier.weight(1f).background(if (i == today) t.today else t.today.copy(alpha = 0f), cellShape).padding(vertical = FmtSpace.s1),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    FmtText(d, FmtTheme.type.small.copy(fontSize = FmtTheme.metrics.textXs), color = t.dayInk, align = TextAlign.Center)
                    FmtText(dates.getOrElse(i) { "" }, FmtTheme.type.chip.copy(fontWeight = FontWeight.Bold), color = t.ink, align = TextAlign.Center)
                }
            }
        }
        rows.forEach { r ->
            Column(verticalArrangement = Arrangement.spacedBy(FmtSpace.s1)) {
                FmtText(r.label, FmtTheme.type.chip.copy(fontWeight = FontWeight.Bold), color = t.ink)
                Row(Modifier.fillMaxWidth()) {
                    r.days.forEachIndexed { i, day ->
                        Box(
                            Modifier.weight(1f).background(if (i == today) t.today else t.today.copy(alpha = 0f), cellShape).padding(vertical = FmtSpace.s1)
                                .semantics(mergeDescendants = true) { contentDescription = "${FULL_DAYS[i]}, ${r.label}: ${day.words}" },
                            contentAlignment = Alignment.Center,
                        ) {
                            val dot = Modifier.size(FmtSpace.s8)
                            when (day) {
                                CalendarDay.None -> Box(dot)
                                CalendarDay.Planned -> Box(dot.border(FmtBorder.outline, t.planned(r.tone), CircleShape))
                                CalendarDay.Partial -> Box(dot.background(t.partial(r.tone), CircleShape).border(FmtBorder.outline, t.partialLine(r.tone), CircleShape))
                                CalendarDay.Done -> Box(dot.background(t.doneFill, CircleShape), contentAlignment = Alignment.Center) {
                                    FmtIcon(FmtIcons.tick, t.doneInk, FmtSize.iconSm)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
