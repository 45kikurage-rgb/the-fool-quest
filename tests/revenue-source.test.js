const test=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const app=fs.readFileSync(path.join(__dirname,'..','app.js'),'utf8');
const worker=fs.readFileSync(path.join(__dirname,'..','sw.js'),'utf8');

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
 assert.match(worker,/20261002-ledger-revenue-v1/);
});
