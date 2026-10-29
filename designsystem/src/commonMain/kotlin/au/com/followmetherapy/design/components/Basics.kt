package au.com.followmetherapy.design.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.followmetherapy.design.icons.FmtIcon
import au.com.followmetherapy.design.icons.FmtIcons
import au.com.followmetherapy.design.theme.FmtTheme
import au.com.followmetherapy.design.tokens.FmtBorder
import au.com.followmetherapy.design.tokens.FmtColors
import au.com.followmetherapy.design.tokens.FmtMotion
import au.com.followmetherapy.design.tokens.FmtShadowToken
import au.com.followmetherapy.design.tokens.FmtShadow
import au.com.followmetherapy.design.tokens.FmtSize
import au.com.followmetherapy.design.tokens.FmtSpace

/** The four tones. Fixed meanings: purple = who it's for, teal = telehealth, sun = in person, pink = reports. */
enum class Tone { Purple, Teal, Sun, Pink }

/** A tone's colours: solid, ink (text on its tint), tint (ground) and line (outline). */
data class ToneColors(val solid: Color, val ink: Color, val tint: Color, val line: Color, val icon: Color)

val Tone.colors: ToneColors
    get() = when (this) {
        Tone.Purple -> ToneColors(FmtColors.purple, FmtColors.purpleInk, FmtColors.purpleTint, FmtColors.purpleLine, FmtColors.purple)
        Tone.Teal -> ToneColors(FmtColors.teal, FmtColors.tealInk, FmtColors.tealTint, FmtColors.tealLine, FmtColors.teal)
        Tone.Sun -> ToneColors(FmtColors.sun, FmtColors.sunInk, FmtColors.sunTint, FmtColors.sunLine, FmtColors.sunDeep)
        Tone.Pink -> ToneColors(FmtColors.pinkDeep, FmtColors.pinkDeep, FmtColors.pinkTint, FmtColors.pinkLine, FmtColors.pink)
    }

/** Plain text in a design-system style. */
@Composable
fun FmtText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    align: TextAlign = TextAlign.Unspecified,
) {
    BasicText(text, modifier, style.copy(color = if (color == Color.Unspecified) style.color else color, textAlign = align))
}

@Composable
fun FmtText(
    text: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    align: TextAlign = TextAlign.Unspecified,
) {
    BasicText(text, modifier, style.copy(color = if (color == Color.Unspecified) style.color else color, textAlign = align))
}

/** A soft shadow from a shadow token (screen only; on paper, separate with tint or an outline). */
fun Modifier.fmtShadow(token: FmtShadowToken, shape: Shape): Modifier =
    shadow(elevation = (token.blur / 2f).dp, shape = shape, clip = false, ambientColor = token.color, spotColor = token.color)

/**
 * True while focused from the keyboard only, like the website's
 * :focus-visible: a mouse click or a tap focuses without drawing the ring.
 */
@Composable
fun MutableInteractionSource.collectIsFocusVisibleAsState(): State<Boolean> {
    val focused = collectIsFocusedAsState()
    val mode = LocalInputModeManager.current
    return remember(focused, mode) { derivedStateOf { focused.value && mode.inputMode == InputMode.Keyboard } }
}

/**
 * The focus ring: border-focus wide in `focus`, offset by the same amount,
 * on everything interactive (WCAG 2.2 AAA focus appearance).
 */
fun Modifier.fmtFocusRing(focused: Boolean, shape: Shape): Modifier = drawBehind {
    if (!focused) return@drawBehind
    val w = FmtBorder.focus.toPx()
    val off = w * 1.5f
    val outline = shape.createOutline(Size(size.width + off * 2, size.height + off * 2), layoutDirection, this)
    translate(-off, -off) { drawOutline(outline, FmtColors.focus, style = Stroke(w)) }
}

enum class ButtonKind { Primary, Teal, Outline, OutlineTeal, Quiet }

/**
 * A pill button, size-button (52dp) tall. Primary is purple (Start a referral),
 * Teal is Ask a question. Lifts on hover, springs on press, focus ring on focus.
 * Pass [shape] to nest it in a set (radius-tile instead of a pill).
 */
