const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

test('名前からPAY SYNCを開き、未導入時はVatonへ移動できる', () => {
  const html = fs.readFileSync(path.join(__dirname, '../index.html'), 'utf8');
  assert.match(html, /id="open-csv"[\s\S]*href="intent:\/\/pay\/sync#Intent;/);
  assert.match(html, /scheme=arunoassist;package=com\.aruno\.assist/);
  assert.match(html, /browser_fallback_url=https%3A%2F%2Fwallet\.vaton\.jp%2Fpoint%2Fpoint_logs/);
  assert.match(html, /id="open-vaton-history"/);
  assert.match(html, /href="https:\/\/wallet\.vaton\.jp\/point\/point_logs"/);
  assert.match(html, /id="choose-csv"[^>]*>▶ ダウンロード済みCSVを選択<\/button>/);
});
