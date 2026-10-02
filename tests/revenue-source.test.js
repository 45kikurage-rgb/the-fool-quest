const test=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const app=fs.readFileSync(path.join(__dirname,'..','app.js'),'utf8');
const worker=fs.readFileSync(path.join(__dirname,'..','sw.js'),'utf8');
const html=fs.readFileSync(path.join(__dirname,'..','index.html'),'utf8');
const style=fs.readFileSync(path.join(__dirname,'..','style.css'),'utf8');

test('Coupon当月収益は中央管理台帳Readerの小さい月次レスポンスだけを使う',()=>{
 assert.match(app,/aruno-consolidated-ledger-api\.45kikurage\.workers\.dev/);
 assert.match(app,/\/api\/v1\/revenue\/monthly\?month=/);
 assert.match(app,/data\.coupon_revenue/);
 assert.doesNotMatch(app,/winning-url-api|\/api\/portal-revenue/);
 assert.doesNotMatch(app,/setInterval\(syncCoupon/);
 assert.match(app,/id="refresh-coupon"/);
});

test('2026-10の初回切替で旧Coupon値を0円へ分離し、PWAキャッシュを更新する',()=>{
 assert.match(app,/state\.month < '2026-10'/);
 assert.match(app,/couponSource:'central-ledger-v1'/);
 assert.match(app,/state\.coupon = 0/);
 assert.match(worker,/20261002-pwa-scroll-v1/);
});

test('小画面PWAでも縦スクロールでき、リンク3段目とsafe-areaへ到達できる',()=>{
 assert.match(html,/style\.css\?v=20261002-pwa-scroll-v1/);
 assert.match(style,/@media\(max-width:520px\)[\s\S]*overflow-y:auto/);
 assert.match(style,/@media\(max-width:520px\)[\s\S]*height:auto;[\s\S]*min-height:100dvh;[\s\S]*overflow:visible/);
 assert.match(style,/safe-area-inset-bottom/);
});
