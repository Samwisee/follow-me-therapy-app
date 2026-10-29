package au.com.followmetherapy.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.localStorage
import kotlinx.browser.window

private fun jsToday(): String = js("(() => { const d = new Date(); return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0'); })()")
private fun jsWeekStart(): String = js("(() => { const d = new Date(); d.setDate(d.getDate() - ((d.getDay() + 6) % 7)); return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0'); })()")
private fun jsWeekday(): Int = js("(new Date().getDay() + 6) % 7")
private fun jsHour(): Int = js("new Date().getHours()")

/** The browser: local storage on this device (private windows may forget it) and the device clock. */
private object WebPlatform : Platform {
    private const val PREFIX = "fmt."
    override fun load(key: String): String? = try { localStorage.getItem(PREFIX + key) } catch (e: Throwable) { null }
    override fun save(key: String, value: String?) {
        try { if (value == null) localStorage.removeItem(PREFIX + key) else localStorage.setItem(PREFIX + key, value) } catch (e: Throwable) { }
    }
    override fun today() = jsToday()
    override fun weekday() = jsWeekday()
    override fun hour() = jsHour()
    override fun weekStart() = jsWeekStart()
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Each place in the app has its own address (#today, #calm, #morning…),
    // replaced rather than pushed, so the browser's Back button leaves the app.
    val start = window.location.hash.removePrefix("#")
    ComposeViewport("app") {
        App(platform = WebPlatform, startAt = start, onNavigate = { slug ->
            window.history.replaceState(null, "", if (slug.isEmpty()) window.location.pathname else "#$slug")
        })
    }
}
