const CACHE='the-fool-quest-v20260909-corporate-rounded';
const SHARE_CACHE='the-fool-quest-share-v1';
const ASSETS=['./','./index.html','./style.css','./app.js','./fonts/Corporate-Logo-Rounded-Bold-ver3.woff2','./manifest.webmanifest','./title-logo.png','./icon-any.png','./icon-maskable.png'];

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
  const url=new URL(req.url);
  if(req.method==='POST'&&url.searchParams.get('work-usage-share')==='1'){
    event.respondWith((async()=>{
      const data=await req.formData();
      const file=data.get('workUsageScreenshot');
      if(file instanceof File&&file.type.startsWith('image/')){
        const cache=await caches.open(SHARE_CACHE);
        await cache.put(new URL('./__work_usage_screenshot__',self.registration.scope).toString(),new Response(file,{headers:{'Content-Type':file.type}}));
      }
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
