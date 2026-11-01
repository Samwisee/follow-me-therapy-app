package au.com.followmetherapy.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import au.com.followmetherapy.design.components.ButtonKind
import au.com.followmetherapy.design.components.FmtAction
import au.com.followmetherapy.design.components.FmtButton
import au.com.followmetherapy.design.components.FmtFlowScaffold
import au.com.followmetherapy.design.components.FmtGroup
import au.com.followmetherapy.design.components.FmtHead
import au.com.followmetherapy.design.components.FmtInfoNote
import au.com.followmetherapy.design.components.FmtText
import au.com.followmetherapy.design.components.Tone
import au.com.followmetherapy.design.components.WriteField
import au.com.followmetherapy.design.icons.FmtIcons
import au.com.followmetherapy.design.theme.FmtTheme
import androidx.compose.foundation.layout.Row

/**
 * The Planner opens only with Face ID (or Touch ID) on a phone where it's been
 * set up with the setup code; until then its families stay encrypted (vault.js).
 * It locks itself after five minutes in the background.
 */
@Composable
fun PlannerLock(store: AppStore, platform: Platform, content: @Composable () -> Unit) {
    var status by remember { mutableStateOf(platform.vaultStatus()) }
    LaunchedEffect(Unit) { platform.onVaultLock { status = "locked" } }
    when (status) {
        "open" -> content()
        "new" -> PlannerSetup(store, platform) { status = it }
        "locked" -> PlannerUnlock(platform) { status = it }
        "unsupported" -> FmtFlowScaffold {
            FmtHead("My families", swash = "families", tag = "Planner")
            FmtGroup { FmtText("The Planner opens with Face ID, Touch ID or Windows Hello, and this browser can't do that here.", FmtTheme.type.lead)
                FmtText("It works in Safari on an iPhone or iPad with iOS 18 or later, in Safari or Chrome on a Mac with Touch ID, and in Chrome or Edge on Windows with Windows Hello.", FmtTheme.type.body) }
        }
        else -> FmtFlowScaffold {
            FmtHead("My families", swash = "families", tag = "Planner")
            FmtGroup {
                FmtText("The Planner has its own page, with its lock.", FmtTheme.type.lead)
                Row { FmtButton("Open the Planner", { platform.openPlanner() }, trailingChevron = true) }
            }
        }
    }
}

@Composable
private fun PlannerSetup(store: AppStore, platform: Platform, onStatus: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    FmtFlowScaffold(end = FmtAction(if (busy) "Setting up…" else if (platform.device() == Device.IPhone) "Set up with Face ID" else "Set up", chevron = true, enabled = !busy && code.isNotBlank()) {
        busy = true; message = null
        platform.vaultSetup(code, store.plannerSeedJson()) { r ->
            busy = false
            when (r) {
                "ok" -> { store.clearOpenPlanner(); onStatus("open") }
                "bad-code" -> message = "That setup code isn't right. Check it with Sam."
                "cancelled" -> message = "Face ID was cancelled. Try again when you're ready."
                "unsupported" -> onStatus("unsupported")
                else -> message = "Something went wrong setting up. Try again."
            }
        }
    }) {
        FmtHead("Set up the Planner", swash = "Planner", tag = "Planner")
        FmtGroup {
            FmtText("The Planner is just for Sam and me. Set it up once on each phone or computer with the setup code, then it opens with Face ID, Touch ID or Windows Hello.", FmtTheme.type.lead)
            WriteField(code, { code = it }, placeholder = "Setup code", kid = false)
            message?.let { FmtText(it, FmtTheme.type.body, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        }
        FmtInfoNote(
            "families' plans are kept on this device, locked so only Face ID or Touch ID on this device can open them. Each phone or computer keeps its own.",
            leadIn = "Privacy:", tone = Tone.Teal, icon = FmtIcons.shieldTick,
        )
    }
}

@Composable
private fun PlannerUnlock(platform: Platform, onStatus: (String) -> Unit) {
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    FmtFlowScaffold(end = FmtAction(if (busy) "Opening…" else if (platform.device() == Device.IPhone) "Open with Face ID" else "Unlock", chevron = true, enabled = !busy) {
        busy = true; message = null
        platform.vaultUnlock { r ->
            busy = false
            when (r) {
                "ok" -> onStatus("open")
                "cancelled" -> message = "Face ID was cancelled."
                else -> message = "It didn't open. Try again."
            }
        }
    }) {
        FmtHead("My families", swash = "families", tag = "Planner")
        FmtGroup {
            FmtText(if (platform.device() == Device.IPhone) "Locked. Open it with Face ID." else "Locked. Unlock it with Touch ID or Windows Hello.", FmtTheme.type.lead)
            message?.let { FmtText(it, FmtTheme.type.body, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        }
        FmtGroup("Can't open it?") {
            FmtText("If Face ID no longer opens it (a new phone, or the passkey was deleted), you can start again on this phone. The families saved here can't be recovered.", FmtTheme.type.small)
            Row {
                FmtButton(if (confirmReset) "Tap again to erase and start again" else "Start again on this phone", {
                    if (confirmReset) { platform.vaultReset(); onStatus("new") } else confirmReset = true
                }, kind = ButtonKind.Outline)
            }
        }
    }
}
