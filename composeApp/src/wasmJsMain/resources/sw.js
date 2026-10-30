// Follow Me Therapy: opens straight from the phone, works without the internet.
//
// One worker (sw.js, no version in its address) keeps whole builds, each in
// its own cache (fmt-<build>, every file listed in that build's files.json),
// and remembers which one it serves ("current") and which is downloaded and
// ready ("pending"), in the fmt-state cache.
//   - Every launch after the first opens from the phone's copy, at once.
//   - The page tells the worker the newest build (version.json); the worker
//     downloads all of it in the background (all or nothing).
//   - The next launch (a page load) switches to it before anything is served,
//     so a build never mixes with another and nothing waits on the browser's
//     worker hand-over.
// version.json, files.json and sw.js itself always come from the network.
const STATE = "fmt-state";
const STATE_KEY = "/__fmt/state";

const readState = async () => {
  const r = await (await caches.open(STATE)).match(STATE_KEY);
  return r ? r.json() : { current: null, pending: null, previous: null };
};
const writeState = async (s) => (await caches.open(STATE)).put(STATE_KEY, new Response(JSON.stringify(s), { headers: { "content-type": "application/json" } }));
const here = (f) => new URL(f, self.registration.scope).href;

// Download a whole build into its own cache; true when every file is in.
let downloading = null;
const download = (build) => downloading || (downloading = (async () => {
  try {
    const list = await fetch(here("files.json") + "?t=" + Date.now(), { cache: "no-store" }).then((r) => r.json());
    if (list.build !== build) return false; // the live build moved on; next time
    const c = await caches.open("fmt-" + build);
    await Promise.all(list.files.map(async (f) => {
      if (await c.match(here(f))) return;
      // A file named by its content (the .wasm files) is the same in any build: reuse it.
      if (/[0-9a-f]{16,}\./.test(f)) { const kept = await caches.match(here(f)); if (kept) return c.put(here(f), kept); }
      const res = await fetch(here(f) + "?v=" + build, { cache: "no-cache" });
      if (!res.ok) throw new Error(f + " " + res.status);
      await c.put(here(f), res);
    }));
    return true;
  } catch (e) {
    return false;
  } finally {
    downloading = null;
  }
})());

// Make the newest build ready: on the first visit it becomes current at once;
// later it waits as pending until the next launch.
const prepare = async (build) => {
  const s = await readState();
  if (!build || build === s.current || build === s.pending) return s;
  if (!(await download(build))) return s;
  const t = await readState();
  if (!t.current) t.current = build; else t.pending = build;
  await writeState(t);
  return t;
};

// A launch: switch to a downloaded build, keep the one before for any page
// still open, and clear the rest.
const launch = async () => {
  const s = await readState();
  if (s.pending) {
    s.previous = s.current; s.current = s.pending; s.pending = null;
    await writeState(s);
    const keep = new Set([STATE, "fmt-" + s.current, "fmt-" + s.previous]);
    caches.keys().then((ks) => ks.filter((k) => k.startsWith("fmt-") && !keep.has(k)).forEach((k) => caches.delete(k)));
  }
  return s;
};

self.addEventListener("install", (e) => {
  self.skipWaiting(); // a new sw.js (rare) takes over at once: builds are kept the same way
  e.waitUntil(fetch(here("version.json") + "?t=" + Date.now(), { cache: "no-store" }).then((r) => r.json()).then((v) => prepare(v.build)).catch(() => {}));
});
self.addEventListener("activate", (e) => {
  // Older workers kept one cache per build under fmt-<build> too; keep only ours.
  e.waitUntil(readState().then((s) => caches.keys().then((ks) => Promise.all(ks
    .filter((k) => k.startsWith("fmt-") && ![STATE, "fmt-" + s.current, "fmt-" + s.previous, "fmt-" + s.pending].includes(k))
    .map((k) => caches.delete(k))))).then(() => self.clients.claim()));
});
self.addEventListener("message", (e) => {
  if (e.data && e.data.type === "fmt-newest") e.waitUntil(prepare(e.data.build));
});

self.addEventListener("fetch", (e) => {
  const r = e.request;
  const url = new URL(r.url);
  if (r.method !== "GET" || url.origin !== self.location.origin) return;
  if (url.pathname === new URL(STATE_KEY, self.registration.scope).pathname || url.pathname.endsWith(STATE_KEY)) {
    e.respondWith(readState().then((s) => new Response(JSON.stringify(s), { headers: { "content-type": "application/json" } })));
    return;
  }
  if (/\/(version\.json|files\.json|sw\.js)$/.test(url.pathname)) return;
  e.respondWith((async () => {
    const s = r.mode === "navigate" ? await launch() : await readState();
    // Pages: the page this build stored (any #hash or ?v= is the same page).
    const key = r.mode === "navigate" ? here(url.pathname.endsWith("planner.html") ? "planner.html" : "index.html") : r.url;
    for (const b of [s.current, s.previous]) {
      if (!b) continue;
      const hit = await (await caches.open("fmt-" + b)).match(key, { ignoreSearch: true });
      if (hit) return hit;
    }
    // Not kept (the first visit, or a file outside the list): the network.
    try {
      return await fetch(r);
    } catch (err) {
      return Response.error();
    }
  })());
});
