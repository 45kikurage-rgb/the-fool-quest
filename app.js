(() => {
  'use strict';

  const CONFIG = Object.freeze({
    birthDate: { year: 1980, month: 11, day: 17 },
    revenueApi: 'https://winning-url-api.45kikurage.workers.dev',
    defaultGoals: { total: 1000000, tiktok: 500000, coupon: 500000 },
    metrics: [
      { name: 'total', label: 'Total' },
      { name: 'tiktok', label: 'TikTok' },
      { name: 'coupon', label: 'Coupon' }
    ]
  });

  const STORAGE = Object.freeze({
    tiktok: 'tfq_tiktok',
    csvMeta: 'tfq_tiktok_csv_meta',
    coupon: 'foolQuestCouponRevenueLastGood',
    goals: 'foolQuestGoalAmounts'
  });

  const state = {
    tiktok: readNumber(STORAGE.tiktok),
    coupon: readNullableNumber(STORAGE.coupon) ?? 0,
    goals: readGoals()
  };

  const $ = id => document.getElementById(id);
  const money = value => `¥${Math.max(0, Math.round(Number(value) || 0)).toLocaleString('ja-JP')}`;

  function readNumber(key) {
    try {
      const value = Number(localStorage.getItem(key));
      return Number.isFinite(value) && value >= 0 ? value : 0;
    } catch { return 0; }
  }

  function readNullableNumber(key) {
    try {
      const raw = localStorage.getItem(key);
      if (raw === null || raw === '') return null;
      const value = Number(raw);
      return Number.isFinite(value) && value >= 0 ? value : null;
    } catch { return null; }
  }

  function save(key, value) {
    try { localStorage.setItem(key, String(value)); } catch {}
  }

  function readGoals() {
    try {
      const saved = JSON.parse(localStorage.getItem(STORAGE.goals) || '{}');
      return Object.fromEntries(Object.entries(CONFIG.defaultGoals).map(([name, fallback]) => {
        const value = Math.trunc(Number(saved[name]));
        return [name, value > 0 ? value : fallback];
      }));
    } catch { return { ...CONFIG.defaultGoals }; }
  }

  function jstDateParts() {
    const parts = new Intl.DateTimeFormat('ja-JP', {
      timeZone: 'Asia/Tokyo', year: 'numeric', month: 'numeric', day: 'numeric'
    }).formatToParts(new Date());
    const get = type => Number(parts.find(part => part.type === type)?.value || 0);
    const year = get('year'), month = get('month'), day = get('day');
    return { year, month, day, daysInMonth: new Date(Date.UTC(year, month, 0)).getUTCDate() };
  }

  function calculateLevel(now) {
    let level = now.getFullYear() - CONFIG.birthDate.year;
    const birthdayPassed = now.getMonth() + 1 > CONFIG.birthDate.month ||
      (now.getMonth() + 1 === CONFIG.birthDate.month && now.getDate() >= CONFIG.birthDate.day);
    if (!birthdayPassed) level--;
    return Math.max(0, level);
  }

  function renderDateInfo() {
    const now = new Date();
    $('level').textContent = String(calculateLevel(now)).padStart(2, '0');
    const day14 = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 14);
    $('day14').textContent = `${String(day14.getMonth() + 1).padStart(2, '0')}/${String(day14.getDate()).padStart(2, '0')}`;
  }

  function metricValue(name) {
    if (name === 'tiktok') return state.tiktok;
    if (name === 'coupon') return state.coupon;
    return state.tiktok + state.coupon;
  }

  function renderMetric(name) {
    const value = metricValue(name);
    const goal = state.goals[name];
    const percent = goal > 0 ? value / goal * 100 : 0;
    const { day, daysInMonth } = jstDateParts();
    const paceOk = value >= goal * day / daysInMonth;

    $(`${name}-value`).textContent = money(value);
    $(`${name}-target`).textContent = `/ ${money(goal)}`;
    $(`${name}-bar`).style.width = `${Math.min(100, Math.max(0, percent))}%`;
    $(`${name}-bar`).classList.toggle('pace-ok', paceOk);
    $(`${name}-bar`).classList.toggle('pace-behind', !paceOk);
    $(`${name}-percent`).textContent = `${percent.toFixed(1)}%`;
    $(`${name}-flame`).hidden = !(value > goal);
  }

  function renderAll() {
    renderDateInfo();
    CONFIG.metrics.forEach(({ name }) => renderMetric(name));
  }

  function buildDynamicUi() {
    $('metrics').innerHTML = CONFIG.metrics.map(({ name, label }) => `
      <div class="metric" data-metric="${name}">
        <div class="metric-line">
          <div class="metric-label">${label}:</div>
          <div class="metric-achieved"><b id="${name}-value">¥0</b></div>
          <div id="${name}-target" class="metric-target"></div>
        </div>
        <div class="gauge-wrap">
          <div class="gauge">
            <div id="${name}-bar" class="gauge-fill"></div>
            <span id="${name}-percent" class="gauge-percent">0.0%</span>
          </div>
          <span id="${name}-flame" class="gauge-flame" aria-label="目標超過" hidden>🔥</span>
        </div>
      </div>`).join('');

    $('goal-inputs').innerHTML = CONFIG.metrics.map(({ name, label }) => `
      <label class="goal-input-row">
        <span>${label}</span>
        <span class="goal-input-wrap"><span>¥</span><input id="goal-${name}-input" type="number" inputmode="numeric" min="1" step="1000"></span>
      </label>`).join('');
  }

  function parseCsv(text) {
    const rows = [], row = [];
    let current = row, cell = '', quoted = false;
    const source = text.replace(/^\uFEFF/, '');
    for (let i = 0; i < source.length; i++) {
      const ch = source[i];
      if (quoted) {
        if (ch === '"' && source[i + 1] === '"') { cell += '"'; i++; }
        else if (ch === '"') quoted = false;
        else cell += ch;
      } else if (ch === '"') quoted = true;
      else if (ch === ',') { current.push(cell); cell = ''; }
      else if (ch === '\n') { current.push(cell.replace(/\r$/, '')); rows.push(current); current = []; cell = ''; }
      else cell += ch;
    }
    if (cell || current.length) { current.push(cell.replace(/\r$/, '')); rows.push(current); }
    return rows;
  }

  function importCsv(text, fileName) {
    const rows = parseCsv(text);
    const headerIndex = rows.findIndex(row => row.some(cell => cell.trim() === 'チャージポイント'));
    if (headerIndex < 0) throw new Error('「チャージポイント」列が見つかりません');
    const column = rows[headerIndex].findIndex(cell => cell.trim() === 'チャージポイント');
    let total = 0, count = 0;
    rows.slice(headerIndex + 1).forEach(row => {
      if (!row.some(cell => cell.trim())) return;
      const value = Number(String(row[column] ?? '').replace(/,/g, '').trim());
      if (Number.isFinite(value)) { total += Math.abs(value); count++; }
    });
    state.tiktok = total;
    save(STORAGE.tiktok, total);
    save(STORAGE.csvMeta, JSON.stringify({ fileName, count, importedAt: new Date().toISOString() }));
    renderAll();
    return { total, count };
  }

  function setupCsvDialog() {
    const dialog = $('csv-dialog'), input = $('csv-file'), result = $('csv-result');
    $('open-csv').addEventListener('click', () => {
      result.className = 'csv-result';
      try {
        const meta = JSON.parse(localStorage.getItem(STORAGE.csvMeta) || 'null');
        result.textContent = meta ? `前回：${meta.fileName}\n${new Date(meta.importedAt).toLocaleString('ja-JP')}` : 'CSVを選択してください';
      } catch { result.textContent = 'CSVを選択してください'; }
      dialog.showModal();
    });
    $('choose-csv').addEventListener('click', () => input.click());
    input.addEventListener('change', async () => {
      const file = input.files?.[0];
      if (!file) return;
      try {
        const imported = importCsv(await file.text(), file.name);
        result.className = 'csv-result success';
        result.textContent = `更新完了\n${money(imported.total)}（${imported.count}件）`;
      } catch (error) {
        result.className = 'csv-result';
        result.textContent = `読込エラー：${error.message}`;
      } finally { input.value = ''; }
    });
  }

  function setupGoalDialog() {
    const dialog = $('goal-manage-dialog'), message = $('goal-manage-message');
    const fill = values => CONFIG.metrics.forEach(({ name }) => { $(`goal-${name}-input`).value = values[name]; });
    $('goal-manage-open').addEventListener('click', () => { fill(state.goals); message.textContent = ''; dialog.showModal(); });
    $('goal-reset-btn').addEventListener('click', () => { fill(CONFIG.defaultGoals); message.textContent = '初期値を入力しました'; });
    $('goal-save-btn').addEventListener('click', () => {
      const next = Object.fromEntries(CONFIG.metrics.map(({ name }) => [name, Math.trunc(Number($(`goal-${name}-input`).value))]));
      if (Object.values(next).some(value => value < 1 || !Number.isFinite(value))) {
        message.textContent = '目標額は1円以上で入力してください'; return;
      }
      state.goals = next;
      save(STORAGE.goals, JSON.stringify(next));
      renderAll();
      message.textContent = '保存しました ✓';
      setTimeout(() => dialog.close(), 450);
    });
  }

  async function syncCouponRevenue() {
    try {
      const response = await fetch(`${CONFIG.revenueApi}/api/revenue-summary`, { headers: { Accept: 'application/json' }, cache: 'no-store' });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const data = await response.json();
      const month = String(data.current_month || '').trim();
      const row = Array.isArray(data.revenues) ? data.revenues.find(item => String(item?.month || '').trim() === month) : null;
      const coupon = Number(row?.amount);
      if (!month || row?.amount === '' || row?.amount == null || !Number.isFinite(coupon) || coupon < 0) throw new Error('当月収益が不正です');
      state.coupon = coupon;
      save(STORAGE.coupon, coupon);
      renderAll();
    } catch (error) {
      console.error('Coupon revenue sync failed; keeping last good value:', error);
    }
  }

  function registerServiceWorker() {
    if ('serviceWorker' in navigator) window.addEventListener('load', () => navigator.serviceWorker.register('./sw.js'));
  }

  buildDynamicUi();
  setupCsvDialog();
  setupGoalDialog();
  renderAll();
  syncCouponRevenue();
  setInterval(renderAll, 60 * 1000);
  setInterval(syncCouponRevenue, 10 * 60 * 1000);
  registerServiceWorker();
})();
