package au.com.followmetherapy.design.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
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
import au.com.followmetherapy.design.tokens.FmtButtonTokens
import au.com.followmetherapy.design.tokens.FmtChipTokens
import au.com.followmetherapy.design.tokens.FmtColors
import au.com.followmetherapy.design.tokens.FmtTagTokens
import au.com.followmetherapy.design.tokens.FmtTileTokens
import au.com.followmetherapy.design.tokens.FmtTone
import au.com.followmetherapy.design.tokens.FmtToneRole
import au.com.followmetherapy.design.tokens.FmtTones
import au.com.followmetherapy.design.tokens.FmtMotion
import au.com.followmetherapy.design.tokens.FmtShadowToken
import au.com.followmetherapy.design.tokens.FmtShadow
import au.com.followmetherapy.design.tokens.FmtSize
import au.com.followmetherapy.design.tokens.FmtSpace

/** The four tones (generated from tokens.json › tone). Fixed meanings: purple = who it's for, teal = telehealth, sun = in person, pink = reports. */
typealias Tone = FmtTone

/** A tone's colours by role, for parts that aren't shared components (shared ones read their Fmt…Tokens). */
data class ToneColors(val solid: Color, val ink: Color, val tint: Color, val line: Color, val icon: Color, val ground: Color)

val Tone.colors: ToneColors
    get() = ToneColors(
        FmtTones.color(this, FmtToneRole.Solid), FmtTones.color(this, FmtToneRole.Ink), FmtTones.color(this, FmtToneRole.Tint),
        FmtTones.color(this, FmtToneRole.Line), FmtTones.color(this, FmtToneRole.Icon), FmtTones.color(this, FmtToneRole.Ground),
    )

/** True inside a tinted container (a band, header, note or bar): tiles there turn paper. */
@Composable
internal fun onTintedGround(): Boolean = LocalFmtGround.current != FmtColors.paper

/** True when the ground is [tone]'s own ground: a chip there turns paper. */
@Composable
internal fun onOwnGround(tone: Tone): Boolean = LocalFmtGround.current == FmtTones.color(tone, FmtToneRole.Ground)

/**
 * The ground a component sits on. Tinted containers (bands, headers, the app
 * bar, notes) provide their tint, so parts inside can adapt: a chip on its
 * own tint switches to paper with its line outline, as on the website.
 */
val LocalFmtGround = staticCompositionLocalOf { FmtColors.paper }

/** Provides [ground] to [content] (use inside any tinted container). */
@Composable
fun FmtOnGround(ground: Color, content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalFmtGround provides ground, content = content)

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

    // Every colour from FmtButtonTokens (tokens.json › component.button), as on the website.
    val t = FmtButtonTokens
    val tone = if (kind == ButtonKind.Teal || kind == ButtonKind.OutlineTeal) Tone.Teal else Tone.Purple
    val (bg, fg, line) = if (!enabled) Triple(t.disabledFill, t.disabledInk, t.disabledLine) else when (kind) {
        ButtonKind.Primary, ButtonKind.Teal -> Triple(if (hovered) t.fillHover(tone) else t.fill(tone), t.ink(tone), if (hovered) t.fillHover(tone) else t.line(tone))
        ButtonKind.Outline, ButtonKind.OutlineTeal -> Triple(
            if (hovered) t.outlineFillHover(tone) else t.outlineFill,
            if (hovered) t.outlineInkHover(tone) else t.outlineInk(tone),
            t.line(tone),
        )
        ButtonKind.Quiet -> Triple(t.quietFill, t.quietInk, t.quietLine)
    }
    // Colours, width and words change smoothly, so a button that stays on
    // screen between steps (Done → Next) morphs rather than flickers.
    val bgA by animateColorAsState(bg, tween(FmtMotion.durBase, easing = FmtMotion.easeStandard))
    val fgA by animateColorAsState(fg, tween(FmtMotion.durBase, easing = FmtMotion.easeStandard))
    val lineA by animateColorAsState(line, tween(FmtMotion.durBase, easing = FmtMotion.easeStandard))
    val shadow = if (!enabled) null else when (kind) {
        ButtonKind.Primary -> FmtShadow.glowPurple
        ButtonKind.Teal -> FmtShadow.glowTeal
        else -> null
    }
    Row(
        modifier = modifier
            .scale(scale)
            .padding(top = FmtSpace.hair * (1 - lift), bottom = FmtSpace.hair * lift)
            .fmtFocusRing(focused, shape)
            .then(if (shadow != null) Modifier.fmtShadow(shadow, shape) else Modifier)
            .background(bgA, shape)
            .border(FmtBorder.outline, lineA, shape)
            .hoverable(source)
            .clickable(interactionSource = source, indication = null, role = Role.Button, enabled = enabled, onClick = onClick)
            .heightIn(min = FmtSize.button)
            .animateContentSize(tween(FmtMotion.durBase, easing = FmtMotion.easeStandard))
            .padding(horizontal = FmtSpace.s6, vertical = FmtSpace.s2),
        horizontalArrangement = Arrangement.spacedBy(FmtSpace.s2, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = text,
            transitionSpec = { fadeIn(tween(FmtMotion.durBase, easing = FmtMotion.easeOut)) togetherWith fadeOut(tween(FmtMotion.durFast)) },
            contentAlignment = Alignment.Center,
        ) { t -> FmtText(t, FmtTheme.type.button, color = fgA, align = TextAlign.Center) }
        if (trailingChevron) FmtIcon(FmtIcons.chevron, fgA, FmtSize.iconSm)
    }
}

