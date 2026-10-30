package au.com.followmetherapy.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import au.com.followmetherapy.design.components.FmtWeekCalendar
import au.com.followmetherapy.design.components.FmtIconButton
import au.com.followmetherapy.design.components.CalendarRow
import au.com.followmetherapy.design.components.CalendarDay
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import au.com.followmetherapy.design.components.ButtonKind
import au.com.followmetherapy.design.components.FmtAction
import au.com.followmetherapy.design.components.FmtButton
import au.com.followmetherapy.design.components.FmtFlowScaffold
import au.com.followmetherapy.design.components.FmtGroup
import au.com.followmetherapy.design.components.FmtHead
import au.com.followmetherapy.design.components.FmtInfoNote
import au.com.followmetherapy.design.components.FmtListRow
import au.com.followmetherapy.design.components.FmtPanel
import au.com.followmetherapy.design.components.FmtText
import au.com.followmetherapy.design.components.FmtToggleChip
import au.com.followmetherapy.design.components.Tone
import au.com.followmetherapy.design.components.WriteField
import au.com.followmetherapy.design.icons.FmtIcons
import au.com.followmetherapy.design.theme.FmtTheme
import au.com.followmetherapy.design.tokens.FmtColors
import au.com.followmetherapy.design.tokens.FmtSpace

/** A plan on its way in (from a link or a paste), waiting for the family to add it. */
class PlanInbox {
    var plan by mutableStateOf<Plan?>(null)
    /** Simone is previewing a plan as the family will see it (nothing is added). */
    var preview by mutableStateOf(false)
}


// ── The family's Plan tab ──

@Composable
fun PlanTab(store: AppStore, nav: Nav, platform: Platform, inbox: PlanInbox) {
    val p = store.plan
    var message by remember { mutableStateOf<String?>(null) }
    if (p != null) {
        FmtHead("My plan", swash = "plan", tag = p.service ?: "From Simone", tone = Tone.Purple)
        p.note?.takeIf { it.isNotBlank() }?.let { FmtInfoNote(it, leadIn = "From Simone:", icon = FmtIcons.heart) }
        PlanCalendar(store)
        FmtGroup("Each week") {}
        PlanRows(p, store, nav)
        FmtPanel("Reminders and progress", Tone.Purple) {
            if (p.hasReminders) {
                FmtText("Add a reminder to your phone's calendar for each activity, at its time.", FmtTheme.type.small)
                Row {
                    FmtButton("Add reminders to my calendar", {
                        platform.saveFile("follow-me-plan.ics", p.calendar(store.today, platform.now(), platform.appUrl()), "text/calendar")
                    }, kind = ButtonKind.Outline)
                }
            }
            FmtText("Before our session, send me how the week went. It goes only where you send it.", FmtTheme.type.small)
            Row { FmtButton("Send my progress to Simone", { sendProgress(store, platform) { message = it } }, kind = ButtonKind.Outline) }
            message?.let { FmtText(it, FmtTheme.type.small, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = FmtColors.ink) }
        }
        PastePlan(platform, nav, inbox, "Got a new plan from me? Open the link, or copy it and paste it here.")
    } else {
        FmtGroup {
            FmtText("Your plan from me goes here: which activities, on which days. After a session I'll send you a link. Open it, or copy it and paste it here.", FmtTheme.type.lead)
        }
        PastePlan(platform, nav, inbox, null)
        PlanCalendar(store)
    }
}

/**
 * The week as the practice week's stars: each activity's days planned, partly
 * done or done, today marked; this week's practice stars can be tapped. Step
 * back through earlier weeks (or one ahead).
 */
@Composable
private fun PlanCalendar(store: AppStore) {
    var offset by remember { mutableStateOf(0) }
    val monday = Dates.add(store.weekStart, offset * 7)
    val dates = (0..6).map { Dates.add(monday, it) }
    val plan = store.plan
    val rows = buildList {
        if (plan == null || plan.routine != null) {
            val planned = plan?.routine?.days
            add(CalendarRow("Morning routine", Tone.Purple, dates.mapIndexed { i, d ->
                val n = store.routineStepsOn(d)
                when {
                    n >= store.routineSteps.size -> CalendarDay.Done
                    n > 0 -> CalendarDay.Partial
                    planned != null && i in planned -> CalendarDay.Planned
                    else -> CalendarDay.None
                }
            }))
        }
        if (plan?.calm == true || store.calmSaved) {
            add(CalendarRow("Calm-down plan · when needed", Tone.Teal, dates.map { if (store.calmUsedOn(it) > 0) CalendarDay.Done else CalendarDay.None }))
        }
        store.weekNames.forEachIndexed { r, name ->
            if (name.isNotBlank()) {
                val planned = plan?.practice?.getOrNull(r)?.days
                // This week's practice stars tap like the practice week's (and burst).
                add(CalendarRow(name, Tone.Sun, (0..6).map { d ->
                    when {
                        store.practisedOn(monday, r, d) -> CalendarDay.Done
                        planned != null && d in planned -> CalendarDay.Planned
                        else -> CalendarDay.None
                    }
                }, onToggle = if (offset == 0) { d -> store.togglePractice(r, d) } else null))
            }
        }
    }.filter { row ->
        // Without a plan, only what's been done shows (nothing is planned yet).
        plan != null || row.days.any { it != CalendarDay.None }
    }
    FmtGroup {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FmtSpace.s2)) {
            FmtIconButton(FmtIcons.back, "Last week", { if (offset > -12) offset-- })
            FmtText(
                when (offset) { 0 -> "This week"; -1 -> "Last week"; 1 -> "Next week"; else -> "Week of ${Dates.show(monday)}" },
                FmtTheme.type.groupTitle, Modifier.weight(1f).semantics { heading() }, align = TextAlign.Center,
            )
            FmtIconButton(FmtIcons.chevron, "Next week", { if (offset < 1) offset++ })
        }
    }
    // At the text edge, like the week's title above it.
    FmtGroup {
        if (rows.isEmpty()) FmtText(if (offset > 0) "Nothing planned yet." else "Nothing done yet this week. What you do will show here, day by day.", FmtTheme.type.body)
        else FmtWeekCalendar(rows, dates.map { it.substringAfterLast('-').trimStart('0') }, today = if (offset == 0) store.weekday else null)
    }
}

