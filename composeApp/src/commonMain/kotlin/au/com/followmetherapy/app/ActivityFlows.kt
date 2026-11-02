package au.com.followmetherapy.app

import au.com.followmetherapy.design.tokens.FmtCategories
import androidx.compose.runtime.Composable
import au.com.followmetherapy.design.components.ButtonKind
import au.com.followmetherapy.design.components.CalendarDay
import au.com.followmetherapy.design.components.CalendarRow
import au.com.followmetherapy.design.components.FmtAction
import au.com.followmetherapy.design.components.FmtCelebration
import au.com.followmetherapy.design.components.FmtFlowScaffold
import au.com.followmetherapy.design.components.FmtGroup
import au.com.followmetherapy.design.components.FmtHead
import au.com.followmetherapy.design.components.FmtInfoNote
import au.com.followmetherapy.design.components.FmtKidText
import au.com.followmetherapy.design.components.FmtStepFocus
import au.com.followmetherapy.design.components.FmtStepList
import au.com.followmetherapy.design.components.FmtWeekCalendar
import au.com.followmetherapy.design.components.Tone
import au.com.followmetherapy.design.components.WriteField
import au.com.followmetherapy.design.icons.FmtIcons

// ── Step-by-step activities (as on their worksheets): an intro, one step a screen, a celebration ──

/** An activity done in steps, the same steps as its printed sheet. Drafts for Simone to check. */
class StepsActivity(
    val id: String, val title: String, val swash: String, val tag: String, val tone: Tone,
    val kidIntro: String, val steps: List<String>, val tip: String, val cheer: String,
)

val STEP_ACTIVITIES = listOf(
    StepsActivity(
        "dressing", "Getting dressed", "dressed", "Self-care", FmtCategories.tone("Self-care"),
        "Let's get dressed, one thing at a time.",
        listOf("Undies", "Pants or shorts", "Top", "Jumper", "Socks", "Shoes"),
        "lay the clothes out in this order and sit down to do it. Help with most of a step and let your child finish it on their own.",
        "You got dressed!",
    ),
    StepsActivity(
        "obstacle", "My obstacle course", "obstacle", "Moving", FmtCategories.tone("Moving"),
        "Say the steps out loud, then go!",
        listOf("Climb over the cushions", "Crawl under the blanket tunnel", "Walk along the line", "Jump to the end!"),
        "build it from what's at home: cushions, a blanket over two chairs and a line of tape. Clear the space first, and let your child say the steps before they start.",
        "You did the whole course!",
    ),
)

/** The route's step: which activity (× 20) and where in it (0 intro, 1… a step, then the celebration). */
fun stepsRoute(activity: Int, at: Int = 0) = Route(Area.Steps, activity * 20 + at)

@Composable
fun StepsFlow(store: AppStore, nav: Nav, step: Int) {
    val a = STEP_ACTIVITIES.getOrNull(step / 20) ?: STEP_ACTIVITIES.first()
    val idx = STEP_ACTIVITIES.indexOf(a)
    val at = step % 20
    val n = a.steps.size
    val done = store.stepsDone(a.id)
    val go = { s: Int -> nav.go(stepsRoute(idx, s)) }
    when {
        at == 0 -> FmtFlowScaffold(end = FmtAction("Start", chevron = true) { go(1) }) {
            FmtHead(a.title, swash = a.swash, tag = a.tag, tone = a.tone)
            FmtGroup {
                FmtKidText(a.kidIntro)
                FmtStepList(a.steps, done, onToggle = { store.toggleStep(a.id, it) })
            }
            FmtInfoNote(a.tip, leadIn = "For grown-ups:", tone = a.tone)
        }
        at <= n -> {
            val i = at - 1
            val isDone = i in done
            val next = { if (store.stepsDone(a.id).size >= n) go(n + 1) else go(at + 1) }
            FmtFlowScaffold(
                start = FmtAction("Back", ButtonKind.Outline) { go(at - 1) },
                end = if (isDone) FmtAction("Next", chevron = true) { next() }
                else FmtAction("Done", ButtonKind.Teal, chevron = true) { store.toggleStep(a.id, i, true); next() },
                centre = true,
            ) { FmtStepFocus(at, n, a.steps[i], isDone) }
        }
        else -> FmtFlowScaffold(end = FmtAction("Finish") { nav.tab(2) }, centre = true) {
            FmtCelebration("All done!", swash = "done", message = a.cheer)
        }
    }
}

// ── Catch and throw: a week of stars for four ball skills ──

val BALL_SKILLS = listOf("Roll and catch", "Throw and catch", "Bounce and catch", "Catch with one hand")

@Composable
fun BallFlow(store: AppStore, nav: Nav) {
    FmtFlowScaffold(end = FmtAction("Done") { nav.tab(2) }) {
        FmtHead("Catch and throw", swash = "throw", tag = "Moving", tone = FmtCategories.tone("Moving"))
        FmtGroup {
            FmtKidText("Tap a star each day you practise.")
            FmtWeekCalendar(
                BALL_SKILLS.mapIndexed { r, name ->
                    CalendarRow(name, FmtCategories.tone("Moving"), (0..6).map { d -> if (store.ballDone(r, d)) CalendarDay.Done else CalendarDay.Planned },
                        onToggle = { d -> store.toggleBall(r, d) })
                },
                (0..6).map { Dates.add(store.weekStart, it).substringAfterLast('-').trimStart('0') },
                today = store.weekday,
            )
        }
        FmtGroup("My best score: catches in a row", inset = false) {
            WriteField(store.ballBest, store::setBallBest, placeholder = "How many?")
        }
        FmtInfoNote(
            "start with a big, soft ball, close together, then a smaller ball and further apart as it gets easier. Count catches together and try to beat the score.",
            leadIn = "For grown-ups:", tone = FmtCategories.tone("Moving"), icon = FmtIcons.ball,
        )
    }
}
