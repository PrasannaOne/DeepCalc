/* DeepCalc V8 service worker
   Core documents are cached for offline startup. Optional artwork is cached best-effort.
   Same-origin requests are revalidated so published releases appear without stale assets.
*/
const CACHE_NAME = "deepcalc-shell-v8";
const CORE_FILES = ["./", "./index.html", "./styles.css?v=8", "./app.js?v=8", "./manifest.webmanifest"];
const OPTIONAL_FILES = ["./icon-192.png", "./icon-512.png", "./icon-maskable-512.png", "./apple-touch-icon.png", "./favicon-32.png", "./og-image.png"];
const SHELL_DOCUMENT = new URL("./index.html", self.registration.scope).href;

self.addEventListener("install", event => {
  event.waitUntil((async () => {
    const cache = await caches.open(CACHE_NAME);
    // If a core file is missing, fail installation rather than activate a broken release.
    await cache.addAll(CORE_FILES);
    // An unavailable icon or social image must not prevent the calculator from updating.
    await Promise.allSettled(OPTIONAL_FILES.map(path => cache.add(path)));
    await self.skipWaiting();
  })());
});

self.addEventListener("activate", event => {
  event.waitUntil((async () => {
    const keys = await caches.keys();
    await Promise.all(keys
      .filter(key => key.startsWith("deepcalc-shell-") && key !== CACHE_NAME)
      .map(key => caches.delete(key)));
    await self.clients.claim();
  })());
});

self.addEventListener("fetch", event => {
  const request = event.request;
  if (request.method !== "GET") return;
  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return;

  if (request.mode === "navigate") {
    event.respondWith((async () => {
      try {
        const response = await fetch(request, { cache: "no-store" });
        if (response && response.ok) {
          const cache = await caches.open(CACHE_NAME);
          await cache.put(SHELL_DOCUMENT, response.clone());
        }
        return response;
      } catch (_) {
        return (await caches.match(SHELL_DOCUMENT)) || (await caches.match(new URL("./", self.registration.scope)));
      }
    })());
    return;
  }

  event.respondWith((async () => {
    try {
      const response = await fetch(request, { cache: "no-cache" });
      if (response && response.ok) {
        const cache = await caches.open(CACHE_NAME);
        await cache.put(request, response.clone());
      }
      return response;
    } catch (_) {
      return (await caches.match(request)) || Response.error();
    }
  })());
});
