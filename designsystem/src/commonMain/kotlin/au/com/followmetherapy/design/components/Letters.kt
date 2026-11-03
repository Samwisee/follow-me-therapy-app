package au.com.followmetherapy.design.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import au.com.followmetherapy.design.letters.FmtGlyph
import au.com.followmetherapy.design.letters.FmtLetters
import au.com.followmetherapy.design.tokens.FmtLetterTokens
import au.com.followmetherapy.design.tokens.FmtSpace
import kotlin.math.hypot
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/**
 * A letter (or pre-writing shape) to trace with a finger (the shared
 * LetterTrace): writing lines across, the letter dashed, a gold dot where to
 * start. Following a stroke from its start, the way it goes, turns it purple;
 * then the next stroke's dot shows. When every stroke is done a star bursts
 * and [onDone] is called. Shapes come from design/letters.json (FmtLetters).
 */
@Composable
fun FmtLetterTrace(name: String, onDone: () -> Unit = {}, modifier: Modifier = Modifier, height: Dp = FmtSpace.s30 * 2.4f) {
    val glyph = remember(name) { FmtLetters.glyph(name) } ?: return
    val t = FmtLetterTokens
    var stroke by remember(name) { mutableIntStateOf(0) }
    var reached by remember(name) { mutableIntStateOf(0) }   // points of the current stroke followed so far
    var along by remember(name) { mutableFloatStateOf(0f) }  // and how far on to the next (0..1): the dot glides, not hops
    var tracing by remember(name) { mutableStateOf(false) }
    var bursts by remember(name) { mutableIntStateOf(0) }
    val done = stroke >= glyph.strokes.size
    val words = if (name.length > 1) "Shape to trace: $name." else "Letter $name."
    Box(
        modifier.fillMaxWidth().height(height)
            .semantics {
                contentDescription = words
                stateDescription = if (done) "done" else "stroke ${stroke + 1} of ${glyph.strokes.size}: start at the gold dot"
            },
        contentAlignment = Alignment.TopEnd,
    ) {
        Canvas(
            Modifier.fillMaxWidth().height(height).pointerInput(name) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    if (stroke >= glyph.strokes.size) return@awaitEachGesture
                    val g = Geometry(size.width.toFloat(), size.height.toFloat(), glyph)
                    val pts = glyph.strokes[stroke]
                    // Start on the stroke where it's been followed to (its start, the first time).
                    tracing = g.distance(down.position, g.point(pts, reached, along)) < g.tolerance
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        if (!tracing) continue
                        change.consume()
                        // Follow the finger forward along the stroke, a little at a time: onto
                        // the nearest point of the stroke's next stretch (by length along it, so a
                        // sharp turn back along the same line is never skipped), never backwards.
                        val f = g.toGlyph(change.position)
                        var best: Pair<Int, Float>? = null
                        var bestD = Float.MAX_VALUE
                        var ahead = 0f
                        var i = reached
                        while (i < pts.lastIndex && ahead < LOOK_AHEAD) {
                            val (u, d) = project(f, pts[i], pts[i + 1])
                            val forward = i > reached || u >= along
                            // The earliest close stretch wins, unless a later one is clearly closer.
                            if (forward && d < bestD - 1.5f) { best = i to u; bestD = d }
                            ahead += hypot(pts[i + 1].first - pts[i].first, pts[i + 1].second - pts[i].second)
                            i++
                        }
                        if (best != null && bestD * g.scale < g.tolerance) { reached = best.first; along = best.second }
                        if (reached >= pts.lastIndex - 1 && along > 0.8f) {
                            stroke++; reached = 0; along = 0f; tracing = false
                            if (stroke >= glyph.strokes.size) { bursts++; onDone() }
                            break
                        }
                    }
                    tracing = false
                }
            },
        ) {
            val g = Geometry(size.width, size.height, glyph)
            val unit = g.scale
            // Writing lines across the whole width.
            fun across(y: Float, color: androidx.compose.ui.graphics.Color, w: Float, dashed: Boolean = false) =
                drawLine(color, Offset(0f, g.y(y)), Offset(size.width, g.y(y)), w * unit, pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(3 * unit, 3 * unit)) else null)
            across(FmtLetters.TOP, t.guide, 0.8f)
            across(FmtLetters.WAIST, t.guide, 0.8f, dashed = true)
            across(FmtLetters.BASE, t.base, 1.2f)
            across(FmtLetters.DESCENDER, t.guide, 0.8f)
            glyph.strokes.forEachIndexed { i, pts ->
                when {
                    i < stroke -> drawStroke(g, pts, pts.lastIndex, t.model, 5f)
                    else -> {
                        drawStroke(g, pts, pts.lastIndex, t.trace, 2.6f, dashed = true)
                        if (i == stroke && (reached > 0 || along > 0f)) drawStroke(g, pts, reached, t.model, 5f, tip = g.point(pts, reached, along))
                    }
                }
            }
            if (!done) {
                val pts = glyph.strokes[stroke]
                val at = g.at(g.point(pts, reached, along))
                drawWayArrow(g, pts, reached, t.model, along)
                drawCircle(t.start, 4.5f * unit, at)
                drawCircle(t.startLine, 4.5f * unit, at, style = Stroke(1.6f * unit))
            }
        }
        if (done) FmtStarMark(StarState.Done, bursts)
    }
}

