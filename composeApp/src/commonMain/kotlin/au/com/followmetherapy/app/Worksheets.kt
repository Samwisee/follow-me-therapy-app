package au.com.followmetherapy.app

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

/** Opens the print page (worksheets/print.html) with one worksheet, ready to print. */
@Composable
fun PrintWorksheet(store: AppStore, sheet: String, label: String = "Print the worksheet") {
    Row { FmtButton(label, { store.printWorksheets(listOf(sheet)) }, kind = ButtonKind.Outline, trailingChevron = true) }
}

/**
 * Several worksheets to print together: a chip each to choose (All chooses
 * every one), then Print, which opens them as one print job.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrintSheets(store: AppStore, sheets: List<Pair<String, String>>, title: String = "Print worksheets") {
    val chosen = remember { mutableStateListOf<String>() }
    val all = chosen.size == sheets.size
    FmtGroup(title) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(FmtTheme.metrics.groupGap), verticalArrangement = Arrangement.spacedBy(FmtTheme.metrics.groupGap)) {
            FmtToggleChip("All", all, { on -> chosen.clear(); if (on) chosen.addAll(sheets.map { it.second }) })
            sheets.forEach { (label, sheet) ->
                FmtToggleChip(label, sheet in chosen, { on -> if (on) chosen.add(sheet) else chosen.remove(sheet) })
            }
        }
        Row {
            FmtButton(
                if (chosen.isEmpty()) "Print" else "Print ${chosen.size} ${if (chosen.size == 1) "sheet" else "sheets"}",
                { store.printWorksheets(sheets.map { it.second }.filter { it in chosen }) },
                kind = ButtonKind.Primary, trailingChevron = true, enabled = chosen.isNotEmpty(),
            )
        }
    }
}

/** Every writing sheet, for the Letters and shapes page. */
@Composable
fun PrintLetterSheets(store: AppStore) = PrintSheets(
    store,
    listOf("Shapes" to "Ready-to-write") +
        LETTER_SHEETS.map { it.replace("-", "–") to "Letters-$it" } +
        LETTER_SHEETS.map { it.uppercase().replace("-", "–") to "Capitals-${it.uppercase()}" },
    title = "Print a worksheet",
)
