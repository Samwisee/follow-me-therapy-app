package au.com.followmetherapy.design.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import au.com.followmetherapy.design.icons.FmtIcon
import au.com.followmetherapy.design.icons.FmtIcons
import au.com.followmetherapy.design.theme.FmtTheme
import au.com.followmetherapy.design.tokens.FmtBorder
import au.com.followmetherapy.design.tokens.FmtColors
import au.com.followmetherapy.design.tokens.FmtMotion
import au.com.followmetherapy.design.tokens.FmtRadius
import au.com.followmetherapy.design.tokens.FmtShadow
import au.com.followmetherapy.design.tokens.FmtSize
import au.com.followmetherapy.design.tokens.FmtSpace

/*
 * App chrome: the parts that make the activities feel like an app rather than
 * a web page. Same tokens and rules as everything else: lilac bars, outlined
 * containers with nested corners, pill buttons, 44dp targets, focus rings.
 */

/** A round icon button (size-target), outlined in purple-line on paper. Needs a [label] for screen readers. */
@Composable
fun FmtIconButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusVisibleAsState()
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, tween(FmtMotion.durBase, easing = FmtMotion.easeSpring))
    Box(
        modifier
            .size(FmtSize.target)
            .scale(scale)
            .fmtFocusRing(focused, CircleShape)
            .background(FmtColors.paper, CircleShape)
            .border(FmtBorder.outline, FmtColors.purpleLine, CircleShape)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        FmtIcon(icon, FmtColors.purple, FmtSize.iconSm)
    }
}

/**
 * A slim progress bar: a purple-line track with a purple fill that glides to
 * its new length. Announced as progress to screen readers.
 */
@Composable
fun FmtProgressBar(progress: Float, modifier: Modifier = Modifier, label: String = "Progress") {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(FmtMotion.durSlow, easing = FmtMotion.easeStandard))
    Box(
        modifier
            .fillMaxWidth()
            .height(FmtSpace.s2)
            .background(FmtColors.purpleLine, CircleShape)
            .semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
            }
            .drawBehind {
                val w = size.width * p
                if (w > 0f) drawRoundRect(FmtColors.purple, size = size.copy(width = maxOf(w, size.height)), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
            },
    )
}

/**
 * The app bar: lilac, an optional round back button, the screen's title in
 * Fredoka, and an optional progress bar beneath (for flows).
 */
@Composable
fun FmtAppBar(
    title: String,
    onBack: (() -> Unit)? = null,
    progress: Float? = null,
    progressLabel: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val m = FmtTheme.metrics
    Column(Modifier.fillMaxWidth().background(FmtColors.purpleTint)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.widthIn(max = m.content).fillMaxWidth().padding(horizontal = m.gutter, vertical = FmtSpace.s3),
                verticalArrangement = Arrangement.spacedBy(FmtSpace.s3),
            ) {
                Row(Modifier.fillMaxWidth().heightIn(min = FmtSize.target), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FmtSpace.s3)) {
                    if (onBack != null) FmtIconButton(FmtIcons.back, "Back", onBack)
                    FmtText(title, FmtTheme.type.subheading.copy(fontSize = m.textLg, fontWeight = FontWeight.SemiBold), Modifier.weight(1f).semantics { heading() })
                    trailing?.invoke()
                }
                if (progress != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FmtSpace.s3)) {
                        FmtProgressBar(progress, Modifier.weight(1f), progressLabel ?: "Progress")
                        if (progressLabel != null) FmtText(progressLabel, FmtTheme.type.chip, color = FmtColors.purpleInk)
                    }
                }
            }
        }
    }
}

data class FmtTab(val label: String, val icon: ImageVector)

/**
 * The bottom tab bar: paper, a purple-line rule on top; each tab is an icon
 * and its label (never an icon alone), the chosen one sits in a lilac pill
 * and its label turns purple-ink, so it doesn't rely on colour alone.
 */
