package au.com.followmetherapy.design.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import au.com.followmetherapy.design.tokens.FmtSpace
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign

/**
 * Elastic edges, like a phone's own apps: pull past the top or bottom and the
 * page follows your finger with growing resistance, then springs back when you
 * let go; a fast fling that reaches an edge bounces softly. Every scrolling
 * area gets it through [FmtTheme] (LocalOverscrollFactory).
 */
@OptIn(ExperimentalFoundationApi::class)
internal class FmtBounceOverscroll(private val density: Density) : OverscrollEffect {
    /** How far the page is pulled past its edge, in px (positive: past the top). */
    private var offset by mutableFloatStateOf(0f)
    private var pressed = false
    private var settle: Job? = null
    private val bounceNode = BounceNode()

    /** The furthest a pull can stretch (it gets harder as it goes). */
    private val reach: Float get() = with(density) { FmtSpace.s30.toPx() * 2.5f }

    override val isInProgress: Boolean get() = offset != 0f
    override val node: DelegatableNode get() = bounceNode

    override fun applyToScroll(delta: Offset, source: NestedScrollSource, performScroll: (Offset) -> Offset): Offset {
        settle?.cancel()
        var y = delta.y
        // Pulled past an edge and moving back: undo the pull first.
        var undone = 0f
        if (offset != 0f && y != 0f && sign(y) != sign(offset)) {
            val next = if (offset > 0f) maxOf(0f, offset + y) else min(0f, offset + y)
            undone = next - offset
            offset = next
            y -= undone
        }
        val consumed = performScroll(Offset(delta.x, y))
        val left = y - consumed.y
        if (abs(left) > 0.5f && source == NestedScrollSource.UserInput) {
            // Rubber band: most of the finger's movement at first, less the further it's stretched.
            offset += left * 0.7f * (1f - min(1f, abs(offset) / reach))
        }
        // A mouse wheel never lets go, so spring back once it stops.
        if (!pressed && offset != 0f) springBackSoon()
        return Offset(consumed.x, undone + consumed.y + left)
    }

    override suspend fun applyToFling(velocity: Velocity, performFling: suspend (Velocity) -> Velocity) {
        settle?.cancel()
        if (offset != 0f) {
            // Let go while stretched: spring back, with a little give past the edge.
            animate(offset, 0f, animationSpec = SPRING_BACK) { v, _ -> offset = v }
            return
        }
        val left = performFling(velocity)
        if (abs(left.y) > 1f) {
            // A fling that reached the edge: a small, soft bounce carrying some of its speed.
            animate(0f, 0f, initialVelocity = left.y * 0.25f, animationSpec = BOUNCE) { v, _ -> offset = v }
        }
    }

    private fun springBackSoon() {
        if (!bounceNode.isAttached) { offset = 0f; return }
        settle = bounceNode.coroutineScope.launch {
            delay(SETTLE_MS)
            animate(offset, 0f, animationSpec = SPRING_BACK) { v, _ -> offset = v }
        }
    }

    /** Moves the scrolling area by the stretch (a layer offset: no re-layout) and notices fingers on it. */
    private inner class BounceNode : Modifier.Node(), LayoutModifierNode, PointerInputModifierNode {
        override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
            val p = measurable.measure(constraints)
            return layout(p.width, p.height) { p.placeWithLayer(0, 0) { translationY = offset } }
        }
        override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
            if (pass == PointerEventPass.Initial) pressed = pointerEvent.changes.any { it.pressed }
        }
        override fun onCancelPointerInput() { pressed = false }
    }

    private companion object {
        const val SETTLE_MS = 120L
        val SPRING_BACK = spring<Float>(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
        val BOUNCE = spring<Float>(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal class FmtBounceFactory(private val density: Density) : OverscrollFactory {
    override fun createOverscrollEffect(): OverscrollEffect = FmtBounceOverscroll(density)
    override fun equals(other: Any?) = other is FmtBounceFactory && other.density == density
    override fun hashCode() = density.hashCode()
}
