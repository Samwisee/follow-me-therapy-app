package au.com.followmetherapy.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Each screen has its own address (#calm, #morning, #week), replaced rather
    // than pushed so the browser's Back button leaves the app, like the website.
    val start = window.location.hash.removePrefix("#")
    ComposeViewport("app") {
        App(startAt = start, onNavigate = { slug ->
            window.history.replaceState(null, "", if (slug.isEmpty()) window.location.pathname else "#$slug")
        })
    }
}