@Composable
fun FmtButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    shape: Shape = CircleShape,
    trailingChevron: Boolean = false,
    enabled: Boolean = true,
) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusVisibleAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(FmtMotion.durBase, easing = FmtMotion.easeSpring))
    val lift by animateFloatAsState(if (hovered && !pressed) 1f else 0f, tween(FmtMotion.durBase, easing = FmtMotion.easeStandard))

    val (bg, fg, line) = if (!enabled) Triple(FmtColors.paper, FmtColors.inkSoft, FmtColors.purpleLine) else when (kind) {
        ButtonKind.Primary -> Triple(FmtColors.purple, FmtColors.paper, FmtColors.purple)
        ButtonKind.Teal -> Triple(FmtColors.teal, FmtColors.paper, FmtColors.teal)
        ButtonKind.Outline -> Triple(if (hovered) FmtColors.purpleTint else FmtColors.paper, FmtColors.purple, FmtColors.purple)
        ButtonKind.OutlineTeal -> Triple(if (hovered) FmtColors.tealTint else FmtColors.paper, FmtColors.tealInk, FmtColors.teal)
        ButtonKind.Quiet -> Triple(FmtColors.sunTint, FmtColors.ink, FmtColors.sunLine)
    }
    val shadow = if (!enabled) null else when (kind) {
        ButtonKind.Primary -> FmtShadow.glowPurple
        ButtonKind.Teal -> FmtShadow.glowTeal
        else -> null
    }
    Row(
        modifier = modifier
            .scale(scale)
            .padding(top = (2 * (1 - lift)).dp, bottom = (2 * lift).dp)
            .fmtFocusRing(focused, shape)
            .then(if (shadow != null) Modifier.fmtShadow(shadow, shape) else Modifier)
            .background(bg, shape)
            .border(FmtBorder.outline, line, shape)
            .hoverable(source)
            .clickable(interactionSource = source, indication = null, role = Role.Button, enabled = enabled, onClick = onClick)
            .heightIn(min = FmtSize.button)
            .padding(horizontal = FmtSpace.s6, vertical = FmtSpace.s2),
        horizontalArrangement = Arrangement.spacedBy(FmtSpace.s2, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FmtText(text, FmtTheme.type.button, color = fg, align = TextAlign.Center)
        if (trailingChevron) FmtIcon(FmtIcons.chevron, fg, FmtSize.iconSm)
    }
}

/** A small solid label above a title ("Self-care"). Sun takes ink text; the others white. */
@Composable
fun FmtTag(text: String, tone: Tone = Tone.Purple, modifier: Modifier = Modifier) {
    val bg = when (tone) {
        Tone.Purple -> FmtColors.purple
        Tone.Teal -> FmtColors.teal
        Tone.Sun -> FmtColors.sun
        Tone.Pink -> FmtColors.pinkDeep
    }
    val fg = if (tone == Tone.Sun) FmtColors.ink else FmtColors.paper
    Box(modifier.background(bg, CircleShape).padding(horizontal = FmtSpace.s3, vertical = FmtSpace.s1)) {
        FmtText(text, FmtTheme.type.chip.copy(fontSize = FmtTheme.metrics.textXs), color = fg)
    }
}

/** A bordered pastel pill that states one fact. Say it in words; the colour only reinforces. */
@Composable
fun FmtChip(text: String, tone: Tone = Tone.Purple, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, modifier: Modifier = Modifier) {
    val c = tone.colors
    Row(
        modifier
            .heightIn(min = FmtSize.target)
            .background(c.tint, CircleShape)
            .border(FmtBorder.outline, c.line, CircleShape)
            .padding(horizontal = FmtSpace.s4, vertical = FmtSpace.s2),
        horizontalArrangement = Arrangement.spacedBy(FmtSpace.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) FmtIcon(icon, c.ink, FmtSize.icon)
        FmtText(text, FmtTheme.type.chip, color = c.ink)
    }
}

/**
 * A rounded tint square with a 40×40 line icon in the tone's colour.
 * [small] is the 60dp tile (in notes and choices); otherwise 76dp.
 */
@Composable
fun FmtIconTile(icon: androidx.compose.ui.graphics.vector.ImageVector, tone: Tone = Tone.Purple, small: Boolean = false, ground: Color? = null, modifier: Modifier = Modifier) {
    val c = tone.colors
    val size: Dp = if (small) FmtSize.tileMd else FmtSize.tileLg
    val shape = RoundedCornerShape(au.com.followmetherapy.design.tokens.FmtRadius.tile)
    Box(
        modifier
            .size(size)
            .background(ground ?: c.tint, shape)
            .border(FmtBorder.outline, c.line, shape),
        contentAlignment = Alignment.Center,
    ) {
        FmtIcon(icon, c.icon, if (small) FmtSize.iconLg else FmtSize.iconXl)
    }
}

/** Draws a gentle wave across [width] at [centreY]: the swash and the section-title squiggle. */
internal fun squigglePath(startX: Float, width: Float, centreY: Float, amplitude: Float, halfPeriod: Float): androidx.compose.ui.graphics.Path {
    val p = androidx.compose.ui.graphics.Path()
    val halves = maxOf(1, (width / halfPeriod).toInt())
    val h = width / halves
    p.moveTo(startX, centreY)
    var x = startX
    var dir = -1f
    repeat(halves) {
        p.cubicTo(x + h * 6f / 17f, centreY + amplitude * dir, x + h * 11f / 17f, centreY + amplitude * dir, x + h, centreY)
        x += h
        dir = -dir
    }
    return p
}
