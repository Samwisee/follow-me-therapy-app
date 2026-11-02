package au.com.followmetherapy.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import au.com.followmetherapy.design.components.ButtonKind
import au.com.followmetherapy.design.components.FmtAction
import au.com.followmetherapy.design.components.FmtFilterChip
import au.com.followmetherapy.design.components.FmtFlowScaffold
import au.com.followmetherapy.design.components.FmtGroup
import au.com.followmetherapy.design.components.FmtHead
import au.com.followmetherapy.design.components.FmtInfoNote
import au.com.followmetherapy.design.components.FmtKidText
import au.com.followmetherapy.design.components.FmtLetterTrace
import au.com.followmetherapy.design.components.Tone
import au.com.followmetherapy.design.letters.FmtLetters
import au.com.followmetherapy.design.tokens.FmtSpace

// ── Letters and shapes: trace with a finger (Ready to write, a–z, A–Z) ──
// The step says what's being traced: 0 the picker, 1–6 the shapes, 11–36 a–z, 41–66 A–Z.

private val SHAPE_WORDS = mapOf("down" to "Down", "across" to "Across", "circle" to "Circle", "cross" to "Cross", "mountains" to "Mountains", "loops" to "Loops")
private val SHAPES: List<String> get() = FmtLetters.shapes.keys.toList()
private val LOWER: List<String> get() = FmtLetters.lowercase.keys.toList()
private val CAPS: List<String> get() = FmtLetters.capitals.keys.toList()

/** What a step traces, and the step after it (the next in its set, or back to the picker). */
fun letterAt(step: Int): String? = when (step) {
    in 1..SHAPES.size -> SHAPES[step - 1]
    in 11..(10 + LOWER.size) -> LOWER[step - 11]
    in 41..(40 + CAPS.size) -> CAPS[step - 41]
    else -> null
}
private fun nextStep(step: Int): Int = when {
    step < SHAPES.size || (step in 11 until 10 + LOWER.size) || (step in 41 until 40 + CAPS.size) -> step + 1
    else -> 0
}
fun letterTitle(step: Int): String = letterAt(step)?.let { SHAPE_WORDS[it] ?: it } ?: "Letters and shapes"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LettersFlow(store: AppStore, nav: Nav, step: Int) {
    val go = { s: Int -> nav.go(Route(Area.Letters, s)) }
    val name = letterAt(step)
    if (name == null) {
        FmtFlowScaffold {
            FmtHead("Letters and shapes", swash = "Letters", tag = "Writing", tone = Tone.Sun)
            FmtGroup { FmtKidText("Pick one, then trace it with your finger.") }
            FmtGroup("Ready to write") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(FmtSpace.s2), verticalArrangement = Arrangement.spacedBy(FmtSpace.s2)) {
                    SHAPES.forEachIndexed { i, s -> FmtFilterChip(SHAPE_WORDS[s] ?: s, store.letterTraced(s), { go(i + 1) }, tone = Tone.Sun) }
                }
            }
            FmtGroup("Small letters") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(FmtSpace.s2), verticalArrangement = Arrangement.spacedBy(FmtSpace.s2)) {
                    LOWER.forEachIndexed { i, s -> FmtFilterChip(s, store.letterTraced(s), { go(11 + i) }, tone = Tone.Sun, label = "Letter $s") }
                }
            }
            FmtGroup("Capital letters") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(FmtSpace.s2), verticalArrangement = Arrangement.spacedBy(FmtSpace.s2)) {
                    CAPS.forEachIndexed { i, s -> FmtFilterChip(s, store.letterTraced(s), { go(41 + i) }, tone = Tone.Sun, label = "Capital $s") }
                }
            }
            FmtInfoNote(
                "the letters are the shapes Victorian schools teach. Show your child where the gold dot is, and let them go slowly: following the line matters more than speed. The same letters are on my printed sheets.",
                leadIn = "For grown-ups:", tone = Tone.Sun,
            )
        }
        return
    }
    var done by remember(step) { mutableStateOf(false) }
    FmtFlowScaffold(
        start = FmtAction("All letters", ButtonKind.Outline) { go(0) },
        end = FmtAction(if (nextStep(step) == 0) "Finish" else "Next", chevron = true, kind = if (done) ButtonKind.Teal else ButtonKind.Primary) { go(nextStep(step)) },
        centre = true,
    ) {
        FmtKidText(if (done) "You did it!" else "Start at the gold dot and follow the line.")
        FmtLetterTrace(name, onDone = { done = true; store.markLetterTraced(name) })
    }
}
