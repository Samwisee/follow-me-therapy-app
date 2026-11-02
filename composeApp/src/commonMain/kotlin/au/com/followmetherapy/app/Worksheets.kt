package au.com.followmetherapy.app

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
// The PDFs are made from the worksheets canvas by the website's
// tools/build_worksheet_pdfs.py into resources/worksheets (not kept for
// offline: they're only fetched when printed).

/** The worksheet for each how-to page (Tasks.kt), by its name. */
val TASK_WORKSHEET = mapOf(
    "Cutting practice" to "Cutting-practice", "Threading beads" to "Threading-beads", "Playdough squeeze" to "Playdough-mat",
    "Ball skills" to "Catch-and-throw", "Animal walks" to "Animal-walks", "Obstacle course" to "Obstacle-course",
    "Getting dressed" to "Getting-dressed", "Shoelaces" to "Shoelaces", "Using a spoon and fork" to "Spoon-and-fork",
    "Handwriting practice" to "Ready-to-write",
)

/** The letter sheets, four letters each, as on the canvas. */
private val LETTER_SHEETS = listOf("a-d", "e-h", "i-l", "m-p", "q-t", "u-x", "y-z")

/** Opens the worksheet's PDF in a new tab, to print from there. */
@Composable
fun PrintWorksheet(store: AppStore, sheet: String, label: String = "Print the worksheet") {
    Row { FmtButton(label, { store.openWorksheet(sheet) }, kind = ButtonKind.Outline, trailingChevron = true) }
}

/** Every writing sheet, for the Letters and shapes page. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrintLetterSheets(store: AppStore) {
    FmtGroup("Print a worksheet") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(FmtTheme.metrics.groupGap), verticalArrangement = Arrangement.spacedBy(FmtTheme.metrics.groupGap)) {
            FmtButton("Shapes", { store.openWorksheet("Ready-to-write") }, kind = ButtonKind.Outline)
            LETTER_SHEETS.forEach { r -> FmtButton(r.replace("-", "–"), { store.openWorksheet("Letters-$r") }, kind = ButtonKind.Outline) }
            LETTER_SHEETS.forEach { r -> FmtButton(r.uppercase().replace("-", "–"), { store.openWorksheet("Capitals-${r.uppercase()}") }, kind = ButtonKind.Outline) }
        }
    }
}