@Composable
fun FmtTabBar(tabs: List<FmtTab>, selected: Int, onSelect: (Int) -> Unit) {
    val m = FmtTheme.metrics
    Box(
        Modifier.fillMaxWidth().background(FmtColors.paper).drawBehind {
            drawLine(FmtColors.purpleLine, Offset(0f, 0f), Offset(size.width, 0f), FmtBorder.outline.toPx())
        },
        contentAlignment = Alignment.Center,
    ) {
        Row(Modifier.widthIn(max = m.content).fillMaxWidth().padding(horizontal = m.gutter / 2, vertical = FmtSpace.s2)) {
            tabs.forEachIndexed { i, tab -> TabItem(tab, i == selected, { onSelect(i) }) }
        }
    }
}

@Composable
private fun RowScope.TabItem(tab: FmtTab, on: Boolean, select: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusVisibleAsState()
    val pill by animateColorAsState(if (on) FmtColors.purpleTint else FmtColors.paper, tween(FmtMotion.durBase, easing = FmtMotion.easeStandard))
    val ink = if (on) FmtColors.purpleInk else FmtColors.inkSoft
    Column(
        Modifier.weight(1f)
            .fmtFocusRing(focused, RoundedCornerShape(FmtRadius.tile))
            .selectable(selected = on, interactionSource = source, indication = null, role = Role.Tab, onClick = select)
            .heightIn(min = FmtSize.target + FmtSpace.s4)
            .padding(vertical = FmtSpace.s1),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FmtSpace.s1, Alignment.CenterVertically),
    ) {
        Box(Modifier.width(FmtSpace.s16).height(FmtSpace.s8).background(pill, CircleShape).then(if (on) Modifier.border(FmtBorder.outline, FmtColors.purpleLine, CircleShape) else Modifier), contentAlignment = Alignment.Center) {
            FmtIcon(tab.icon, if (on) FmtColors.purple else FmtColors.inkSoft, FmtSize.icon)
        }
        FmtText(tab.label, FmtTheme.type.chip.copy(fontSize = FmtTheme.metrics.textXs, fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold), color = ink, align = TextAlign.Center)
    }
}

/**
 * One screen of a flow: the app bar on top, the content scrolling in the
 * centred column, and the actions pinned at the bottom (Back and the next
 * step), always in the same place so a child learns where to tap.
 */
@Composable
fun FmtFlowScaffold(
    appBar: @Composable () -> Unit,
    actions: (@Composable RowScope.() -> Unit)? = null,
    centre: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val m = FmtTheme.metrics
    Column(Modifier.fillMaxSize().background(FmtColors.paper)) {
        appBar()
        FmtWave(FmtColors.purpleTint, down = true)
        Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = m.content).fillMaxWidth().padding(horizontal = m.gutter).padding(bottom = m.stack),
                verticalArrangement = if (centre) Arrangement.spacedBy(m.stack, Alignment.CenterVertically) else Arrangement.spacedBy(m.stack),
                horizontalAlignment = if (centre) Alignment.CenterHorizontally else Alignment.Start,
                content = content,
            )
        }
        if (actions != null) {
            Box(
                Modifier.fillMaxWidth().background(FmtColors.paper).drawBehind {
                    drawLine(FmtColors.purpleLine, Offset(0f, 0f), Offset(size.width, 0f), FmtBorder.outline.toPx())
                },
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    Modifier.widthIn(max = m.content).fillMaxWidth().padding(horizontal = m.gutter, vertical = FmtSpace.s4),
                    horizontalArrangement = Arrangement.spacedBy(m.gridGap),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }
    }
}

/**
 * One step, big: the step's number in a disc and its words in Fredoka, for a
 * child to read across the room. The disc fills teal with a tick when done.
 */
