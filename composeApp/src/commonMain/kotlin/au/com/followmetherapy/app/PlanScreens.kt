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
}

/** Simone's services a plan can belong to (as on the website). */
private val SERVICES = listOf("5 Steps Forward", "Steady Steps", "Early Start Denver Model")

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
 * The week as a calendar: each activity's days planned, partly done or done,
 * today marked; step back through earlier weeks (or one ahead).
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
            add(CalendarRow("Calm-down plan", Tone.Teal, dates.map { if (store.calmUsedOn(it) > 0) CalendarDay.Done else CalendarDay.None }))
        }
        store.weekNames.forEachIndexed { r, name ->
            if (name.isNotBlank()) {
                val planned = plan?.practice?.getOrNull(r)?.days
                add(CalendarRow(name, Tone.Sun, (0..6).map { d ->
                    when {
                        store.practisedOn(monday, r, d) -> CalendarDay.Done
                        planned != null && d in planned -> CalendarDay.Planned
                        else -> CalendarDay.None
                    }
                }))
            }
        }
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
    FmtWeekCalendar(rows, dates.map { it.substringAfterLast('-').trimStart('0') }, today = if (offset == 0) store.weekday else null)
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
    val replacing = store.plan != null
    FmtFlowScaffold(
        start = FmtAction("Not now", ButtonKind.Outline) { inbox.plan = null; nav.home() },
        end = FmtAction(if (replacing) "Use this plan" else "Add my plan", chevron = true) {
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
        if (platform.device() == Device.IPhone && !platform.installed()) {
            FmtInfoNote(
                "the app on your Home Screen keeps its own copy. Copy this link, open Follow Me from your Home Screen, and tap Plan › Paste a plan link.",
                leadIn = "Using Follow Me from your Home Screen?", tone = Tone.Teal,
            )
        }
    }
}

// ── Simone's plan builder (#planner) ──

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayPicker(days: SnapshotStateList<Int>, tone: Tone) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(FmtSpace.s2), verticalArrangement = Arrangement.spacedBy(FmtSpace.s2)) {
        Plan.DAY_SHORT.forEachIndexed { i, d ->
            FmtToggleChip(d, i in days, { on -> if (on) { if (i !in days) days.add(i) } else days.remove(i) }, tone)
        }
    }
}

