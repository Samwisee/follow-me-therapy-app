package au.com.followmetherapy.design.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import au.com.followmetherapy.design.tokens.FmtBarTokens
import au.com.followmetherapy.design.tokens.FmtCardTokens
import au.com.followmetherapy.design.tokens.FmtColors
import au.com.followmetherapy.design.tokens.FmtLayout
import androidx.compose.ui.unit.dp
import au.com.followmetherapy.design.tokens.FmtMotion
import au.com.followmetherapy.design.tokens.FmtRadius
import au.com.followmetherapy.design.tokens.FmtSize
import au.com.followmetherapy.design.tokens.FmtSpace

/*
 * App chrome: the parts that make the activities feel like an app rather than
 * a web page. Same tokens and rules as everything else: lilac bars, outlined
 * containers with nested corners, pill buttons, 44dp targets, focus rings.
 */

/** Sizes of the app's bars, from tokens.json › component.bar (FmtBarTokens). */
object FmtAppChrome {
    /** Depth of the wave under the app bar. */
    val wave: Dp get() = FmtBarTokens.wave
    /** How far the tab bar's crest rises over the chosen tab. */
    val crest: Dp get() = FmtBarTokens.crest
}

/**
 * Space the device keeps at its bottom edge (an iPhone's home indicator when
 * the app runs from the Home Screen). The tab bar and the action bar add it
 * below themselves, so nothing sits under the indicator. The app provides it.
 */
val LocalFmtSafeBottom = staticCompositionLocalOf { 0.dp }

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
 * Fredoka (or, with [brand], the Follow Me Therapy lockup, for the home
 * screen), and an optional progress bar beneath (for flows). Put it once
 * above the screens, not inside them: it stays put while screens change, and
 * its parts animate (the title cross-fades, the back button and the progress
 * bar slide in and out, the progress glides to its new length).
 */
@Composable
fun FmtAppBar(
    title: String,
    onBack: (() -> Unit)? = null,
    brand: Boolean = false,
    progress: Float? = null,
    progressLabel: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val m = FmtTheme.metrics
    // While the progress bar slides away it keeps its last length.
    var lastProgress by remember { mutableStateOf(progress ?: 0f) }
    var lastLabel by remember { mutableStateOf(progressLabel) }
    SideEffect {
        if (progress != null) lastProgress = progress
        if (progressLabel != null) lastLabel = progressLabel
    }
    var lastBack by remember { mutableStateOf(onBack) }
    SideEffect { if (onBack != null) lastBack = onBack }
    val enter = fadeIn(tween(FmtMotion.durBase, easing = FmtMotion.easeOut))
    val exit = fadeOut(tween(FmtMotion.durFast))
    Column(Modifier.fillMaxWidth().background(FmtBarTokens.fill)) {
        FmtOnGround(FmtBarTokens.fill) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(Modifier.widthIn(max = m.content).fillMaxWidth().padding(horizontal = m.gutter, vertical = FmtSpace.s1)) {
                Row(Modifier.fillMaxWidth().heightIn(min = FmtSize.target), verticalAlignment = Alignment.CenterVertically) {
                    AnimatedVisibility(
                        onBack != null,
                        enter = enter + expandHorizontally(tween(FmtMotion.durBase, easing = FmtMotion.easeStandard), expandFrom = Alignment.Start),
                        exit = exit + shrinkHorizontally(tween(FmtMotion.durBase, easing = FmtMotion.easeStandard), shrinkTowards = Alignment.Start),
                    ) {
                        Row { FmtIconButton(FmtIcons.back, "Back", { (onBack ?: lastBack)?.invoke() }); Spacer(Modifier.width(FmtSpace.s3)) }
                    }
                    AnimatedContent(
                        targetState = if (brand) null else title,
                        modifier = Modifier.weight(1f),
                        transitionSpec = {
                            (enter + slideInVertically(tween(FmtMotion.durBase, easing = FmtMotion.easeOut)) { h -> h / 4 }) togetherWith exit
                        },
                        contentAlignment = Alignment.CenterStart,
                    ) { t ->
                        if (t == null) FmtLockup(Modifier.semantics { heading() }, compact = true)
                        else FmtText(t, FmtTheme.type.subheading.copy(fontSize = m.textLg, fontWeight = FontWeight.SemiBold), Modifier.fillMaxWidth().semantics { heading() }, color = FmtBarTokens.ink)
                    }
                    trailing?.invoke()
                }
                AnimatedVisibility(
                    progress != null,
                    enter = enter + expandVertically(tween(FmtMotion.durBase, easing = FmtMotion.easeStandard), expandFrom = Alignment.Top),
                    exit = exit + shrinkVertically(tween(FmtMotion.durBase, easing = FmtMotion.easeStandard), shrinkTowards = Alignment.Top),
                ) {
                    Row(Modifier.padding(top = FmtSpace.s1, bottom = FmtSpace.s1), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FmtSpace.s3)) {
                        FmtProgressBar(progress ?: lastProgress, Modifier.weight(1f), progressLabel ?: lastLabel ?: "Progress")
                        AnimatedVisibility(progressLabel != null, enter = enter, exit = exit) {
                            FmtText(progressLabel ?: lastLabel ?: "", FmtTheme.type.chip, color = FmtBarTokens.tabInkSelected)
                        }
                    }
                }
            }
        }
        }
    }
}

