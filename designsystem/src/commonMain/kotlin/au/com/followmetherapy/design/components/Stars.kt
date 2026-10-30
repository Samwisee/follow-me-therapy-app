package au.com.followmetherapy.design.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import au.com.followmetherapy.design.icons.FmtIcon
import au.com.followmetherapy.design.icons.FmtIcons
import au.com.followmetherapy.design.tokens.FmtCalendarTokens
import au.com.followmetherapy.design.tokens.FmtMotion
import au.com.followmetherapy.design.tokens.FmtSize
import au.com.followmetherapy.design.tokens.FmtSpace
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.unit.Dp

/** The reader asked for less motion (prefers-reduced-motion): bursts and springs are skipped. */
val LocalFmtReducedMotion = staticCompositionLocalOf { false }

/** How a star looks: planned (outline), partly done (pale), done (gold). */
enum class StarState { Planned, Partial, Done }

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

/**
 * A star (the WeekCalendar's and the practice week's). When [burstKey] changes
 * to a new value while done, it bursts: springs in from small with a twist and
 * sparkles fly out in purple, teal, pink and sun. Not with reduced motion.
 */
@Composable
internal fun FmtStarMark(state: StarState, burst: Int, modifier: Modifier = Modifier, iconSize: Dp = FmtSize.iconLg) {
    val t = FmtCalendarTokens
    val reduced = LocalFmtReducedMotion.current
    val p = remember { Animatable(1f) }
    LaunchedEffect(burst) {
        if (burst > 0 && !reduced) { p.snapTo(0f); p.animateTo(1f, tween(FmtMotion.durSlower, easing = LinearEasing)) }
    }
    val x = p.value
    val outT = FmtMotion.easeOut.transform((x / 0.6f).coerceIn(0f, 1f))
    val backT = FmtMotion.easeInOut.transform(((x - 0.6f) / 0.4f).coerceIn(0f, 1f))
    val scale = if (x < 0.6f) lerp(0.3f, 1.35f, outT) else lerp(1.35f, 1f, backT)
    val turn = if (x < 0.6f) lerp(-72f, 12f, outT) else lerp(12f, 0f, backT)
    val colours = listOf(t.burstPurple, t.burstTeal, t.burstPink, t.burstSun)
    Box(
        modifier.size(FmtSize.target).drawBehind {
            if (x <= 0f || x >= 1f) return@drawBehind
            val reach = FmtSpace.s6.toPx() * 1.6f * FmtMotion.easeOut.transform(x)
            val r = FmtSpace.s1.toPx() * lerp(0.4f, 1f, x)
            val alpha = if (x < 0.6f) 1f else 1f - (x - 0.6f) / 0.4f
            for (k in 0 until 8) {
                val a = k * PI / 4 - PI / 2
                drawCircle(colours[k % 4].copy(alpha = alpha), r, center + Offset((cos(a) * reach).toFloat(), (sin(a) * reach).toFloat()))
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.scale(scale).rotate(turn)) {
            when (state) {
                StarState.Planned -> FmtIcon(FmtIcons.star, t.planned, iconSize)
                StarState.Partial -> { FmtIcon(FmtIcons.starFilled, t.partial, iconSize); FmtIcon(FmtIcons.star, t.partialLine, iconSize) }
                StarState.Done -> { FmtIcon(FmtIcons.starFilled, t.doneFill, iconSize); FmtIcon(FmtIcons.star, t.doneLine, iconSize) }
            }
        }
    }
}

/**
 * A star to tap (44dp target): a purple outline, a gold star when done.
 * Adding one bursts (see [FmtStarMark]); taking it away just goes back.
 */
@Composable
fun FmtStarToggle(on: Boolean, label: String, iconSize: Dp = FmtSize.iconLg, toggle: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusVisibleAsState()
    val pressed by source.collectIsPressedAsState()
    var bursts by remember { mutableStateOf(0) }
    var was by remember { mutableStateOf(on) }
    LaunchedEffect(on) { if (on && !was) bursts++; was = on }
    Box(
        Modifier.size(FmtSize.target)
            .fmtFocusRing(focused, CircleShape)
            .toggleable(value = on, interactionSource = source, indication = null, role = Role.Checkbox, onValueChange = { toggle() })
            .semantics { stateDescription = if (on) "practised" else "not yet"; contentDescription = label }
            .scale(if (pressed) 0.85f else 1f),
        contentAlignment = Alignment.Center,
    ) {
        FmtStarMark(if (on) StarState.Done else StarState.Planned, bursts, iconSize = iconSize)
    }
}