/** A small solid label above a title ("Self-care"). Sun takes ink text; the others white. */
@Composable
fun FmtTag(text: String, tone: Tone = Tone.Purple, modifier: Modifier = Modifier) {
    val bg = FmtTagTokens.fill(tone)
    val fg = FmtTagTokens.ink(tone)
    Box(modifier.background(bg, CircleShape).padding(horizontal = FmtSpace.s3, vertical = FmtSpace.s1)) {
        FmtText(text, FmtTheme.type.chip.copy(fontSize = FmtTheme.metrics.textXs), color = fg)
    }
}

/** A bordered pastel pill that states one fact. Say it in words; the colour only reinforces. */
@Composable
fun FmtChip(text: String, tone: Tone = Tone.Purple, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, modifier: Modifier = Modifier) {
    val t = FmtChipTokens
    // On its own tone's ground (an ages chip on the lilac hero or app bar) a chip turns paper, keeping its line and ink.
    val fill = if (onOwnGround(tone)) t.fillOnGround else t.fill(tone)
    Row(
        modifier
            .heightIn(min = FmtSize.target)
            .background(fill, CircleShape)
            .border(FmtBorder.outline, t.line(tone), CircleShape)
            .padding(horizontal = FmtSpace.s4, vertical = FmtSpace.s2),
        horizontalArrangement = Arrangement.spacedBy(FmtSpace.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) FmtIcon(icon, t.ink(tone), FmtSize.icon)
        FmtText(text, FmtTheme.type.chip, color = t.ink(tone))
    }
}

/**
 * A chip you tap to choose (a day, an option). Off: paper with the tone's line
 * and ink. On: the tone's tint, a solid outline and a tick, so it never relies
 * on colour alone. A 44dp target, announced as a checkbox that's on or off.
 */
@Composable
fun FmtToggleChip(text: String, on: Boolean, onToggle: (Boolean) -> Unit, tone: Tone = Tone.Purple, modifier: Modifier = Modifier) {
    val t = FmtChipTokens
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusVisibleAsState()
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, tween(FmtMotion.durBase, easing = FmtMotion.easeSpring))
    val fill by animateColorAsState(if (on) t.fill(tone) else t.outlineFill, tween(FmtMotion.durFast, easing = FmtMotion.easeStandard))
    val line by animateColorAsState(if (on) t.lineHover(tone) else t.line(tone), tween(FmtMotion.durFast, easing = FmtMotion.easeStandard))
    Row(
        modifier
            .scale(scale)
            .fmtFocusRing(focused, CircleShape)
            .heightIn(min = FmtSize.target)
            .background(fill, CircleShape)
            .border(FmtBorder.outline, line, CircleShape)
            .toggleable(value = on, interactionSource = source, indication = null, role = Role.Checkbox, onValueChange = onToggle)
            .padding(horizontal = FmtSpace.s3, vertical = FmtSpace.s2),
        horizontalArrangement = Arrangement.spacedBy(FmtSpace.s1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (on) FmtIcon(FmtIcons.tick, t.ink(tone), FmtSize.iconSm)
        FmtText(text, FmtTheme.type.chip, color = t.ink(tone))
    }
}

