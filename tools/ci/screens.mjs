// Screenshot every screen at phone and laptop width and time scrolling,
// for checking the deployed design from environments that can't open it.
//   node tools/ci/screens.mjs <dist-dir> <out-dir>
import { chromium } from 'playwright';
import http from 'node:http';
import { readFile, mkdir, writeFile } from 'node:fs/promises';
import { extname, join } from 'node:path';

const [dist, out] = process.argv.slice(2);
const types = { '.html': 'text/html', '.js': 'text/javascript', '.mjs': 'text/javascript', '.wasm': 'application/wasm', '.ttf': 'font/ttf', '.svg': 'image/svg+xml', '.json': 'application/json', '.png': 'image/png', '.webmanifest': 'application/manifest+json', '.cvr': 'application/octet-stream' };
const server = http.createServer(async (req, res) => {
  const raw = decodeURIComponent(req.url.split('?')[0]);
  const path = raw.endsWith('/') ? raw + 'index.html' : raw;
  try {
    const body = await readFile(join(dist, path));
    res.writeHead(200, { 'content-type': types[extname(path)] || 'application/octet-stream' });
    res.end(body);
  } catch { res.writeHead(404); res.end(); }
}).listen(8099);
await mkdir(out, { recursive: true });
// The runner's own Chrome (no browser download needed).
const browser = await chromium.launch({ channel: 'chrome', args: ['--enable-unsafe-swiftshader'] });
const report = {};
// Opened: the splash has handed over to the app (it removes itself), then a beat for the first frame.
const opened = async (page, timeout = 30000) => {
  await page.waitForFunction(() => { const s = document.getElementById('splash'); return !s || s.classList.contains('is-done'); }, null, { timeout }).catch(() => {});
  await page.waitForTimeout(500);
};
for (const [name, viewport] of [['phone', { width: 390, height: 844 }], ['laptop', { width: 1280, height: 800 }]]) {
  for (const screen of ['', 'plan', 'activities', 'grown-ups', 'morning', 'morning/2', 'calm/1', 'calm-now', 'week-setup', 'design-system', 'install', 'install/1', 'planner']) {
    const page = await browser.newPage({ viewport, deviceScaleFactor: name === 'phone' ? 2 : 1 });
    const errors = [];
    page.on('pageerror', e => errors.push(String(e)));
    page.on('console', m => m.type() === 'error' && errors.push(m.text()));
    await page.goto(`http://localhost:8099/${screen ? '#' + screen : ''}`);
    await opened(page);
    const label = `${name}-${(screen || 'today').replace('/', '-')}`;
    await page.screenshot({ path: join(out, `${label}.png`) });
    // Scroll down in steps and record frame times.
    const timing = await page.evaluate(async () => {
      const ft = []; let last = performance.now(); let on = true;
      const loop = t => { ft.push(t - last); last = t; if (on) requestAnimationFrame(loop); };
      requestAnimationFrame(loop);
      await new Promise(r => setTimeout(r, 1500));
      on = false;
      return ft;
    }).catch(() => []);
    await page.mouse.move(viewport.width / 2, viewport.height / 2);
    const scrollTimes = await page.evaluate(() => { window.__ft = []; let l = performance.now(); window.__on = true; const f = t => { window.__ft.push(t - l); l = t; if (window.__on) requestAnimationFrame(f); }; requestAnimationFrame(f); });
    for (let i = 0; i < 12; i++) { await page.mouse.wheel(0, 120); await page.waitForTimeout(50); }
    await page.waitForTimeout(400);
    const ft = await page.evaluate(() => { window.__on = false; return window.__ft; });
    await page.screenshot({ path: join(out, `${label}-scrolled.png`) });
    const s = [...ft].slice(2).sort((a, b) => a - b);
    const pct = p => s.length ? Math.round(s[Math.floor(s.length * p)] * 10) / 10 : null;
    report[label] = { scrollFrames: s.length, medianMs: pct(0.5), p90Ms: pct(0.9), maxMs: s.length ? Math.round(s[s.length - 1]) : null, idleFrames: timing.length, errors };
    await page.close();
  }
}
// Installable and offline: the manifest and service worker are in place, and
// after one visit the app opens with the network off.
{
  const ctx = await browser.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 2 });
  const page = await ctx.newPage();
  await page.goto('http://localhost:8099/');
  await opened(page);
  await page.waitForFunction(() => !!navigator.serviceWorker.controller, null, { timeout: 60000 }).catch(() => {});
  const pwa = await page.evaluate(async () => {
    const reg = await navigator.serviceWorker.getRegistration();
    const m = await fetch('manifest.webmanifest').then(r => r.json()).catch(() => null);
    return { serviceWorker: !!reg, controlled: !!navigator.serviceWorker.controller, manifest: m && { name: m.name, display: m.display, icons: m.icons.length, theme: m.theme_color } };
  });
  await page.reload(); await opened(page); // now under the worker's control, caching everything
  await ctx.setOffline(true);
  await page.reload().catch(() => {});
  await opened(page);
  // Compose draws in a shadow root: look there for its canvas.
  pwa.offlineCanvas = await page.evaluate(() => !![document, ...[...document.querySelectorAll('*')].map(e => e.shadowRoot).filter(Boolean)].find(r => r.querySelector('canvas'))).catch(() => false);
  await page.screenshot({ path: join(out, 'phone-offline.png') });
  report.pwa = pwa;
  await ctx.close();
}
// Touch scrolling as on a phone: 3x pixels, a CPU 4x slower than the runner,
// flings up and down (to the bottom and back), every frame timed.
{
  const ctx = await browser.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 3, isMobile: true, hasTouch: true });
  const page = await ctx.newPage();
  const cdp = await ctx.newCDPSession(page);
  report.touch = {};
  for (const screen of ['', 'activities', 'week']) {
    await page.goto(`http://localhost:8099/#${screen}`);
    await opened(page);
    await cdp.send('Emulation.setCPUThrottlingRate', { rate: 4 });
    await page.evaluate(() => { window.__ft = []; let l = performance.now(); window.__on = true; const f = t => { window.__ft.push(t - l); l = t; if (window.__on) requestAnimationFrame(f); }; requestAnimationFrame(f); });
    for (const dy of [-700, -700, 900]) {
      await cdp.send('Input.synthesizeScrollGesture', { x: 195, y: 500, yDistance: dy, speed: 2400, gestureSourceType: 'touch', preventFling: false });
      await page.waitForTimeout(1200);
    }
    const ft = await page.evaluate(() => { window.__on = false; return window.__ft; });
    await cdp.send('Emulation.setCPUThrottlingRate', { rate: 1 });
    const f = ft.slice(2), sorted = [...f].sort((a, b) => a - b);
    const pct = q => Math.round(sorted[Math.floor(sorted.length * q)] * 10) / 10;
    report.touch[screen || 'today'] = { frames: f.length, medianMs: pct(0.5), p90Ms: pct(0.9), p99Ms: pct(0.99), over20: f.filter(x => x > 20).length, over34: f.filter(x => x > 34).length };
  }
  await ctx.close();
}
// Launching again opens from the phone's copy at once, even on slow 3G; a new
// build downloads in the background and takes over on the next launch.
// Four rounds, each from a fresh browser: the switch has to be reliable.
report.updateRounds = [];
for (let round = 0; round < 4; round++) {
  const ctx = await browser.newContext({ viewport: { width: 390, height: 844 } });
  const page = await ctx.newPage();
  const ready = () => page.waitForFunction(() => !document.getElementById('splash'), null, { timeout: 60000 }).then(() => true).catch(() => false);
  await page.goto('http://localhost:8099/'); await ready();
  // First visit: the worker keeps every file, then takes charge of the page.
  await page.waitForFunction(() => !!navigator.serviceWorker.controller, null, { timeout: 60000 }).catch(() => {});
  const cdp = await ctx.newCDPSession(page);
  await cdp.send('Network.enable');
  await cdp.send('Network.emulateNetworkConditions', { offline: false, latency: 400, downloadThroughput: 50 * 1024, uploadThroughput: 50 * 1024 });
  let t = Date.now(); await page.reload(); const ok = await ready();
  const update = { cachedLaunchSlow3gMs: ok ? Date.now() - t : null };
  await cdp.send('Network.emulateNetworkConditions', { offline: false, latency: 0, downloadThroughput: -1, uploadThroughput: -1 });
  // Pretend a new build is live.
  const realVersion = await readFile(join(dist, 'version.json'), 'utf8');
  const files = JSON.parse(await readFile(join(dist, 'files.json'), 'utf8'));
  const realIndex = await readFile(join(dist, 'index.html'), 'utf8');
  const built = JSON.parse(realVersion).build;
  await writeFile(join(dist, 'index.html'), realIndex.replaceAll(built, 'next'));
  await writeFile(join(dist, 'version.json'), JSON.stringify({ build: 'next' }));
  await writeFile(join(dist, 'files.json'), JSON.stringify({ ...files, build: 'next' }));
  try {
  const state = () => page.evaluate(() => fetch('__fmt/state').then(r => r.json()).catch(() => null));
  // This launch is still the old build; it tells the worker about the new one, which downloads it.
  t = Date.now(); await page.reload(); await ready(); update.launchWhileNewBuildDownloadsMs = Date.now() - t;
  await page.waitForFunction(() => fetch('__fmt/state').then(r => r.json()).then(s => s.pending === 'next').catch(() => false), null, { timeout: 60000, polling: 500 }).catch(() => {});
  update.waitingAfter = await state();
  // The next launch is the new build.
  t = Date.now(); await page.reload(); await ready(); update.launchTakingOverMs = Date.now() - t;
  update.after = await page.evaluate(() => fetch('__fmt/state').then(r => r.json()).then(s => ({ page: window.FMT_BUILD, current: s.current, pending: s.pending, previous: s.previous })).catch(e => ({ page: window.FMT_BUILD, error: String(e) })));
  update.files = files.files.length;

  } finally {
  await writeFile(join(dist, 'version.json'), realVersion);
  await writeFile(join(dist, 'files.json'), JSON.stringify(files));
  await writeFile(join(dist, 'index.html'), realIndex);
  }
  report.updateRounds.push({ page: update.after?.page, current: update.after?.current, waiting: update.waitingAfter?.pending, ms: update.launchTakingOverMs });
  if (round === 0) report.update = update;
  await ctx.close();
}
// Data saved by older versions (one key per entry) moves into the record, once.
{
  const ctx = await browser.newContext();
  const page = await ctx.newPage();
  await page.goto('http://localhost:8099/version.json');
  await page.evaluate(() => { localStorage.clear(); localStorage.setItem('fmt.calm.idea', 'Hug my dog'); localStorage.setItem('fmt.calm.chosen', '0,2'); });
  await page.goto('http://localhost:8099/');
  await opened(page);
  report.migration = await page.evaluate(() => {
    const r = JSON.parse(localStorage.getItem('fmt.record') || 'null');
    return { schema: r && r.schema, idea: r && r.entries['calm.idea'] && r.entries['calm.idea'].v, chosen: r && r.entries['calm.chosen'] && r.entries['calm.chosen'].v,
             oldKeysLeft: Object.keys(localStorage).filter(k => k !== 'fmt.record' && k.startsWith('fmt.')).length };
  });
  await ctx.close();
}
// A dropped connection while opening: the first try at a font fails. The app
// must still open (fonts retry) and the splash must hand over.
{
  const ctx = await browser.newContext({ viewport: { width: 390, height: 844 }, serviceWorkers: 'block' });
  const page = await ctx.newPage();
  let failed = 0;
  await page.route(/fredoka_600\.ttf/, route => failed++ === 0 ? route.abort() : route.continue());
  await page.goto('http://localhost:8099/');
  await opened(page);
  report.fontRetry = await page.evaluate(() => ({
    splashGone: !document.getElementById('splash') || document.getElementById('splash').classList.contains('is-done'),
    canvas: !![document, ...[...document.querySelectorAll('*')].map(e => e.shadowRoot).filter(Boolean)].find(r => r.querySelector('canvas')),
  }));
  report.fontRetry.failedRequests = failed;
  await page.screenshot({ path: join(out, 'phone-font-retry.png') });
  await ctx.close();
}
// A plan from Simone: open its link, see it, add it (the action bar's end
// button), then the Plan tab and Today follow it.
{
  const plan = { v: 1, s: '5 Steps Forward', n: 'This week we are working on getting dressed on his own.',
    r: { n: 'Morning routine', d: [0, 1, 2, 3, 4, 5, 6], t: '07:30' }, c: 1,
    p: [{ n: 'Cutting practice', d: [0, 1, 2, 3, 4, 5, 6], t: '16:00' }, { n: 'Ball skills', d: [1, 3], t: '16:30' }], m: '2026-10-30' };
  const code = Buffer.from(JSON.stringify(plan)).toString('base64url');
  const ctx = await browser.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 2 });
  const page = await ctx.newPage();
  await page.goto(`http://localhost:8099/#plan=${code}`);
  await opened(page);
  await page.screenshot({ path: join(out, 'phone-plan-import.png') });
  const address = await page.evaluate(() => location.hash);
  await page.mouse.click(390 - 90, 844 - 40); // "Add my plan"
  await page.waitForTimeout(2500);
  await page.screenshot({ path: join(out, 'phone-plan-added.png') });
  const saved = await page.evaluate(() => { const r = JSON.parse(localStorage.getItem('fmt.record') || '{}'); return r.entries && r.entries.plan && r.entries.plan.v; });
  await page.goto('http://localhost:8099/');
  await opened(page);
  await page.screenshot({ path: join(out, 'phone-today-with-plan.png') });
  report.plan = { addressAfterOpening: address, saved: saved || null };
  await ctx.close();
}
// The morning routine, tapped through end to end: Start, Done on each step
// (with a Back in the middle), the celebration, the reflection and Finish.
{
  const ctx = await browser.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 1 });
  const page = await ctx.newPage();
  const errors = [];
  page.on('pageerror', e => errors.push(String(e)));
  page.on('console', m => m.type() === 'error' && errors.push(m.text()));
  await page.goto('http://localhost:8099/#morning');
  await opened(page);
  const end = [390 - 90, 844 - 40], start = [90, 844 - 40];
  const taps = ['end', 'end', 'end', 'start', 'end', 'end', 'end', 'end', 'end', 'end', 'end', 'end'];
  report.morningWalk = [{ tap: 'open', hash: await page.evaluate(() => location.hash) }];
  for (const [i, t] of taps.entries()) {
    await page.mouse.click(...(t === 'end' ? end : start));
    await page.waitForTimeout(1200);
    report.morningWalk.push({ tap: t, hash: await page.evaluate(() => location.hash), record: await page.evaluate(() => (localStorage.getItem('fmt.record') || '').match(/routine\.done[^}]*/g)) });
    await page.screenshot({ path: join(out, `walk-morning-${String(i + 1).padStart(2, '0')}.png`) });
  }
  report.morningWalkErrors = errors;
  await ctx.close();
}
// The Planner's lock: set up with the setup code and Face ID (a virtual
// authenticator with PRF), families kept encrypted, locked after a reload,
// opened again with Face ID. The real code isn't in the repo: the test serves
// vault.js with its own code for the run.
{
  const vaultPath = join(dist, 'vault.js');
  const realVault = await readFile(vaultPath, 'utf8');
  const { pbkdf2Sync, randomBytes } = await import('node:crypto');
  const testCode = 'TEST-CODE-ABCD-2345', salt = randomBytes(16);
  const hash = pbkdf2Sync(testCode, salt, 310000, 32, 'sha256').toString('base64');
  await writeFile(vaultPath, realVault.replace(/const CODE = \{[^}]*\};/, `const CODE = { salt: "${salt.toString('base64')}", hash: "${hash}", iterations: 310000 };`));
  try {
    const ctx = await browser.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 2 });
    const page = await ctx.newPage();
    const cdp = await ctx.newCDPSession(page);
    await cdp.send('WebAuthn.enable');
    await cdp.send('WebAuthn.addVirtualAuthenticator', { options: { protocol: 'ctap2', ctap2Version: 'ctap2_1', transport: 'internal', hasResidentKey: true, hasUserVerification: true, isUserVerified: true, hasPrf: true, automaticPresenceSimulation: true } });
    await page.goto('http://localhost:8099/planner.html'); await opened(page);
    await page.screenshot({ path: join(out, 'phone-planner-setup.png') });
    const lock = { first: await page.evaluate(() => window.fmtVault.status()) };
    lock.badCode = await page.evaluate(() => window.fmtVault.setup('WRONG-CODE-0000-0000', {}));
    lock.setup = await page.evaluate((c) => window.fmtVault.setup(c, { 'planner.family.t1': '{"n":"Zebra Test"}' }), testCode);
    await page.waitForTimeout(300);
    lock.plaintextStored = await page.evaluate(() => JSON.stringify(localStorage).includes('Zebra Test'));
    await page.reload(); await opened(page);
    lock.afterReload = await page.evaluate(() => window.fmtVault.status());
    await page.screenshot({ path: join(out, 'phone-planner-locked.png') });
    await page.mouse.click(390 - 90, 844 - 40); // Open with Face ID
    await page.waitForFunction(() => window.fmtVault.status() === 'open', null, { timeout: 10000 }).catch(() => {});
    lock.afterUnlock = await page.evaluate(() => window.fmtVault.status());
    lock.family = await page.evaluate(() => window.fmtVault.get('planner.family.t1'));
    await page.waitForTimeout(800);
    await page.screenshot({ path: join(out, 'phone-planner-unlocked.png') });
    report.plannerLock = lock;
    await ctx.close();
  } catch (e) {
    report.plannerLock = { error: String(e) };
  } finally {
    await writeFile(vaultPath, realVault);
  }
}
// Simone's planner opens from its own page (planner.html): its Home Screen
// name and start, the families list, and a new plan (the action bar's end).
{
  const ctx = await browser.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 2 });
  const page = await ctx.newPage();
  await page.goto('http://localhost:8099/planner.html');
  await opened(page);
  await page.screenshot({ path: join(out, 'phone-planner-home.png') });
  report.planner = await page.evaluate(async () => {
    const m = await fetch('planner.webmanifest').then(r => r.json()).catch(() => null);
    let qrModules = 0;
    if (typeof qrcode === 'function') { const q = qrcode(0, 'M'); q.addData(location.origin + '/#plan=' + 'x'.repeat(300)); q.make(); qrModules = q.getModuleCount(); }
    return { manifest: m && { name: m.name, start: m.start_url }, qrModules, hash: location.hash };
  });
  await page.mouse.click(390 - 90, 844 - 40); // New plan
  await page.waitForTimeout(2000);
  await page.screenshot({ path: join(out, 'phone-planner-new.png') });
  await page.mouse.wheel(0, 1600); await page.waitForTimeout(800);
  await page.screenshot({ path: join(out, 'phone-planner-new-scrolled.png') });
  await ctx.close();
}
// ── Checks: each must hold, or the build fails and nothing is released. ──
const failures = [];
const check = (ok, what) => { if (!ok) failures.push(what); };
const KNOWN = /wasmExports. is deprecated/; // a Kotlin/Wasm notice, not an error of ours
for (const [k, v] of Object.entries(report)) {
  if (v && Array.isArray(v.errors)) check(v.errors.every(e => KNOWN.test(e)), `${k} opens without errors (${v.errors.filter(e => !KNOWN.test(e)).join(' | ')})`);
}
check(report.pwa?.serviceWorker && report.pwa?.controlled, 'the service worker keeps the app on the phone');
check(report.pwa?.offlineCanvas, 'the app opens with no internet');
check(report.update?.cachedLaunchSlow3gMs != null && report.update.cachedLaunchSlow3gMs < 2500, `it opens from the phone's copy within 2.5s on slow 3G (${report.update?.cachedLaunchSlow3gMs}ms)`);
check(report.updateRounds.every(r => r.waiting === 'next'), 'a new build downloads in the background');
check(report.updateRounds.length === 4 && report.updateRounds.every(r => r.current === 'next' && r.page === 'next'), `a new build takes over on the next launch, every time (${report.updateRounds.map(r => r.page === 'next' ? 'ok' : 'stuck').join(', ')})`);
check(report.migration?.idea === 'Hug my dog' && report.migration?.oldKeysLeft === 0, 'older saved data moves into the record');
check(report.fontRetry?.splashGone && report.fontRetry?.canvas, 'the app opens when a font download fails once');
check(report.plan?.saved && !String(report.plan?.addressAfterOpening).includes('plan='), "a plan link adds the plan and leaves the address");
const walk = (report.morningWalk || []).map(w => w.hash);
const done = (report.morningWalk || []).map(w => (w.record || []).join('')).pop() || '';
check(walk.includes('#morning/7') && walk.includes('#morning/8') && done.includes('"v":"0,1,2,3,4,5"'), `the morning routine taps through every step (${walk.join(' ')})`);
check(report.planner?.manifest?.name === 'Follow Me Planner' && report.planner?.qrModules > 0, "Simone's planner installs on its own and makes QR codes");
const L = report.plannerLock || {};
check(L.first === 'new' && L.badCode === 'bad-code' && L.setup === 'ok', `the Planner sets up only with the setup code and Face ID (${L.first}, ${L.badCode}, ${L.setup})`);
check(L.plaintextStored === false, "the Planner's families are stored encrypted");
check(L.afterReload === 'locked' && L.afterUnlock === 'open' && L.family === '{"n":"Zebra Test"}', `the Planner is locked when opened and opens with Face ID (${L.afterReload}, ${L.afterUnlock})`);
for (const [k, t] of Object.entries(report.touch || {})) {
  check(t.medianMs <= 17.5 && t.p90Ms <= 34, `scrolling ${k} keeps 60fps on a slow phone (median ${t.medianMs}ms, p90 ${t.p90Ms}ms)`);
}
report.failures = failures;
await writeFile(join(out, 'report.json'), JSON.stringify(report, null, 1));
console.log(JSON.stringify(report, null, 1));
await browser.close();
server.close();
for (const f of failures) console.log(`::error::Check failed: ${f}`);
process.exit(failures.length ? 1 : 0);
