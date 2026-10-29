// Screenshot every screen at phone and laptop width and time scrolling,
// for checking the deployed design from environments that can't open it.
//   node tools/ci/screens.mjs <dist-dir> <out-dir>
import { chromium } from 'playwright';
import http from 'node:http';
import { readFile, mkdir, writeFile } from 'node:fs/promises';
import { extname, join } from 'node:path';

const [dist, out] = process.argv.slice(2);
const types = { '.html': 'text/html', '.js': 'text/javascript', '.mjs': 'text/javascript', '.wasm': 'application/wasm', '.ttf': 'font/ttf', '.svg': 'image/svg+xml', '.json': 'application/json', '.cvr': 'application/octet-stream' };
const server = http.createServer(async (req, res) => {
  const path = decodeURIComponent(req.url.split('?')[0]);
  try {
    const body = await readFile(join(dist, path === '/' ? 'index.html' : path));
    res.writeHead(200, { 'content-type': types[extname(path)] || 'application/octet-stream' });
    res.end(body);
  } catch { res.writeHead(404); res.end(); }
}).listen(8099);
await mkdir(out, { recursive: true });
// The runner's own Chrome (no browser download needed).
const browser = await chromium.launch({ channel: 'chrome', args: ['--enable-unsafe-swiftshader'] });
const report = {};
for (const [name, viewport] of [['phone', { width: 390, height: 844 }], ['laptop', { width: 1280, height: 800 }]]) {
  for (const screen of ['', 'morning', 'calm', 'week', 'design-system']) {
    const page = await browser.newPage({ viewport, deviceScaleFactor: name === 'phone' ? 2 : 1 });
    const errors = [];
    page.on('pageerror', e => errors.push(String(e)));
    page.on('console', m => m.type() === 'error' && errors.push(m.text()));
    await page.goto(`http://localhost:8099/${screen ? '#' + screen : ''}`);
    await page.waitForTimeout(4000);
    const label = `${name}-${screen || 'home'}`;
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
await writeFile(join(out, 'report.json'), JSON.stringify(report, null, 1));
console.log(JSON.stringify(report, null, 1));
await browser.close();
server.close();
