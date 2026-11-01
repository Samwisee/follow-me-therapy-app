package au.com.followmetherapy.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import au.com.followmetherapy.design.components.FmtFilterChip
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.dp
import au.com.followmetherapy.design.tokens.FmtMotion
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import au.com.followmetherapy.design.components.FmtFlowScaffold
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
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
import au.com.followmetherapy.design.components.FmtPanel
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
    // At the text edge, in line with the words inside the cards below.
    FmtGroup {
        FmtSwashTitle("$greeting!", swash = greeting.substringAfter(" "))
        FmtText("Here's the plan for the day!", FmtTheme.type.lead)
    }

    // With a plan from Simone, today shows what's planned for today.
    val plan = store.plan
    val day = store.weekday
    val showRoutine = plan == null || plan.routineOn(day)
    val showCalm = plan == null || plan.calm
    val practiceToday = if (plan == null) store.activities
        else store.activities.filter { (r, _) -> plan.practice.getOrNull(r)?.let { day in it.days } ?: false }

    // Morning routine, with today's progress.
    val done = store.routineDone.size
    val total = store.routineSteps.size
    val routineTime = plan?.routine?.time?.let { Plan.showTime(it) + " · " } ?: ""
    if (showRoutine) {
        FmtListRow(
            title = "My morning routine",
            subtitle = if (store.routineComplete) "All done today. Great job!" else if (done == 0) routineTime + "Six steps, one at a time" else "$done of $total steps done",
            icon = FmtIcons.sunrise, tone = Tone.Purple,
            onClick = {
                val next = store.routineSteps.indices.firstOrNull { it !in store.routineDone }
                nav.go(Route(Area.Morning, if (next == null || done == 0) 0 else next + 1))
            },
            progress = done / total.toFloat(),
            progressLabel = "$done of $total steps done",
            done = store.routineComplete,
            celebrate = remember(store.routineComplete) { store.firstSeenDone("routine:${store.today}", store.routineComplete) },
        )
    }

    // Calm-down plan: use it, or make it.
    if (!showCalm) Unit
    else if (store.calmSaved) {
        FmtListRow("Use my calm-down plan", "For when feelings get big", FmtIcons.breathe, Tone.Teal, onClick = { nav.go(Route(Area.CalmNow)) })
    } else {
        FmtListRow("Make my calm-down plan", "Choose what helps, in four short steps", FmtIcons.breathe, Tone.Teal, onClick = { nav.go(Route(Area.Calm)) },
            progress = store.calmProgress, progressLabel = "Making the plan")
    }

    // Practice: today's stars, or set the week up.
    if (store.weekSetUp && practiceToday.isNotEmpty()) {
        FmtGroup("Did you practise today?") {
            practiceToday.forEach { (r, name) ->
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
    } else if (plan == null) {
        FmtListRow("Set up my practice week", "Add the activities we chose together", FmtIcons.calendar, Tone.Sun, onClick = { nav.go(Route(Area.WeekSetup)) })
    }
    if (plan != null && !showRoutine && practiceToday.isEmpty()) {
        FmtGroup { FmtText("Nothing else is planned for today. Enjoy it!", FmtTheme.type.body) }
    }
}

/** A tappable star (44dp), filled sun when practised. */
@Composable
fun StarButton(on: Boolean, label: String, toggle: () -> Unit) = FmtStarToggle(on, label, toggle = toggle)


/** The kinds of activity, as on the worksheets' tags: the Activities tab filters by them. */
private enum class Kind(val label: String, val tone: Tone) { SelfCare("Self-care", Tone.Purple), Feelings("Feelings", Tone.Teal), HomePractice("Home practice", Tone.Sun) }

/**
 * Activities: everything there is to do, filtered by kind (more kinds arrive
 * as the worksheets do), each filter in its kind's colour, the words for
 * grown-ups above them. Rows slide in and out as the filter changes. Each shows its progress, and a tick when it's done.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivitiesTab(store: AppStore, nav: Nav) {
    var only by remember { mutableStateOf<Kind?>(null) }
    FmtText("Small, everyday practice between our sessions, for you and your child to do together. They're the same activities as my printed worksheets: use whichever suits your family.", FmtTheme.type.lead)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(FmtSpace.s2), verticalArrangement = Arrangement.spacedBy(FmtSpace.s2)) {
        FmtFilterChip("All", only == null, { only = null })
        Kind.entries.forEach { k -> FmtFilterChip(k.label, only == k, { only = k }, tone = k.tone) }
    }
    Column {
        val shown = Kind.entries.filter { only == null || only == it }
        val firstRow = shown.firstOrNull()
        Reveal(Kind.SelfCare in shown, first = firstRow == Kind.SelfCare) {
            val done = store.routineDone.size
            val total = store.routineSteps.size
            FmtListRow(
                "My morning routine",
                "Self-care · " + if (store.routineComplete) "all done today" else "$done of $total steps today",
                FmtIcons.sunrise, Tone.Purple, onClick = { nav.go(Route(Area.Morning)) },
                progress = done / total.toFloat(), progressLabel = "$done of $total steps done today", done = store.routineComplete,
                celebrate = remember(store.routineComplete) { store.firstSeenDone("routine:${store.today}", store.routineComplete) },
            )
        }
        Reveal(Kind.Feelings in shown, first = firstRow == Kind.Feelings) {
            FmtListRow(
                "My calm-down plan",
                if (store.calmSaved) "Feelings · your plan is ready" else "Feelings · choose what helps when feelings get big",
                FmtIcons.breathe, Tone.Teal,
                onClick = { nav.go(if (store.calmSaved) Route(Area.Calm, CALM_PLAN_STEP) else Route(Area.Calm)) },
                progress = store.calmProgress, progressLabel = if (store.calmSaved) "Plan ready" else "Making the plan", done = store.calmSaved,
                celebrate = remember(store.calmSaved) { store.firstSeenDone("calm", store.calmSaved) },
            )
        }
        Reveal(Kind.HomePractice in shown, first = firstRow == Kind.HomePractice) {
            val target = store.practiceTarget
            FmtListRow(
                "My practice week",
                "Home practice · " + if (store.weekSetUp) "${store.starsThisWeek} of $target stars this week" else "a star for each day you practise",
                FmtIcons.calendar, Tone.Sun,
                onClick = { nav.go(Route(if (store.weekSetUp) Area.Week else Area.WeekSetup)) },
                progress = store.practiceProgress, progressLabel = "${store.starsThisWeek} of $target stars this week",
                done = target > 0 && store.starsThisWeek >= target,
                celebrate = remember(store.starsThisWeek, target) { store.firstSeenDone("practice:${store.weekStart}", target > 0 && store.starsThisWeek >= target) },
            )
        }
        // Each practice activity, explained (how to do it, why it helps).
        store.activities.mapNotNull { (_, name) -> taskIndex(name)?.let { it to name } }.forEach { (t, name) ->
            Reveal(Kind.HomePractice in shown, first = false) {
                FmtListRow(name, "Home practice · how to do it", FmtIcons.star, Tone.Sun, onClick = { nav.go(Route(Area.Task, t)) })
            }
        }
    }
}

/** A part of a list that grows in (and fades) when shown and folds away when hidden, a stack gap above it unless it's [first]. */
@Composable
private fun Reveal(visible: Boolean, first: Boolean, content: @Composable () -> Unit) {
    val gap by animateDpAsState(if (first) 0.dp else FmtTheme.metrics.stack, tween(FmtMotion.durBase, easing = FmtMotion.easeStandard))
    AnimatedVisibility(
        visible,
        enter = expandVertically(tween(FmtMotion.durBase, easing = FmtMotion.easeStandard)) + fadeIn(tween(FmtMotion.durBase, FmtMotion.durFast)),
        exit = fadeOut(tween(FmtMotion.durFast)) + shrinkVertically(tween(FmtMotion.durBase, easing = FmtMotion.easeStandard)),
    ) {
        Box(Modifier.padding(top = gap)) { content() }
    }
}

/** For grown-ups (from the menu button on the tabs): install, privacy, backups and starting again. */
@Composable
fun GrownUpsScreen(store: AppStore, nav: Nav, platform: Platform) {
    FmtFlowScaffold { GrownUpsContent(store, nav, platform) }
}

@Composable
private fun ColumnScope.GrownUpsContent(store: AppStore, nav: Nav, platform: Platform) {
    var confirm by remember { mutableStateOf(false) }
    var cleared by remember { mutableStateOf(false) }
    var kept by remember { mutableStateOf<Boolean?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val installed = remember { platform.installed() }
    // Ask the device to keep this app's data even when it's short of space.
    LaunchedEffect(Unit) { platform.keepData { kept = it } }
    if (!installed) {
        FmtListRow("Add the app to your Home Screen", "It opens like an app, works without the internet, and keeps your progress safer", FmtIcons.addToHome, Tone.Purple, onClick = { nav.go(Route(Area.Install)) })
    }
    FmtInfoNote(
        "everything you enter stays on this device. Nothing is sent to me or anyone else, and there's no account.",
        leadIn = "Your privacy:", tone = Tone.Teal, icon = FmtIcons.shieldTick,
    )
    FmtPanel("Keep a backup", Tone.Purple) {
        FmtText(
            when (kept) {
                true -> "This device will keep your progress even when it's short of space. Clearing your browser's data still clears it, so keep a backup too."
                else -> "Your browser can clear your progress when you clear its data, or when the device is short of space." +
                    (if (installed) "" else " Adding the app to your Home Screen helps.") + " A backup keeps it safe."
            },
            FmtTheme.type.small,
        )
        FmtText(
            "A backup is one small file with everything you've saved here (${store.savedCount} ${if (store.savedCount == 1) "thing" else "things"}). Keep it in Files or Downloads, and restore it after clearing your browser or on a new phone.",
            FmtTheme.type.small,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(FmtTheme.metrics.gridGap)) {
            FmtButton("Save a backup", { store.saveBackup(); message = null }, kind = ButtonKind.Outline)
            FmtButton("Restore", {
                platform.openFile { text ->
                    message = when (val r = store.restore(text)) {
                        RestoreResult.NotABackup -> "That file isn't a Follow Me Therapy backup. Nothing was changed."
                        is RestoreResult.Restored -> if (r.changed == 0) "Nothing new in that backup: this device already has everything in it."
                            else "Restored ${r.changed} ${if (r.changed == 1) "thing" else "things"} from your backup."
                    }
                }
            }, kind = ButtonKind.Outline)
        }
        message?.let { FmtText(it, FmtTheme.type.small, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = FmtColors.ink) }
    }
    FmtPanel("Start again", Tone.Sun) {
        FmtText("Clear the routine, the calm-down plan, your plan from me and the stars from this device.", FmtTheme.type.small)
        Row {
            FmtButton(if (confirm) "Tap again to clear everything" else "Clear everything on this device", {
                if (confirm) { store.forgetEverything(); confirm = false; cleared = true } else { confirm = true; cleared = false }
            }, kind = ButtonKind.Outline)
        }
        // Say it's done: clearing can't be seen otherwise.
        if (cleared) {
            FmtInfoNote(
                "everything this app saved on this device is gone. You can start again whenever you like.",
                leadIn = "Cleared:", tone = Tone.Teal, icon = FmtIcons.tick,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
    FmtListRow("The design system", "Colours, type and parts this app is built from", FmtIcons.blocks, Tone.Purple, onClick = { nav.go(Route(Area.DesignSystem)) })
    // For Simone (and Sam): her Planner, on a phone or a computer. It opens only after setup and Face ID / Touch ID.
    FmtListRow("For Simone: the Planner", "Families' plans, templates and requests. Opens with Face ID or Touch ID.", FmtIcons.calendar, Tone.Purple,
        onClick = { platform.openUrl(platform.appUrl() + "planner.html") })
    // The sign-off, on the same text edge as everything above.
    FmtGroup {
        Row(horizontalArrangement = Arrangement.spacedBy(FmtSpace.s2), verticalAlignment = Alignment.CenterVertically) {
            FmtIcon(FmtIcons.heart, FmtColors.purple, FmtSize.iconSm)
            FmtText("Simone · Paediatric OT · followmetherapy.com.au", FmtTheme.type.small)
        }
        FmtText("Version ${store.build}", FmtTheme.type.small.copy(fontSize = FmtTheme.metrics.textXs))
    }
}
