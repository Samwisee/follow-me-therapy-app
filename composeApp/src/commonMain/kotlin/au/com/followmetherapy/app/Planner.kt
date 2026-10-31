package au.com.followmetherapy.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
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
import au.com.followmetherapy.design.tokens.FmtBorder
import au.com.followmetherapy.design.tokens.FmtColors
import au.com.followmetherapy.design.tokens.FmtRadius
import au.com.followmetherapy.design.tokens.FmtSpace

/*
 * Simone's planner: her families (a first name or initials each, with the
 * plan she last sent and the progress they sent back), templates for her
 * services, a list of activities to pick from, and sending by link or QR.
 * Everything stays on her phone. It opens on its own from the Planner icon
 * (planner.html) or at #planner.
 */

/** The planner's screens (the step of Area.Planner). */
const val PLANNER_LIST = 0
const val PLANNER_EDIT = 1
const val PLANNER_SEND = 2
const val PLANNER_REQUEST = 3

/** Simone's services a plan can belong to (as on the website). */
val SERVICES = listOf("5 Steps Forward", "Steady Steps", "Early Start Denver Model")

/** Practice activities to pick from: the names of TASKS (Tasks.kt), so each one has its how-to page. */
val ACTIVITY_LIBRARY: List<String> get() = TASKS.map { it.name }

/** A family on Simone's phone: what she calls them, their plan, when she last sent it, what they sent back. */
data class Family(val id: String, val name: String, val plan: Plan, val sent: String? = null, val progress: String? = null, val progressOn: String? = null) {
    fun toJson(): String = buildString {
        append("{\"n\":").append(q(name)).append(",\"p\":").append(q(plan.toJson()))
        sent?.let { append(",\"s\":").append(q(it)) }
        progress?.let { append(",\"g\":").append(q(it)) }
        progressOn?.let { append(",\"go\":").append(q(it)) }
        append('}')
    }

    companion object {
        fun parse(id: String, json: String): Family? {
            val m = (try { Json(json).value() } catch (e: Throwable) { null }) as? Map<*, *> ?: return null
            val plan = (m["p"] as? String)?.let { Plan.parse(it) } ?: return null
            return Family(id, m["n"] as? String ?: "", plan, m["s"] as? String, m["g"] as? String, m["go"] as? String)
        }
        internal fun q(s: String) = buildString {
            append('"')
            for (c in s) when (c) {
                '"' -> append("\\\""); '\\' -> append("\\\\"); '\n' -> append("\\n"); '\r' -> append("\\r"); '\t' -> append("\\t")
                else -> if (c < ' ') append("\\u").append(c.code.toString(16).padStart(4, '0')) else append(c)
            }
            append('"')
        }
    }
}

/** Starting points for each service; Simone changes them and saves her own (Save as my template). */
object Templates {
    private val WEEKDAYS = (0..4).toSet()
    fun builtIn(service: String?): Plan = when (service) {
        "5 Steps Forward" -> Plan(service, routine = PlanItem("Morning routine", WEEKDAYS, "07:30"), calm = true)
        "Steady Steps" -> Plan(service, routine = PlanItem("Morning routine", WEEKDAYS, "07:30"), calm = true,
            practice = listOf(PlanItem("Cutting practice", setOf(0, 2, 4), "16:00")))
        "Early Start Denver Model" -> Plan(service, routine = PlanItem("Morning routine", (0..6).toSet(), "07:30"),
            practice = listOf(PlanItem("Play routine together", (0..6).toSet(), "16:00")))
        else -> Plan(service, routine = PlanItem("Morning routine", WEEKDAYS, "07:30"), calm = true)
    }
}

/** What Simone is working on: the family open in the planner. */
class PlannerSession {
    var family by mutableStateOf<Family?>(null)
}

@Composable
fun PlannerScreen(store: AppStore, nav: Nav, platform: Platform, step: Int, inbox: PlanInbox, session: PlannerSession) {
    when (step) {
        PLANNER_EDIT -> PlannerEdit(store, nav, platform, session)
        PLANNER_SEND -> PlannerSend(store, nav, platform, inbox, session)
        PLANNER_REQUEST -> RequestScreen(platform, nav)
        else -> PlannerList(store, nav, session)
    }
}

// ── Her families ──

@Composable
private fun PlannerList(store: AppStore, nav: Nav, session: PlannerSession) {
    val families = remember { store.families() }
    FmtFlowScaffold(end = FmtAction("New plan", chevron = true) {
        session.family = Family(store.newFamilyId(), "", store.template(null).copy(made = store.today))
        nav.go(Route(Area.Planner, PLANNER_EDIT))
    }) {
        FmtHead("My families", swash = "families", tag = "Planner", tone = Tone.Purple)
        FmtGroup {
            FmtText(
                if (families.isEmpty()) "Make a family's plan, send it as a link or a QR code, and it's saved here to change and send again."
                else "Tap a family to change their plan and send it again.",
                FmtTheme.type.body,
            )
        }
        families.forEach { f ->
            val sent = f.sent?.let { "sent ${Dates.show(it)}" } ?: "not sent yet"
            FmtListRow(f.name.ifBlank { "No name" }, (f.plan.service?.let { "$it · " } ?: "") + sent, FmtIcons.calendar, Tone.Purple,
                onClick = { session.family = f; nav.go(Route(Area.Planner, PLANNER_EDIT)) })
        }
        FmtInfoNote("families' plans stay on this phone, locked with Face ID. Use a first name or initials, never a full name.", leadIn = "Privacy:", tone = Tone.Teal, icon = FmtIcons.shieldTick)
        FmtListRow("Request a change", "Something's wrong, or an idea for the Planner", FmtIcons.heart, Tone.Pink, onClick = { nav.go(Route(Area.Planner, PLANNER_REQUEST)) })
    }
}

