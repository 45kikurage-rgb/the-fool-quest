const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');

const source = fs.readFileSync(require('node:path').join(__dirname, '..', 'app.js'), 'utf8');

test('PAY SYNC accepts only validated aggregate metadata', () => {
  assert.match(source, /function applyPaySyncFromHash\(\)/);
  assert.match(source, /payload\.source !== 'aruno-assist'/);
  assert.match(source, /\^\[a-f0-9\]\{64\}\$/i);
  assert.match(source, /tiktokCsv: total/);
});

test('PAY SYNC replaces the month total instead of adding it', () => {
  const body = source.match(/function applyPaySyncFromHash\(\) \{[\s\S]*?\n  \}\n  function setupCsv/)?.[0] || '';
  assert.match(body, /const tiktok = total \+ manualTotal\(month\)/);
  assert.doesNotMatch(body, /tiktokCsv[^\n]*\+/);
});

test('PAY SYNC JavaScript uses a fresh PWA cache version', () => {
  const html = fs.readFileSync(require('node:path').join(__dirname, '..', 'index.html'), 'utf8');
  const worker = fs.readFileSync(require('node:path').join(__dirname, '..', 'sw.js'), 'utf8');
  assert.match(html, /app\.js\?v=20261015-manual-csv-v1/);
  assert.match(worker, /the-fool-quest-20261015-manual-csv-v1/);
});

test('CSV入口はポップアップを開き、アプリ自動取得を呼び出さない', () => {
  const body = source.match(/function setupCsv\(\) \{[\s\S]*?\n  \}\n\n  function setupTiktokManual/)?.[0] || '';
  assert.match(body, /dialog\.showModal\(\)/);
  assert.doesNotMatch(body, /arunoassist|com\.aruno\.assist|return_url/);
  assert.match(body, /package=com\.android\.chrome/);
  assert.match(body, /wallet\.vaton\.jp\/point\/point_logs/);
});

test('home keeps dashboard data only and removes the portal link grid', () => {
  const html = fs.readFileSync(require('node:path').join(__dirname, '..', 'index.html'), 'utf8');
  assert.doesNotMatch(html, /class="link-panel"/);
  assert.doesNotMatch(html, /Coming soon/);
  assert.doesNotMatch(html, /id="link-manage-open"/);
});

test('ARUNO ASSIST can return GPT usage alongside PAY SYNC', () => {
  assert.match(source, /function applyWorkUsageFromHash\(\)/);
  assert.match(source, /params\.get\('workUsage'\)/);
  assert.match(source, /save\(KEY\.workUsage/);
  assert.match(source, /GPT残量を自動更新しました/);
});

