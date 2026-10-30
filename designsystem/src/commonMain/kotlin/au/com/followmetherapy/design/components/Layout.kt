package au.com.followmetherapy.design.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import au.com.followmetherapy.design.icons.FmtIcon
import au.com.followmetherapy.design.tokens.FmtSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.scale
import au.com.followmetherapy.design.tokens.FmtMotion
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import au.com.followmetherapy.design.icons.FmtIcons
import au.com.followmetherapy.design.theme.FmtTheme
import au.com.followmetherapy.design.tokens.FmtBorder
import au.com.followmetherapy.design.tokens.FmtCardTokens
import au.com.followmetherapy.design.tokens.FmtColors
import au.com.followmetherapy.design.tokens.FmtHeadTokens
import au.com.followmetherapy.design.tokens.FmtNoteTokens
import au.com.followmetherapy.design.tokens.FmtSwashTokens
import au.com.followmetherapy.design.tokens.FmtShadow
import au.com.followmetherapy.design.tokens.FmtSpace

/**
 * One activity on a screen (phone, iPad, laptop, the app): the screen
 * counterpart of a worksheet. Blocks sit `stack` apart, the column is at most
 * `content` wide with `gutter` at the edges. Put it in an [FmtPage] to scroll.
 */
@Composable
fun FmtScreen(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val m = FmtTheme.metrics
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = m.content).fillMaxWidth().padding(horizontal = m.gutter).padding(top = m.stack, bottom = m.sectionSpace),
            verticalArrangement = Arrangement.spacedBy(m.stack),
            content = content,
        )
    }
}

/**
 * A heading and what it introduces, group-gap apart. Inset to the text edge
 * (card-pad + outline), so it lines up under the words in the header and
 * reads as belonging to its heading; containers (header, notes) span wider.
 */
@Composable
fun FmtGroup(title: String? = null, modifier: Modifier = Modifier, inset: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    val m = FmtTheme.metrics
    Column(
        modifier.fillMaxWidth().padding(horizontal = if (inset) m.textInset else 0.dp),
        verticalArrangement = Arrangement.spacedBy(m.groupGap),
    ) {
        if (title != null) FmtText(title, FmtTheme.type.groupTitle, Modifier.semantics { heading() })
        content()
    }
}

/** Words a child reads. */
@Composable
fun FmtKidText(text: String, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Unspecified) =
    FmtText(text, FmtTheme.type.kid, modifier, align = align)

/**
 * A title with the sun swash under one key word (one swash per page).
 * The squiggle repeats along the word and leaves room for itself below.
 */
@Composable
fun FmtSwashTitle(text: String, swash: String?, modifier: Modifier = Modifier, style: androidx.compose.ui.text.TextStyle = FmtTheme.type.title) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val start = swash?.let { text.indexOf(it) } ?: -1
    BasicText(
        text = text,
        style = style,
        onTextLayout = { layout = it },
        modifier = modifier
            .semantics { heading() }
            .padding(bottom = FmtSpace.s3)
            .drawBehind {
                val l = layout ?: return@drawBehind
                if (start < 0 || swash == null) return@drawBehind
                val end = start + swash.length
                val line = l.getLineForOffset(start)
                val left = l.getBoundingBox(start).left
                val right = l.getBoundingBox(end - 1).right
                val y = l.getLineBottom(line) + FmtSpace.s1.toPx()
                val s = FmtSwashTokens
                val path = squigglePath(left, right - left, y, s.height.toPx() / 4, s.wavelength.toPx() / 2)
                drawPath(path, s.colour, style = Stroke(width = s.stroke.toPx(), cap = StrokeCap.Round))
            },
    )
}

/**
 * The header panel of an activity: tinted in its tone and outlined in the
 * tone's line colour, title (with one swash) and the tag beneath it. The
 * corner nests: radius-tile + card-pad + outline.
 */
