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
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sqrt
import kotlin.math.E

/**
 * Elastic edges, like a phone's own apps: pull past the top or bottom and the
 * page follows your finger with growing resistance, then springs back when you
 * let go; a fling that reaches an edge glides past it at its own speed and
 * eases back in one smooth arc (critically damped springs: no bump, no hard stop). Every scrolling
 * area gets it through [FmtTheme] (LocalOverscrollFactory).
 */
@OptIn(ExperimentalFoundationApi::class)
internal class FmtBounceOverscroll(private val density: Density) : OverscrollEffect {
    /** How far the page is pulled past its edge, in px (positive: past the top). */
    private var offset by mutableFloatStateOf(0f)
    private var pressed = false
    private var settle: Job? = null
    /** Which way a fling ran into an edge (the sign of what the page couldn't take), 0 if it hasn't. */
    private var edge = 0f
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
        // A fling that runs out of page: remember which edge, so only that glides past.
        if (abs(left) > 0.5f && source != NestedScrollSource.UserInput) edge = sign(left)
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
            // Let go while stretched: spring back, carrying the finger's speed so there's no jolt.
            animate(offset, 0f, initialVelocity = velocity.y, animationSpec = SPRING_BACK) { v, _ -> offset = v }
            return
        }
        edge = 0f
        val left = performFling(velocity)
        // Only a fling that actually reached the top or bottom glides past it. A fling that
        // slows to a stop on its own still has a little speed left when it ends; gliding
        // on that moved the whole page a few pixels and back: the bump at every natural stop.
        if (edge != 0f && sign(left.y) == edge && abs(left.y) > 1f) {
            // It carries on past the edge at the same speed, slows and glides back
            // (critically damped: one smooth arc, no bump, no hard stop).
            // A very fast fling is slowed before it starts, so its arc peaks at the cap rather
            // than flattening against it (clamping it was a second, harder bump).
            val cap = reach * 0.6f
            val most = cap * sqrt(Spring.StiffnessLow) * E.toFloat()
            animate(0f, 0f, initialVelocity = left.y.coerceIn(-most, most), animationSpec = GLIDE) { v, _ -> offset = v }
        }
    }

    /**
     * Runs every path once, off screen (this one is never drawn), so the first
     * real pull or fling at an edge doesn't stall: a web build compiles each
     * piece of code the first time it runs, and that pause was the bump at the
     * first edge. Called once as the app opens, under the opening logo (FmtTheme).
     */
    suspend fun warmUp() {
        pressed = true
        applyToScroll(Offset(0f, WARM_PULL), NestedScrollSource.UserInput) { Offset.Zero }
        applyToScroll(Offset(0f, -WARM_PULL / 2), NestedScrollSource.UserInput) { it }
        // A few frames of each animation are enough to have run its code.
        withTimeoutOrNull(WARM_STEP_MS) { applyToFling(Velocity(0f, WARM_SPEED)) { it } }
        pressed = false
        offset = 0f
        withTimeoutOrNull(WARM_STEP_MS) { applyToFling(Velocity(0f, -WARM_SPEED)) { edge = sign(it.y); it } }
        offset = 0f
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
        const val WARM_PULL = 40f
        const val WARM_SPEED = 900f
        const val WARM_STEP_MS = 120L
        val SPRING_BACK = spring<Float>(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow)
        val GLIDE = spring<Float>(dampingRatio = 1f, stiffness = Spring.StiffnessLow)
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal class FmtBounceFactory(private val density: Density) : OverscrollFactory {
    override fun createOverscrollEffect(): OverscrollEffect = FmtBounceOverscroll(density)
    override fun equals(other: Any?) = other is FmtBounceFactory && other.density == density
    override fun hashCode() = density.hashCode()
}
