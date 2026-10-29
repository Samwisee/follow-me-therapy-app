package au.com.followmetherapy.design.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import au.com.followmetherapy.design.icons.FmtIcon
import au.com.followmetherapy.design.icons.FmtIcons
import au.com.followmetherapy.design.theme.FmtTheme
import au.com.followmetherapy.design.tokens.FmtBorder
import au.com.followmetherapy.design.tokens.FmtBreakpoint
import au.com.followmetherapy.design.tokens.FmtColors
import au.com.followmetherapy.design.tokens.FmtLayout
import au.com.followmetherapy.design.tokens.FmtMotion
import au.com.followmetherapy.design.tokens.FmtShadow
import au.com.followmetherapy.design.tokens.FmtSize
import au.com.followmetherapy.design.tokens.FmtSpace

/**
 * The website's section edge: a band's colour as a gentle wave, the same
 * 240×40 tile repeated every 12 waves' width. [down] hangs it below a band
 * (the inner pages' top bar); otherwise it rises into the band above.
 */
@Composable
fun FmtWave(color: Color, modifier: Modifier = Modifier, down: Boolean = false) {
    val h = FmtLayout.wave.max.dp
    Canvas(modifier.fillMaxWidth().height(h)) {
        val tileW = h.toPx() * 12
        val sx = tileW / 240f
        val sy = size.height / 40f
        val p = Path()
        var x0 = 0f
        while (x0 < size.width) {
            // M0 40V22C40 6 80 6 120 22S200 38 240 22V40Z (S reflected: C160 6 → 160 38)
            p.moveTo(x0, 40f * sy)
            p.lineTo(x0, 22f * sy)
            p.cubicTo(x0 + 40f * sx, 6f * sy, x0 + 80f * sx, 6f * sy, x0 + 120f * sx, 22f * sy)
            p.cubicTo(x0 + 160f * sx, 38f * sy, x0 + 200f * sx, 38f * sy, x0 + 240f * sx, 22f * sy)
            p.lineTo(x0 + 240f * sx, 40f * sy)
            p.close()
            x0 += tileW - 0.5f
        }
        if (down) {
            // Flip: the band's colour hangs from the top edge.
            val q = Path()
            q.addPath(p)
            withTransform({ scale(1f, -1f, pivot = center) }) { drawPath(q, color) }
        } else {
            drawPath(p, color)
        }
    }
}

/**
 * A full-width section band in one tint (lilac, butter, mint, paper or deep),
 * its content in the centred column, section-top above and section-space below.
 */
@Composable
fun FmtBand(
    ground: Color,
    modifier: Modifier = Modifier,
    waveTop: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val m = FmtTheme.metrics
    Column(modifier.fillMaxWidth()) {
        if (waveTop) FmtWave(ground, Modifier.offset(y = 1.dp))
        Box(Modifier.fillMaxWidth().background(ground), contentAlignment = Alignment.TopCenter) {
            FmtOnGround(ground) {
                Column(
                    Modifier.widthIn(max = m.content).fillMaxWidth().padding(horizontal = m.gutter, vertical = m.sectionTop),
                    verticalArrangement = Arrangement.spacedBy(m.stack),
                    content = content,
                )
            }
        }
    }
}

/**
 * The inner pages' back button: a paper pill outlined in purple-line, a
 * chevron and the label; steps left and lifts on hover, like the website.
 */
@Composable
fun FmtBackButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val focused by source.collectIsFocusVisibleAsState()
    val nudge by animateDpAsState(if (hovered || focused) -FmtSpace.s1 else 0.dp, tween(FmtMotion.durBase, easing = FmtMotion.easeSpring))
    Row(
        modifier
            .offset(x = nudge)
            .fmtFocusRing(focused, CircleShape)
            .fmtShadow(if (hovered) FmtShadow.soft else FmtShadow.rest, CircleShape)
            .background(FmtColors.paper, CircleShape)
            .border(FmtBorder.outline, FmtColors.purpleLine, CircleShape)
            .hoverable(source)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .heightIn(min = FmtSize.target)
            .padding(start = FmtSpace.s3, end = FmtSpace.s4),
        horizontalArrangement = Arrangement.spacedBy(FmtSpace.s1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FmtIcon(FmtIcons.back, FmtColors.purple, FmtSize.iconSm)
        FmtText(label, FmtTheme.type.button.copy(fontSize = FmtTheme.metrics.textMd, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = FmtColors.purple)
    }
}

/**
 * The top bar: lilac, the practice name (mark + words at weight-brand),
 * an optional back button, and the wave hanging beneath it.
 */
@Composable
fun FmtSiteHead(back: (() -> Unit)? = null, backLabel: String = "Back", modifier: Modifier = Modifier, wave: Boolean = true) {
    val m = FmtTheme.metrics
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().background(FmtColors.purpleTint), contentAlignment = Alignment.Center) {
            Row(
                Modifier.widthIn(max = m.content).fillMaxWidth().padding(horizontal = m.gutter, vertical = FmtSpace.s3),
                horizontalArrangement = Arrangement.spacedBy(m.gridGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (back != null) FmtBackButton(backLabel, back)
                // The name stays on one line; on a narrow phone it steps down a size.
                FmtLockup(Modifier.semantics { contentDescription = "Follow Me Therapy" }, compact = m.widthDp < FmtBreakpoint.xs.value)
            }
        }
        if (wave) FmtWave(FmtColors.purpleTint, Modifier.offset(y = (-1).dp), down = true)
    }
}

/** The deep footer band: logo in white, sign-off and the copyright line. */
@Composable
fun FmtFooter(modifier: Modifier = Modifier) {
    val m = FmtTheme.metrics
    Column(modifier.fillMaxWidth()) {
        FmtWave(FmtColors.deep, Modifier.offset(y = 1.dp))
        Box(Modifier.fillMaxWidth().background(FmtColors.deep), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = m.content).fillMaxWidth().padding(horizontal = m.gutter, vertical = m.stack),
                verticalArrangement = Arrangement.spacedBy(m.groupGap),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FmtText("Simone · Paediatric OT · followmetherapy.com.au", FmtTheme.type.small, color = FmtColors.paper, align = TextAlign.Center)
                FmtText("© Follow Me Therapy 2026", FmtTheme.type.small.copy(fontSize = m.textXs), color = FmtColors.paper.copy(alpha = 0.85f), align = TextAlign.Center)
            }
        }
    }
}

/**
 * A whole page: the top bar, the content and the footer scrolling together,
 * on paper-white. [key] resets the scroll position when the screen changes.
 */
@Composable
fun FmtPage(
    key: Any?,
    back: (() -> Unit)? = null,
    backLabel: String = "Back",
    heroBelow: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scroll = androidx.compose.runtime.key(key) { androidx.compose.foundation.rememberScrollState() }
    Column(modifier.fillMaxWidth().background(FmtColors.paper).verticalScroll(scroll)) {
        // On a page that opens with a lilac hero, the bar runs straight into it (no wave).
        FmtSiteHead(back, backLabel, wave = !heroBelow)
        Column(Modifier.fillMaxWidth(), content = content)
        FmtFooter()
    }
}

