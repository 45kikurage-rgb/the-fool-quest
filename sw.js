importScripts('./portal-screenshots.js?v=20260923-usage-fallback-v8');
const CACHE="the-fool-quest-20260923-auto-sync-v1";
const SHARE_CACHE='the-fool-quest-share-v1';
const ASSETS=['./portal-screenshots.js?v=20260923-usage-fallback-v8','./','./index.html','./style.css?v=20260923-vaton-entry-v1','./app.js?v=20260923-auto-sync-v1','./fonts/Corporate-Logo-Rounded-Bold-ver3.woff2','./manifest.webmanifest?v=20260914-white-splash','./title-logo.png','./icon-any-192.png?v=20260914-white-splash','./icon-any.png?v=20260914-white-splash','./icon-maskable-192.png?v=20260914-white-splash','./icon-maskable.png?v=20260914-white-splash'];

self.addEventListener('install',event=>{
  event.waitUntil(caches.open(CACHE).then(cache=>cache.addAll(ASSETS)).then(()=>self.skipWaiting()));
});

self.addEventListener('activate',event=>{
  event.waitUntil(
    caches.keys().then(keys=>Promise.all(keys.filter(k=>k!==CACHE&&k!==SHARE_CACHE).map(k=>caches.delete(k)))).then(()=>self.clients.claim())
  );
});

self.addEventListener('fetch',event=>{
  const req=event.request;
  const url=new URL(req.url);
  if(req.method==='POST'&&url.searchParams.get('work-usage-share')==='1'){
    event.respondWith((async()=>{
      const data=await req.formData();
      await PortalScreenshots.enqueue(data.getAll('workUsageScreenshot'), self.registration.scope);
      const windows=await self.clients.matchAll({type:'window',includeUncontrolled:true});
      windows.forEach(client=>client.postMessage({type:'portal-screenshot-queued'}));
      return Response.redirect(new URL('./?work-usage-share=1',self.registration.scope).toString(),303);
    })());
    return;
  }
  if(req.method!=='GET')return;
  if(url.pathname.endsWith('/__work_usage_screenshot__')){
    event.respondWith(caches.open(SHARE_CACHE).then(cache=>cache.match(req)).then(response=>response||new Response('',{status:404})));
    return;
  }
  event.respondWith(fetch(req).then(response=>{
    const copy=response.clone();
    caches.open(CACHE).then(cache=>cache.put(req,copy)).catch(()=>{});
    return response;
  }).catch(()=>caches.match(req).then(cached=>cached||caches.match('./index.html'))));
});
