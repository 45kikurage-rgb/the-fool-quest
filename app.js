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

    // 次の接続段階で各サイトのAPI取得値に置き換える設定箇所です。
    const tiktok = Number(localStorage.getItem('tfq_tiktok') || 0);
    const coupon = Number(localStorage.getItem('tfq_coupon') || 0);
    renderMetric('tiktok', tiktok);
    renderMetric('coupon', coupon);
    renderMetric('total', tiktok + coupon);
  }

  render();
  setInterval(render, 60 * 1000);
  if ('serviceWorker' in navigator) window.addEventListener('load', () => navigator.serviceWorker.register('./sw.js'));
})();
