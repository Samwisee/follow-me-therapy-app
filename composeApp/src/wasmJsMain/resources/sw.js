// Follow Me Therapy: opens straight from the phone, works without the internet.
//
// Each build has its own cache (sw.js?v=<build>). Installing a build's worker
// downloads every file of that build (files.json) in the background while the
// app keeps running from the previous build's cache; only when all of it is
// in does the new worker wait to take over, which it does on the next launch
// (index.html asks it to before the app starts). So:
//   - every launch after the first opens from the phone's copy, at once, even
//     on a slow connection or none;
//   - a new build never mixes with an old one, and arrives one launch later.
// version.json is never cached (it's how a page learns there's a new build).
const BUILD = new URL(self.location.href).searchParams.get("v") || "dev";
const CACHE = "fmt-" + BUILD;

self.addEventListener("install", (e) => {
  e.waitUntil((async () => {
    const list = await fetch("files.json?v=" + BUILD, { cache: "no-store" }).then((r) => r.json()).catch(() => null);
    const files = list && list.files ? list.files : ["index.html", "planner.html", "manifest.webmanifest", "planner.webmanifest"];
    const c = await caches.open(CACHE);
    // All or nothing: a half-downloaded build would mix files; then the old one stays.
    await Promise.all(files.map(async (f) => {
      const res = await fetch(f + (f.includes("?") ? "&" : "?") + "v=" + BUILD, { cache: "no-cache" });
      if (!res.ok) throw new Error(f + " " + res.status);
      await c.put(new URL(f, self.location.href).href, res);
    }));
    // The very first install has no app to wait for: start serving straight away.
    if (!(await self.registration.active)) self.skipWaiting();
  })());
});

self.addEventListener("message", (e) => { if (e.data === "fmt-take-over") self.skipWaiting(); });

self.addEventListener("activate", (e) => {
  e.waitUntil(caches.keys()
    .then((ks) => Promise.all(ks.filter((k) => k.startsWith("fmt-") && k !== CACHE).map((k) => caches.delete(k))))
    .then(() => self.clients.claim()));
});

self.addEventListener("fetch", (e) => {
  const r = e.request;
  const url = new URL(r.url);
  if (r.method !== "GET" || url.origin !== self.location.origin) return;
  if (url.pathname.endsWith("version.json") || url.pathname.endsWith("files.json") || url.pathname.endsWith("sw.js")) return;
  e.respondWith((async () => {
    const c = await caches.open(CACHE);
    // Pages: the page this build stored (any #hash or ?v= is the same page).
    let key = r;
    if (r.mode === "navigate") key = new URL(url.pathname.endsWith("planner.html") ? "planner.html" : "index.html", self.location.href).href;
    const hit = await c.match(key, { ignoreSearch: true });
    if (hit) return hit;
    // Not stored (the first visit, or a file the list missed): the network, kept for next time.
    try {
      const res = await fetch(r);
      if (res.ok && r.mode !== "navigate") c.put(r, res.clone());
      return res;
    } catch (err) {
      return (await caches.match(key, { ignoreSearch: true })) || Response.error();
    }
  })());
});