/**
 * A chip that filters a list, in the tone of what it filters (Self-care purple,
 * Feelings teal, Home practice sun), or an [icon]-only chip that shows or hides
 * part of a page (info). Off: paper, the tone's line and ink. On: the tone's
 * ink as the fill with white words, no tick; the light-to-dark change shows it.
 * An icon-only chip needs a spoken [label]. [toggle] chips are announced on/off,
 * the rest as a selected tab of the filters.
 */
@Composable
fun FmtFilterChip(text: String?, on: Boolean, onSelect: () -> Unit, tone: Tone = Tone.Purple, icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
                  label: String? = null, toggle: Boolean = false, modifier: Modifier = Modifier) {
    val t = FmtChipTokens
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusVisibleAsState()
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, tween(FmtMotion.durBase, easing = FmtMotion.easeSpring))
    val anim = tween<Color>(FmtMotion.durFast, easing = FmtMotion.easeStandard)
    val fill by animateColorAsState(if (on) t.fillSelected(tone) else t.outlineFill, anim)
    val line by animateColorAsState(if (on) t.fillSelected(tone) else t.line(tone), anim)
    val ink by animateColorAsState(if (on) t.inkSelected else t.ink(tone), anim)
    val target = if (toggle) Modifier.toggleable(value = on, interactionSource = source, indication = null, role = Role.Checkbox) { onSelect() }
    else Modifier.filterSelectable(on, source, onSelect)
    Row(
        modifier
            .scale(scale)
            .fmtFocusRing(focused, CircleShape)
            .heightIn(min = FmtSize.target)
            .widthIn(min = FmtSize.target)
            .background(fill, CircleShape)
            .border(FmtBorder.outline, line, CircleShape)
            .then(target)
            .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier)
            .padding(horizontal = if (text == null) FmtSpace.s2 else FmtSpace.s3, vertical = FmtSpace.s2),
        horizontalArrangement = Arrangement.spacedBy(FmtSpace.s1, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { FmtIcon(it, ink, FmtSize.iconSm) }
        text?.let { FmtText(it, FmtTheme.type.chip, color = ink) }
    }
}

private fun Modifier.filterSelectable(on: Boolean, source: MutableInteractionSource, onSelect: () -> Unit) =
    selectable(selected = on, interactionSource = source, indication = null, role = Role.Tab, onClick = onSelect)

/**
 * A rounded tint square with a 40×40 line icon in the tone's colour.
 * [small] is the 60dp tile (in notes and choices); otherwise 76dp.
 */
@Composable
fun FmtIconTile(icon: androidx.compose.ui.graphics.vector.ImageVector, tone: Tone = Tone.Purple, small: Boolean = false, ground: Color? = null, modifier: Modifier = Modifier) {
    val t = FmtTileTokens
    val size: Dp = if (small) FmtSize.tileMd else FmtSize.tileLg
    val shape = RoundedCornerShape(au.com.followmetherapy.design.tokens.FmtRadius.tile)
    // Inside a tinted container (a note, a header) the tile is paper.
    val fill = ground ?: if (onTintedGround()) t.fillOnGround else t.fill(tone)
    Box(
        modifier
            .size(size)
            .background(fill, shape)
            .border(FmtBorder.outline, t.line(tone), shape),
        contentAlignment = Alignment.Center,
    ) {
        FmtIcon(icon, t.icon(tone), if (small) FmtSize.iconLg else FmtSize.iconXl)
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
