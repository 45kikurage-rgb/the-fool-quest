const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

test('あるのはCSV UPDATE、Lvは履歴ページの入口になる', () => {
  const html = fs.readFileSync(path.join(__dirname, '../index.html'), 'utf8');
  assert.match(html, /<button id="open-csv"[^>]*>あるの<\/button>/);
  assert.doesNotMatch(html, /arunoassist|com\.aruno\.assist/);
  assert.match(html, /id="open-level-history"[\s\S]*?>Lv <strong id="level">/);
  assert.match(html, /id="open-vaton-history"/);
  assert.match(html, /href="https:\/\/wallet\.vaton\.jp\/point\/point_logs"/);
  assert.match(html, /id="choose-csv"[^>]*>▶ ダウンロード済みCSVを選択<\/button>/);
});