@Composable
fun FmtHead(title: String, swash: String?, tag: String, tone: Tone = Tone.Purple, modifier: Modifier = Modifier,
            note: String? = null, noteLeadIn: String? = null, trailing: (@Composable () -> Unit)? = null) {
    val m = FmtTheme.metrics
    val ground = FmtHeadTokens.fill(tone)
    Row(
        modifier.fillMaxWidth()
            .background(ground, RoundedCornerShape(m.containerRadius))
            .border(FmtBorder.outline, FmtHeadTokens.line(tone), RoundedCornerShape(m.containerRadius))
            .padding(m.cardPad + FmtBorder.outline),
        horizontalArrangement = Arrangement.spacedBy(m.gridGap),
        verticalAlignment = Alignment.Bottom,
    ) {
        FmtOnGround(ground) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(m.groupGap)) {
                FmtSwashTitle(title, swash)
                FmtTag(tag, tone)
                // A note that belongs to the header (From Simone: …) sits inside it, under the tag.
                note?.let {
                    FmtText(
                        androidx.compose.ui.text.buildAnnotatedString {
                            noteLeadIn?.let { l -> pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)); append("$l "); pop() }
                            append(it)
                        },
                        FmtTheme.type.body,
                    )
                }
            }
        }
        FmtOnGround(ground) { trailing?.invoke() }
    }
}

/**
 * A centred section title with the short hand-drawn squiggle in the band's
 * accent, space-4 under it, and an optional one-line subtitle.
 */
@Composable
fun FmtSectionTitle(title: String, accent: Tone = Tone.Purple, subtitle: String? = null, modifier: Modifier = Modifier) {
    val accentColor = when (accent) {
        Tone.Teal -> FmtColors.teal
        Tone.Sun -> FmtColors.sun
        Tone.Pink -> FmtColors.pink
        Tone.Purple -> FmtColors.purple
    }
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(FmtSpace.s4)) {
        FmtText(title, FmtTheme.type.heading, Modifier.semantics { heading() }, align = TextAlign.Center)
        Box(
            Modifier.widthIn(min = FmtSpace.s20, max = FmtSpace.s20).fillMaxWidth().padding(vertical = FmtSpace.s2)
                .drawBehind {
                    val s = FmtSwashTokens
                    val path = squigglePath(FmtSpace.s1.toPx(), size.width - FmtSpace.s2.toPx(), size.height / 2, s.height.toPx() / 4, s.wavelength.toPx() / 2)
                    drawPath(path, accentColor, style = Stroke(width = s.stroke.toPx(), cap = StrokeCap.Round))
                },
        ) {}
        if (subtitle != null) FmtText(subtitle, FmtTheme.type.lead, align = TextAlign.Center)
    }
}

/**
 * A tinted, outlined note with its icon tile. Centred: the tile sits card-pad
 * from the top, left and bottom, card-pad from the words, which centre beside it.
 * The lead-in ("For grown-ups:") is bold ink.
 */
@Composable
fun FmtInfoNote(text: String, leadIn: String? = null, tone: Tone = Tone.Purple, icon: ImageVector = FmtIcons.info, modifier: Modifier = Modifier) {
    val m = FmtTheme.metrics
    val t = FmtNoteTokens
    val ground = t.fill(tone)
    val shape = RoundedCornerShape(m.containerRadius)
    val words: AnnotatedString = buildAnnotatedString {
        if (leadIn != null) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = t.leadIn)) { append(leadIn) }
            append(" ")
        }
        append(text)
    }
    Row(
        modifier.fillMaxWidth().background(ground, shape).border(FmtBorder.outline, t.line(tone), shape).padding(m.cardPad + FmtBorder.outline),
        horizontalArrangement = Arrangement.spacedBy(m.cardPad),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FmtIconTile(icon, tone, small = true, ground = t.tileFill)
        FmtText(words, FmtTheme.type.body.copy(lineHeight = FmtTheme.type.body.fontSize * 1.5f), Modifier.weight(1f))
    }
}

