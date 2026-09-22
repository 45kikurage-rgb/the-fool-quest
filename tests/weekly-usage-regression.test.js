'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const P = require('../portal-screenshots.js');
const source = fs.readFileSync(process.env.TFQ_APP_SOURCE || path.join(__dirname, '../app.js'), 'utf8');

// Manually transcribed from the 2026-09-23 report. OCR itself is mocked;
// these tests cover classification, parsing, storage and gauge rendering.
const screenshotText = `Codex と Work のアナリティクス
残高
Codex と Work は同じ利用上限を共有しています。
週間利用上限
96% 残り
リセット: 2026/09/30 0:04
残りのクレジット 0
利用制限のリセット
リセットを使って、5時間の上限、週ごとの上限、またはその両方を復元できます。
利用可能回数 1 履歴
完全リセット
有効期限: 10月5日 8:00
自動チャージ
クレジットの自動チャージ 最大40%お得
使用状況の内訳 個人の使用状況 100% 0%`;

function fixture(texts = []) {
  const stored = new Map(), elements = new Map(), crops = [];
  let calls = 0;
  const $ = id => {
    if (!elements.has(id)) elements.set(id, {
      textContent: '', style: {}, hidden: false,
      classList: {remove(){}, add(){}, toggle(){}}, removeAttribute(){}
    });
    return elements.get(id);
  };
  const context = {
    PortalScreenshots: P, KEY: {workUsage: 'usage'}, $,
    load: (key, fallback) => stored.has(key) ? JSON.parse(stored.get(key)) : fallback,
    localStorage: {setItem: (key, value) => stored.set(key, value)},
    window: {Tesseract: {recognize: async () => ({data: {text: texts[calls++] || ''}})}},
    document: {createElement: () => ({getContext: () => ({drawImage: (...args) => crops.push(args)})})},
    createImageBitmap: async () => ({width: 684, height: 1536, close(){}}),
    setTimeout(){}, clearTimeout(){}, Date, Intl
  };
  vm.createContext(context);
  vm.runInContext(source.slice(source.indexOf('  function normalizedWorkUsage'), source.indexOf('  function setupHomeAmounts')), context);
  return {context, stored, $, crops, calls: () => calls};
}
const image = () => new Blob(['image'], {type: 'image/png'});

function assertWeekly(usage, percent = 96, reset = '09/30 00:04') {
  assert.equal(usage.mode, 'pro');
  assert.equal(usage.fivePercent, null);
  assert.equal(usage.weekPercent, percent);
  assert.equal(usage.weekReset, reset);
}

test('週間96%の画面で説明文の5時間とリセット券の期限を無視する', () => {
  assertWeekly(fixture().context.parseWorkUsageText(screenshotText));
});

test('通常切抜きから残量保存・96%ゲージ表示まで反映しPovoを保持する', async () => {
  const f = fixture([screenshotText]);
  const povo = JSON.stringify({entries: [{id: 'existing', expiry: '2026-09-25T12:00:00+09:00'}], seenIds: ['existing']});
  f.stored.set(P.POVO_KEY, povo);
  f.stored.set('usage', JSON.stringify({mode: 'pro', weekPercent: 53, weekReset: 'なし'}));
  assert.equal((await f.context.readPortalScreenshot(image(), 'reported-image')).kind, 'work');
  assertWeekly(JSON.parse(f.stored.get('usage')));
  assert.equal(f.$('work-week-fill').style.width, '96%');
  assert.equal(f.$('work-week-percent').textContent, '96％');
  assert.equal(f.$('work-week-reset').textContent, '09/30 00:04');
  assert.equal(f.$('work-five-percent').textContent, '対象外');
  assert.equal(f.stored.get(P.POVO_KEY), povo);
  assert.equal(f.calls(), 1);
});

test('年付きの完全リセット券もPovoの有効期限として保存しない', async () => {
  const f = fixture([screenshotText.replace('10月5日 8:00', '2026年10月5日 8:00')]);
  await f.context.readPortalScreenshot(image(), 'year-in-voucher');
  assertWeekly(JSON.parse(f.stored.get('usage')));
  assert.equal(f.stored.has(P.POVO_KEY), false);
});

test('リセット説明の見出しが読めなくても5時間を実際の残量カードと誤認しない', () => {
  const text = screenshotText.replace('利用制限のリセット\n', '');
  assertWeekly(fixture().context.parseWorkUsageText(text));
});

test('カード切抜きへフォールバックでき、切抜きは週カード全体を含む', async () => {
  const f = fixture(['unreadable', '週間利用上限\n96% 残り\nリセット: 2026/09/30 0:04']);
  await f.context.readPortalScreenshot(image(), 'card-fallback');
  assertWeekly(JSON.parse(f.stored.get('usage')));
  assert.equal(f.calls(), 2);
  const [, x, y, w, h] = f.crops[1];
  // Label/value/reset in the reported 684x1536 screenshot, excluding help at y=679.
  assert.ok(x <= 232 && x + w >= 382);
  assert.ok(y <= 505 && y + h >= 594 && y + h < 679);
});

test('全体画像へのフォールバックでも有効期限に引きずられない', async () => {
  const f = fixture(['unreadable', 'unreadable', screenshotText]);
  await f.context.readPortalScreenshot(image(), 'full-fallback');
  assertWeekly(JSON.parse(f.stored.get('usage')));
  assert.equal(f.calls(), 3);
});

test('日本語のOCR空白と全角数字を正規化して週間カードを読む', () => {
  const text = screenshotText.replace('週間利用上限', '週 間 利 用 上 限')
    .replace('96% 残り', '９６％ 残 り').replaceAll('リセット', 'リ セ ッ ト');
  assertWeekly(fixture().context.parseWorkUsageText(text));
});

test('残量が読めなければ割引40%やグラフ100%で上書きしない', async () => {
  const bad = screenshotText.replace('96% 残り', '判読不能');
  const f = fixture([bad, bad, bad]);
  const before = JSON.stringify({mode: 'pro', weekPercent: 53, weekReset: '09/23 12:06'});
  f.stored.set('usage', before);
  await assert.rejects(() => f.context.readPortalScreenshot(image(), 'bad-value'));
  assert.equal(f.stored.get('usage'), before);
  assert.equal(f.stored.has(P.POVO_KEY), false);
});

test('週間カードの日時が読めなければ10月5日の券期限を代入しない', () => {
  const text = screenshotText.replace('リセット: 2026/09/30 0:04', '');
  assertWeekly(fixture().context.parseWorkUsageText(text), 96, '――');
});

test('実際に5時間と週間の両カードがある画面は従来通り読む', () => {
  const text = '5時間あたりの利用上限 87% 残り リセット 10:00 週間利用上限 96% 残り リセット 2026/09/30 0:04\n' + screenshotText.slice(screenshotText.indexOf('利用制限のリセット'));
  const usage = fixture().context.parseWorkUsageText(text);
  assert.equal(usage.mode, 'standard');
  assert.equal(usage.fivePercent, 87);
  assert.equal(usage.weekPercent, 96);
  assert.equal(usage.weekReset, '09/30 00:04');
});

test('週間カードが欠けたとき5時間の残量を週間残量として複写しない', () => {
  assert.throws(() => fixture().context.parseWorkUsageText('5時間 87% remaining リセット10:00'));
});

test('週間0%と100%も有効な値として反映する', () => {
  for (const percent of [0, 100]) {
    assertWeekly(fixture().context.parseWorkUsageText(screenshotText.replace('96%', `${percent}%`)), percent);
  }
});
