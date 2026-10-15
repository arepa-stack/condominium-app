// App shell cache. The API (other origin) always hits the network.
// - .wasm files have content-hashed names → cache-first (never change once published).
// - Everything else (index.html, shared.js, resources) → network-first, cache only as offline fallback,
//   so a new deploy shows up on the first reload instead of the second.
// Bump CACHE when the caching strategy itself changes.
const CACHE = 'condominio-v4';

self.addEventListener('install', () => self.skipWaiting());

self.addEventListener('activate', e => e.waitUntil(
  caches.keys()
    .then(keys => Promise.all(keys.filter(k => (k.startsWith('condominio-') && k !== CACHE) || k === 'compose_web_resources_cache').map(k => caches.delete(k))))
    .then(() => self.clients.claim())
));

self.addEventListener('fetch', e => {
  const url = new URL(e.request.url);
  if (e.request.method !== 'GET' || url.origin !== location.origin) return;
  e.respondWith(caches.open(CACHE).then(async cache => {
    if (url.pathname.endsWith('.wasm')) {
      const hit = await cache.match(e.request);
      if (hit) return hit;
    }
    try {
      // no-cache: always revalidate with the server (cheap 304s) so a stale browser HTTP cache
      // can never pair an old resources file with a new app build.
      const res = await fetch(url.href, { cache: 'no-cache' }); // URL, not Request: navigate-mode Requests reject init
      if (res.ok) cache.put(e.request, res.clone());
      return res;
    } catch (err) {
      const hit = await cache.match(e.request);
      if (hit) return hit;
      throw err;
    }
  }));
});
