package au.com.followmetherapy.app

import au.com.followmetherapy.design.tokens.FmtCategories
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import au.com.followmetherapy.design.components.ButtonKind
import au.com.followmetherapy.design.components.Choice
import au.com.followmetherapy.design.components.FmtAction
import au.com.followmetherapy.design.components.FmtBreathing
import au.com.followmetherapy.design.components.FmtButton
import au.com.followmetherapy.design.components.FmtCelebration
import au.com.followmetherapy.design.components.FmtChoices
import au.com.followmetherapy.design.components.FmtFlowScaffold
import au.com.followmetherapy.design.components.FmtGroup
import au.com.followmetherapy.design.components.FmtHead
import au.com.followmetherapy.design.components.FmtIconTile
import au.com.followmetherapy.design.components.FmtInfoNote
import au.com.followmetherapy.design.components.FmtKidText
import au.com.followmetherapy.design.components.FmtListRow
import au.com.followmetherapy.design.components.FmtStepFocus
import au.com.followmetherapy.design.components.FmtStepList
import au.com.followmetherapy.design.components.FmtText
import au.com.followmetherapy.design.components.FmtTracker
import au.com.followmetherapy.design.components.Tone
import au.com.followmetherapy.design.components.WriteField
import au.com.followmetherapy.design.icons.FmtIcons
import au.com.followmetherapy.design.theme.FmtTheme

// ── Morning routine: intro, one step per screen, a celebration, a reflection ──

private const val MORNING_CELEBRATE = 7
private const val MORNING_REFLECT = 8

@Composable
fun MorningFlow(store: AppStore, nav: Nav, step: Int) {
    val steps = store.routineSteps
    val total = steps.size
    val go = { s: Int -> nav.go(Route(Area.Morning, s)) }
    when (step) {
        0 -> FmtFlowScaffold(
            end = FmtAction("Start", chevron = true) { go(1) },
        ) {
            FmtHead("My morning routine", swash = "morning", tag = "Self-care", tone = FmtCategories.tone("Self-care"))
            FmtGroup {
                FmtKidText("Let's get ready, one step at a time.")
                FmtStepList(steps, store.routineDone.toSet(), onToggle = { store.toggleRoutineStep(it, it !in store.routineDone) })
            }
            FmtInfoNote("point to the next step instead of saying it, and let your child do the tapping.", leadIn = "For grown-ups:")
        }
        in 1..total -> {
            val i = step - 1
            val done = i in store.routineDone
            FmtFlowScaffold(
                start = FmtAction("Back", ButtonKind.Outline) { go(step - 1) },
                end = if (done) FmtAction("Next", chevron = true) { go(if (store.routineComplete) MORNING_CELEBRATE else step + 1) }
                else FmtAction("Done", ButtonKind.Teal, chevron = true) {
                    store.toggleRoutineStep(i, true)
                    go(if (store.routineComplete) MORNING_CELEBRATE else step + 1)
                },
                centre = true,
            ) {
                FmtStepFocus(step, total, steps[i], done)
            }
        }
        MORNING_CELEBRATE -> FmtFlowScaffold(
            end = FmtAction("Next", chevron = true) { go(MORNING_REFLECT) },
            centre = true,
        ) {
            FmtCelebration("All done!", swash = "done", message = "You did your whole morning routine.")
        }
        else -> FmtFlowScaffold(
            start = FmtAction("Back", ButtonKind.Outline) { go(MORNING_CELEBRATE) },
            end = FmtAction("Finish") { nav.home() },
        ) {
            FmtGroup("What was tricky today?", inset = false) {
                FmtKidText("You can skip this.")
                WriteField(store.routineTricky, store::setTricky, placeholder = "Type it, or tell a grown-up", minLines = 3)
            }
            FmtInfoNote("if one step is tricky most days, bring it to our next session and we'll work on it together.", leadIn = "For grown-ups:")
        }
    }
}

