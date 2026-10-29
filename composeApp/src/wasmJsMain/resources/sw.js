// Follow Me Therapy: works without the internet.
// Network first, so online it's always the newest build; offline it serves the
// last copy of each file. One cache per build (sw.js?v=<build>); a new build's
// worker deletes the old ones. version.json is never cached (the update check).
const BUILD = new URL(self.location.href).searchParams.get("v") || "dev";
const CACHE = "fmt-" + BUILD;

self.addEventListener("install", (e) => {
  self.skipWaiting();
  e.waitUntil(caches.open(CACHE).then((c) => c.addAll(["./", "./manifest.webmanifest", "./icons/icon-192.png"]).catch(() => {})));
});
self.addEventListener("activate", (e) => {
  e.waitUntil(caches.keys().then((ks) => Promise.all(ks.filter((k) => k.startsWith("fmt-") && k !== CACHE).map((k) => caches.delete(k)))).then(() => self.clients.claim()));
});
self.addEventListener("fetch", (e) => {
  const r = e.request;
  const url = new URL(r.url);
  if (r.method !== "GET" || url.origin !== self.location.origin || url.pathname.endsWith("version.json")) return;
  e.respondWith(
    fetch(r).then((res) => {
      if (res.ok) { const copy = res.clone(); caches.open(CACHE).then((c) => c.put(r, copy)); }
      return res;
    }).catch(() => caches.match(r, { ignoreSearch: true }).then((hit) => hit || (r.mode === "navigate" ? caches.match("./", { ignoreSearch: true }) : undefined)).then((hit) => hit || Response.error()))
  );
});
