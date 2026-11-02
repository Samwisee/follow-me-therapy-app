package au.com.followmetherapy.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import au.com.followmetherapy.design.components.ButtonKind
import au.com.followmetherapy.design.components.FmtButton
import au.com.followmetherapy.design.components.FmtChip
import au.com.followmetherapy.design.components.FmtFlowScaffold
import au.com.followmetherapy.design.components.FmtGroup
import au.com.followmetherapy.design.components.FmtIconTile
import au.com.followmetherapy.design.components.FmtInfoNote
import au.com.followmetherapy.design.components.FmtProgressBar
import au.com.followmetherapy.design.components.FmtSectionTitle
import au.com.followmetherapy.design.components.FmtTag
import au.com.followmetherapy.design.components.FmtText
import au.com.followmetherapy.design.components.Tone
import au.com.followmetherapy.design.icons.FmtIcons
import au.com.followmetherapy.design.theme.FmtTheme
import au.com.followmetherapy.design.tokens.FmtBorder
import au.com.followmetherapy.design.tokens.FmtColors
import au.com.followmetherapy.design.tokens.FmtRadius
import au.com.followmetherapy.design.tokens.FmtSpace

/** A living page of the design system: the tokens at this window width and the parts. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesignSystemScreen(nav: Nav) {
    val m = FmtTheme.metrics
    val type = FmtTheme.type
    FmtFlowScaffold {
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
                FmtButton("Not yet", {}, enabled = false)
            }
        }

        FmtGroup("Chips, tags, tiles and progress") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(FmtSpace.s2), verticalArrangement = Arrangement.spacedBy(FmtSpace.s2)) {
                FmtChip("Ages 0–18", Tone.Purple, FmtIcons.smile)
                FmtChip("Telehealth", Tone.Teal, FmtIcons.video)
                FmtChip("Community sessions", Tone.Sun, FmtIcons.pin)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(FmtSpace.s2)) {
                FmtTag("Self-care"); FmtTag("Feelings", Tone.Teal); FmtTag("Writing", Tone.Sun); FmtTag("Fine motor", Tone.Green); FmtTag("Moving", Tone.Pink); FmtTag("Home practice", Tone.Blue)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(m.gridGap)) {
                FmtIconTile(FmtIcons.stairs, Tone.Teal)
                FmtIconTile(FmtIcons.blocks, Tone.Sun)
                FmtIconTile(FmtIcons.clipboard, Tone.Pink)
                FmtIconTile(FmtIcons.umbrella, Tone.Purple)
            }
            FmtProgressBar(0.6f, label = "60%")
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