@Composable
fun FmtStepFocus(number: Int, total: Int, label: String, done: Boolean, modifier: Modifier = Modifier) {
    val m = FmtTheme.metrics
    val disc by animateColorAsState(if (done) FmtColors.teal else FmtColors.purpleTint, tween(FmtMotion.durBase, easing = FmtMotion.easeStandard))
    val pop by animateFloatAsState(if (done) 1.08f else 1f, tween(FmtMotion.durSlow, easing = FmtMotion.easeSpring))
    Column(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(m.stack),
    ) {
        FmtText("Step $number of $total", FmtTheme.type.chip.copy(fontSize = m.textMd), color = FmtColors.purpleInk)
        Box(
            Modifier.size(FmtSpace.s30).scale(pop).background(disc, CircleShape).border(FmtBorder.outline * 2, if (done) FmtColors.teal else FmtColors.purpleLine, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (done) FmtIcon(FmtIcons.tick, FmtColors.paper, FmtSize.iconXl * 1.4f)
            else FmtText(number.toString(), FmtTheme.type.title.copy(fontSize = m.textDisplay), color = FmtColors.purpleInk)
        }
        FmtText(label, FmtTheme.type.title, align = TextAlign.Center)
    }
}

/**
 * A celebration: a sun disc with a star that springs in, a title with the
 * swash, and a short message. Shown once at the end of a flow.
 */
@Composable
fun FmtCelebration(title: String, swash: String?, message: String, modifier: Modifier = Modifier) {
    val m = FmtTheme.metrics
    val pop = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, tween(FmtMotion.durEntrance, easing = FmtMotion.easeSpring)) }
    Column(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(m.stack),
    ) {
        Box(
            Modifier.size(FmtSpace.s30 + FmtSpace.s10).scale(pop.value).background(FmtColors.sunTint, CircleShape).border(FmtBorder.outline * 2, FmtColors.sunLine, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(contentAlignment = Alignment.Center) {
                FmtIcon(FmtIcons.starFilled, FmtColors.sun, FmtSpace.s24)
                FmtIcon(FmtIcons.star, FmtColors.sunDeep, FmtSpace.s24)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(m.groupGap)) {
            FmtSwashTitle(title, swash, style = FmtTheme.type.title.copy(textAlign = TextAlign.Center))
            FmtText(message, FmtTheme.type.lead, align = TextAlign.Center)
        }
    }
}

/**
 * A breathing guide: a teal disc that grows for four seconds (breathe in) and
 * shrinks for four (breathe out), with the words changing to match. Ambient
 * loops keep their own longer timing (8s), like the website's illustrations.
 */
@Composable
fun FmtBreathing(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition()
    val phase by t.animateFloat(0f, 1f, infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart))
    val inhale = phase < 0.5f
    val x = if (inhale) phase * 2f else (1f - phase) * 2f
    val eased = FmtMotion.easeInOut.transform(x)
    val m = FmtTheme.metrics
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(m.stack)) {
        Box(Modifier.size(FmtSpace.s30 * 2), contentAlignment = Alignment.Center) {
            Box(Modifier.size(FmtSpace.s30 * 2).scale(0.55f + 0.45f * eased).background(FmtColors.tealTint, CircleShape).border(FmtBorder.outline * 2, FmtColors.tealLine, CircleShape))
            Box(Modifier.size(FmtSpace.s16).background(FmtColors.teal, CircleShape))
        }
        FmtText(if (inhale) "Breathe in…" else "Breathe out…", FmtTheme.type.title, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, align = TextAlign.Center)
    }
}

/**
 * A row in a list of options or summaries: icon tile, words, and a chevron;
 * the whole row is the target. Outlined in its tone, corner nested.
 */
@Composable
fun FmtListRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    tone: Tone = Tone.Purple,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val m = FmtTheme.metrics
    val c = tone.colors
    val shape = RoundedCornerShape(m.containerRadius)
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusVisibleAsState()
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, tween(FmtMotion.durBase, easing = FmtMotion.easeSpring))
    Row(
        modifier.fillMaxWidth()
            .scale(scale)
            .fmtFocusRing(focused, shape)
            .fmtShadow(FmtShadow.rest, shape)
            .background(FmtColors.paper, shape)
            .border(FmtBorder.outline, c.line, shape)
            .then(if (onClick != null) Modifier.clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick) else Modifier)
            .padding(m.cardPad),
        horizontalArrangement = Arrangement.spacedBy(m.cardPad),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) FmtIconTile(icon, tone, small = true)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(FmtSpace.s1)) {
            FmtText(title, FmtTheme.type.subheading.copy(fontSize = m.textLg, fontWeight = FontWeight.SemiBold))
            if (subtitle != null) FmtText(subtitle, FmtTheme.type.small)
        }
        trailing?.invoke()
        if (onClick != null && trailing == null) FmtIcon(FmtIcons.chevron, c.ink, FmtSize.iconSm)
    }
}

/** Fills the remaining width in a row of actions (pushes the next button right). */
@Composable
fun RowScope.FmtActionSpacer() = Spacer(Modifier.weight(1f))
