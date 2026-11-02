package au.com.followmetherapy.design.components

import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.Dp
import au.com.followmetherapy.design.resources.Res
import au.com.followmetherapy.design.resources.fmt_mark
import au.com.followmetherapy.design.theme.FmtTheme
import au.com.followmetherapy.design.tokens.FmtColors
import au.com.followmetherapy.design.tokens.FmtSpace
import org.jetbrains.compose.resources.painterResource

/** The mark (adult and child holding hands) in brand-ink, the same token as the name. */
@Composable
fun FmtMark(height: Dp, modifier: Modifier = Modifier, label: String? = null) {
    Image(
        painter = painterResource(Res.drawable.fmt_mark),
        contentDescription = label,
        colorFilter = ColorFilter.tint(FmtColors.brandInk),
        modifier = modifier.height(height),
    )
}

/**
 * The mark beside the practice name at weight-brand; the mark is 1.45× the
 * text size. Always one line; [compact] steps the words down to text-lg.
 */
@Composable
fun FmtLockup(modifier: Modifier = Modifier, compact: Boolean = false) {
    val style = if (compact) FmtTheme.type.brand.copy(fontSize = FmtTheme.metrics.textLg, lineHeight = FmtTheme.metrics.textLg * 1.1f) else FmtTheme.type.brand
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(FmtSpace.s2), verticalAlignment = Alignment.CenterVertically) {
        FmtMark(Dp(style.fontSize.value * 1.45f))
        BasicText("Follow Me Therapy", style = style, maxLines = 1, softWrap = false)
    }
}

/**
 * The lockup with its descriptor (the website's lockup-horizontal): the mark,
 * then Follow Me Therapy over Paediatric Occupational Therapy. The two lines are
 * one width (the descriptor's size is fitted to the name) and together the
 * mark's height, as the Design System's Logo rule says.
 */
@Composable
fun FmtLockupFull(modifier: Modifier = Modifier, label: String = "Follow Me Therapy, Paediatric Occupational Therapy") {
    val name = FmtTheme.type.brand
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val descriptorBase = FmtTheme.type.small.copy(fontWeight = FontWeight.SemiBold, color = FmtColors.brandInk)
    val sizes = remember(name, descriptorBase) {
        val n = measurer.measure("Follow Me Therapy", name, maxLines = 1)
        val d = measurer.measure("Paediatric Occupational Therapy", descriptorBase, maxLines = 1)
        val fitted = descriptorBase.copy(fontSize = descriptorBase.fontSize * (n.size.width.toFloat() / d.size.width), lineHeight = descriptorBase.fontSize * (n.size.width.toFloat() / d.size.width))
        val d2 = measurer.measure("Paediatric Occupational Therapy", fitted, maxLines = 1)
        fitted to with(density) { (n.size.height + d2.size.height).toDp() }
    }
    Row(modifier.semantics(mergeDescendants = true) { contentDescription = label }, horizontalArrangement = Arrangement.spacedBy(FmtSpace.s2), verticalAlignment = Alignment.CenterVertically) {
        FmtMark(sizes.second)
        Column {
            BasicText("Follow Me Therapy", style = name, maxLines = 1, softWrap = false)
            BasicText("Paediatric Occupational Therapy", style = sizes.first, maxLines = 1, softWrap = false)
        }
    }
}
