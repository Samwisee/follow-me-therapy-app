package au.com.followmetherapy.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
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
import au.com.followmetherapy.design.components.FmtFilterChip
import au.com.followmetherapy.design.components.FmtFlowScaffold
import au.com.followmetherapy.design.components.FmtGroup
import au.com.followmetherapy.design.components.FmtHead
import au.com.followmetherapy.design.components.FmtInfoNote
import au.com.followmetherapy.design.components.FmtText
import au.com.followmetherapy.design.components.Tone
import au.com.followmetherapy.design.components.WriteField
import au.com.followmetherapy.design.icons.FmtIcons
import au.com.followmetherapy.design.theme.FmtTheme
import au.com.followmetherapy.design.tokens.FmtSpace

/** Where requests go: a private repo Claude checks each day (see the scheduled task). */
const val REQUESTS_REPO = "https://github.com/Samwisee/follow-me-requests"

/** The form on GitHub, filled in (issue forms take their fields from the address). */
fun requestUrl(bug: Boolean, text: String, build: String): String {
    fun enc(s: String) = buildString {
        for (b in s.encodeToByteArray()) {
            val c = b.toInt().toChar()
            if (c.isLetterOrDigit() && b >= 0 || c in "-_.~") append(c) else append('%').append((b.toInt() and 0xff).toString(16).uppercase().padStart(2, '0'))
        }
    }
    val title = text.lineSequence().firstOrNull { it.isNotBlank() }?.trim()?.take(70) ?: ""
    return "$REQUESTS_REPO/issues/new?template=${if (bug) "bug.yml" else "idea.yml"}&title=${enc(title)}&details=${enc(text)}&where=${enc("The Planner")}&build=${enc(build)}"
}

/** Something's wrong, or an idea: written here, sent to the private requests repo (or anywhere, by the share sheet). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RequestScreen(platform: Platform, nav: Nav) {
    var bug by remember { mutableStateOf(true) }
    var text by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val build = remember { platform.build() }
    val body = { (if (bug) "Something's wrong" else "An idea") + "\n\n" + text.trim() + "\n\n(Follow Me Planner, build $build)" }
    FmtFlowScaffold(end = FmtAction("Send on GitHub", chevron = true, enabled = text.isNotBlank()) { platform.openUrl(requestUrl(bug, text.trim(), build)) }) {
        FmtHead("Request a change", swash = "change", tag = "Planner")
        FmtGroup {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(FmtSpace.s2), verticalArrangement = Arrangement.spacedBy(FmtSpace.s2)) {
                FmtFilterChip("Something's wrong", bug, { bug = true }, tone = Tone.Pink)
                FmtFilterChip("An idea", !bug, { bug = false }, tone = Tone.Teal)
            }
            FmtText(if (bug) "What happened, and what did you expect? Which screen were you on?" else "What would you like to change or add, and why?", FmtTheme.type.body)
            WriteField(text, { text = it }, placeholder = if (bug) "When I tapped…" else "It would help if…", kid = false, minLines = 4)
        }
        FmtInfoNote(
            "don't put families' names or details in here. Requests are kept in a private GitHub repo that only Sam, I and Claude can see.",
            leadIn = "Privacy:", tone = Tone.Teal, icon = FmtIcons.shieldTick,
        )
        FmtGroup("Not signed in to GitHub?") {
            FmtText("Send it to Sam another way instead: Messages, Mail or anything on the share sheet.", FmtTheme.type.small)
            Row {
                FmtButton("Share it instead", {
                    platform.shareText("Request for the Planner", body()) { shared ->
                        if (!shared) platform.copyText(body()) { ok -> message = if (ok) "Copied. Paste it into a message to Sam." else body() }
                    }
                }, kind = ButtonKind.Outline)
            }
            message?.let { FmtText(it, FmtTheme.type.small, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        }
    }
}
