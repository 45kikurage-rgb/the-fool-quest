(() => {
  const BIRTH_DATE = { year: 1980, month: 11, day: 17 };
  const TARGETS = { total: 1000000, tiktok: 500000, coupon: 500000 };

  function calculateLevel(now) {
    let level = now.getFullYear() - BIRTH_DATE.year;
    const beforeBirthday = now.getMonth() + 1 < BIRTH_DATE.month ||
      (now.getMonth() + 1 === BIRTH_DATE.month && now.getDate() < BIRTH_DATE.day);
    if (beforeBirthday) level -= 1;
    return Math.max(0, level);
  }

  function dayPlus14(now) {
    const date = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 14);
    return `${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')}`;
  }

  function yen(value) {
    return `¥${Math.max(0, Number(value) || 0).toLocaleString('ja-JP')}`;
  }

  function renderMetric(name, value) {
    const target = TARGETS[name];
    const ratio = value / target;
    document.getElementById(`${name}-value`).textContent = yen(value);
    document.getElementById(`${name}-bar`).style.width = `${Math.min(100, Math.max(0, ratio * 100))}%`;
    document.getElementById(`${name}-over`).classList.toggle('show', value > target);
  }

  function render() {
    const now = new Date();
    document.getElementById('level').textContent = String(calculateLevel(now)).padStart(3, '0');
    document.getElementById('day14').textContent = dayPlus14(now);

    // 冒険の書の接続先。API公開後、ADVENTURE_BOOK_URLからチャージ合計を取得します。
    const tiktok = Number(localStorage.getItem('tfq_tiktok') || 0);
    const coupon = Number(localStorage.getItem('tfq_coupon') || 0);
    renderMetric('tiktok', tiktok);
    renderMetric('coupon', coupon);
    renderMetric('total', tiktok + coupon);
  }

  function parseCsv(text) {
    const rows = [];
    let row = [], cell = '', quoted = false;
    const source = text.replace(/^\uFEFF/, '');
    for (let i = 0; i < source.length; i++) {
      const ch = source[i];
      if (quoted) {
        if (ch === '"' && source[i + 1] === '"') { cell += '"'; i++; }
        else if (ch === '"') quoted = false;
        else cell += ch;
      } else if (ch === '"') quoted = true;
      else if (ch === ',') { row.push(cell); cell = ''; }
      else if (ch === '\n') { row.push(cell.replace(/\r$/, '')); rows.push(row); row = []; cell = ''; }
      else cell += ch;
    }
    if (cell || row.length) { row.push(cell.replace(/\r$/, '')); rows.push(row); }
    return rows;
  }

  function importCsv(text, fileName) {
    const rows = parseCsv(text);
    const headerIndex = rows.findIndex(row => row.some(cell => cell.trim() === 'チャージポイント'));
    if (headerIndex < 0) throw new Error('「チャージポイント」列が見つかりません');
    const chargeIndex = rows[headerIndex].findIndex(cell => cell.trim() === 'チャージポイント');
    let total = 0, count = 0;
    for (const row of rows.slice(headerIndex + 1)) {
      if (!row.some(cell => cell.trim())) continue;
      const value = Number(String(row[chargeIndex] ?? '').replace(/,/g, '').trim());
      if (Number.isFinite(value)) { total += Math.abs(value); count++; }
    }
    localStorage.setItem('tfq_tiktok', String(total));
    localStorage.setItem('tfq_tiktok_csv_meta', JSON.stringify({ fileName, count, importedAt: new Date().toISOString() }));
    render();
    return { total, count };
  }

  render();
  const dialog = document.getElementById('csv-dialog');
  const fileInput = document.getElementById('csv-file');
  const result = document.getElementById('csv-result');
  document.getElementById('open-csv').addEventListener('click', () => {
    result.className = 'csv-result';
    const meta = JSON.parse(localStorage.getItem('tfq_tiktok_csv_meta') || 'null');
    result.textContent = meta ? `前回：${meta.fileName}\n${new Date(meta.importedAt).toLocaleString('ja-JP')}` : 'CSVを選択してください';
    dialog.showModal();
  });
  document.getElementById('choose-csv').addEventListener('click', () => fileInput.click());
  fileInput.addEventListener('change', async () => {
    const file = fileInput.files?.[0];
    if (!file) return;
    try {
      const imported = importCsv(await file.text(), file.name);
      result.className = 'csv-result success';
      result.textContent = `更新完了\n${yen(imported.total)}（${imported.count}件）`;
    } catch (error) {
      result.className = 'csv-result';
      result.textContent = `読込エラー：${error.message}`;
    } finally {
      fileInput.value = '';
    }
  });
  setInterval(render, 60 * 1000);
  if ('serviceWorker' in navigator) window.addEventListener('load', () => navigator.serviceWorker.register('./sw.js'));
})();