// ── Calm-down plan: choose, add your own, notice your body, then the plan ──

val CALM_CHOICES = listOf(
    Choice("Take 5 slow breaths", FmtIcons.breathe),
    Choice("Squeeze a cushion", FmtIcons.cushion),
    Choice("Push the wall", FmtIcons.pushWall),
    Choice("Have a drink of water", FmtIcons.water),
    Choice("Ask for a hug", FmtIcons.hug),
    Choice("Go to my quiet spot", FmtIcons.quietSpot),
)
const val CALM_PLAN_STEP = 4

@Composable
fun CalmFlow(store: AppStore, nav: Nav, step: Int) {
    val go = { s: Int -> nav.go(Route(Area.Calm, s)) }
    when (step) {
        0 -> FmtFlowScaffold(
            end = FmtAction("Start", ButtonKind.Teal, chevron = true) { go(1) },
        ) {
            FmtHead("My calm-down plan", swash = "calm-down", tag = "Feelings", tone = FmtCategories.tone("Feelings"))
            FmtKidText("Let's make a plan for when your feelings get big. It takes four short steps.")
            FmtInfoNote("make the plan together when your child is already calm, so it's easy to reach for when feelings run high.", leadIn = "For grown-ups:", tone = FmtCategories.tone("Feelings"))
        }
        1 -> FmtFlowScaffold(
            start = FmtAction("Back", ButtonKind.Outline) { go(0) },
            end = FmtAction("Next", ButtonKind.Teal, chevron = true, enabled = store.calmChosen.isNotEmpty()) { go(2) },
        ) {
            FmtGroup("When my feelings get big, I can…", inset = false) {
                FmtKidText("Tap the ones that help you.")
                FmtChoices(CALM_CHOICES, store.calmChosen.toSet(), onToggle = store::toggleCalm, tone = FmtCategories.tone("Feelings"))
            }
        }
        2 -> FmtFlowScaffold(
            start = FmtAction("Back", ButtonKind.Outline) { go(1) },
            end = FmtAction(if (store.calmIdea.isBlank()) "Skip" else "Next", ButtonKind.Teal, chevron = true) { go(3) },
        ) {
            FmtGroup("My own idea", inset = false) {
                FmtKidText("Is there something else that helps you?")
                WriteField(store.calmIdea, store::setCalmIdea, placeholder = "Something else that helps me")
            }
        }
        3 -> FmtFlowScaffold(
            start = FmtAction("Back", ButtonKind.Outline) { go(2) },
            end = FmtAction("See my plan", ButtonKind.Teal, chevron = true) { store.saveCalmPlan(); go(CALM_PLAN_STEP) },
        ) {
            FmtGroup("My body tells me my feelings are getting big when…", inset = false) {
                FmtKidText("What do you notice? Hot face, tight tummy, fast heart?")
                WriteField(store.calmBody, store::setCalmBody, placeholder = "My tummy feels…", minLines = 2)
            }
        }
        else -> FmtFlowScaffold(
            start = FmtAction("Change", ButtonKind.Outline) { go(1) },
            end = FmtAction("Use my plan now", ButtonKind.Teal, chevron = true) { nav.go(Route(Area.CalmNow)) },
        ) {
            FmtHead("My calm-down plan", swash = "calm-down", tag = "Feelings", tone = FmtCategories.tone("Feelings"))
            if (store.calmBody.isNotBlank()) {
                FmtGroup("I know my feelings are getting big when") { FmtKidText(store.calmBody) }
            }
            FmtGroup("Then I can") {
                store.calmChosen.sorted().forEach { i -> FmtListRow(CALM_CHOICES[i].label, icon = CALM_CHOICES[i].icon, tone = FmtCategories.tone("Feelings")) }
                if (store.calmIdea.isNotBlank()) FmtListRow(store.calmIdea, icon = FmtIcons.heart, tone = FmtCategories.tone("Feelings"))
            }
        }
    }
}

