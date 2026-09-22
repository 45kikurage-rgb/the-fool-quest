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
  assert.match(html, /app\.js\?v=20260923-pay-sync-v1/);
  assert.match(worker, /the-fool-quest-20260923-pay-sync-v1/);
});
