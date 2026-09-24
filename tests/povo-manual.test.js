const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const read = file => fs.readFileSync(path.join(__dirname, '..', file), 'utf8');

test('Povo枠から端末1th・2ndの期限を手動更新できる', () => {
  const html = read('index.html');
  const app = read('app.js');
  assert.match(html, /<button id="povo-expiry-open"/);
  assert.match(html, /id="povo-expiry-dialog"/);
  assert.match(html, /id="povo-expiry-input-one"/);
  assert.match(html, /id="povo-expiry-input-two"/);
  assert.match(html, /placeholder="MM\/dd hh:mm"/);
  assert.match(app, /function setupPovoExpiryEditor\(\)/);
  assert.match(app, /formatPovoEditorInput/);
  assert.match(app, /parsePovoEditorExpiry/);
  assert.match(app, /localStorage\.setItem\(PortalScreenshots\.POVO_KEY/);
  assert.doesNotMatch(app, /function setupPovoExpiryOrder\(\)/);
});

test('Povo手動更新用のPWAキャッシュへ更新されている', () => {
  const html = read('index.html');
  const worker = read('sw.js');
  assert.match(html, /20260924-povo-manual-v2/);
  assert.match(worker, /the-fool-quest-20260924-povo-manual-v2/);
});

test('数字8桁を表示形式へ整形し、不正な日時は保存しない', () => {
  const app = read('app.js');
  const start = app.indexOf('  function formatPovoEditorInput');
  const end = app.indexOf('  function setupPovoExpiryEditor');
  const context = {Date, Intl};
  vm.createContext(context);
  vm.runInContext(`${app.slice(start, end)}\nthis.povoTestApi={formatPovoEditorInput,parsePovoEditorExpiry};`, context);
  assert.equal(context.povoTestApi.formatPovoEditorInput('09241830'), '09/24 18:30');
  assert.equal(context.povoTestApi.formatPovoEditorInput('09/24 18:30'), '09/24 18:30');
  assert.throws(() => context.povoTestApi.parsePovoEditorExpiry('02/30 12:00'));
  assert.equal(context.povoTestApi.parsePovoEditorExpiry(''), null);
});
