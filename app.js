(() => {
  'use strict';
  const CONFIG = {
    api: 'https://winning-url-api.45kikurage.workers.dev',
    goals: { total: 1000000, tiktok: 500000, coupon: 500000 },
    metrics: [['total','Total'],['tiktok','TikTok'],['coupon','Coupon']],
    colors: ['blue','red','green'],
    maxSlimes: 20
  };
  const KEY = {
    tiktok: 'tfq_tiktok', csv: 'tfq_tiktok_csv_meta',
    coupon: 'foolQuestCouponRevenueLastGood', couponMeta: 'foolQuestCouponRevenueLastGoodMeta',
    goals: 'foolQuestGoalAmounts', monthly: 'foolQuestMonthlyRevenueV1',
    slimeColors: 'foolQuestSlimeColorsV1'
  };
  const $ = id => document.getElementById(id);
  const json = (raw, fallback) => { try { return JSON.parse(raw) ?? fallback; } catch { return fallback; } };
  const load = (key, fallback = {}) => { try { return json(localStorage.getItem(key), fallback); } catch { return fallback; } };
  const save = (key, value) => { try { localStorage.setItem(key, typeof value === 'string' ? value : JSON.stringify(value)); } catch {} };
  const money = value => `¥${Math.max(0, Math.round(Number(value) || 0)).toLocaleString('ja-JP')}`;

  function jst() {
    const parts = new Intl.DateTimeFormat('ja-JP', {
      timeZone:'Asia/Tokyo', year:'numeric', month:'numeric', day:'numeric'
    }).formatToParts(new Date());
    const get = type => Number(parts.find(part => part.type === type)?.value || 0);
    const year = get('year'), month = get('month'), day = get('day');
    return { year, month, day, days: new Date(Date.UTC(year, month, 0)).getUTCDate() };
  }
  const monthKey = value => `${value.year}-${String(value.month).padStart(2,'0')}`;
  function normalizeMonth(value) {
    const match = String(value ?? '').match(/(20\d{2})\D*(\d{1,2})/);
    const month = Number(match?.[2]);
    return match && month >= 1 && month <= 12 ? `${match[1]}-${String(month).padStart(2,'0')}` : '';
  }
  function previousMonth(key) {
    const [year, month] = key.split('-').map(Number);
    const date = new Date(Date.UTC(year, month - 2, 1));
    return `${date.getUTCFullYear()}-${String(date.getUTCMonth()+1).padStart(2,'0')}`;
  }
  function readGoals() {
    const stored = load(KEY.goals);
    return Object.fromEntries(Object.entries(CONFIG.goals).map(([name, fallback]) => {
      const value = Math.trunc(Number(stored[name]));
      return [name, value > 0 ? value : fallback];
    }));
  }

  const state = {
    month: monthKey(jst()), goals: readGoals(), monthly: load(KEY.monthly),
    tiktok: 0, coupon: 0
  };
  function legacyNumber(key) {
    try { const n = Number(localStorage.getItem(key)); return Number.isFinite(n) && n >= 0 ? n : 0; }
    catch { return 0; }
  }
  function persistCurrent() {
    state.monthly[state.month] = {
      ...(state.monthly[state.month] || {}), month: state.month,
      tiktok: state.tiktok, coupon: state.coupon,
      updatedAt: new Date().toISOString(), finalized: false
    };
    save(KEY.monthly, state.monthly);
  }
  function initializeMonth() {
    const record = state.monthly[state.month];
    if (record) {
      state.tiktok = Math.max(0, Number(record.tiktok) || 0);
      state.coupon = Math.max(0, Number(record.coupon) || 0);
      return;
    }
    const meta = load(KEY.couponMeta), csvMeta = load(KEY.csv);
    const hasMonthlyHistory = Object.keys(state.monthly).length > 0;
    state.tiktok = !hasMonthlyHistory || normalizeMonth(csvMeta.month) === state.month
      ? legacyNumber(KEY.tiktok)
      : 0;
    state.coupon = normalizeMonth(meta.month) === state.month ? Math.max(0, Number(meta.value) || 0) : 0;
    persistCurrent();
  }
  function checkMonth() {
    const current = monthKey(jst());
    if (current === state.month) return;
    state.month = current;
    state.tiktok = Math.max(0, Number(state.monthly[current]?.tiktok) || 0);
    state.coupon = Math.max(0, Number(state.monthly[current]?.coupon) || 0);
    persistCurrent();
    renderAll();
    renderSlimes();
  }
  function valueOf(name) {
    return name === 'tiktok' ? state.tiktok : name === 'coupon' ? state.coupon : state.tiktok + state.coupon;
  }
  function level() {
    return Math.max(0, Math.floor(valueOf('total') / Math.max(1, state.goals.total) * 100));
  }

  function buildUi() {
    $('metrics').innerHTML = CONFIG.metrics.map(([name,label]) => `
      <div class="metric"><div class="metric-line">
        <div class="metric-label">${label}:</div>
        <div class="metric-achieved"><b id="${name}-value">¥0</b></div>
        <div id="${name}-target" class="metric-target"></div>
      </div><div class="gauge-wrap"><div class="gauge">
        <div id="${name}-bar" class="gauge-fill"></div>
        <span id="${name}-percent" class="gauge-percent">0.0%</span>
      </div><span id="${name}-flame" class="gauge-flame" hidden>🔥</span></div></div>`).join('');
    $('goal-inputs').innerHTML = CONFIG.metrics.map(([name,label]) => `
      <label class="goal-input-row"><span>${label}</span><span class="goal-input-wrap">
        <span>¥</span><input id="goal-${name}-input" type="number" inputmode="numeric" min="1" step="1000">
      </span></label>`).join('');
  }
  function renderDate() {
    $('level').textContent = String(level());
    const now = new Date(), date = new Date(now.getFullYear(), now.getMonth(), now.getDate()+14);
    $('day14').textContent = `${String(date.getMonth()+1).padStart(2,'0')}/${String(date.getDate()).padStart(2,'0')}`;
  }
  function renderMetric(name) {
    const value = valueOf(name), goal = state.goals[name], percent = goal > 0 ? value / goal * 100 : 0;
    const date = jst(), pace = value >= goal * date.day / date.days;
    $(`${name}-value`).textContent = money(value);
    $(`${name}-target`).textContent = `/ ${money(goal)}`;
    $(`${name}-bar`).style.width = `${Math.min(100, Math.max(0, percent))}%`;
    $(`${name}-bar`).classList.toggle('pace-ok', pace);
    $(`${name}-bar`).classList.toggle('pace-behind', !pace);
    $(`${name}-percent`).textContent = `${percent.toFixed(1)}%`;
    $(`${name}-flame`).hidden = !(value > goal);
  }
  function renderAll() {
    renderDate();
    CONFIG.metrics.forEach(([name]) => renderMetric(name));
    renderHistory();
  }

  function countForLevel(lv) {
    return lv < 1 ? 0 : Math.min(CONFIG.maxSlimes, Math.floor((lv - 1) / 10) + 1);
  }
  function fixedColors(count) {
    let colors = load(KEY.slimeColors, []);
    if (!Array.isArray(colors)) colors = [];
    while (colors.length < count) colors.push(CONFIG.colors[Math.floor(Math.random() * 3)]);
    save(KEY.slimeColors, colors);
    return colors.slice(0, count);
  }
  function shuffle(items) {
    for (let i=items.length-1;i>0;i--) {
      const j=Math.floor(Math.random()*(i+1));
      [items[i],items[j]]=[items[j],items[i]];
    }
    return items;
  }
  function placements(count) {
    const slots = shuffle([
      {left:13,bottom:34},{left:29,bottom:33},{left:46,bottom:31},
      {left:64,bottom:33},{left:81,bottom:35}
    ]);
    const result = slots.slice(0, Math.min(5,count)).map(slot => ({...slot,angle:0,scale:1}));
    if (count <= 5) return result;
    result.push({left:12+Math.random()*76,bottom:59+Math.random()*16,angle:5+Math.random()*40,scale:1});
    for (let i=6;i<count;i++) {
      const base = result[Math.floor(Math.random()*Math.min(6,result.length))];
      const layer = 1 + Math.floor((i-6)/6);
      result.push({
        left:Math.min(91,Math.max(7,base.left+Math.random()*8-4)),
        bottom:Math.min(88,base.bottom+9+layer*5+Math.random()*4),
        angle:(Math.random()<.5?-1:1)*(5+Math.random()*40),
        scale:Math.max(.72,1-layer*.08)
      });
    }
    return result;
  }
  let renderedSlimeCount = -1;
  function renderSlimes() {
    const count = countForLevel(level());
    if (count === renderedSlimeCount) return;
    renderedSlimeCount = count;
    const colors = fixedColors(count), spots = placements(count);
    $('logo-slime-layer').replaceChildren(...colors.map((color,index) => {
      const img=document.createElement('img'), spot=spots[index];
      img.src=`./slime-${color}.png`; img.className='logo-slime'; img.alt='';
      img.style.left=`${spot.left}%`; img.style.bottom=`${spot.bottom}%`;
      img.style.setProperty('--slime-angle',`${spot.angle}deg`);
      img.style.setProperty('--slime-scale',String(spot.scale));
      img.style.zIndex=String(index+1);
      return img;
    }));
  }

  function parseCsv(text) {
    const rows=[]; let row=[],cell='',quoted=false; const source=text.replace(/^\uFEFF/,'');
    for(let i=0;i<source.length;i++){
      const ch=source[i];
      if(quoted){if(ch==='"'&&source[i+1]==='"'){cell+='"';i++;}else if(ch==='"')quoted=false;else cell+=ch;}
      else if(ch==='"')quoted=true;
      else if(ch===','){row.push(cell);cell='';}
      else if(ch==='\n'){row.push(cell.replace(/\r$/,''));rows.push(row);row=[];cell='';}
      else cell+=ch;
    }
    if(cell||row.length){row.push(cell.replace(/\r$/,''));rows.push(row);}
    return rows;
  }
  function importCsv(text,fileName) {
    const rows=parseCsv(text), header=rows.findIndex(row=>row.some(cell=>cell.trim()==='チャージポイント'));
    if(header<0)throw new Error('「チャージポイント」列が見つかりません');
    const column=rows[header].findIndex(cell=>cell.trim()==='チャージポイント');
    let total=0,count=0;
    rows.slice(header+1).forEach(row=>{
      if(!row.some(cell=>cell.trim()))return;
      const value=Number(String(row[column]??'').replace(/,/g,'').trim());
      if(Number.isFinite(value)){total+=Math.abs(value);count++;}
    });
    state.tiktok=total; save(KEY.tiktok,String(total));
    save(KEY.csv,{fileName,count,month:state.month,importedAt:new Date().toISOString()});
    persistCurrent(); renderAll(); renderSlimes();
    return {total,count};
  }
  function setupCsv() {
    const dialog=$('csv-dialog'),input=$('csv-file'),result=$('csv-result');
    $('open-csv').addEventListener('click',()=>{
      result.className='csv-result';const meta=load(KEY.csv);
      result.textContent=meta.fileName?`前回：${meta.fileName}\n${new Date(meta.importedAt).toLocaleString('ja-JP')}`:'CSVを選択してください';
      dialog.showModal();
    });
    $('choose-csv').addEventListener('click',()=>input.click());
    input.addEventListener('change',async()=>{
      const file=input.files?.[0];if(!file)return;
      try{const data=importCsv(await file.text(),file.name);result.className='csv-result success';result.textContent=`更新完了\n${money(data.total)}（${data.count}件）`;}
      catch(error){result.className='csv-result';result.textContent=`読込エラー：${error.message}`;}
      finally{input.value='';}
    });
  }

  function renderHistory() {
    const body=$('history-body');if(!body)return;
    const records=Object.values(state.monthly).filter(item=>item?.month).sort((a,b)=>b.month.localeCompare(a.month));
    body.innerHTML=records.map(record=>{
      const total=(Number(record.tiktok)||0)+(Number(record.coupon)||0);
      const lv=record.level??Math.floor(total/Math.max(1,Number(record.totalGoal)||state.goals.total)*100);
      const status=record.month===state.month?'集計中':record.finalized?'確定':'確定待ち';
      return `<tr><td>${record.month.replace('-','/')}</td><td>${money(record.tiktok)}</td><td>${money(record.coupon)}</td><td>${money(total)}</td><td>${lv}</td><td>${status}</td></tr>`;
    }).join('')||'<tr><td colspan="6">履歴はまだありません</td></tr>';
  }
  function setupGoals() {
    const dialog=$('goal-manage-dialog'),message=$('goal-manage-message');
    const fill=values=>CONFIG.metrics.forEach(([name])=>{$(`goal-${name}-input`).value=values[name];});
    $('goal-manage-open').addEventListener('click',()=>{fill(state.goals);message.textContent='';renderHistory();dialog.showModal();});
    $('goal-reset-btn').addEventListener('click',()=>{fill(CONFIG.goals);message.textContent='初期値を入力しました';});
    $('goal-save-btn').addEventListener('click',()=>{
      const next=Object.fromEntries(CONFIG.metrics.map(([name])=>[name,Math.trunc(Number($(`goal-${name}-input`).value))]));
      if(Object.values(next).some(value=>value<1||!Number.isFinite(value))){message.textContent='目標額は1円以上で入力してください';return;}
      state.goals=next;save(KEY.goals,next);renderAll();renderSlimes();message.textContent='保存しました ✓';
      setTimeout(()=>dialog.close(),450);
    });
  }
  function finalize(rows) {
    const cutoff=jst().day>=6?previousMonth(state.month):previousMonth(previousMonth(state.month));
    rows.forEach(row=>{
      const month=normalizeMonth(row?.month),coupon=Number(row?.amount);
      if(!month||month>cutoff||!Number.isFinite(coupon)||coupon<0)return;
      const old=state.monthly[month]||{month,tiktok:0};
      const total=(Number(old.tiktok)||0)+coupon,goal=Number(old.totalGoal)||state.goals.total;
      state.monthly[month]={...old,coupon,finalized:true,finalizedAt:new Date().toISOString(),totalGoal:goal,level:Math.floor(total/Math.max(1,goal)*100)};
    });
    save(KEY.monthly,state.monthly);
  }
  async function syncCoupon() {
    try{
      const response=await fetch(`${CONFIG.api}/api/revenue-summary`,{headers:{Accept:'application/json'},cache:'no-store'});
      if(!response.ok)throw new Error(`HTTP ${response.status}`);
      const data=await response.json(),rows=Array.isArray(data.revenues)?data.revenues:[];
      finalize(rows);
      const current=rows.find(row=>normalizeMonth(row?.month)===state.month),coupon=Number(current?.amount);
      if(!current||current.amount===''||current.amount==null||!Number.isFinite(coupon)||coupon<0)throw new Error('当月収益が不正です');
      if(coupon===0&&state.coupon>0)throw new Error('同月の収益が突然0円になったため前回値を維持しました');
      state.coupon=coupon;save(KEY.coupon,String(coupon));
      save(KEY.couponMeta,{month:state.month,value:coupon,savedAt:new Date().toISOString()});
      persistCurrent();renderAll();renderSlimes();
    }catch(error){console.error('Coupon revenue sync failed; keeping last good value:',error);}
  }

  initializeMonth();buildUi();setupCsv();setupGoals();renderAll();renderSlimes();syncCoupon();
  setInterval(()=>{checkMonth();renderAll();},60000);
  setInterval(syncCoupon,600000);
  if('serviceWorker'in navigator)window.addEventListener('load',()=>navigator.serviceWorker.register('./sw.js'));
})();