/** Using the plan in the moment: one strategy at a time, with a breathing guide. */
@Composable
fun CalmNowFlow(store: AppStore, nav: Nav, step: Int) {
    val items: List<Choice> = store.calmChosen.sorted().map { CALM_CHOICES[it] } + listOfNotNull(store.calmIdea.takeIf { it.isNotBlank() }?.let { Choice(it, FmtIcons.heart) })
    val go = { s: Int -> nav.go(Route(Area.CalmNow, s)) }
    if (items.isEmpty()) { CalmFlow(store, nav, 0); return }
    if (step >= items.size) {
        // Counted (once per visit) for the week's summary to Simone.
        LaunchedEffect(Unit) { store.calmUsed() }
        FmtFlowScaffold(
            start = FmtAction("Again", ButtonKind.Outline) { go(0) },
            end = FmtAction("Done", ButtonKind.Teal) { nav.home() },
            centre = true,
        ) {
            FmtCelebration("Well done", swash = "done", message = "You used your plan. Feelings come and go, and you know what helps.")
        }
        return
    }
    val c = items[step]
    FmtFlowScaffold(
        start = if (step > 0) FmtAction("Back", ButtonKind.Outline) { go(step - 1) } else null,
        end = FmtAction(if (step == items.size - 1) "I feel calmer" else "Try the next one", ButtonKind.Teal, chevron = true) { go(step + 1) },
        centre = true,
    ) {
        if (c.icon == FmtIcons.breathe) {
            FmtBreathing()
            FmtText("Take 5 slow breaths. Follow the circle.", FmtTheme.type.kid, align = TextAlign.Center)
        } else {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(FmtTheme.metrics.stack)) {
                c.icon?.let { FmtIconTile(it, FmtCategories.tone("Feelings")) }
                FmtText(c.label, FmtTheme.type.title, align = TextAlign.Center)
            }
        }
    }
}

// ── Practice week: set up the activities once, then a star a day ──

@Composable
fun WeekSetupFlow(store: AppStore, nav: Nav) {
    FmtFlowScaffold(
        end = FmtAction("Save", chevron = true, enabled = store.activities.isNotEmpty()) { store.finishWeekSetup(); nav.go(Route(Area.Week)) },
    ) {
        FmtHead("My practice week", swash = "week", tag = "Home practice", tone = FmtCategories.tone("Home practice"))
        FmtGroup("What are we practising?") {
            store.weekNames.forEachIndexed { i, name ->
                WriteField(name, { store.renameActivity(i, it) }, placeholder = "Activity ${i + 1}", kid = false)
            }
        }
        FmtInfoNote("write in the activities we chose together in our session. One or two is plenty to start with.", leadIn = "For grown-ups:", tone = FmtCategories.tone("Home practice"))
    }
}

@Composable
fun WeekFlow(store: AppStore, nav: Nav) {
    if (!store.weekSetUp) { WeekSetupFlow(store, nav); return }
    FmtFlowScaffold(
        start = FmtAction("Change activities", ButtonKind.Outline) { nav.go(Route(Area.WeekSetup)) },
        end = FmtAction("Done") { nav.home() },
    ) {
        FmtHead("My practice week", swash = "week", tag = "Home practice", tone = FmtCategories.tone("Home practice"))
        FmtGroup {
            FmtKidText("Tap a star each day you practise.")
            FmtTracker(store.weekNames, onRename = store::renameActivity, done = store.weekDone.toSet(), onToggle = store::togglePractice)
        }
        FmtGroup("This week I'm proud of…") { WriteField(store.weekProud, store::setProud, placeholder = "I'm proud that I…", minLines = 2) }
        FmtInfoNote("short and often beats long and rare. Bring this to our next session and we'll look at it together.", leadIn = "For grown-ups:", tone = FmtCategories.tone("Home practice"))
    }
}
