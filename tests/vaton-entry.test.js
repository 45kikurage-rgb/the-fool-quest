const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

test('CSV更新画面にVaton履歴ページへの入口がある', () => {
  const html = fs.readFileSync(path.join(__dirname, '../index.html'), 'utf8');
  assert.match(html, /id="open-vaton-history"/);
  assert.match(html, /href="https:\/\/wallet\.vaton\.jp\/point\/point_logs"/);
  assert.match(html, /id="choose-csv"[^>]*>▶ ダウンロード済みCSVを選択<\/button>/);
});
