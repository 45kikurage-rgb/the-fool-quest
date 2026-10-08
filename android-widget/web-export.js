/* Read-only adapter: matches THE FOOL QUEST main 3a58ddf. No localStorage writes. */
(function(root){
  'use strict';
  function currentMonth(now){return new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Tokyo',year:'numeric',month:'2-digit'}).format(now).replace('/', '-');}
  function readSnapshot(storage, now=Date.now()){
    const month=currentMonth(now);
    const read=(key,fallback)=>{const raw=storage.getItem(key);if(raw==null)return fallback;try{return JSON.parse(raw)??fallback;}catch{throw new Error('サイトの保存データを読み取れません：'+key);}};
    const monthly=read('foolQuestMonthlyRevenueV1',{}),manual=read('foolQuestTiktokManualChargesV1',[]);
    const goals=read('foolQuestGoalAmounts',{}),record=monthly[month];
    const valid=n=>Number.isSafeInteger(n)&&n>=0;
    if(!monthly||typeof monthly!=='object'||Array.isArray(monthly)||!Array.isArray(manual))throw new Error('保存形式を確認してください');
    let base;
    if(record){base=Number(record.tiktokCsv??record.tiktok);}
    else{
      const csv=read('tfq_tiktok_csv_meta',{});
      const m=String(csv.month??'').match(/(20\d{2})\D*(\d{1,2})/);
      const csvMonth=m?m[1]+'-'+m[2].padStart(2,'0'):'';
      const hasHistory=Object.keys(monthly).length>0;
      const legacy=storage.getItem('tfq_tiktok');
      // No record anywhere is not evidence of zero revenue. Require a source visit first.
      if(!hasHistory&&legacy==null)throw new Error('このブラウザにTHE FOOL QUESTの収益データがありません。普段使うサイトを一度開いてください。');
      base=!hasHistory||csvMonth===month?Number(legacy??0):0;
    }
    if(!valid(base))throw new Error('TikTok金額が不正です');
    let extra=0;
    for(const entry of manual){if(String(entry?.date??'').slice(0,7)===month){const n=Number(entry.amount);if(!valid(n))throw new Error('手動チャージ金額が不正です');extra+=n;}}
    const tiktok=base+extra;if(!valid(tiktok))throw new Error('TikTok合計が範囲外です');
    const goal=(key,fallback)=>{const n=Math.trunc(Number(goals[key]));return valid(n)&&n>0?n:fallback;};
    return {v:'1',month,tiktok,goalTotal:goal('total',1000000),goalTiktok:goal('tiktok',500000),goalCoupon:goal('coupon',500000),at:now};
  }
  function toUri(snapshot){return 'tfqwidget://snapshot?'+new URLSearchParams(snapshot).toString();}
  const api={readSnapshot,toUri,currentMonth};
  if(typeof module==='object'&&module.exports)module.exports=api;else root.FoolQuestExport=api;
})(typeof globalThis==='object'?globalThis:this);
