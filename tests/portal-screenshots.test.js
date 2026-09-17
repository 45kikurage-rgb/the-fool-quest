const test=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const P=require('../portal-screenshots.js');

test('Povoの日本語日時・午前午後・年末を読み、表示では年を省く',()=>{
  for(const [text,expected] of [
    ['有効期限 2026年 9月 16日 午後7:23','2026-09-16T19:23:00+09:00'],
    ['有 効 期 限 ２０２６年９月１６日 午後７：２７','2026-09-16T19:27:00+09:00'],
    ['有効期限 2026年9月16日 午前12:05','2026-09-16T00:05:00+09:00'],
    ['有効期限 2026年9月16日 午後12:05','2026-09-16T12:05:00+09:00'],
    ['データ使用期限 2027/01/01 00:01','2027-01-01T00:01:00+09:00'],
    ['有効期限2026年9月16日19時27分','2026-09-16T19:27:00+09:00']
  ]) assert.equal(P.parsePovoExpiry(text),expected);
  assert.equal(P.formatExpiry('2027-01-01T00:01:00+09:00'),'01/01 00:01');
  for(const text of ['有効期限2026年2月30日19:27','有効期限2026年9月16日午後13:27','有効期限2026年9月16日19:77','有効期限残り3日']) assert.throws(()=>P.parsePovoExpiry(text));
  assert.equal(P.parsePovoExpiry('週間14% リセット09/19 18:28'),null);
});

test('Cursor/Grok画面をChatGPTスクショと区別する',()=>{
  for(const text of [
    'カーソルモデル Cursor GrokとComposerが含まれています 3 %使用済み',
    'Cursor Models 22% used',
    'オンデマンド支出 Cursorを通じて請求'
  ]) assert.equal(P.isCursorUsageScreenshot(text),true);
  assert.equal(P.isCursorUsageScreenshot('Codex & Work 週間利用上限 53% 残り'),false);
});

test('2枚の同一期限を2件として保存し、新しいものから2件保持する',()=>{
  const expiry='2026-09-16T19:27:00+09:00';
  let state=P.pushExpiry({},expiry,'a');
  state=P.pushExpiry(state,expiry,'b');
  assert.deepEqual(state.entries.map(x=>x.id),['b','a']);
  assert.equal(P.pushExpiry(state,expiry,'a'),state);
  state=P.pushExpiry(state,'2027-01-01T00:00:00+09:00','c');
  assert.deepEqual(state.entries.map(x=>x.id),['c','b']);
  assert.equal(P.pushExpiry(state,expiry,'a'),state);
});

function cacheFixture(){
  const values=new Map();const key=x=>typeof x==='string'?x:x.url;
  return {put:async(k,v)=>values.set(key(k),v.clone()),match:async k=>values.get(key(k))?.clone(),delete:async k=>values.delete(key(k)),keys:async()=>[...values.keys()].map(url=>({url}))};
}

test('共有の2画像・連続共有を別々に保持し、旧バージョンの画像も引き継ぐ',async()=>{
  const cache=cacheFixture();
  const scope='https://portal.test/';
  global.caches={open:async()=>cache};
  const image=()=>new Blob(['image'],{type:'image/png'});
  await Promise.all([P.enqueue([image(),image()],scope),P.enqueue([image()],scope)]);
  let requests=await P.pending(scope);
  assert.equal(requests.length,3);
  assert.equal(new Set(requests.map(x=>x.url)).size,3);
  await cache.put(scope+'__work_usage_screenshot__',new Response(image()));
  requests=await P.pending(scope);
  assert.equal(requests.length,4);
  assert.equal(await cache.match(scope+'__work_usage_screenshot__'),undefined);
  assert.equal((await P.pending(scope)).length,4);
  delete global.caches;
});

test('共有POSTは同じフィールドの全画像を受信する',async()=>{
  const listeners={};let received;
  const context={URL,Response,importScripts(){},PortalScreenshots:{enqueue:async files=>{received=files}},
    self:{registration:{scope:'https://portal.test/'},clients:{matchAll:async()=>[]},addEventListener:(type,fn)=>listeners[type]=fn}};
  vm.runInNewContext(fs.readFileSync(require('node:path').join(__dirname,'../sw.js'),'utf8'),context);
  const files=[new Blob(['1'],{type:'image/png'}),new Blob(['2'],{type:'image/png'})];
  let response;
  listeners.fetch({request:{method:'POST',url:'https://portal.test/?work-usage-share=1',formData:async()=>({getAll:name=>{assert.equal(name,'workUsageScreenshot');return files}})},respondWith:p=>{response=p}});
  assert.equal((await response).status,303);
  assert.deepEqual(received,files);
});

