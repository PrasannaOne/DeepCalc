/* DeepCalc V7 service worker
   Network-first keeps deployed HTML/CSS/JS fresh; cached files remain available offline. */
const CACHE_NAME = "deepcalc-shell-v7";
const APP_SHELL = [
  "./",
  "./index.html",
  "./styles.css?v=7",
  "./app.js?v=7",
  "./manifest.webmanifest",
  "./icon-192.png",
  "./icon-512.png",
  "./icon-maskable-512.png",
  "./apple-touch-icon.png",
  "./favicon-32.png",
  "./og-image.png"
];
const SHELL_DOCUMENT = new URL("./index.html", self.registration.scope).href;

self.addEventListener("install", event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => cache.addAll(APP_SHELL))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", event => {
  event.waitUntil(
    caches.keys()
      .then(keys => Promise.all(keys
        .filter(key => key.startsWith("deepcalc-shell-") && key !== CACHE_NAME)
        .map(key => caches.delete(key))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", event => {
  const request = event.request;
  if (request.method !== "GET") return;

  const requestUrl = new URL(request.url);
  if (requestUrl.origin !== self.location.origin) return;

  if (request.mode === "navigate") {
    event.respondWith((async () => {
      try {
        // Bypass the browser HTTP cache so a newly deployed index is picked up promptly.
        const freshResponse = await fetch(request, { cache: "no-store" });
        if (freshResponse && freshResponse.ok) {
          const cache = await caches.open(CACHE_NAME);
          await cache.put(SHELL_DOCUMENT, freshResponse.clone());
        }
        return freshResponse;
      } catch (_) {
        const cached = await caches.match(SHELL_DOCUMENT);
        return cached || caches.match(new URL("./", self.registration.scope));
      }
    })());
    return;
  }

  event.respondWith((async () => {
    try {
      // Revalidate assets on every request. Offline fallback uses the last good response.
      const freshResponse = await fetch(request, { cache: "no-cache" });
      if (freshResponse && freshResponse.ok) {
        const cache = await caches.open(CACHE_NAME);
        await cache.put(request, freshResponse.clone());
      }
      return freshResponse;
    } catch (_) {
      return await caches.match(request) || Response.error();
    }
  })());
});