/** Where the glyph sits: centred, its 100-high box filling the height. */
private class Geometry(val w: Float, val h: Float, val glyph: FmtGlyph) {
    val scale = h / 100f
    val left = (w - glyph.width * scale) / 2
    val tolerance = 13f * scale
    fun x(v: Float) = left + v * scale
    fun y(v: Float) = v * scale
    fun at(p: Pair<Float, Float>) = Offset(x(p.first), y(p.second))
    fun distance(o: Offset, p: Pair<Float, Float>) = hypot(o.x - x(p.first), o.y - y(p.second))
    fun toGlyph(o: Offset) = (o.x - left) / scale to o.y / scale
    /** The point [u] of the way from point [i] to the next. */
    fun point(pts: List<Pair<Float, Float>>, i: Int, u: Float): Pair<Float, Float> {
        if (i >= pts.lastIndex) return pts.last()
        val a = pts[i]; val b = pts[i + 1]
        return (a.first + (b.first - a.first) * u) to (a.second + (b.second - a.second) * u)
    }
}

/** Where [p] falls on the line from [a] to [b]: how far along (0..1) and how far off it. */
private fun project(p: Pair<Float, Float>, a: Pair<Float, Float>, b: Pair<Float, Float>): Pair<Float, Float> {
    val dx = b.first - a.first; val dy = b.second - a.second
    val len2 = dx * dx + dy * dy
    val u = if (len2 == 0f) 0f else (((p.first - a.first) * dx + (p.second - a.second) * dy) / len2).coerceIn(0f, 1f)
    return u to hypot(p.first - (a.first + dx * u), p.second - (a.second + dy * u))
}

/** How far ahead along the stroke (in the glyph's units, 100 high) the finger can be picked up. */
private const val LOOK_AHEAD = 12f

private fun DrawScope.drawStroke(g: Geometry, pts: List<Pair<Float, Float>>, upTo: Int, color: androidx.compose.ui.graphics.Color, width: Float, dashed: Boolean = false, tip: Pair<Float, Float>? = null) {
    if (upTo < 1 && tip == null) return
    val path = Path()
    path.moveTo(g.x(pts[0].first), g.y(pts[0].second))
    for (i in 1..minOf(upTo, pts.lastIndex)) path.lineTo(g.x(pts[i].first), g.y(pts[i].second))
    if (tip != null) path.lineTo(g.x(tip.first), g.y(tip.second))
    drawPath(path, color, style = Stroke(width * g.scale, cap = StrokeCap.Round, join = StrokeJoin.Round,
        pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(4 * g.scale, 4 * g.scale)) else null))
}

/**
 * Which way to go from the gold dot: a short purple arrow along the stroke just
 * ahead of the dot, following its curve, its head pointing the way.
 */
private fun DrawScope.drawWayArrow(g: Geometry, pts: List<Pair<Float, Float>>, from: Int, color: androidx.compose.ui.graphics.Color, along: Float = 0f) {
    val begin = 7.5f
    val end = 19f
    val path = Path()
    var gone = 0f
    var started = false
    if (from >= pts.lastIndex) return
    var last = g.point(pts, from, along)
    var tail = last
    for (i in from + 1..pts.lastIndex) {
        val p = pts[i]
        gone += hypot(p.first - last.first, p.second - last.second)
        if (gone >= begin) {
            if (!started) { path.moveTo(g.x(p.first), g.y(p.second)); started = true } else path.lineTo(g.x(p.first), g.y(p.second))
            tail = last
        }
        last = p
        if (gone >= end) break
    }
    if (!started || gone < begin + 4f) return
    val w = 2f * g.scale
    drawPath(path, color, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
    // The head: two short lines back from the tip, either side of the way it's going.
    val tip = g.at(last)
    val dir = atan2(last.second - tail.second, last.first - tail.first)
    val head = 4.5f * g.scale
    for (side in listOf(-1f, 1f)) {
        val a = dir + PI.toFloat() + side * 0.6f
        drawLine(color, tip, tip + Offset(cos(a) * head, sin(a) * head), w, cap = StrokeCap.Round)
    }
}
