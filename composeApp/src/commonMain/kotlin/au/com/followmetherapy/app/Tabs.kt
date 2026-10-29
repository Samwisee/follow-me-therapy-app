package au.com.followmetherapy.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import au.com.followmetherapy.design.components.ButtonKind
import au.com.followmetherapy.design.components.FmtButton
import au.com.followmetherapy.design.components.FmtChip
import au.com.followmetherapy.design.components.FmtGroup
import au.com.followmetherapy.design.components.FmtInfoNote
import au.com.followmetherapy.design.components.FmtListRow
import au.com.followmetherapy.design.components.FmtLockup
import au.com.followmetherapy.design.components.FmtProgressBar
import au.com.followmetherapy.design.components.FmtStarToggle
import au.com.followmetherapy.design.components.FmtSwashTitle
import au.com.followmetherapy.design.components.FmtText
import au.com.followmetherapy.design.components.Tone
import au.com.followmetherapy.design.icons.FmtIcon
import au.com.followmetherapy.design.icons.FmtIcons
import au.com.followmetherapy.design.theme.FmtTheme
import au.com.followmetherapy.design.tokens.FmtColors
import au.com.followmetherapy.design.tokens.FmtSize
import au.com.followmetherapy.design.tokens.FmtSpace

private val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

/** Today: a greeting, then what's on today, each one a tap away. */
@Composable
fun TodayTab(store: AppStore, nav: Nav) {
    val m = FmtTheme.metrics
    val greeting = when {
        store.hour < 12 -> "Good morning"
        store.hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(m.groupGap)) {
        FmtSwashTitle("$greeting!", swash = greeting.substringAfter(" "))
        FmtText("Here's today, ${days[store.weekday]}.", FmtTheme.type.lead)
    }

    // Morning routine, with today's progress.
    val done = store.routineDone.size
    val total = store.routineSteps.size
    Column(verticalArrangement = Arrangement.spacedBy(m.groupGap)) {
        FmtListRow(
            title = "My morning routine",
            subtitle = if (store.routineComplete) "All done today. Great job!" else if (done == 0) "Six steps, one at a time" else "$done of $total steps done",
            icon = FmtIcons.sunrise, tone = Tone.Purple,
            onClick = {
                val next = store.routineSteps.indices.firstOrNull { it !in store.routineDone }
                nav.go(Route(Area.Morning, if (next == null || done == 0) 0 else next + 1))
            },
        )
        if (done > 0) FmtProgressBar(done / total.toFloat(), Modifier.padding(horizontal = m.textInset), label = "$done of $total steps done")
    }

    // Calm-down plan: use it, or make it.
    if (store.calmSaved) {
        FmtListRow("Use my calm-down plan", "For when feelings get big", FmtIcons.breathe, Tone.Teal, onClick = { nav.go(Route(Area.CalmNow)) })
    } else {
        FmtListRow("Make my calm-down plan", "Choose what helps, in four short steps", FmtIcons.breathe, Tone.Teal, onClick = { nav.go(Route(Area.Calm)) })
    }

    // Practice: today's stars, or set the week up.
    if (store.weekSetUp && store.activities.isNotEmpty()) {
        FmtGroup("Did you practise today?") {
            store.activities.forEach { (r, name) ->
                val on = (r to store.weekday) in store.weekDone
                FmtListRow(
                    title = name, subtitle = if (on) "Done today" else "Tap the star when it's done", tone = Tone.Sun,
                    trailing = { StarButton(on, "$name, ${days[store.weekday]}") { store.togglePractice(r, store.weekday) } },
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                FmtChip("${store.starsThisWeek} stars this week", Tone.Sun, FmtIcons.star)
                FmtButton("My week", { nav.go(Route(Area.Week)) }, kind = ButtonKind.Outline, trailingChevron = true)
            }
        }
    } else {
        FmtListRow("Set up my practice week", "Add the activities we chose together", FmtIcons.calendar, Tone.Sun, onClick = { nav.go(Route(Area.WeekSetup)) })
    }
}

/** A tappable star (44dp), filled sun when practised. */
@Composable
fun StarButton(on: Boolean, label: String, toggle: () -> Unit) = FmtStarToggle(on, label, toggle)

/** Activities: everything Simone has given the family, in one list. */
@Composable
fun ActivitiesTab(store: AppStore, nav: Nav) {
    val m = FmtTheme.metrics
    FmtText("Small, everyday practice between our sessions, for you and your child to do together.", FmtTheme.type.lead)
    Column(verticalArrangement = Arrangement.spacedBy(m.gridGap)) {
        FmtListRow("My morning routine", "Self-care · six steps from waking up to out the door", FmtIcons.sunrise, Tone.Purple, onClick = { nav.go(Route(Area.Morning)) })
        FmtListRow("My calm-down plan", if (store.calmSaved) "Feelings · your plan is ready" else "Feelings · choose what helps when feelings get big", FmtIcons.breathe, Tone.Teal,
            onClick = { nav.go(if (store.calmSaved) Route(Area.Calm, CALM_PLAN_STEP) else Route(Area.Calm)) })
        FmtListRow("My practice week", if (store.weekSetUp) "Home practice · ${store.starsThisWeek} stars this week" else "Home practice · a star for each day you practise", FmtIcons.calendar, Tone.Sun,
            onClick = { nav.go(Route(if (store.weekSetUp) Area.Week else Area.WeekSetup)) })
    }
    FmtInfoNote("these are the same activities as my printed worksheets. Use whichever suits your family: paper on the fridge, or this on a phone or tablet.", leadIn = "For grown-ups:")
}

/** For grown-ups: what this is, privacy, and a way to clear the device. */
@Composable
fun GrownUpsTab(store: AppStore, nav: Nav) {
    var confirm by remember { mutableStateOf(false) }
    FmtLockup()
    FmtText(
        "I'm Simone, a paediatric occupational therapist. These activities are the ones we practise in our sessions, for you and your child to keep going with at home. Short and often is best: five minutes a day is plenty.",
        FmtTheme.type.body,
    )
    FmtInfoNote(
        "everything you enter stays on this device. Nothing is sent to me or anyone else, and there's no account. Clearing your browser's data (or using a private window) clears it too.",
        leadIn = "Your privacy:", tone = Tone.Teal, icon = FmtIcons.shieldTick,
    )
    FmtGroup("Start again") {
        FmtText("Clear the routine, the calm-down plan and this week's stars from this device.", FmtTheme.type.small)
        Row {
            FmtButton(if (confirm) "Tap again to clear everything" else "Clear everything on this device", {
                if (confirm) { store.forgetEverything(); confirm = false } else confirm = true
            }, kind = ButtonKind.OutlineTeal)
        }
    }
    FmtListRow("The design system", "Colours, type and parts this app is built from", FmtIcons.blocks, Tone.Purple, onClick = { nav.go(Route(Area.DesignSystem)) })
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(FmtSpace.s2), verticalAlignment = Alignment.CenterVertically) {
            FmtIcon(FmtIcons.heart, FmtColors.purple, FmtSize.iconSm)
            FmtText("Simone · Paediatric OT · followmetherapy.com.au", FmtTheme.type.small)
        }
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        FmtText("Version ${store.build}", FmtTheme.type.small.copy(fontSize = FmtTheme.metrics.textXs))
    }
}