data class FmtTab(val label: String, val icon: ImageVector)

/**
 * The bottom tab bar: lilac, like the app bar. Its top edge rises in a wave
 * crest over the chosen tab and glides to the next one; the chosen tab's
 * icon turns purple and its label bold purple-ink, so it doesn't rely on
 * colour alone. The crest draws above the bar, over the content's last
 * [FmtAppChrome.crest] of space (leave that much room at the bottom).
 */
@Composable
fun FmtTabBar(tabs: List<FmtTab>, selected: Int, onSelect: (Int) -> Unit) {
    val m = FmtTheme.metrics
    val at by animateFloatAsState(selected.toFloat(), tween(FmtMotion.durSlow, easing = FmtMotion.easeSpring))
    FmtOnGround(FmtBarTokens.fill) {
        Box(
            Modifier.fillMaxWidth().zIndex(1f).background(FmtBarTokens.fill)
                .drawBehind {
                    // The crest: a smooth hump centred on the chosen tab, and a
                    // purple-line stroke along the whole top edge and over the crest.
                    val rowW = minOf(size.width, m.content.toPx())
                    val inset = (m.gutter / 2).toPx()
                    val left = (size.width - rowW) / 2 + inset
                    val tabW = (rowW - inset * 2) / tabs.size
                    val cx = left + tabW * (at + 0.5f)
                    val half = tabW * 0.6f
                    val top = -FmtAppChrome.crest.toPx()
                    val crest = Path().apply {
                        moveTo(cx - half, 0f)
                        cubicTo(cx - half / 2, 0f, cx - half / 2, top, cx, top)
                        cubicTo(cx + half / 2, top, cx + half / 2, 0f, cx + half, 0f)
                    }
                    drawPath(Path().apply { addPath(crest); close() }, FmtBarTokens.fill)
                    val edge = Path().apply {
                        moveTo(0f, 0f); lineTo(cx - half, 0f)
                        addPath(crest)
                        moveTo(cx + half, 0f); lineTo(size.width, 0f)
                    }
                    drawPath(edge, FmtBarTokens.line, style = Stroke(FmtBorder.outline.toPx()))
                },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                Modifier.widthIn(max = m.content).fillMaxWidth().padding(horizontal = m.gutter / 2)
                    .padding(bottom = FmtSpace.s1 + LocalFmtSafeBottom.current),
            ) {
                tabs.forEachIndexed { i, tab -> TabItem(tab, i == selected, { onSelect(i) }) }
            }
        }
    }
}

@Composable
private fun RowScope.TabItem(tab: FmtTab, on: Boolean, select: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusVisibleAsState()
    val pressed by source.collectIsPressedAsState()
    val ink = if (on) FmtBarTokens.tabInkSelected else FmtBarTokens.tabInk
    val lift by animateDpAsState(if (on) -FmtSpace.s1 else 0.dp, tween(FmtMotion.durSlow, easing = FmtMotion.easeSpring))
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, tween(FmtMotion.durBase, easing = FmtMotion.easeSpring))
    Column(
        Modifier.weight(1f)
            .fmtFocusRing(focused, RoundedCornerShape(FmtRadius.tile))
            .selectable(selected = on, interactionSource = source, indication = null, role = Role.Tab, onClick = select)
            .heightIn(min = FmtSize.target)
            .scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FmtSpace.s1 / 2, Alignment.CenterVertically),
    ) {
        Box(Modifier.offset(y = lift)) { FmtIcon(tab.icon, if (on) FmtBarTokens.tabIconSelected else FmtBarTokens.tabInk, FmtSize.icon) }
        FmtText(tab.label, FmtTheme.type.chip.copy(fontSize = FmtTheme.metrics.textXs, fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold), color = ink, align = TextAlign.Center)
    }
}

