package au.com.followmetherapy.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import au.com.followmetherapy.design.components.ButtonKind
import au.com.followmetherapy.design.components.Choice
import au.com.followmetherapy.design.components.FmtBand
import au.com.followmetherapy.design.components.FmtButton
import au.com.followmetherapy.design.components.FmtCard
import au.com.followmetherapy.design.components.FmtChip
import au.com.followmetherapy.design.components.FmtChoices
import au.com.followmetherapy.design.components.FmtGroup
import au.com.followmetherapy.design.components.FmtHead
import au.com.followmetherapy.design.components.FmtIconTile
import au.com.followmetherapy.design.components.FmtInfoNote
import au.com.followmetherapy.design.components.FmtKidText
import au.com.followmetherapy.design.components.FmtPage
import au.com.followmetherapy.design.components.FmtScreen
import au.com.followmetherapy.design.components.FmtSectionTitle
import au.com.followmetherapy.design.components.FmtStepList
import au.com.followmetherapy.design.components.FmtSwashTitle
import au.com.followmetherapy.design.components.FmtTag
import au.com.followmetherapy.design.components.FmtText
import au.com.followmetherapy.design.components.FmtTracker
import au.com.followmetherapy.design.components.FmtWave
import au.com.followmetherapy.design.components.Tone
import au.com.followmetherapy.design.components.WriteField
import au.com.followmetherapy.design.icons.FmtIcons
import au.com.followmetherapy.design.theme.FmtTheme
import au.com.followmetherapy.design.tokens.FmtBorder
import au.com.followmetherapy.design.tokens.FmtColors
import au.com.followmetherapy.design.tokens.FmtMotion
import au.com.followmetherapy.design.tokens.FmtRadius
import au.com.followmetherapy.design.tokens.FmtSpace

private enum class Screen { Home, Morning, Calm, Week, DesignSystem }

private val Screen.slug: String
    get() = when (this) {
        Screen.Home -> ""
        Screen.Morning -> "morning"
        Screen.Calm -> "calm"
        Screen.Week -> "week"
        Screen.DesignSystem -> "design-system"
    }

private fun screenFor(slug: String) = Screen.entries.firstOrNull { it.slug == slug } ?: Screen.Home

/**
 * [startAt] is the screen to open first (from the web address, so a link can
 * go straight to one activity); [onNavigate] reports each screen's slug.
 */