/**
 * Where Simone makes a family's plan: her service, a note, which activities
 * on which days and when. It ends in a link she sends the family. Reached by
 * its address (#planner); her draft stays on her device.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlannerScreen(store: AppStore, nav: Nav, platform: Platform, step: Int, inbox: PlanInbox) {
    if (step == 1) { PlannerLink(store, nav, platform, inbox); return }
    val draft = remember { store.plannerDraft() ?: Plan(routine = PlanItem("Morning routine", (0..4).toSet(), "07:30"), calm = true) }
    var service by remember { mutableStateOf(draft.service) }
    var note by remember { mutableStateOf(draft.note ?: "") }
    var routineOn by remember { mutableStateOf(draft.routine != null) }
    val routineDays = remember { mutableStateListOf<Int>().apply { addAll(draft.routine?.days ?: (0..4).toSet()) } }
    var routineTime by remember { mutableStateOf(draft.routine?.time?.let { Plan.showTime(it) } ?: "7:30am") }
    var calm by remember { mutableStateOf(draft.calm) }
    val names = remember { mutableStateListOf<String>().apply { repeat(4) { add(draft.practice.getOrNull(it)?.name ?: "") } } }
    val days = remember { List(4) { i -> mutableStateListOf<Int>().apply { addAll(draft.practice.getOrNull(i)?.days ?: setOf(0, 2, 4)) } } }
    val times = remember { mutableStateListOf<String>().apply { repeat(4) { add(draft.practice.getOrNull(it)?.time?.let { t -> Plan.showTime(t) } ?: "4pm") } } }
    fun build() = Plan(
        service = service,
        note = note.trim().ifEmpty { null },
        routine = if (routineOn) PlanItem("Morning routine", routineDays.toSet(), Plan.normaliseTime(routineTime)) else null,
        calm = calm,
        practice = (0..3).filter { names[it].isNotBlank() }.map { PlanItem(names[it].trim(), days[it].toSet(), Plan.normaliseTime(times[it])) },
        made = store.today,
    )
    FmtFlowScaffold(end = FmtAction("Make the link", chevron = true) { store.savePlannerDraft(build()); nav.go(Route(Area.Planner, 1)) }) {
        FmtHead("Make a plan", swash = "plan", tag = "For Simone", tone = Tone.Purple)
        FmtGroup("Service") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(FmtSpace.s2), verticalArrangement = Arrangement.spacedBy(FmtSpace.s2)) {
                SERVICES.forEach { s -> FmtToggleChip(s, service == s, { on -> service = if (on) s else null }) }
            }
        }
        FmtGroup("A note for the family") {
            WriteField(note, { note = it }, placeholder = "What we're working on, in a sentence or two", kid = false, minLines = 2)
        }
        FmtPanel("Morning routine", Tone.Purple) {
            Row { FmtToggleChip("In the plan", routineOn, { routineOn = it }) }
            if (routineOn) {
                DayPicker(routineDays, Tone.Purple)
                WriteField(routineTime, { routineTime = it }, placeholder = "Reminder time, like 7:30am", kid = false)
            }
        }
        FmtPanel("Calm-down plan", Tone.Teal) {
            FmtText("For when feelings get big, so it has no days or reminders.", FmtTheme.type.small)
            Row { FmtToggleChip("In the plan", calm, { calm = it }, Tone.Teal) }
        }
        FmtPanel("Practice", Tone.Sun) {
            FmtText("Up to four activities, each with its days and a reminder time. Leave a name empty to skip it.", FmtTheme.type.small)
            for (i in 0..3) {
                WriteField(names[i], { names[i] = it }, placeholder = "Activity ${i + 1}, like Cutting practice", kid = false)
                if (names[i].isNotBlank()) {
                    DayPicker(days[i], Tone.Sun)
                    WriteField(times[i], { times[i] = it }, placeholder = "Reminder time, like 4pm", kid = false)
                }
            }
        }
    }
}

/** The plan's link, to send: shared (Messages, Mail…) or copied. */
@Composable
private fun PlannerLink(store: AppStore, nav: Nav, platform: Platform, inbox: PlanInbox) {
    val plan = remember { store.plannerDraft() ?: Plan(made = store.today) }
    val link = remember { plan.link(platform.appUrl()) }
    var message by remember { mutableStateOf<String?>(null) }
    FmtFlowScaffold(
        start = FmtAction("Edit", ButtonKind.Outline) { nav.go(Route(Area.Planner)) },
        end = FmtAction("Share the link", chevron = true) {
            platform.shareText("Your plan from Simone", "Here's your plan for Follow Me. Open this link on your phone: $link") { shared ->
                if (!shared) platform.copyText(link) { ok -> message = if (ok) "Link copied. Paste it into a message or email." else link }
            }
        },
    ) {
        FmtHead("Send the plan", swash = "plan", tag = "For Simone", tone = Tone.Purple)
        FmtGroup {
            FmtText("Send the family this link. Opening it adds the plan to Follow Me on their phone. The plan is inside the link, so it isn't stored anywhere else.", FmtTheme.type.body)
        }
        FmtGroup("Each week") {}
        PlanRows(plan, store, null)
        FmtGroup {
            Row(horizontalArrangement = Arrangement.spacedBy(FmtTheme.metrics.gridGap)) {
                FmtButton("Copy the link", { platform.copyText(link) { ok -> message = if (ok) "Link copied." else link } }, kind = ButtonKind.Outline)
                FmtButton("See it as the family will", { inbox.plan = plan; nav.go(Route(Area.PlanImport)) }, kind = ButtonKind.Outline)
            }
            message?.let { FmtText(it, FmtTheme.type.small, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = FmtColors.ink) }
        }
    }
}
