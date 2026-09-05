const CACHE='the-fool-quest-v20260906-verification-bottom-gap4';
const ASSETS=['./','./index.html','./style.css','./app.js','./manifest.webmanifest','./title-logo.png','./icon-any.png','./icon-maskable.png'];

self.addEventListener('install',event=>{
  event.waitUntil(caches.open(CACHE).then(cache=>cache.addAll(ASSETS)).then(()=>self.skipWaiting()));
});

self.addEventListener('activate',event=>{
  event.waitUntil(
    caches.keys().then(keys=>Promise.all(keys.filter(k=>k!==CACHE).map(k=>caches.delete(k)))).then(()=>self.clients.claim())
  );
});

self.addEventListener('fetch',event=>{
  const req=event.request;
  if(req.method!=='GET')return;
  event.respondWith(fetch(req).then(response=>{
    const copy=response.clone();
    caches.open(CACHE).then(cache=>cache.put(req,copy)).catch(()=>{});
    return response;
  }).catch(()=>caches.match(req).then(cached=>cached||caches.match('./index.html'))));
});
