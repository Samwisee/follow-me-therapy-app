package au.com.followmetherapy.design.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import au.com.followmetherapy.design.tokens.FmtSize

/*
 * The design system's line icons (round caps and joins, no fill), drawn from
 * the same SVG paths as the website and worksheets. 24×24 icons go beside
 * words (stroke 2.2); 40×40 icons go in an IconTile (stroke 2.6).
 */

private fun circle(cx: Float, cy: Float, r: Float) =
    "M${cx - r},$cy a$r,$r 0 1,0 ${2 * r},0 a$r,$r 0 1,0 ${-2 * r},0"

private fun rect(x: Float, y: Float, w: Float, h: Float, rx: Float) =
    "M${x + rx},$y H${x + w - rx} A$rx,$rx 0 0 1 ${x + w},${y + rx} V${y + h - rx} " +
        "A$rx,$rx 0 0 1 ${x + w - rx},${y + h} H${x + rx} A$rx,$rx 0 0 1 $x,${y + h - rx} " +
        "V${y + rx} A$rx,$rx 0 0 1 ${x + rx},$y Z"

private fun icon(name: String, viewport: Float, stroke: Float, paths: List<String>, filled: Boolean = false): ImageVector {
    val b = ImageVector.Builder(name = name, defaultWidth = Dp(viewport), defaultHeight = Dp(viewport), viewportWidth = viewport, viewportHeight = viewport)
    for (d in paths) {
        b.addPath(
            pathData = addPathNodes(d),
            fill = if (filled) SolidColor(Color.Black) else null,
            stroke = if (filled) null else SolidColor(Color.Black),
            strokeLineWidth = stroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    return b.build()
}

private fun i24(name: String, vararg d: String) = icon(name, 24f, 2.2f, d.toList())
private fun i40(name: String, vararg d: String) = icon(name, 40f, 2.6f, d.toList())

object FmtIcons {
    // 24×24, beside words
    val smile = i24("smile", circle(12f, 12f, 9f), "M8.5 14.5c1.8 2 5.2 2 7 0", "M9 9.8v.1M15 9.8v.1")
    val video = i24("video", rect(3f, 6f, 13f, 12f, 3f), "M16 10.5l5-3v9l-5-3")
    val pin = i24("pin", "M12 21s-6.5-6-6.5-11a6.5 6.5 0 0 1 13 0c0 5-6.5 11-6.5 11z", circle(12f, 10f, 2.4f))
    val info = i24("info", circle(12f, 12f, 9f), "M12 11v5.5", "M12 7.6v.1")
    val book = i24("book", "M3 5.5c3-1.3 6-1.3 9 .5 3-1.8 6-1.8 9-.5V19c-3-1.3-6-1.3-9 .5-3-1.8-6-1.8-9-.5z", "M12 6v13.5")
    val heart = i24("heart", "M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10z")
    val chevron = i24("chevron", "M9 5l7 7-7 7")
    val back = i24("back", "M15 5l-7 7 7 7")
    val tick = i24("tick", "M5 12.5l4.5 4.5L19 7.5")
    val star = i24("star", "M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.8l6.1-.9z")
    val starFilled = icon("star-filled", 24f, 2.2f, listOf("M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.8l6.1-.9z"), filled = true)

    // 40×40, in tiles
    val stairs = i40("stairs", "M6 31h8v-7h7v-7h7v-7h6")
    val blocks = i40("blocks", rect(6f, 22f, 12f, 12f, 2.5f), rect(22f, 22f, 12f, 12f, 2.5f), rect(14f, 7f, 12f, 12f, 2.5f))
    val clipboard = i40("clipboard", rect(9f, 7f, 22f, 28f, 4f), rect(15f, 4f, 10f, 6f, 2f), "M15 22l4 4 7-8")
    val umbrella = i40("umbrella", "M6 20a14 14 0 0 1 28 0", "M6 20c2.3-2.2 4.7-2.2 7 0 2.3-2.2 4.7-2.2 7 0 2.3-2.2 4.7-2.2 7 0 2.3-2.2 4.7-2.2 7 0", "M20 20v10.5a3.5 3.5 0 0 1-7 0", "M20 6V4")
    val medicareCross = i40("medicare-cross", rect(7f, 7f, 26f, 26f, 7f), "M20 13v14M13 20h14")
    val shieldTick = i40("shield-tick", "M20 5l12 5v9c0 8-5 13-12 16C13 32 8 27 8 19v-9z", "M15 20l4 4 7-8")
    val breathe = i40("breathe", "M7 16h17a5 5 0 1 0-5-5", "M7 24h21a5 5 0 1 1-5 5", "M7 20h11")
    val cushion = i40("cushion", "M9 11c7 3 15 3 22 0c-2 6-2 12 0 18c-7-3-15-3-22 0c2-6 2-12 0-18z")
    val pushWall = i40("push-wall", "M32 6v28", "M26 13v11", "M22 11v13", "M18 12v12", "M14 16v11a6 6 0 0 0 6 6h3a3 3 0 0 0 3-3v-6")
    val water = i40("water", "M11 9h18l-2.5 23h-13z", "M12.5 18h15")
    val hug = i40("hug", "M20 32s-11-7-11-15a6 6 0 0 1 11-3.5a6 6 0 0 1 11 3.5c0 8-11 15-11 15z")
    val quietSpot = i40("quiet-spot", "M5 32L20 8l15 24z", "M15 32l5-8 5 8")
    val sunrise = i40("sunrise", "M8 28h24", "M13 28a7 7 0 0 1 14 0", "M20 13v3", "M10.5 17.5l2 2", "M29.5 17.5l-2 2", "M12 33h16")
    val calendar = i40("calendar", rect(7f, 9f, 26f, 24f, 4f), "M7 16h26", "M14 6v6M26 6v6", "M14 23l3 3 6-6")
}

/** Draws an icon in [tint]. Decorative unless [label] is given. */
@Composable
fun FmtIcon(icon: ImageVector, tint: Color, size: Dp = FmtSize.icon, label: String? = null, modifier: Modifier = Modifier) {
    Image(
        imageVector = icon,
        contentDescription = label,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(size),
    )
}
