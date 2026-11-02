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
                    tracing = g.distance(down.position, pts[reached]) < g.tolerance
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        if (!tracing) continue
                        change.consume()
                        // Follow the finger forward along the stroke, a little at a time.
                        val ahead = (reached + 1..minOf(reached + 14, pts.lastIndex))
                        val next = ahead.minByOrNull { g.distance(change.position, pts[it]) }
                        if (next != null && g.distance(change.position, pts[next]) < g.tolerance) reached = next
                        if (reached >= pts.lastIndex - 1) {
                            stroke++; reached = 0; tracing = false
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
                        if (i == stroke && reached > 0) drawStroke(g, pts, reached, t.model, 5f)
                    }
                }
            }
            if (!done) {
                val pts = glyph.strokes[stroke]
                val at = g.at(pts[reached])
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
}

private fun DrawScope.drawStroke(g: Geometry, pts: List<Pair<Float, Float>>, upTo: Int, color: androidx.compose.ui.graphics.Color, width: Float, dashed: Boolean = false) {
    if (upTo < 1) return
    val path = Path()
    path.moveTo(g.x(pts[0].first), g.y(pts[0].second))
    for (i in 1..upTo) path.lineTo(g.x(pts[i].first), g.y(pts[i].second))
    drawPath(path, color, style = Stroke(width * g.scale, cap = StrokeCap.Round, join = StrokeJoin.Round,
        pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(4 * g.scale, 4 * g.scale)) else null))
}