// ── A family's plan ──

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayPicker(days: SnapshotStateList<Int>, tone: Tone) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(FmtSpace.s2), verticalArrangement = Arrangement.spacedBy(FmtSpace.s2)) {
        Plan.DAY_SHORT.forEachIndexed { i, d ->
            FmtToggleChip(d, i in days, { on -> if (on) { if (i !in days) days.add(i) } else days.remove(i) }, tone)
        }
    }
}

/** One practice activity being planned. */
private class PracticeDraft(name: String, days: Set<Int>, time: String) {
    var name by mutableStateOf(name)
    val days = mutableStateListOf<Int>().apply { addAll(days) }
    var time by mutableStateOf(time)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlannerEdit(store: AppStore, nav: Nav, platform: Platform, session: PlannerSession) {
    val start = session.family ?: Family(store.newFamilyId(), "", store.template(null))
    val isNew = start.sent == null
    var name by remember { mutableStateOf(start.name) }
    var service by remember { mutableStateOf(start.plan.service) }
    var note by remember { mutableStateOf(start.plan.note ?: "") }
    var routineOn by remember { mutableStateOf(start.plan.routine != null) }
    val routineDays = remember { mutableStateListOf<Int>() }
    var routineTime by remember { mutableStateOf("") }
    var calm by remember { mutableStateOf(start.plan.calm) }
    val practice = remember { mutableStateListOf<PracticeDraft>() }
    var other by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf(start.progress ?: "") }
    var message by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun load(p: Plan) {
        routineOn = p.routine != null
        routineDays.clear(); routineDays.addAll(p.routine?.days ?: (0..4).toSet())
        routineTime = p.routine?.time?.let { Plan.showTime(it) } ?: "7:30am"
        calm = p.calm
        practice.clear(); p.practice.forEach { practice.add(PracticeDraft(it.name, it.days, it.time?.let { t -> Plan.showTime(t) } ?: "4pm")) }
    }
    remember { load(start.plan); true }

    fun plan() = Plan(
        service = service,
        note = note.trim().ifEmpty { null },
        routine = if (routineOn) PlanItem("Morning routine", routineDays.toSet(), Plan.normaliseTime(routineTime)) else null,
        calm = calm,
        practice = practice.filter { it.name.isNotBlank() }.map { PlanItem(it.name.trim(), it.days.toSet(), Plan.normaliseTime(it.time)) },
        made = store.today,
    )
    fun family() = start.copy(name = name.trim(), plan = plan(), progress = progress.ifBlank { null }, progressOn = if (progress != (start.progress ?: "")) store.today else start.progressOn)