/**
 * A button in a flow's action bar. Two actions are equal when they look the
 * same (label, kind, chevron, enabled), whatever they do.
 */
class FmtAction(
    val label: String,
    val kind: ButtonKind = ButtonKind.Primary,
    val chevron: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
) {
    internal fun calling(f: () -> Unit) = FmtAction(label, kind, chevron, enabled, f)
    override fun equals(other: Any?) = other is FmtAction && other.label == label && other.kind == kind && other.chevron == chevron && other.enabled == enabled
    override fun hashCode() = ((label.hashCode() * 31 + kind.hashCode()) * 31 + chevron.hashCode()) * 31 + enabled.hashCode()
}

/** The two places in the action bar: Back (or another way out) at the start, the next step at the end. */
data class FmtActions(val start: FmtAction? = null, val end: FmtAction? = null)

/**
 * Holds the action bar for an app whose screens change beneath it, so the
 * buttons stay on screen between steps and morph (Done → Next) instead of
 * sliding away with the screen. Each screen publishes its actions under its
 * own key; the app shows the current screen's. See [FmtActionBarHost].
 */
class FmtActionHost {
    internal val specs = mutableStateMapOf<Any, FmtActions>()
    internal var height by mutableStateOf(0.dp)
    fun actionsFor(key: Any): FmtActions? = specs[key]
}

/** Which screen is publishing actions, and to which host (provided by the app around each screen). */
class FmtActionSlot(val host: FmtActionHost, val key: Any)

val LocalFmtActionSlot = staticCompositionLocalOf<FmtActionSlot?> { null }

/**
 * The action bar: the start action on the left, the end one on the right,
 * floating with no panel behind them, gutter from the edges, in the centred
 * column. Each button stays put while its words and colour change, and
 * fades in or out when a step has no such action.
 */