function appFixture(texts){
  const elements=new Map(),stored=new Map();let calls=0;
  const $=id=>{
    if(!elements.has(id)) elements.set(id,{textContent:'',style:{},classList:{remove(){},add(){},toggle(){}},removeAttribute(){}});
    return elements.get(id);
  };
  const cache=cacheFixture();global.caches={open:async()=>cache};
  const context={PortalScreenshots:P,load:(key,fallback)=>stored.has(key)?JSON.parse(stored.get(key)):fallback,
    localStorage:{setItem:(key,value)=>stored.set(key,value)},KEY:{workUsage:'usage'},$,
    window:{Tesseract:{recognize:async()=>({data:{text:texts[calls++]||''}})}},
    document:{baseURI:'https://portal.test/',createElement:()=>({getContext:()=>({drawImage(){}})})},
    createImageBitmap:async()=>({width:684,height:1536,close(){}}),
    caches:global.caches,navigator:{locks:{request:async(name,fn)=>fn()}},
    setTimeout(){},clearTimeout(){},console:{error(){}},Date,Intl};
  const source=fs.readFileSync(require('node:path').join(__dirname,'../app.js'),'utf8');
  vm.createContext(context);
  vm.runInContext(source.slice(source.indexOf('  function normalizedWorkUsage'),source.indexOf('  function setupHomeAmounts')),context);
  return {context,stored,cache,$,calls:()=>calls};
}

test('切抜き失敗時は全体で読み、失敗画像は既存期限を変えず次の2枚を処理する',async()=>{
  const valid='有効期限 2026年 9月 16日 午後7:23';
  const f=appFixture(['bad crop','unreadable',valid,'unreadable','unreadable','unreadable',valid]);
  const image=()=>new Blob(['image'],{type:'image/png'});
  await P.enqueue([image(),image(),image()],'https://portal.test/');
  await f.context.processScreenshotQueue();
  const data=JSON.parse(f.stored.get(P.POVO_KEY));
  assert.equal(data.entries.length,2);
  assert.equal(new Set(data.entries.map(x=>x.id)).size,2);
  assert.equal(f.$('povo-expiry-one').textContent,'09/16 19:23');
  assert.equal(f.$('povo-expiry-two').textContent,'09/16 19:23');
  assert.match(f.$('work-usage-status').textContent,/2枚を更新.*1枚は/);
  assert.equal((await f.cache.keys()).length,0);
  assert.equal(f.calls(),7);
  delete global.caches;
});

test('ChatGPTのPlus/Pro読取はPovoを変更せず、画像再処理でも二重に押し出さない',async()=>{
  const f=appFixture(['有効期限2026年9月16日午後7:23','5時間 残量87% リセット10:00 週間残量14% リセット09/19 18:28','Codex & Work 週間 残量14% リセット09/19 18:28']);
  const image=new Blob(['image'],{type:'image/png'});
  await f.context.readPortalScreenshot(image,'receipt-a');
  const before=f.stored.get(P.POVO_KEY);
  await f.context.readPortalScreenshot(image,'receipt-a');
  assert.equal(f.calls(),1);
  await f.context.readPortalScreenshot(image,'receipt-b');
  assert.equal(JSON.parse(f.stored.get('usage')).fivePercent,87);
  assert.equal(JSON.parse(f.stored.get('usage')).mode,'standard');
  await f.context.readPortalScreenshot(image,'receipt-c');
  assert.equal(JSON.parse(f.stored.get('usage')).mode,'pro');
  assert.equal(f.stored.get(P.POVO_KEY),before);
  delete global.caches;
});

test('Pro新画面は販促・グラフの別パーセントを無視し週間残量を読む',async()=>{
  const f=appFixture(['Codex & Work は同じ利用上限を共有しています。 週間利用上限 75% 残り リセット: 2026/09/21 12:06 残りのクレジット 0 自動チャージ 最大40%お得 グラフ 100% 0%']);
  const image=new Blob(['image'],{type:'image/jpeg'});
  await f.context.readPortalScreenshot(image,'new-pro');
  const usage=JSON.parse(f.stored.get('usage'));
  assert.equal(usage.mode,'pro');
  assert.equal(usage.fivePercent,null);
  assert.equal(usage.weekPercent,75);
  assert.equal(usage.weekReset,'09/21 12:06');
  delete global.caches;
});

test('通常切抜きが読めなくても英語のProカード切抜きで復旧する',async()=>{
  const f=appFixture(['unreadable','Codex & Work weekly limit 75% remaining reset 2026/09/21 12:06']);
  const image=new Blob(['image'],{type:'image/jpeg'});
  await f.context.readPortalScreenshot(image,'pro-fallback');
  const usage=JSON.parse(f.stored.get('usage'));
  assert.equal(usage.mode,'pro');
  assert.equal(usage.weekPercent,75);
  assert.equal(f.calls(),2);
  delete global.caches;
});

test('Cursor/Grok画面を送ってもChatGPTとPovoを変更しない',async()=>{
  const f=appFixture(['カーソルモデル Cursor GrokとComposerが含まれています 3%使用済み その他のモデル 100%使用済み']);
  const image=new Blob(['image'],{type:'image/png'});
  await f.context.readPortalScreenshot(image,'cursor-grok');
  assert.equal(f.stored.has('usage'),false);
  assert.equal(f.stored.has(P.POVO_KEY),false);
  delete global.caches;
});

test('週次パーセントだけの画面をChatGPT Proとして誤保存しない',async()=>{
  const f=appFixture(['週間の使用量 22%使用済み 9月23日にリセット','週間の使用量 22%使用済み','週間の使用量 22%使用済み']);
  const image=new Blob(['image'],{type:'image/png'});
  await assert.rejects(()=>f.context.readPortalScreenshot(image,'weekly-only'));
  assert.equal(f.stored.has('usage'),false);
  delete global.caches;
});
