(function (root) {
  'use strict';
  const SHARE_CACHE = 'the-fool-quest-share-v1';
  const POVO_KEY = 'foolQuestPovoExpiryV1';
  const PREFIX = './__portal_screenshot__/';
  const LEGACY = './__work_usage_screenshot__';
  function parsePovoExpiry(raw) {
    const text = String(raw || '').normalize('NFKC').replace(/\s+/g, '').replace(/：/g, ':');
    const anchor = text.search(/有効期限|データ使用期限/);
    if (anchor < 0) return null;
    const section = text.slice(anchor, anchor + 180);
    const match = section.match(/(20\d{2})[年/.-](\d{1,2})[月/.-](\d{1,2})(?:日|[,、])?(午前|午後|AM|PM)?(\d{1,2})[:時](\d{2})(?:分)?/i);
    if (!match) throw new Error('Povoの有効期限を読み取れませんでした。期限が鮮明な画像を再送してください。');
    const [, yearText, monthText, dayText, meridiem, hourText, minuteText] = match;
    const year = Number(yearText), month = Number(monthText), day = Number(dayText), minute = Number(minuteText);
    let hour = Number(hourText);
    if (meridiem) {
      if (hour < 1 || hour > 12) throw new Error('Povoの時刻を確認できませんでした。画像を再送してください。');
      hour = hour % 12 + (/午後|PM/i.test(meridiem) ? 12 : 0);
    }
    const date = new Date(Date.UTC(year, month - 1, day, hour, minute));
    if (month < 1 || month > 12 || day < 1 || hour > 23 || minute > 59 || date.getUTCFullYear() !== year || date.getUTCMonth() !== month - 1 || date.getUTCDate() !== day) {
      throw new Error('Povoの有効期限が正しい日時ではありません。画像を再送してください。');
    }
    const pad = x => String(x).padStart(2, '0');
    return `${year}-${pad(month)}-${pad(day)}T${pad(hour)}:${pad(minute)}:00+09:00`;
  }
  function formatExpiry(expiry) {
    const match = String(expiry || '').match(/^\d{4}-(\d{2})-(\d{2})T(\d{2}):(\d{2})/);
    return match ? `${match[1]}/${match[2]} ${match[3]}:${match[4]}` : '--/-- --:--';
  }
  function isCursorUsageScreenshot(raw) {
    const text = String(raw || '').normalize('NFKC');
    return /カーソル\s*モデル|Cursor\s*Models?|Cursor\s*Grok|Composer|その他のモデル|オンデマンド(?:支出|利用)|Cursorを通じて請求/i.test(text);
  }
  function pushExpiry(previous, expiry, id, receivedAt = new Date().toISOString()) {
    const current = previous && typeof previous === 'object' ? previous : {};
    const seen = Array.isArray(current.seenIds) ? current.seenIds : [];
    if (seen.includes(id)) return current;
    const entries = Array.isArray(current.entries) ? current.entries : [];
    return {entries:[{id,expiry,receivedAt}, ...entries].slice(0,2), seenIds:[id,...seen].slice(0,256)};
  }
  async function enqueue(files, scope) {
    const cache = await caches.open(SHARE_CACHE);
    const batch = `${Date.now()}-${crypto.randomUUID()}`;
    let count = 0;
    for (const file of files) {
      if (!file || !file.type?.startsWith('image/')) continue;
      const id = `${batch}-${String(count).padStart(4,'0')}`;
      await cache.put(new URL(PREFIX + id, scope).href, new Response(file, {headers:{'Content-Type':file.type}}));
      count++;
    }
    return count;
  }
  async function pending(scope) {
    const cache = await caches.open(SHARE_CACHE);
    // Migrate an image received by the previous service worker without dropping it.
    const legacyURL = new URL(LEGACY, scope).href;
    const legacy = await cache.match(legacyURL);
    if (legacy) {
      await enqueue([await legacy.blob()], scope);
      await cache.delete(legacyURL);
    }
    const prefix = new URL(PREFIX, scope).href;
    return (await cache.keys()).filter(request => request.url.startsWith(prefix)).sort((a,b) => a.url.localeCompare(b.url));
  }
  const api = {SHARE_CACHE,POVO_KEY,PREFIX,parsePovoExpiry,isCursorUsageScreenshot,formatExpiry,pushExpiry,enqueue,pending};
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
  else root.PortalScreenshots = api;
})(globalThis);
