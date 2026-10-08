const test=require('node:test'),assert=require('node:assert/strict');
const {readSnapshot,toUri,currentMonth}=require('../web-export.js');
const at=Date.parse('2026-10-08T23:00:00Z');
function storage(data){return {getItem:k=>k in data?typeof data[k]==='string'?data[k]:JSON.stringify(data[k]):null,setItem:()=>{throw Error('must not write')}};}
test('same monthly CSV + manual formula, custom targets, read-only',()=>{
 const s=readSnapshot(storage({foolQuestMonthlyRevenueV1:{'2026-10':{tiktokCsv:100000,tiktok:120000}},foolQuestTiktokManualChargesV1:[{date:'2026-10-01',amount:20000},{date:'2026-09-01',amount:90000}],foolQuestGoalAmounts:{total:900000,tiktok:400000,coupon:500000}}),at);
 assert.equal(s.tiktok,120000);assert.equal(s.goalTotal,900000);assert.equal(s.month,'2026-10');assert.match(toUri(s),/^tfqwidget:\/\/snapshot\?v=1&month=2026-10/);
});
test('legacy fallback and goals default match source',()=>{const s=readSnapshot(storage({tfq_tiktok:'125000'}),at);assert.equal(s.tiktok,125000);assert.equal(s.goalTotal,1000000);});
test('missing source never silently becomes zero',()=>assert.throws(()=>readSnapshot(storage({}),at),/収益データがありません/));
test('different CSV month with existing history resets current base',()=>{const s=readSnapshot(storage({foolQuestMonthlyRevenueV1:{'2026-09':{tiktok:50000}},tfq_tiktok:'50000',tfq_tiktok_csv_meta:{month:'2026-09'}}),at);assert.equal(s.tiktok,0);});
test('currentMonth respects JST boundary',()=>{assert.equal(currentMonth(Date.parse('2026-09-30T14:59:59Z')),'2026-09');assert.equal(currentMonth(Date.parse('2026-09-30T15:00:00Z')),'2026-10');});
test('malformed/negative/unsafe values fail before emitting a snapshot',()=>{for(const n of [-1,1.5,Number.MAX_SAFE_INTEGER+1])assert.throws(()=>readSnapshot(storage({foolQuestMonthlyRevenueV1:{'2026-10':{tiktok:n}}}),at));assert.throws(()=>readSnapshot(storage({foolQuestMonthlyRevenueV1:'{' }),at));});
test('manual amounts must be valid and no old-month manual data included',()=>assert.throws(()=>readSnapshot(storage({tfq_tiktok:'2',foolQuestTiktokManualChargesV1:[{date:'2026-10-01',amount:-10}]}),at)));
