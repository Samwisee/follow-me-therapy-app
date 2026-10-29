package au.com.followmetherapy.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.localStorage
import kotlinx.browser.window

private fun jsToday(): String = js("(() => { const d = new Date(); return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0'); })()")
private fun jsWeekStart(): String = js("(() => { const d = new Date(); d.setDate(d.getDate() - ((d.getDay() + 6) % 7)); return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0'); })()")
private fun jsWeekday(): Int = js("(new Date().getDay() + 6) % 7")
private fun jsHour(): Int = js("new Date().getHours()")
private fun jsNow(): Double = js("Date.now()")
private fun jsBuild(): String = js("(window.FMT_BUILD && window.FMT_BUILD.indexOf('__') !== 0) ? window.FMT_BUILD : 'dev'")

/** Old versions kept one local-storage key per entry ("fmt.calm.idea"); this lists them as "key\u001evalue\u001d…". */
private fun jsLegacy(): String = js("""(() => { const out = []; try { for (let i = 0; i < localStorage.length; i++) { const k = localStorage.key(i); if (k && k.indexOf('fmt.') === 0 && k !== 'fmt.record') out.push(k.slice(4) + '\u001e' + localStorage.getItem(k)); } } catch (e) {} return out.join('\u001d'); })()""")
private fun jsClearLegacy(): Unit = js("""{ try { const ks = []; for (let i = 0; i < localStorage.length; i++) { const k = localStorage.key(i); if (k && k.indexOf('fmt.') === 0 && k !== 'fmt.record') ks.push(k); } ks.forEach(k => localStorage.removeItem(k)); } catch (e) {} }""")

/** A file to keep: shared (so it can go to Files on an iPhone, even from the Home Screen app), or downloaded. */
private fun jsSaveFile(name: String, text: String): Unit = js("""{
  const file = new File([text], name, { type: 'application/json' });
  const download = () => { const a = document.createElement('a'); a.href = URL.createObjectURL(file); a.download = name; document.body.appendChild(a); a.click(); setTimeout(() => { URL.revokeObjectURL(a.href); a.remove(); }, 1000); };
  const touch = matchMedia('(pointer: coarse)').matches;
  if (touch && navigator.canShare && navigator.canShare({ files: [file] })) navigator.share({ files: [file], title: name }).catch(e => { if (e && e.name !== 'AbortError') download(); });
  else download();
}""")
private fun jsOpenFile(onText: (String) -> Unit): Unit = js("""{
  const i = document.createElement('input'); i.type = 'file'; i.accept = '.json,application/json';
  i.onchange = () => { const f = i.files && i.files[0]; if (f) f.text().then(t => onText(t)); };
  i.click();
}""")
/** 1 kept, 0 not kept, -1 can't tell. */
private fun jsKeepData(onResult: (Int) -> Unit): Unit = js("""{
  if (!(navigator.storage && navigator.storage.persist)) { onResult(-1); return; }
  navigator.storage.persisted().then(p => p ? true : navigator.storage.persist()).then(p => onResult(p ? 1 : 0)).catch(() => onResult(-1));
}""")
private fun jsInstalled(): Boolean = js("matchMedia('(display-mode: standalone)').matches || navigator.standalone === true")
private fun jsDevice(): Int = js("""(() => { const u = navigator.userAgent; if (/iPhone|iPad|iPod/.test(u) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)) return 0; if (/Android/.test(u)) return 1; return 2; })()""")
private fun jsCanInstall(): Boolean = js("!!window.fmtInstallPrompt")
private fun jsInstall(onResult: (Boolean) -> Unit): Unit = js("""{
  const e = window.fmtInstallPrompt; if (!e) { onResult(false); return; }
  e.prompt(); e.userChoice.then(c => { window.fmtInstallPrompt = null; onResult(c.outcome === 'accepted'); }).catch(() => onResult(false));
}""")
private fun jsSafeBottom(): Double = js("""(() => { const d = document.createElement('div'); d.style.cssText = 'position:fixed;bottom:0;height:0;padding-bottom:env(safe-area-inset-bottom)'; document.body.appendChild(d); const v = parseFloat(getComputedStyle(d).paddingBottom) || 0; d.remove(); return v; })()""")

/** The browser: one record in local storage on this device (private windows may forget it), its files and its clock. */
private object WebPlatform : Platform {
    private const val KEY = "fmt.record"
    override fun loadRecord(): String? = try { localStorage.getItem(KEY) } catch (e: Throwable) { null }
    override fun saveRecord(json: String) { try { localStorage.setItem(KEY, json) } catch (e: Throwable) { } }
    override fun legacyEntries(): Map<String, String> =
        jsLegacy().split('\u001d').filter { it.isNotEmpty() }.associate { it.substringBefore('\u001e') to it.substringAfter('\u001e') }
    override fun clearLegacy() = jsClearLegacy()
    override fun now() = jsNow().toLong()
    override fun today() = jsToday()
    override fun weekday() = jsWeekday()
    override fun hour() = jsHour()
    override fun weekStart() = jsWeekStart()
    override fun build() = jsBuild()
    override fun saveFile(name: String, text: String) = jsSaveFile(name, text)
    override fun openFile(onText: (String) -> Unit) = jsOpenFile(onText)
    override fun keepData(onResult: (Boolean?) -> Unit) = jsKeepData { onResult(if (it < 0) null else it == 1) }
    override fun installed() = jsInstalled()
    override fun device() = when (jsDevice()) { 0 -> Device.IPhone; 1 -> Device.Android; else -> Device.Computer }
    override fun canInstall() = jsCanInstall()
    override fun install(onResult: (Boolean) -> Unit) = jsInstall(onResult)
    override fun safeBottom() = jsSafeBottom().toFloat()
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Each place in the app has its own address (#today, #calm, #morning…),
    // replaced rather than pushed, so the browser's Back button leaves the app.
    val start = window.location.hash.removePrefix("#")
    ComposeViewport("app") {
        App(platform = WebPlatform, startAt = start, onNavigate = { slug ->
            window.history.replaceState(null, "", if (slug.isEmpty()) window.location.pathname + window.location.search else "#$slug")
        })
    }
}
