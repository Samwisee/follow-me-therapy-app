package au.com.followmetherapy.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import au.com.followmetherapy.design.components.ButtonKind
import au.com.followmetherapy.design.components.FmtAction
import au.com.followmetherapy.design.components.FmtFlowScaffold
import au.com.followmetherapy.design.components.FmtGroup
import au.com.followmetherapy.design.components.FmtHead
import au.com.followmetherapy.design.components.FmtInfoNote
import au.com.followmetherapy.design.components.FmtText
import au.com.followmetherapy.design.components.Tone
import au.com.followmetherapy.design.icons.FmtIcons
import au.com.followmetherapy.design.theme.FmtTheme
import au.com.followmetherapy.design.tokens.FmtColors
import au.com.followmetherapy.design.tokens.FmtSpace

private val STEPS = mapOf(
    Device.IPhone to ("On an iPhone or iPad" to listOf(
        "Open this page in Safari.",
        "Tap Share (the square with an arrow) at the bottom of the screen, or at the top on an iPad.",
        "Scroll down and tap Add to Home Screen.",
        "Tap Add, then open Follow Me from your Home Screen.",
    )),
    Device.Android to ("On an Android phone or tablet" to listOf(
        "Open this page in Chrome.",
        "Tap the menu (three dots) at the top right.",
        "Tap Add to Home screen, then Install.",
        "Open Follow Me from your Home Screen or your apps.",
    )),
    Device.Computer to ("On a computer" to listOf(
        "Open this page in Chrome or Edge.",
        "Click the install button at the right of the address bar (a screen with an arrow), or open the menu and choose Install.",
        "On a Mac with Safari, choose File › Add to Dock instead.",
    )),
)

/**
 * How to add the app to the Home Screen (a progressive web app): this
 * device's steps first, then the others. Where the browser can install it in
 * one tap (Chrome, Edge), the action bar offers Install now.
 */
@Composable
fun InstallScreen(platform: Platform, nav: Nav) {
    val device = remember { platform.device() }
    val installed = remember { platform.installed() }
    var canInstall by remember { mutableStateOf(platform.canInstall()) }
    val back = { nav.go(Route(Area.GrownUps)) }
    FmtFlowScaffold(
        start = if (canInstall && !installed) FmtAction("Not now", ButtonKind.Outline) { back() } else null,
        end = if (canInstall && !installed) FmtAction("Install now") { platform.install { ok -> if (ok) canInstall = false } }
        else FmtAction("Done") { back() },
    ) {
        FmtHead("Add to your Home Screen", swash = "Home", tag = "For grown-ups", tone = Tone.Purple)
        if (installed) {
            FmtInfoNote("you're using the app from your Home Screen.", leadIn = "All set:", tone = Tone.Teal, icon = FmtIcons.tick)
        } else {
            FmtText(
                "Then Follow Me opens like an app, without the browser around it, works without the internet, and your progress is less likely to be cleared.",
                FmtTheme.type.lead,
            )
        }
        val order = listOf(device) + Device.entries.filter { it != device }
        order.forEach { d ->
            val (title, steps) = STEPS.getValue(d)
            FmtGroup(title) {
                steps.forEachIndexed { i, step -> NumberedLine(i + 1, step) }
            }
        }
        FmtInfoNote(
            "on an iPhone or iPad the Home Screen app keeps its own copy of your progress, separate from Safari. If you've started in Safari, save a backup there first (For grown-ups › Save a backup), then restore it in the app.",
            leadIn = "Already using it in Safari?", tone = Tone.Sun,
        )
    }
}

@Composable
private fun NumberedLine(n: Int, text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FmtSpace.s2)) {
        FmtText("$n.", FmtTheme.type.body, Modifier.widthIn(min = FmtSpace.s5), color = FmtColors.purpleInk)
        FmtText(text, FmtTheme.type.body, Modifier.weight(1f))
    }
}