@Composable
fun FmtActionBar(actions: FmtActions, modifier: Modifier = Modifier) {
    val m = FmtTheme.metrics
    var lastStart by remember { mutableStateOf(actions.start) }
    var lastEnd by remember { mutableStateOf(actions.end) }
    SideEffect {
        if (actions.start != null) lastStart = actions.start
        if (actions.end != null) lastEnd = actions.end
    }
    val enter = fadeIn(tween(FmtMotion.durBase, easing = FmtMotion.easeOut)) + scaleIn(tween(FmtMotion.durBase, easing = FmtMotion.easeSpring), initialScale = 0.9f)
    val exit = fadeOut(tween(FmtMotion.durFast)) + scaleOut(tween(FmtMotion.durFast), targetScale = 0.9f)
    Row(
        modifier.widthIn(max = m.content).fillMaxWidth().padding(horizontal = m.gutter).padding(top = FmtSpace.s3, bottom = FmtSpace.s3 + LocalFmtSafeBottom.current),
        horizontalArrangement = Arrangement.spacedBy(m.gridGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(actions.start != null, enter = enter, exit = exit) {
            (actions.start ?: lastStart)?.let { FmtButton(it.label, it.onClick, kind = it.kind, trailingChevron = it.chevron, enabled = it.enabled) }
        }
        Spacer(Modifier.weight(1f))
        AnimatedVisibility(actions.end != null, enter = enter, exit = exit) {
            (actions.end ?: lastEnd)?.let { FmtButton(it.label, it.onClick, kind = it.kind, trailingChevron = it.chevron, enabled = it.enabled) }
        }
    }
}

/**
 * Where an app shows the shared action bar: at the bottom of the screen
 * area, over the content, while [visible] (hide it on screens without
 * actions, such as tabs). It measures itself so screens leave room for it.
 */
@Composable
fun BoxScope.FmtActionBarHost(host: FmtActionHost, key: Any, visible: Boolean = true) {
    val density = LocalDensity.current
    // A new screen publishes its actions just after it first draws; until
    // then the bar keeps the last ones rather than blinking out.
    val actions = host.actionsFor(key)
    AnimatedVisibility(
        visible,
        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().onSizeChanged { host.height = with(density) { it.height.toDp() } },
        enter = fadeIn(tween(FmtMotion.durBase, easing = FmtMotion.easeOut)) + slideInVertically(tween(FmtMotion.durSlow, easing = FmtMotion.easeOut)) { it },
        exit = fadeOut(tween(FmtMotion.durFast)) + slideOutVertically(tween(FmtMotion.durBase, easing = FmtMotion.easeStandard)) { it },
    ) {
        var last by remember { mutableStateOf(actions ?: FmtActions()) }
        SideEffect { if (actions != null) last = actions }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { FmtActionBar(actions ?: last) }
    }
}

/**
 * One screen of a flow. The content scrolls in the centred column, starting
 * below the app bar's wave; the actions (Back and the next step) float at the
 * bottom with no panel behind them, always in the same place so a child
 * learns where to tap. The content leaves room for them at its end.
 * In an app, leave [appBar] out and keep one [FmtAppBar] and wave above all
 * the screens, and provide a [LocalFmtActionSlot] so the actions show in the
 * app's shared [FmtActionBarHost]: both then stay put while screens change.
 * On its own (no slot), the screen draws its own bar and actions.
 */
@Composable
fun FmtFlowScaffold(
    appBar: (@Composable () -> Unit)? = null,
    start: FmtAction? = null,
    end: FmtAction? = null,
    centre: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val m = FmtTheme.metrics
    val density = LocalDensity.current
    val slot = LocalFmtActionSlot.current
    var ownHeight by remember { mutableStateOf(0.dp) }
    val hasActions = start != null || end != null
    if (slot != null) {
        // Publish to the shared bar. Clicks call this screen's latest actions.
        val latest = remember(slot) { arrayOfNulls<FmtAction>(2) }
        latest[0] = start; latest[1] = end
        SideEffect {
            val spec = FmtActions(start?.calling { latest[0]?.onClick?.invoke() }, end?.calling { latest[1]?.onClick?.invoke() })
            if (slot.host.specs[slot.key] != spec) slot.host.specs[slot.key] = spec
        }
        DisposableEffect(slot) { onDispose { slot.host.specs.remove(slot.key) } }
    }
    val actionsHeight = if (!hasActions) 0.dp else if (slot != null) slot.host.height else ownHeight
    Column(Modifier.fillMaxSize().background(FmtColors.paper)) {
        appBar?.invoke()
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val viewport = maxHeight
            val wave = FmtAppChrome.wave
            Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
                Column(
                    // At least the visible height, so a centred screen sits in the middle;
                    // the top clears the wave, the bottom clears the floating actions.
                    Modifier.widthIn(max = m.content).fillMaxWidth().heightIn(min = viewport)
                        .padding(horizontal = m.gutter).padding(top = wave + m.stack, bottom = actionsHeight + m.stack),
                    verticalArrangement = if (centre) Arrangement.spacedBy(m.stack, Alignment.CenterVertically) else Arrangement.spacedBy(m.stack),
                    horizontalAlignment = if (centre) Alignment.CenterHorizontally else Alignment.Start,
                    content = content,
                )
            }
            if (appBar != null) FmtWave(FmtBarTokens.fill, Modifier.align(Alignment.TopCenter), down = true, height = wave, stroke = FmtBarTokens.line)
            if (slot == null && hasActions) {
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().onSizeChanged { ownHeight = with(density) { it.height.toDp() } }, contentAlignment = Alignment.Center) {
                    FmtActionBar(FmtActions(start, end))
                }
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
    val t = FmtCardTokens
    val shape = RoundedCornerShape(m.containerRadius)
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusVisibleAsState()
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, tween(FmtMotion.durBase, easing = FmtMotion.easeSpring))
    Row(
        modifier.fillMaxWidth()
            .scale(scale)
            .fmtFocusRing(focused, shape)
            .background(t.fill, shape)
            // Tappable: outlined in the tone's solid colour (card.line); a static row keeps the line colour.
            .border(FmtBorder.outline, if (onClick != null) t.line(tone) else tone.colors.line, shape)
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
        if (onClick != null && trailing == null) FmtIcon(FmtIcons.chevron, t.more, FmtSize.iconSm)
    }
}