/**
 * A service-style card: icon tile, title beside it, a sentence or two, and a
 * centred "Learn more ›" line. Outlined in its tone; corner nests round the tile.
 */
@Composable
fun FmtCard(title: String, body: String, icon: ImageVector, tone: Tone = Tone.Purple, more: String? = "Learn more", onClick: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    val m = FmtTheme.metrics
    val t = FmtCardTokens
    val shape = RoundedCornerShape(m.containerRadius)
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusVisibleAsState()
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    val active = onClick != null && (hovered || focused) && !pressed
    val lift by animateDpAsState(if (active) -FmtSpace.s1 else 0.dp, tween(FmtMotion.durSlow, easing = FmtMotion.easeSpring))
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, tween(FmtMotion.durBase, easing = FmtMotion.easeSpring))
    Column(
        modifier.fillMaxWidth()
            .offset(y = lift)
            .scale(scale)
            .fmtFocusRing(focused, shape)
            .fmtShadow(if (active) FmtShadow.raised else FmtShadow.rest, shape)
            .background(t.fill, shape)
            .border(FmtBorder.outline, t.line(tone), shape)
            .then(if (onClick != null) Modifier.hoverable(source).clickableNoRipple(source, onClick) else Modifier)
            .padding(m.cardPad + FmtBorder.outline),
        verticalArrangement = Arrangement.spacedBy(m.groupGap),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(m.cardPad), verticalAlignment = Alignment.CenterVertically) {
            FmtIconTile(icon, tone, small = true)
            FmtText(title, FmtTheme.type.subheading.copy(fontSize = FmtTheme.metrics.textLg, fontWeight = FontWeight.SemiBold), Modifier.weight(1f))
        }
        FmtText(body, FmtTheme.type.body)
        if (more != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                FmtText("$more ›", FmtTheme.type.chip.copy(fontSize = FmtTheme.metrics.textMd), color = t.more)
            }
        }
    }
}

internal fun Modifier.clickableNoRipple(source: MutableInteractionSource, onClick: () -> Unit) =
    clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)

/**
 * Plain lines, each with a teal tick circle (the website's checklist and the
 * worksheets' tick list). Words in body ink-soft; [bold] lines in ink.
 */
@Composable
fun FmtTickList(items: List<String>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(FmtSpace.s3)) {
        items.forEach { item ->
            Row(horizontalArrangement = Arrangement.spacedBy(FmtSpace.s3), verticalAlignment = Alignment.Top) {
                Box(Modifier.padding(top = FmtSpace.hair).size(FmtSize.iconSm).background(FmtColors.teal, CircleShape), contentAlignment = Alignment.Center) {
                    FmtIcon(FmtIcons.tick, FmtColors.paper, FmtSize.iconSm * 0.65f)
                }
                FmtText(item, FmtTheme.type.body, Modifier.weight(1f))
            }
        }
    }
}

/**
 * A tinted section of a screen: a heading and what it holds on the tone's
 * ground, outlined in its line, corner nested. Its words start card-pad +
 * outline in, the same text edge as every group and note on the screen.
 */
@Composable
fun FmtPanel(title: String? = null, tone: Tone = Tone.Purple, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val m = FmtTheme.metrics
    val t = au.com.followmetherapy.design.tokens.FmtPanelTokens
    val shape = RoundedCornerShape(m.containerRadius)
    val ground = t.fill(tone)
    FmtOnGround(ground) {
        Column(
            modifier.fillMaxWidth().background(ground, shape).border(FmtBorder.outline, t.line(tone), shape).padding(m.cardPad + FmtBorder.outline),
            verticalArrangement = Arrangement.spacedBy(m.groupGap),
        ) {
            if (title != null) FmtText(title, FmtTheme.type.groupTitle, Modifier.semantics { heading() })
            content()
        }
    }
}
