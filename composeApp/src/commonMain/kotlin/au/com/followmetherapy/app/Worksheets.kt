package au.com.followmetherapy.app

import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import au.com.followmetherapy.design.tokens.FmtCategories
import androidx.compose.ui.text.font.FontWeight
import au.com.followmetherapy.design.components.FmtText
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import au.com.followmetherapy.design.components.FmtToggleChip
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import au.com.followmetherapy.design.components.ButtonKind
import au.com.followmetherapy.design.components.FmtButton
import au.com.followmetherapy.design.components.FmtGroup
import au.com.followmetherapy.design.theme.FmtTheme

// ── Printing: each activity's worksheet, the same A4 page as my printed one ──
// The sheets are made from the worksheets canvas by the website's
// tools/build_worksheet_pdfs.py into resources/worksheets: print.html shows the
// chosen ones with a Print button (one print job). Not kept for offline: only
// fetched when printed.

/** The worksheet for each how-to page (Tasks.kt), by its name. */
val TASK_WORKSHEET = mapOf(
    "Cutting practice" to "Cutting-practice", "Threading beads" to "Threading-beads", "Playdough squeeze" to "Playdough-mat",
    "Ball skills" to "Catch-and-throw", "Animal walks" to "Animal-walks", "Obstacle course" to "Obstacle-course",
    "Getting dressed" to "Getting-dressed", "Shoelaces" to "Shoelaces", "Using a spoon and fork" to "Spoon-and-fork",
    "Handwriting practice" to "Ready-to-write",
)

/** The letter sheets, four letters each, as on the canvas. */
private val LETTER_SHEETS = listOf("a-d", "e-h", "i-l", "m-p", "q-t", "u-x", "y-z")

/**
 * Opens the print page (worksheets/print.html) with one worksheet. Near the
 * top of an activity; [optional] when the app does the same thing, so the
 * paper copy is a choice, not a step.
 */
@Composable
fun PrintWorksheet(store: AppStore, sheet: String, optional: Boolean = false, label: String = "Print the worksheet") {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FmtTheme.metrics.groupGap)) {
        val busy = rememberPrintBusy(store)
        FmtButton(if (busy.on) "Opening…" else label, { busy.print(listOf(sheet)) }, kind = ButtonKind.Outline, trailingChevron = true, busy = busy.on)
        if (optional) FmtText("Optional: you can do it here in the app.", FmtTheme.type.small, Modifier.weight(1f, fill = false))
    }
}

/** One worksheet to choose: its chip's words, its sheet, its category (for the chip's colour). */
data class Sheet(val label: String, val sheet: String, val category: String = "Writing")

/**
 * Several worksheets to print together: a chip each to choose (All chooses
 * every one), in groups if given, each chip in its category's colour, then
 * Print, which opens them all as one print job.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrintSheets(store: AppStore, groups: List<Pair<String?, List<Sheet>>>, title: String = "Print worksheets") {
    val every = groups.flatMap { it.second }
    val chosen = remember { mutableStateListOf<String>() }
    val all = chosen.size == every.size
    val gap = FmtTheme.metrics.groupGap
    FmtGroup(title) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(gap)) {
            FmtToggleChip("All", all, { on -> chosen.clear(); if (on) chosen.addAll(every.map { it.sheet }) })
            if (groups.size == 1) groups[0].second.forEach { SheetChip(it, chosen) }
        }
        if (groups.size > 1) groups.forEach { (heading, sheets) ->
            heading?.let { FmtText(it, FmtTheme.type.small.copy(fontWeight = FontWeight.SemiBold)) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(gap)) {
                sheets.forEach { SheetChip(it, chosen) }
            }
        }
        Row {
            val busy = rememberPrintBusy(store)
            FmtButton(
                if (busy.on) "Opening…" else if (chosen.isEmpty()) "Print" else "Print ${chosen.size} ${if (chosen.size == 1) "sheet" else "sheets"}",
                { busy.print(every.map { it.sheet }.filter { it in chosen }) },
                kind = ButtonKind.Primary, trailingChevron = true, enabled = chosen.isNotEmpty(), busy = busy.on,
            )
        }
    }
}

@Composable
private fun SheetChip(s: Sheet, chosen: MutableList<String>) =
    FmtToggleChip(s.label, s.sheet in chosen, { on -> if (on) chosen.add(s.sheet) else chosen.remove(s.sheet) }, FmtCategories.tone(s.category))

private val WRITING_SHEETS = listOf(Sheet("Shapes", "Ready-to-write")) +
    LETTER_SHEETS.map { Sheet(it.replace("-", "–"), "Letters-$it") } +
    LETTER_SHEETS.map { Sheet(it.uppercase().replace("-", "–"), "Capitals-${it.uppercase()}") }

/** Every worksheet, by category, for Simone's Planner. */
val ALL_SHEETS: List<Pair<String, List<Sheet>>> = listOf(
    "Self-care" to listOf(Sheet("Morning routine", "Main", "Self-care"), Sheet("Getting dressed", "Getting-dressed", "Self-care"), Sheet("Shoelaces", "Shoelaces", "Self-care"), Sheet("Spoon and fork", "Spoon-and-fork", "Self-care")),
    "Feelings" to listOf(Sheet("Calm-down plan", "Calm-plan", "Feelings")),
    "Writing" to WRITING_SHEETS,
    "Fine motor" to listOf(Sheet("Cutting", "Cutting-practice", "Fine motor"), Sheet("Threading beads", "Threading-beads", "Fine motor"), Sheet("Playdough mat", "Playdough-mat", "Fine motor")),
    "Moving" to listOf(Sheet("Obstacle course", "Obstacle-course", "Moving"), Sheet("Catch and throw", "Catch-and-throw", "Moving"), Sheet("Animal walks", "Animal-walks", "Moving")),
    "Home practice" to listOf(Sheet("Practice week", "Practice-week", "Home practice")),
)

/** Every writing sheet, for the Letters and shapes page. */
@Composable
fun PrintLetterSheets(store: AppStore) = PrintSheets(store, listOf(null to WRITING_SHEETS), title = "Print worksheets")

/**
 * A print button's "Opening…" (a spinner, not tappable twice) from the tap until
 * the print page takes over. The page only changes once "Opening…" is on screen
 * (going straight away left the old frame showing, so it only appeared on the way
 * Back). Coming Back resets it; if the print page never opens, it's ready again.
 */
class PrintBusy internal constructor(private val store: AppStore, private val scope: CoroutineScope) {
    var on by mutableStateOf(false)
        private set
    private var job: Job? = null
    fun print(sheets: List<String>) {
        if (on || sheets.isEmpty()) return
        on = true
        job = scope.launch {
            // Two frames: the first draws "Opening…", the second makes sure it's been shown.
            withFrameNanos { }
            withFrameNanos { }
            store.printWorksheets(sheets)
            delay(PRINT_BUSY_MS)
            on = false
        }
    }
    internal fun reset() { job?.cancel(); on = false }
}

@Composable
fun rememberPrintBusy(store: AppStore): PrintBusy {
    val scope = rememberCoroutineScope()
    val busy = remember(store) { PrintBusy(store, scope) }
    LaunchedEffect(store.returns) { busy.reset() }
    return busy
}

private const val PRINT_BUSY_MS = 10_000L
