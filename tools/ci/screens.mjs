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
for (const [name, viewport] of [['phone', { width: 390, height: 844 }], ['laptop', { width: 1280, height: 800 }]]) {
  for (const screen of ['', 'plan', 'activities', 'grown-ups', 'morning', 'morning/2', 'calm/1', 'calm-now', 'week-setup', 'design-system', 'install', 'install/1', 'planner', 'planner/1']) {
    const page = await browser.newPage({ viewport, deviceScaleFactor: name === 'phone' ? 2 : 1 });
    const errors = [];
    page.on('pageerror', e => errors.push(String(e)));
    page.on('console', m => m.type() === 'error' && errors.push(m.text()));
    await page.goto(`http://localhost:8099/${screen ? '#' + screen : ''}`);
    await page.waitForTimeout(4000);
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
  await page.waitForTimeout(5000);
  const pwa = await page.evaluate(async () => {
    const reg = await navigator.serviceWorker.getRegistration();
    const m = await fetch('manifest.webmanifest').then(r => r.json()).catch(() => null);
    return { serviceWorker: !!reg, controlled: !!navigator.serviceWorker.controller, manifest: m && { name: m.name, display: m.display, icons: m.icons.length, theme: m.theme_color } };
  });
  await page.reload(); await page.waitForTimeout(4000); // now under the worker's control, caching everything
  await ctx.setOffline(true);
  await page.reload().catch(() => {});
  await page.waitForTimeout(5000);
  // Compose draws in a shadow root: look there for its canvas.
  pwa.offlineCanvas = await page.evaluate(() => !![document, ...[...document.querySelectorAll('*')].map(e => e.shadowRoot).filter(Boolean)].find(r => r.querySelector('canvas'))).catch(() => false);
  await page.screenshot({ path: join(out, 'phone-offline.png') });
  report.pwa = pwa;
  await ctx.close();
}
// Data saved by older versions (one key per entry) moves into the record, once.
{
  const ctx = await browser.newContext();
  const page = await ctx.newPage();
  await page.goto('http://localhost:8099/version.json');
  await page.evaluate(() => { localStorage.clear(); localStorage.setItem('fmt.calm.idea', 'Hug my dog'); localStorage.setItem('fmt.calm.chosen', '0,2'); });
  await page.goto('http://localhost:8099/');
  await page.waitForTimeout(5000);
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
  await page.waitForTimeout(8000);
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
  await page.waitForTimeout(5000);
  await page.screenshot({ path: join(out, 'phone-plan-import.png') });
  const address = await page.evaluate(() => location.hash);
  await page.mouse.click(390 - 90, 844 - 40); // "Add my plan"
  await page.waitForTimeout(2500);
  await page.screenshot({ path: join(out, 'phone-plan-added.png') });
  const saved = await page.evaluate(() => { const r = JSON.parse(localStorage.getItem('fmt.record') || '{}'); return r.entries && r.entries.plan && r.entries.plan.v; });
  await page.goto('http://localhost:8099/');
  await page.waitForTimeout(4000);
  await page.screenshot({ path: join(out, 'phone-today-with-plan.png') });
  report.plan = { addressAfterOpening: address, saved: saved || null };
  await ctx.close();
}
await writeFile(join(out, 'report.json'), JSON.stringify(report, null, 1));
console.log(JSON.stringify(report, null, 1));
await browser.close();
server.close();
