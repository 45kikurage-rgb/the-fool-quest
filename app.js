(() => {
  'use strict';
  const CONFIG = {
    api: 'https://winning-url-api.45kikurage.workers.dev',
    goals: { total: 1000000, tiktok: 500000, coupon: 500000 },
    metrics: [['total','Total'],['tiktok','TikTok'],['coupon','Coupon']]
  };
  const DEFAULT_LINKS = {
    assets: 'https://assets-management-8os.pages.dev',
    action: 'https://the-fool-head.45kikurage.workers.dev',
    text: 'https://45kikurage-rgb.github.io/copy-paste',
    win: 'https://winning-url-manager.45kikurage.workers.dev',
    capture: 'https://coupon-capture.45kikurage.workers.dev',
    image: 'https://yahoo-framev2.45kikurage.workers.dev',
    summary: 'https://lit.link/admin/creator/edit'
  };
  const LINK_LABELS = { assets:'ASSET MANAGER', action:'ACTION TOOL', text:'TEXT FORMAT', win:'WIN LINK MANAGER', capture:'URL CAPTURE', image:'IMAGE EDITOR', summary:'LINK SUMMARY' };

  const KEY = {
    tiktok: 'tfq_tiktok', csv: 'tfq_tiktok_csv_meta',
    coupon: 'foolQuestCouponRevenueLastGood', couponMeta: 'foolQuestCouponRevenueLastGoodMeta',
    goals: 'foolQuestGoalAmounts', monthly: 'foolQuestMonthlyRevenueV1',
    simulation: 'foolQuestOperationSimulationV1', links: 'foolQuestPortalLinksV1',
    tiktokManual: 'foolQuestTiktokManualChargesV1', paceColors: 'foolQuestPaceColorsEnabledV1'
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

  const loadedMonthly = load(KEY.monthly, {});
  const state = {
    month: monthKey(jst()), goals: readGoals(),
    monthly: loadedMonthly && typeof loadedMonthly === 'object' && !Array.isArray(loadedMonthly) ? loadedMonthly : {},
    manualCharges: load(KEY.tiktokManual, []), paceColors: load(KEY.paceColors, true) !== false,
    tiktokCsv: 0, tiktok: 0, coupon: 0
  };
  if (!Array.isArray(state.manualCharges)) state.manualCharges = [];
  const manualTotal = month => state.manualCharges
    .filter(entry => String(entry?.date || '').slice(0, 7) === month)
    .reduce((sum, entry) => sum + Math.max(0, Number(entry.amount) || 0), 0);
  function legacyNumber(key) {
    try { const n = Number(localStorage.getItem(key)); return Number.isFinite(n) && n >= 0 ? n : 0; }
    catch { return 0; }
  }
  function persistCurrent() {
    state.monthly[state.month] = {
      ...(state.monthly[state.month] || {}), month: state.month,
      tiktokCsv: state.tiktokCsv, tiktok: state.tiktok, coupon: state.coupon,
      updatedAt: new Date().toISOString(), finalized: false
    };
    save(KEY.monthly, state.monthly);
  }
  function initializeMonth() {
    const record = state.monthly[state.month];
    if (record) {
      state.tiktokCsv = Math.max(0, Number(record.tiktokCsv ?? record.tiktok) || 0);
      state.tiktok = state.tiktokCsv + manualTotal(state.month);
      state.coupon = Math.max(0, Number(record.coupon) || 0);
      persistCurrent();
      return;
    }
    const meta = load(KEY.couponMeta), csvMeta = load(KEY.csv);
    const hasMonthlyHistory = Object.keys(state.monthly).length > 0;
    state.tiktokCsv = !hasMonthlyHistory || normalizeMonth(csvMeta.month) === state.month
      ? legacyNumber(KEY.tiktok)
      : 0;
    state.tiktok = state.tiktokCsv + manualTotal(state.month);
    state.coupon = normalizeMonth(meta.month) === state.month ? Math.max(0, Number(meta.value) || 0) : 0;
    persistCurrent();
  }
  function checkMonth() {
    const current = monthKey(jst());
    if (current === state.month) return;
    state.month = current;
    const record = state.monthly[current];
    state.tiktokCsv = Math.max(0, Number(record?.tiktokCsv ?? record?.tiktok) || 0);
    state.tiktok = state.tiktokCsv + manualTotal(current);
    state.coupon = Math.max(0, Number(state.monthly[current]?.coupon) || 0);
    persistCurrent();
    renderAll();
  }
  function valueOf(name) {
    return name === 'tiktok' ? state.tiktok : name === 'coupon' ? state.coupon : state.tiktok + state.coupon;
  }
  function level() {
    return Math.max(0, Math.floor(valueOf('total') / 10000));
  }

  function buildUi() {
    CONFIG.metrics.forEach(([name,label]) => {
      $(`metric-${name}`).innerHTML = `
      <div class="metric"><div class="metric-line">
        ${name === 'tiktok'
          ? `<button id="open-tiktok-manual" class="metric-label metric-label-button" type="button" aria-label="TikTokチャージを手動追加">${label}:</button>`
          : `<div class="metric-label">${label}:</div>`}
        <div class="metric-achieved"><b id="${name}-value">¥0</b></div>
        <div id="${name}-target" class="metric-target"></div>
      </div><div class="gauge-wrap"><div class="gauge">
        <div id="${name}-bar" class="gauge-fill"></div>
        <span id="${name}-percent" class="gauge-percent">0.0%</span>
      </div><span id="${name}-flame" class="gauge-flame" hidden>🔥</span></div></div>`;
    });
    $('goal-inputs').innerHTML = CONFIG.metrics.map(([name,label]) => `
      <label class="goal-input-row"><span>${label}</span><span class="goal-input-wrap">
        <span>¥</span><input id="goal-${name}-input" type="number" inputmode="numeric" min="1" step="1000" ${name === 'total' ? 'readonly aria-readonly="true"' : ''}>
      </span></label>`).join('');
  }
  function renderDate() {
    $('level').textContent = String(level());
    const today = jst();
    const formatOffset = offset => {
      const date = new Date(Date.UTC(today.year, today.month - 1, today.day + offset));
      return `${String(date.getUTCMonth()+1).padStart(2,'0')}/${String(date.getUTCDate()).padStart(2,'0')}`;
    };
    $('day14').textContent = formatOffset(14);
    $('day5').textContent = formatOffset(5);
  }
  function renderMetric(name) {
    const value = valueOf(name), goal = state.goals[name], percent = goal > 0 ? value / goal * 100 : 0;
    const date = jst(), expected = goal * date.day / date.days;
    const pacePercent = expected > 0 ? value / expected * 100 : 100;
    const paceClass = pacePercent >= 100 ? 'pace-ontrack'
      : pacePercent >= 50 ? 'pace-yellow'
      : 'pace-dark-red';
    const panel = name === 'total' ? document.querySelector('.total-panel') : $(`metric-${name}`);
    ['pace-ontrack','pace-yellow','pace-light-red','pace-dark-red'].forEach(className => panel?.classList.remove(className));
    panel?.classList.add(paceClass);
    $(`${name}-value`).textContent = money(value);
    $(`${name}-target`).textContent = `/ ${money(goal)}`;
    $(`${name}-bar`).style.width = `${Math.min(100, Math.max(0, percent))}%`;
    $(`${name}-percent`).textContent = `${percent.toFixed(1)}%`;
    $(`${name}-flame`).hidden = !(value > goal);
  }
  function renderAll() {
    renderDate();
    CONFIG.metrics.forEach(([name]) => renderMetric(name));
    renderHistory();
  }

  function setupPaceColors() {
    const button = $('pace-color-toggle');
    if (!button) return;
    const apply = () => {
      document.documentElement.classList.toggle('pace-colors-off', !state.paceColors);
      button.textContent = `▶ 進捗カラー　${state.paceColors ? 'ON' : 'OFF'}`;
      button.setAttribute('aria-pressed', String(state.paceColors));
      button.blur();
    };
    button.addEventListener('click', event => {
      event.preventDefault();
      state.paceColors = !state.paceColors;
      save(KEY.paceColors, state.paceColors);
      renderAll();
      apply();
    });
    apply();
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
    state.tiktokCsv=total;
    state.tiktok=state.tiktokCsv+manualTotal(state.month);
    save(KEY.tiktok,String(total));
    save(KEY.csv,{fileName,count,month:state.month,importedAt:new Date().toISOString()});
    persistCurrent(); renderAll();
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

  function setupTiktokManual() {
    const dialog = $('tiktok-manual-dialog');
    const dateInput = $('tiktok-manual-date');
    const amountInput = $('tiktok-manual-amount');
    const message = $('tiktok-manual-message');
    const history = $('tiktok-manual-history');
    const todayValue = () => {
      const today = jst();
      return `${today.year}-${String(today.month).padStart(2,'0')}-${String(today.day).padStart(2,'0')}`;
    };
    const sanitizeAmount = () => {
      amountInput.value = String(amountInput.value || '').replace(/\D/g, '').slice(0, 9);
    };
    const recalculateMonth = month => {
      const old = state.monthly[month] || { month, coupon: 0 };
      const csvBase = month === state.month
        ? state.tiktokCsv
        : Math.max(0, Number(old.tiktokCsv ?? old.tiktok) || 0);
      const tiktok = csvBase + manualTotal(month);
      state.monthly[month] = { ...old, month, tiktokCsv: csvBase, tiktok, updatedAt: new Date().toISOString() };
      if (month === state.month) state.tiktok = tiktok;
      save(KEY.monthly, state.monthly);
    };
    const renderManualHistory = () => {
      const entries = [...state.manualCharges].sort((a, b) => String(b.date).localeCompare(String(a.date)) || String(b.id).localeCompare(String(a.id)));
      history.innerHTML = entries.length ? `<div class="manual-history-list">${entries.map(entry => `
        <div class="manual-history-row">
          <span>${String(entry.date).replace(/-/g, '/')}</span>
          <strong>${money(entry.amount)}</strong>
          <button class="manual-history-delete" type="button" data-manual-delete="${entry.id}" aria-label="${entry.date}の手動追加を削除">削除</button>
        </div>`).join('')}</div>` : '<div class="manual-history-empty">手動追加はありません</div>';
    };
    $('open-tiktok-manual').addEventListener('click', () => {
      dateInput.value = todayValue();
      amountInput.value = '';
      message.textContent = '';
      renderManualHistory();
      dialog.showModal();
    });
    amountInput.addEventListener('input', sanitizeAmount);
    $('tiktok-manual-add').addEventListener('click', () => {
      sanitizeAmount();
      const date = dateInput.value;
      const amount = Math.trunc(Number(amountInput.value));
      const parsedDate = new Date(`${date}T00:00:00Z`);
      const validDate = /^20\d{2}-\d{2}-\d{2}$/.test(date)
        && !Number.isNaN(parsedDate.getTime())
        && parsedDate.toISOString().slice(0, 10) === date;
      if (!validDate) {
        message.textContent = '日付を確認してください';
        return;
      }
      if (!Number.isFinite(amount) || amount < 1) {
        message.textContent = '金額を1円以上で入力してください';
        return;
      }
      const id = globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(16).slice(2)}`;
      state.manualCharges.push({ id, date, amount, createdAt: new Date().toISOString() });
      save(KEY.tiktokManual, state.manualCharges);
      recalculateMonth(date.slice(0, 7));
      amountInput.value = '';
      message.textContent = `${money(amount)}を追加しました ✓`;
      renderManualHistory();
      renderAll();
    });
    history.addEventListener('click', event => {
      const button = event.target.closest('[data-manual-delete]');
      if (!button) return;
      const entry = state.manualCharges.find(item => item.id === button.dataset.manualDelete);
      if (!entry || !confirm(`${entry.date.replace(/-/g, '/')} の ${money(entry.amount)} を削除しますか？`)) return;
      state.manualCharges = state.manualCharges.filter(item => item.id !== entry.id);
      save(KEY.tiktokManual, state.manualCharges);
      recalculateMonth(entry.date.slice(0, 7));
      message.textContent = '削除しました';
      renderManualHistory();
      renderAll();
    });
  }

  function renderHistory() {
    const body=$('history-body');if(!body)return;
    const records=Object.values(state.monthly).filter(item=>item?.month).sort((a,b)=>b.month.localeCompare(a.month));
    body.innerHTML=records.map(record=>{
      const total=(Number(record.tiktok)||0)+(Number(record.coupon)||0);
      const lv=Math.floor(total/10000);
      const status=record.month===state.month?'集計中':record.finalized?'確定':'確定待ち';
      return `<tr><td>${record.month.replace('-','/')}</td><td>${lv}</td><td>${money(total)}</td><td>${money(record.tiktok)}</td><td>${money(record.coupon)}</td><td>${status}</td></tr>`;
    }).join('')||'<tr><td colspan="6">履歴はまだありません</td></tr>';
  }
  function setupGoals() {
    const dialog=$('goal-manage-dialog'),message=$('goal-manage-message');
    const syncTotal=()=>{
      const tiktok=Math.max(0,Math.trunc(Number($('goal-tiktok-input').value))||0);
      const coupon=Math.max(0,Math.trunc(Number($('goal-coupon-input').value))||0);
      $('goal-total-input').value=tiktok+coupon;
    };
    const fill=values=>{CONFIG.metrics.forEach(([name])=>{$(`goal-${name}-input`).value=values[name];});syncTotal();};
    $('goal-tiktok-input').addEventListener('input',syncTotal);
    $('goal-coupon-input').addEventListener('input',syncTotal);
    $('goal-manage-open').addEventListener('click',()=>{fill(state.goals);message.textContent='';renderHistory();dialog.showModal();});
    $('goal-reset-btn').addEventListener('click',()=>{fill(CONFIG.goals);message.textContent='初期値を入力しました';});
    $('goal-save-btn').addEventListener('click',()=>{
      const tiktok=Math.trunc(Number($('goal-tiktok-input').value));
      const coupon=Math.trunc(Number($('goal-coupon-input').value));
      if(tiktok<1||coupon<1||!Number.isFinite(tiktok)||!Number.isFinite(coupon)){message.textContent='目標額は1円以上で入力してください';return;}
      const next={tiktok,coupon,total:tiktok+coupon};
      state.goals=next;save(KEY.goals,next);renderAll();message.textContent='保存しました ✓';
      setTimeout(()=>dialog.close(),450);
    });
  }

  function setupLinks() {
    const dialog = $('link-manage-dialog');
    const manageDialog = $('goal-manage-dialog');
    const message = $('link-manage-message');
    const readLinks = () => ({ ...DEFAULT_LINKS, ...load(KEY.links, {}) });
    const applyLinks = links => {
      document.querySelectorAll('[data-link-key]').forEach(anchor => {
        const key = anchor.dataset.linkKey;
        anchor.href = links[key] || DEFAULT_LINKS[key];
      });
    };
    const fill = links => {
      $('link-inputs').innerHTML = Object.keys(DEFAULT_LINKS).map(key => `
        <label class="link-input-row"><span>${LINK_LABELS[key]}</span><span class="link-input-wrap">
          <input id="link-${key}" type="url" inputmode="url" value="${String(links[key] || DEFAULT_LINKS[key]).replace(/&/g,'&amp;').replace(/"/g,'&quot;')}">
        </span></label>`).join('');
    };
    const validUrl = value => {
      try { const u = new URL(value); return u.protocol === 'http:' || u.protocol === 'https:'; } catch { return false; }
    };
    let links = readLinks();
    applyLinks(links);
    const openBtn = $('link-manage-open');
    const resetBtn = $('link-reset-btn');
    const saveBtn = $('link-save-btn');
    if (!dialog || !message || !openBtn || !resetBtn || !saveBtn || !$('link-inputs')) {
      console.error('Link editor UI is incomplete');
      return;
    }
    openBtn.addEventListener('click', () => {
      fill(links);
      message.textContent='';
      if (manageDialog?.open) manageDialog.close();
      dialog.showModal();
    });
    dialog.addEventListener('close', () => {
      if (!manageDialog?.open) { renderHistory(); manageDialog?.showModal(); }
    });
    resetBtn.addEventListener('click', () => { fill(DEFAULT_LINKS); message.textContent='初期値を入力しました'; });
    saveBtn.addEventListener('click', () => {
      const next = {};
      for (const key of Object.keys(DEFAULT_LINKS)) {
        const value = String($(`link-${key}`).value || '').trim();
        if (!validUrl(value)) { message.textContent = `${LINK_LABELS[key]} のURLを確認してください`; return; }
        next[key] = value;
      }
      links = next; save(KEY.links, links); applyLinks(links); message.textContent='保存しました ✓';
      setTimeout(() => dialog.close(), 450);
    });
  }

  function setupVerification() {
    const dialog = $('verification-dialog');
    const manageDialog = $('goal-manage-dialog');
    const fields = ['sim-amount','sim-rest-days','sim-starts','sim-owned'];
    const stored = load(KEY.simulation, { amount: 2000, restDays: 9, starts: 6, owned: 200 });
    $('sim-amount').value = stored.amount ?? 2000;
    $('sim-rest-days').value = stored.restDays ?? 9;
    $('sim-starts').value = stored.starts ?? 6;
    $('sim-owned').value = stored.owned ?? 200;
    const digitsOnly = input => {
      const limit = Number(input.maxLength) > 0 ? Number(input.maxLength) : 4;
      const cleaned = String(input.value).replace(/\D/g, '').slice(0, limit);
      if (input.value !== cleaned) input.value = cleaned;
    };
    const clampInput = (id, max) => {
      const input = $(id);
      digitsOnly(input);
      const value = Math.max(0, Math.min(max, Math.trunc(Number(input.value) || 0)));
      input.value = String(value);
      return value;
    };
    const calculate = () => {
      const workDays = 14;
      const amount = clampInput('sim-amount', 9999);
      const restDays = clampInput('sim-rest-days', 9999);
      const requestedStarts = clampInput('sim-starts', 9999);
      const owned = clampInput('sim-owned', 999);
      const cycleDays = workDays + restDays;
      const maxStarts = cycleDays > 0 ? owned / cycleDays : 0;
      const daily = requestedStarts * amount;
      const monthly = daily * 30;
      const maxActive = requestedStarts * workDays;

      save(KEY.simulation, { amount, restDays, starts: requestedStarts, owned });

      $('sim-rest-hours').textContent = `＝${(restDays * 24).toLocaleString('ja-JP')}時間`;
      $('sim-max-starts').textContent = maxStarts.toFixed(2);
      $('sim-daily').textContent = money(daily);
      $('sim-monthly').textContent = money(monthly);
      $('sim-max-active').textContent = `${Math.floor(maxActive).toLocaleString('ja-JP')}台`;
    };

    const openBtn = $('verification-open');
    const closeBtn = $('verification-close');
    const backBtn = $('verification-back');
    const title = $('verification-title');
    if (!dialog || !manageDialog || !openBtn || !closeBtn || !backBtn || !title) {
      console.error('Verification UI is incomplete');
      return;
    }
    openBtn.addEventListener('click', () => {
      calculate();
      manageDialog.close();
      dialog.showModal();
      requestAnimationFrame(() => title.focus({ preventScroll: true }));
    });
    const returnToManage = () => {
      dialog.close();
      renderHistory();
      if (!manageDialog.open) manageDialog.showModal();
    };
    closeBtn.addEventListener('click', returnToManage);
    backBtn.addEventListener('click', returnToManage);
    fields.forEach(id => $(id).addEventListener('input', event => {
      digitsOnly(event.currentTarget);
      calculate();
    }));
    calculate();
  }
  function finalize(rows) {
    const cutoff=jst().day>=6?previousMonth(state.month):previousMonth(previousMonth(state.month));
    rows.forEach(row=>{
      const month=normalizeMonth(row?.month),coupon=Number(row?.amount);
      if(!month||month>cutoff||!Number.isFinite(coupon)||coupon<0)return;
      const old=state.monthly[month]||{month,tiktok:0};
      const total=(Number(old.tiktok)||0)+coupon,goal=Number(old.totalGoal)||state.goals.total;
      state.monthly[month]={...old,coupon,finalized:true,finalizedAt:new Date().toISOString(),totalGoal:goal,level:Math.floor(total/10000)};
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
      persistCurrent();renderAll();
    }catch(error){console.error('Coupon revenue sync failed; keeping last good value:',error);}
  }

  const safeSetup = (name, setup) => {
    try { setup(); }
    catch (error) { console.error(`${name} setup failed:`, error); }
  };
  safeSetup('UI', buildUi);
  safeSetup('Month', initializeMonth);
  safeSetup('Pace colors', setupPaceColors);
  safeSetup('CSV', setupCsv);
  safeSetup('TikTok manual charge', setupTiktokManual);
  safeSetup('Goals', setupGoals);
  safeSetup('Links', setupLinks);
  safeSetup('Verification', setupVerification);
  safeSetup('Initial render', renderAll);
  syncCoupon();
  setInterval(()=>{checkMonth();renderAll();},60000);
  setInterval(syncCoupon,600000);
})();