    FmtFlowScaffold(
        start = FmtAction("Save", ButtonKind.Outline) { store.saveFamily(family()); nav.go(Route(Area.Planner)) },
        end = FmtAction("Send", chevron = true, enabled = name.isNotBlank()) {
            val f = family(); store.saveFamily(f); session.family = f
            nav.go(Route(Area.Planner, PLANNER_SEND))
        },
    ) {
        FmtHead(name.ifBlank { "New plan" }, swash = null, tag = service ?: "Planner", tone = Tone.Purple)
        FmtGroup("Who it's for") {
            WriteField(name, { name = it }, placeholder = "First name or initials", kid = false)
        }
        FmtGroup("Service") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(FmtSpace.s2), verticalArrangement = Arrangement.spacedBy(FmtSpace.s2)) {
                SERVICES.forEach { s ->
                    FmtToggleChip(s, service == s, { on ->
                        service = if (on) s else null
                        // A new plan starts from the service's template.
                        if (isNew) load(store.template(service))
                    })
                }
            }
            if (isNew) FmtText("A new plan starts from the service's template. Change anything below.", FmtTheme.type.small)
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
            FmtText("Pick up to four. Each gets its days and a reminder time.", FmtTheme.type.small)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(FmtSpace.s2), verticalArrangement = Arrangement.spacedBy(FmtSpace.s2)) {
                (ACTIVITY_LIBRARY + practice.map { it.name }.filter { it !in ACTIVITY_LIBRARY }).forEach { a ->
                    val chosen = practice.any { it.name == a }
                    FmtToggleChip(a, chosen, { on ->
                        if (on && practice.size < 4) practice.add(PracticeDraft(a, setOf(0, 2, 4), "4pm"))
                        if (!on) practice.removeAll { it.name == a }
                    }, Tone.Sun)
                }
            }
            if (practice.size < 4) {
                WriteField(other, { other = it }, placeholder = "Something else", kid = false)
                if (other.isNotBlank()) Row {
                    FmtButton("Add", { practice.add(PracticeDraft(other.trim(), setOf(0, 2, 4), "4pm")); other = "" }, kind = ButtonKind.Outline)
                }
            }
            practice.forEach { d ->
                FmtText(d.name, FmtTheme.type.groupTitle)
                DayPicker(d.days, Tone.Sun)
                WriteField(d.time, { d.time = it }, placeholder = "Reminder time, like 4pm", kid = false)
            }
        }
        FmtPanel("Their progress", Tone.Pink) {
            FmtText(
                start.progressOn?.let { "What they sent on ${Dates.show(it)}. Paste a newer one to keep it with their plan." }
                    ?: "When they tap Send my progress to Simone, paste what they send here, to plan from it next time.",
                FmtTheme.type.small,
            )
            WriteField(progress, { progress = it }, placeholder = "Paste their progress", kid = false, minLines = 3)
            Row {
                FmtButton("Paste", { platform.readClipboard { t -> if (!t.isNullOrBlank()) progress = t } }, kind = ButtonKind.Outline)
            }
        }
        FmtGroup {
            Row(horizontalArrangement = Arrangement.spacedBy(FmtTheme.metrics.gridGap)) {
                FmtButton(if (service != null) "Save as my $service template" else "Save as my starting plan", {
                    store.saveTemplate(service, plan()); message = "Saved. New ${service ?: "plans"} start from this."
                }, kind = ButtonKind.Outline)
            }
            if (!isNew || store.families().any { it.id == start.id }) Row {
                FmtButton(if (confirmDelete) "Tap again to delete" else "Delete this family", {
                    if (confirmDelete) { store.deleteFamily(start.id); session.family = null; nav.go(Route(Area.Planner)) } else confirmDelete = true
                }, kind = ButtonKind.Outline)
            }
            message?.let { FmtText(it, FmtTheme.type.small, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = FmtColors.ink) }
        }
    }
}

// ── Sending it ──

@Composable
private fun PlannerSend(store: AppStore, nav: Nav, platform: Platform, inbox: PlanInbox, session: PlannerSession) {
    val f = session.family ?: run { PlannerList(store, nav, session); return }
    val link = remember(f) { f.plan.link(platform.appUrl()) }
    val qr = remember(link) { platform.qrCode(link) }
    var message by remember { mutableStateOf<String?>(null) }
    val markSent = { store.saveFamily(f.copy(sent = store.today)); session.family = f.copy(sent = store.today) }
    FmtFlowScaffold(
        start = FmtAction("Edit", ButtonKind.Outline) { nav.go(Route(Area.Planner, PLANNER_EDIT)) },
        end = FmtAction("Share the link", chevron = true) {
            platform.shareText("Your plan from Simone", "Here's your plan for Follow Me. Open this link on your phone: $link") { shared ->
                if (shared) { markSent(); message = "Sent." }
                else platform.copyText(link) { ok -> if (ok) markSent(); message = if (ok) "Link copied. Paste it into a message or email." else link }
            }
        },
    ) {
        FmtHead("Send to ${f.name}", swash = null, tag = f.plan.service ?: "Planner", tone = Tone.Purple)
        if (qr != null) {
            FmtGroup {
                FmtText("In a session: ask them to point their phone's camera here.", FmtTheme.type.body)
            }
            QrCode(qr, "QR code for ${f.name}'s plan")
        }
        FmtGroup {
            FmtText("Or send the link by message or email. The plan is inside the link, so it isn't stored anywhere else.", FmtTheme.type.body)
            Row(horizontalArrangement = Arrangement.spacedBy(FmtTheme.metrics.gridGap)) {
                FmtButton("Copy the link", { platform.copyText(link) { ok -> if (ok) markSent(); message = if (ok) "Link copied." else link } }, kind = ButtonKind.Outline)
                FmtButton("See it as they will", { inbox.plan = f.plan; inbox.preview = true; nav.go(Route(Area.PlanImport)) }, kind = ButtonKind.Outline)
            }
            message?.let { FmtText(it, FmtTheme.type.small, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = FmtColors.ink) }
        }
    }
}

/** A QR code: dark modules on paper with the standard four-module quiet zone, in a nested, outlined frame. */
@Composable
private fun QrCode(modules: List<BooleanArray>, label: String) {
    val m = FmtTheme.metrics
    val shape = RoundedCornerShape(m.containerRadius)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.widthIn(max = FmtSpace.s30 * 3).fillMaxWidth().aspectRatio(1f)
                .background(FmtColors.paper, shape).border(FmtBorder.outline, FmtColors.purpleLine, shape)
                .padding(m.cardPad)
                .semantics { contentDescription = label },
        ) {
            Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
                val n = modules.size + 8
                val cell = size.width / n
                modules.forEachIndexed { y, row ->
                    row.forEachIndexed { x, dark ->
                        if (dark) drawRect(FmtColors.ink, Offset((x + 4) * cell, (y + 4) * cell), Size(cell + 0.5f, cell + 0.5f))
                    }
                }
            }
        }
    }
}