/** The plan's activities, each with its days and time, a tap away. */
@Composable
private fun PlanRows(p: Plan, store: AppStore, nav: Nav?) {
    p.routine?.let {
        FmtListRow("My morning routine", Plan.showWhen(it), FmtIcons.sunrise, Tone.Purple, onClick = nav?.let { n -> { n.go(Route(Area.Morning)) } })
    }
    if (p.calm) {
        FmtListRow(if (store.calmSaved) "Use my calm-down plan" else "Make my calm-down plan", "When feelings get big", FmtIcons.breathe, Tone.Teal,
            onClick = nav?.let { n -> { n.go(Route(if (store.calmSaved) Area.CalmNow else Area.Calm)) } })
    }
    p.practice.forEach {
        FmtListRow(it.name, "Practice · " + Plan.showWhen(it), FmtIcons.star, Tone.Sun, onClick = nav?.let { n -> { n.go(Route(Area.Week)) } })
    }
}

/** The week so far, shared (Messages, Mail…) or copied, for the family to send to Simone. */
private fun sendProgress(store: AppStore, platform: Platform, onMessage: (String?) -> Unit) {
    val text = store.progressSummary()
    platform.shareText("Progress for Simone", text) { shared ->
        if (shared) onMessage(null)
        else platform.copyText(text) { ok -> onMessage(if (ok) "Copied. Paste it into a message or email to me." else text) }
    }
}

/** Paste a plan link: from the clipboard in one tap, or into a field when the browser won't share the clipboard. */
@Composable
private fun PastePlan(platform: Platform, nav: Nav, inbox: PlanInbox, lead: String?) {
    var field by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val take = { t: String ->
        val plan = Plan.fromLink(t)
        if (plan != null) { inbox.plan = plan; nav.go(Route(Area.PlanImport)) } else null
    }
    FmtGroup {
        lead?.let { FmtText(it, FmtTheme.type.small) }
        Row {
            FmtButton("Paste a plan link", {
                platform.readClipboard { t ->
                    if (t == null || take(t) == null) {
                        field = true
                        message = if (t == null) "Paste the link from me here:" else "I couldn't find a plan in what you copied. Paste the link from me here:"
                    }
                }
            }, kind = ButtonKind.Outline)
        }
        if (field) {
            message?.let { FmtText(it, FmtTheme.type.small) }
            WriteField(text, { text = it; take(it) }, placeholder = "The link from Simone", kid = false)
        }
    }
}

// ── A plan arriving (from a link) ──

@Composable
fun PlanImportScreen(store: AppStore, nav: Nav, platform: Platform, inbox: PlanInbox) {
    val p = inbox.plan
    if (p == null) {
        FmtFlowScaffold(end = FmtAction("Done") { nav.home() }) {
            FmtHead("A plan from Simone", swash = "plan", tag = "From Simone")
            FmtGroup { FmtText("That link didn't hold a plan. Ask me to send it again.", FmtTheme.type.lead) }
        }
        return
    }
    val replacing = store.plan != null && !inbox.preview
    FmtFlowScaffold(
        start = if (inbox.preview) null else FmtAction("Not now", ButtonKind.Outline) { inbox.plan = null; nav.home() },
        end = if (inbox.preview) FmtAction("Back to the plan", ButtonKind.Outline) { inbox.plan = null; inbox.preview = false; nav.go(Route(Area.Planner, PLANNER_SEND)) }
        else FmtAction(if (replacing) "Use this plan" else "Add my plan", chevron = true) {
            store.setPlan(p)
            inbox.plan = null
            nav.tab(1)
        },
    ) {
        FmtHead("A plan from Simone", swash = "plan", tag = p.service ?: "From Simone")
        p.note?.takeIf { it.isNotBlank() }?.let { FmtInfoNote(it, leadIn = "From Simone:", icon = FmtIcons.heart) }
        FmtGroup("Each week") {}
        PlanRows(p, store, null)
        if (replacing) {
            FmtInfoNote("this replaces the plan you have now. Your progress so far stays.", leadIn = "Already have a plan?", tone = Tone.Sun)
        }
        if (!inbox.preview && platform.device() == Device.IPhone && !platform.installed()) {
            FmtInfoNote(
                "the app on your Home Screen keeps its own copy. Copy this link, open Follow Me from your Home Screen, and tap Plan › Paste a plan link.",
                leadIn = "Using Follow Me from your Home Screen?", tone = Tone.Teal,
            )
        }
    }
}