@Composable
fun App(startAt: String = "", onNavigate: (String) -> Unit = {}) {
    FmtTheme {
        var screen by rememberSaveable { mutableStateOf(screenFor(startAt)) }
        val go: (Screen) -> Unit = { screen = it; onNavigate(it.slug) }
        val home = { go(Screen.Home) }
        // Moving between screens cross-fades with a small rise, as pages do on the website.
        AnimatedContent(
            targetState = screen,
            modifier = Modifier.fillMaxSize().background(FmtColors.paper),
            transitionSpec = {
                (fadeIn(tween(FmtMotion.durSlower, easing = FmtMotion.easeOut)) +
                    slideInVertically(tween(FmtMotion.durSlower, easing = FmtMotion.easeOut)) { it / 40 }) togetherWith
                    fadeOut(tween(FmtMotion.durBase, easing = FmtMotion.easeStandard))
            },
        ) { s ->
            FmtPage(key = s, back = if (s == Screen.Home) null else home, backLabel = "All activities", heroBelow = s == Screen.Home) {
                when (s) {
                    Screen.Home -> HomeScreen(open = go)
                    Screen.Morning -> MorningRoutineScreen()
                    Screen.Calm -> CalmPlanScreen()
                    Screen.Week -> PracticeWeekScreen()
                    Screen.DesignSystem -> DesignSystemScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeScreen(open: (Screen) -> Unit) {
    val m = FmtTheme.metrics
    // The hero: lilac, like the website's top band.
    FmtBand(FmtColors.purpleTint) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(m.groupGap)) {
            FmtSwashTitle("Activities for home", swash = "home", style = FmtTheme.type.title.copy(textAlign = TextAlign.Center))
            FmtText("Small, everyday practice between our sessions, for you and your child to do together.", FmtTheme.type.lead, align = TextAlign.Center)
        }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FmtSpace.s2, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(FmtSpace.s2)) {
            FmtChip("Ages 0–18", Tone.Purple, FmtIcons.smile)
            FmtChip("Telehealth Australia-wide", Tone.Teal, FmtIcons.video)
        }
    }
    // The white band rises into the lilac hero in the website's wave.
    FmtWave(FmtColors.paper, Modifier.background(FmtColors.purpleTint))
    FmtScreen {
        FmtSectionTitle("Choose an activity")
        Column(verticalArrangement = Arrangement.spacedBy(m.gridGap)) {
            FmtCard("My morning routine", "Six steps from waking up to out the door. Tap each one when it's done.", FmtIcons.sunrise, Tone.Purple, more = "Start", onClick = { open(Screen.Morning) })
            FmtCard("My calm-down plan", "Choose the things that help when feelings get big, so they're easy to reach for.", FmtIcons.breathe, Tone.Teal, more = "Start", onClick = { open(Screen.Calm) })
            FmtCard("My practice week", "Write in the activities we chose together and tap a star each day you practise.", FmtIcons.calendar, Tone.Sun, more = "Start", onClick = { open(Screen.Week) })
        }
        FmtInfoNote(
            "these are the same activities as my printed worksheets. Use whichever suits your family: paper on the fridge, or this on a phone or tablet. Nothing you enter here leaves your device.",
            leadIn = "For grown-ups:",
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            FmtButton("See the design system", { open(Screen.DesignSystem) }, kind = ButtonKind.Outline, trailingChevron = true)
        }
    }
}

@Composable
private fun MorningRoutineScreen() {
    val done = remember { mutableStateListOf<Int>() }
    var tricky by remember { mutableStateOf("") }
    val steps = listOf("Toilet and wash hands", "Get dressed", "Eat breakfast", "Brush teeth", "Shoes on", "Bag on, out the door")
    FmtScreen {
        FmtHead("My morning routine", swash = "morning", tag = "Self-care", tone = Tone.Purple)
        FmtGroup {
            FmtKidText(if (done.size == steps.size) "All done. Great job!" else "Tap each step when it's done.")
            FmtStepList(steps, done.toSet(), onToggle = { if (it in done) done.remove(it) else done.add(it) })
        }
        FmtGroup("What was tricky today?") {
            WriteField(tricky, { tricky = it }, placeholder = "Type it, or tell a grown-up", minLines = 2)
        }
        FmtInfoNote(
            "keep this where your child gets ready. Point to the next step instead of saying it, and let them do the tapping. Change the steps to match your mornings.",
            leadIn = "For grown-ups:",
        )
    }
}

@Composable
private fun CalmPlanScreen() {
    val chosen = remember { mutableStateListOf<Int>() }
    var idea by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    val choices = listOf(
        Choice("Take 5 slow breaths", FmtIcons.breathe),
        Choice("Squeeze a cushion", FmtIcons.cushion),
        Choice("Push the wall", FmtIcons.pushWall),
        Choice("Have a drink of water", FmtIcons.water),
        Choice("Ask for a hug", FmtIcons.hug),
        Choice("Go to my quiet spot", FmtIcons.quietSpot),
    )
    FmtScreen {
        FmtHead("My calm-down plan", swash = "calm-down", tag = "Feelings", tone = Tone.Teal)
        FmtGroup("When my feelings get big, I can…") {
            FmtKidText("Tap the ones that help you.")
            FmtChoices(choices, chosen.toSet(), onToggle = { if (it in chosen) chosen.remove(it) else chosen.add(it) }, tone = Tone.Teal)
        }
        FmtGroup("My own idea") { WriteField(idea, { idea = it }, placeholder = "Something else that helps me") }
        FmtGroup("My body tells me my feelings are getting big when…") {
            WriteField(body, { body = it }, placeholder = "My tummy feels…", minLines = 2)
        }
        FmtInfoNote(
            "practise these together when your child is already calm, so they're easy to reach for when feelings run high. Try one strategy with them rather than talking them through it.",
            leadIn = "For grown-ups:", tone = Tone.Teal,
        )
    }
}

@Composable
private fun PracticeWeekScreen() {
    val names = remember { mutableStateListOf("", "", "", "") }
    val done = remember { mutableStateListOf<Pair<Int, Int>>() }
    var proud by remember { mutableStateOf("") }
    FmtScreen {
        FmtHead("My practice week", swash = "week", tag = "Home practice", tone = Tone.Sun)
        FmtGroup {
            FmtKidText("Tap a star each day you practise.")
            FmtTracker(names, onRename = { i, v -> names[i] = v }, done = done.toSet(), onToggle = { r, d -> if ((r to d) in done) done.remove(r to d) else done.add(r to d) })
        }
        FmtGroup("This week I'm proud of…") { WriteField(proud, { proud = it }, placeholder = "I'm proud that I…", minLines = 2) }
        FmtInfoNote(
            "write in the activities we chose together. Short and often beats long and rare, and five minutes a day is plenty. Bring this to our next session and we'll look at it together.",
            leadIn = "For grown-ups:", tone = Tone.Sun,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DesignSystemScreen() {
    val m = FmtTheme.metrics
    val type = FmtTheme.type
    FmtScreen {
        FmtSectionTitle("Design system", subtitle = "The same tokens and components as the website and worksheets, in Compose.")

        FmtGroup("Colour") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(m.gridGap), verticalArrangement = Arrangement.spacedBy(m.gridGap)) {
                listOf(
                    "purple" to FmtColors.purple, "teal" to FmtColors.teal, "sun" to FmtColors.sun, "pink" to FmtColors.pink,
                    "ink" to FmtColors.ink, "ink-soft" to FmtColors.inkSoft, "deep" to FmtColors.deep, "rule" to FmtColors.rule,
                    "purple-tint" to FmtColors.purpleTint, "teal-tint" to FmtColors.tealTint, "butter" to FmtColors.butter, "pink-tint" to FmtColors.pinkTint,
                ).forEach { (name, c) -> Swatch(name, c) }
            }
        }

        FmtGroup("Type, at this window width (${m.widthDp.toInt()}dp)") {
            FmtText("Title, ${m.text4xl.value.toInt()}sp", type.title)
            FmtText("Heading, ${m.text3xl.value.toInt()}sp", type.heading)
            FmtText("Lead line, ${m.textLg.value.toInt()}sp", type.lead)
            FmtText("Body text for parents, ${m.textBase.value.toInt()}sp. Warm, plain and practical.", type.body)
            FmtText("Words a child reads, ${m.textKid.value.toInt()}sp", type.kid)
        }

        FmtGroup("Spacing rhythm") {
            listOf("group-gap" to m.groupGap, "grid-gap" to m.gridGap, "card-pad" to m.cardPad, "stack" to m.stack, "gutter" to m.gutter).forEach { (n, d) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FmtSpace.s3)) {
                    Box(Modifier.width(d).height(FmtSpace.s4).background(FmtColors.purpleLine, RoundedCornerShape(FmtRadius.sm)))
                    FmtText("$n · ${d.value.toInt()}dp", type.small)
                }
            }
        }

        FmtGroup("Buttons") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(m.gridGap), verticalArrangement = Arrangement.spacedBy(m.gridGap)) {
                FmtButton("Start a referral", {}, kind = ButtonKind.Primary)
                FmtButton("Ask a question", {}, kind = ButtonKind.Teal)
                FmtButton("Learn more", {}, kind = ButtonKind.Outline, trailingChevron = true)
            }
        }

        FmtGroup("Chips, tags and tiles") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(FmtSpace.s2), verticalArrangement = Arrangement.spacedBy(FmtSpace.s2)) {
                FmtChip("Ages 0–18", Tone.Purple, FmtIcons.smile)
                FmtChip("Telehealth", Tone.Teal, FmtIcons.video)
                FmtChip("Community sessions", Tone.Sun, FmtIcons.pin)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(FmtSpace.s2)) {
                FmtTag("Self-care"); FmtTag("Feelings", Tone.Teal); FmtTag("Fine motor", Tone.Sun); FmtTag("Assessments", Tone.Pink)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(m.gridGap)) {
                FmtIconTile(FmtIcons.stairs, Tone.Teal)
                FmtIconTile(FmtIcons.blocks, Tone.Sun)
                FmtIconTile(FmtIcons.clipboard, Tone.Pink)
                FmtIconTile(FmtIcons.umbrella, Tone.Purple)
            }
        }

        FmtInfoNote("every container with its own corners wears an outline in its colour, and its corner nests: radius-tile + card-pad + outline.", leadIn = "Shape:")
    }
}

@Composable
private fun Swatch(name: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(FmtSpace.s1)) {
        Box(Modifier.size(FmtSpace.s16).background(color, RoundedCornerShape(FmtRadius.tile)).border(FmtBorder.outline, FmtColors.purpleLine, RoundedCornerShape(FmtRadius.tile)))
        FmtText(name, FmtTheme.type.small.copy(fontSize = FmtTheme.metrics.textXs))
    }
}
