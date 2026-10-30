package au.com.followmetherapy.app

import androidx.compose.runtime.Composable
import au.com.followmetherapy.design.components.FmtAction
import au.com.followmetherapy.design.components.FmtFlowScaffold
import au.com.followmetherapy.design.components.FmtGroup
import au.com.followmetherapy.design.components.FmtHead
import au.com.followmetherapy.design.components.FmtInfoNote
import au.com.followmetherapy.design.components.FmtText
import au.com.followmetherapy.design.components.FmtTickList
import au.com.followmetherapy.design.components.Tone
import au.com.followmetherapy.design.theme.FmtTheme

/**
 * How to do each home-practice activity from the library (ACTIVITY_LIBRARY),
 * in Simone's voice: a few steps for grown-ups and a line on why it helps.
 * Drafts for Simone to check and change.
 */
data class Task(val name: String, val steps: List<String>, val why: String)

val TASKS = listOf(
    Task("Cutting practice", listOf(
        "Use child-safe scissors and thick card or paper.",
        "Show the thumb on top: \"thumbs up\" while cutting.",
        "Start by snipping fringes, then cut along thick straight lines, then curves.",
        "Their other hand holds and turns the paper.",
    ), "It builds hand strength and both hands working together, which helps with drawing and getting dressed too."),
    Task("Ball skills", listOf(
        "Use a big, soft ball to start, then a smaller one.",
        "Roll it back and forth sitting on the floor, then throw and catch standing up.",
        "Count catches together and try to beat your score.",
        "Stand a little further apart as it gets easier.",
    ), "It practises watching, timing and moving together, which helps with playground games and sport."),
    Task("Getting dressed", listOf(
        "Pick one piece of clothing to practise, like socks or a jumper.",
        "Sit down to do it: it's easier to balance.",
        "Do most of it together, and let your child do the last step on their own.",
        "As they get it, let them do more of the steps.",
    ), "Doing the last step themselves builds confidence and a feeling of 'I did it', one step at a time."),
    Task("Handwriting practice", listOf(
        "Keep it short: five minutes is plenty.",
        "Sit with feet flat on the floor and the paper slightly tilted.",
        "Trace big letters first, then copy them, then write a few words.",
        "Praise the effort, not just the neatness.",
    ), "Short, regular practice builds the habit and the small hand movements behind writing."),
    Task("Threading beads", listOf(
        "Start with big beads and a stiff lace or pipe cleaner.",
        "Hold the lace near its end and push it through, then pull it out the other side.",
        "Make a pattern together, like two red, one blue.",
        "Move to smaller beads as it gets easier.",
    ), "It practises pinching and both hands working together for jobs like buttons and zips."),
    Task("Playdough squeeze", listOf(
        "Squeeze, roll and pinch a ball of playdough.",
        "Roll snakes, make balls, and pinch out little pieces.",
        "Hide small beads inside and dig them out with fingers.",
        "A few minutes before writing or cutting works well.",
    ), "It builds strength in the small muscles of the hand, which helps with pencils, scissors and fasteners."),
    Task("Animal walks", listOf(
        "Clear a space and take turns being animals.",
        "Try a bear walk (hands and feet), a crab walk, a frog jump and a flamingo stand.",
        "Go from one side of the room to the other, or along a hallway.",
        "Make it a race or a game of follow the leader.",
    ), "It builds strength, balance and body awareness, and it's a fun way to move before sitting down."),
    Task("Obstacle course", listOf(
        "Use cushions, chairs, a blanket tunnel and a line of tape.",
        "Climb over, crawl under, step along the line and jump to the end.",
        "Say the steps out loud before starting: \"over, under, along, jump\".",
        "Let your child design the next course.",
    ), "It practises planning and following a sequence of movements, along with balance and coordination."),
    Task("Shoelaces", listOf(
        "Practise on a shoe on the table first, not on the foot.",
        "Use two different coloured laces so each side is easy to follow.",
        "Teach one step at a time, like the first cross and tuck.",
        "Say the same words each time so they become a little script.",
    ), "Breaking it into small steps, said the same way each time, makes a tricky job learnable."),
    Task("Using a spoon and fork", listOf(
        "Practise at a calm meal or with play food.",
        "Start with foods that stay on easily, like yoghurt on a spoon or pieces on a fork.",
        "Hold the spoon or fork with fingers, not a fist, if they can.",
        "Expect some mess: it's part of learning.",
    ), "It builds independence at meals and the hand control that goes with it."),
)

fun taskIndex(name: String): Int? = TASKS.indexOfFirst { it.name.equals(name.trim(), ignoreCase = true) }.takeIf { it >= 0 }

/** One activity explained: what to do, why it helps, and the practice week to tap a star. */
@Composable
fun TaskScreen(nav: Nav, step: Int) {
    val t = TASKS.getOrNull(step) ?: TASKS.first()
    FmtFlowScaffold(end = FmtAction("Tap a star", chevron = true) { nav.go(Route(Area.Week)) }) {
        FmtHead(t.name, swash = null, tag = "Home practice", tone = Tone.Sun)
        FmtGroup("What to do") { FmtTickList(t.steps) }
        FmtGroup("Why it helps") { FmtText(t.why, FmtTheme.type.body) }
        FmtInfoNote("a few minutes most days beats a long go once a week. Stop while it's still fun.", leadIn = "For grown-ups:", tone = Tone.Sun)
    }
}
