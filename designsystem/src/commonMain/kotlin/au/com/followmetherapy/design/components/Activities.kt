package au.com.followmetherapy.design.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import au.com.followmetherapy.design.icons.FmtIcon
import au.com.followmetherapy.design.icons.FmtIcons
import au.com.followmetherapy.design.theme.FmtTheme
import au.com.followmetherapy.design.tokens.FmtBorder
import au.com.followmetherapy.design.tokens.FmtColors
import au.com.followmetherapy.design.tokens.FmtMotion
import au.com.followmetherapy.design.tokens.FmtRadius
import au.com.followmetherapy.design.tokens.FmtSize
import au.com.followmetherapy.design.tokens.FmtSpace

data class Choice(val label: String, val icon: ImageVector? = null)

/**
 * Tiles to choose from. On paper a child circles them; on a screen each tile
 * is a toggle (checkbox role, 44dp+), pressed tiles fill with the tint and take
 * a double outline in the tone (not colour alone). At most three across, two on
 * a phone. Icons sit level along a row; labels centre under them.
 */
@Composable
fun FmtChoices(
    items: List<Choice>,
    selected: Set<Int>,
    onToggle: (Int) -> Unit,
    tone: Tone = Tone.Purple,
    modifier: Modifier = Modifier,
) {
    val m = FmtTheme.metrics
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val minTile = FmtSpace.s30 + FmtSpace.s5
        val fit = ((maxWidth + m.gridGap) / (minTile + m.gridGap)).toInt()
        val cols = fit.coerceIn(1, 3)
        Column(verticalArrangement = Arrangement.spacedBy(m.gridGap)) {
            items.withIndex().chunked(cols).forEach { row ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(m.gridGap)) {
                    row.forEach { (i, c) ->
                        ChoiceTile(c, i in selected, { onToggle(i) }, tone, Modifier.weight(1f).fillMaxHeight())
                    }
                    repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun ChoiceTile(choice: Choice, on: Boolean, toggle: () -> Unit, tone: Tone, modifier: Modifier) {
    val m = FmtTheme.metrics
    val t = au.com.followmetherapy.design.tokens.FmtChoiceTokens
    val shape = RoundedCornerShape(m.containerRadius)
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusVisibleAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(FmtMotion.durBase, easing = FmtMotion.easeSpring))
    val ground by animateColorAsState(if (on) t.fillSelected(tone) else t.fill, tween(FmtMotion.durFast, easing = FmtMotion.easeStandard))
    val line = if (on) t.lineSelected(tone) else if (hovered) t.lineHover(tone) else t.line(tone)
    Column(
        modifier
            .scale(scale)
            .fmtFocusRing(focused, shape)
            .background(ground, shape)
            .border(if (on) FmtBorder.outline * 2 else FmtBorder.outline, line, shape)
            .hoverable(source)
            .toggleable(value = on, interactionSource = source, indication = null, role = Role.Checkbox, onValueChange = { toggle() })
            .semantics { stateDescription = if (on) "chosen" else "not chosen" }
            .heightIn(min = FmtSize.target)
            .padding(m.cardPad),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(m.groupGap),
    ) {
        if (choice.icon != null) FmtIconTile(choice.icon, tone, small = true)
        Box(Modifier.weight(1f, fill = true).fillMaxWidth(), contentAlignment = Alignment.Center) {
            FmtText(choice.label, FmtTheme.type.kid, align = TextAlign.Center)
        }
    }
}

/**
 * Numbered steps for a routine. Tap a step to mark it done: the number fills
 * teal and screen readers hear "done". The numbers come from the order.
 */
@Composable
fun FmtStepList(steps: List<String>, done: Set<Int>, onToggle: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(FmtSpace.s2)) {
        steps.forEachIndexed { i, label -> StepRow(i + 1, label, i in done) { onToggle(i) } }
    }
}

@Composable
private fun StepRow(n: Int, label: String, on: Boolean, toggle: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusVisibleAsState()
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(FmtMotion.durBase, easing = FmtMotion.easeSpring))
    val disc by animateColorAsState(if (on) FmtColors.teal else FmtColors.purpleTint, tween(FmtMotion.durBase, easing = FmtMotion.easeStandard))
    val shape = RoundedCornerShape(FmtRadius.pill)
    Row(
        Modifier.fillMaxWidth()
            .scale(scale)
            .fmtFocusRing(focused, shape)
            .toggleable(value = on, interactionSource = source, indication = null, role = Role.Checkbox, onValueChange = { toggle() })
            .semantics { stateDescription = if (on) "done" else "to do" }
            .heightIn(min = FmtSize.target),
        horizontalArrangement = Arrangement.spacedBy(FmtSpace.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(FmtSpace.s12).background(disc, CircleShape).border(FmtBorder.outline, if (on) FmtColors.teal else FmtColors.purpleLine, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (on) FmtIcon(FmtIcons.tick, FmtColors.paper, FmtSize.icon)
            else FmtText(n.toString(), FmtTheme.type.button.copy(fontSize = FmtTheme.metrics.textLg), color = FmtColors.purpleInk)
        }
        FmtText(label, FmtTheme.type.kid, Modifier.weight(1f))
    }
}

/**
 * A week of practice: a row per activity (typed in by the grown-up), a star per
 * day to tap. Stars are outlined purple, and filled sun when done (with a
 * sun-deep outline, so done and not-done differ in shape and lightness too).
 */
@Composable
fun FmtTracker(
    activities: List<String>,
    onRename: (Int, String) -> Unit,
    done: Set<Pair<Int, Int>>,
    onToggle: (Int, Int) -> Unit,
    days: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
    modifier: Modifier = Modifier,
) {
    val type = FmtTheme.type
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val narrow = maxWidth < FmtSpace.s30 * 5
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(FmtSpace.s1)) {
            Row(Modifier.fillMaxWidth().padding(bottom = FmtSpace.s1), verticalAlignment = Alignment.CenterVertically) {
                if (!narrow) FmtText("My activity", type.chip.copy(fontSize = FmtTheme.metrics.textMd), Modifier.weight(2.2f))
                days.forEach { FmtText(if (narrow) it.take(1) else it, type.chip, Modifier.weight(1f), align = TextAlign.Center) }
            }
            activities.forEachIndexed { r, name ->
                if (narrow) {
                    WriteField(name, { onRename(r, it) }, placeholder = "Activity ${r + 1}", kid = false)
                }
                Row(Modifier.fillMaxWidth().dashedBottom(), verticalAlignment = Alignment.CenterVertically) {
                    if (!narrow) {
                        Box(Modifier.weight(2.2f).padding(end = FmtSpace.s2)) {
                            WriteField(name, { onRename(r, it) }, placeholder = "Activity ${r + 1}", kid = false, underline = false)
                        }
                    }
                    days.forEachIndexed { d, day ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            FmtStarToggle((r to d) in done, "$day, ${name.ifBlank { "activity ${r + 1}" }}") { onToggle(r, d) }
                        }
                    }
                }
            }
        }
    }
}

/** A dashed write-on line under the content, in `rule` (3:1 or better on every ground). */
fun Modifier.dashedBottom(color: Color = FmtColors.rule): Modifier = drawBehind {
    val w = FmtBorder.outline.toPx()
    val y = size.height - w / 2
    drawLine(color, Offset(0f, y), Offset(size.width, y), w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * 3, w * 3)))
}

/**
 * On paper these are lines to write on; on a screen they're a text field that
 * keeps the dashed rule underneath. [kid] uses the child-sized text.
 */
@Composable
fun WriteField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    kid: Boolean = true,
    minLines: Int = 1,
    underline: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val style = if (kid) FmtTheme.type.kid else FmtTheme.type.body.copy(color = FmtColors.ink)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = style,
        minLines = minLines,
        cursorBrush = SolidColor(FmtColors.purple),
        modifier = modifier.fillMaxWidth().heightIn(min = FmtSize.target).then(if (underline) Modifier.dashedBottom() else Modifier).padding(vertical = FmtSpace.s2),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) FmtText(placeholder, style.copy(color = FmtColors.inkSoft))
                inner()
            }
        },
    )
}
