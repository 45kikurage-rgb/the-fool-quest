const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');

const script = fs.readFileSync(path.join(__dirname, '../app/src/main/res/raw/pay_history_fetch.js'), 'utf8');
const month = '2026-11';
const host = 'https://wallet.vaton.jp';
const filename = 'ポイント履歴_2026年11月.csv';
const bytes = Buffer.from('\uFEFF日付,チャージポイント\n2026/11/01,100\n');

function fixture({origin = host, loggedOut = false} = {}) {
    let downloads = 0;
    let clicks = 0;
    let dialogOpen = false;
    const element = (label, click) => ({
        textContent: label,
        click,
        getClientRects: () => [{}],
        getAttribute: () => null
    });
    class Anchor {
        hasAttribute(name) { return name === 'download'; }
        click() { downloads++; }
    }
    class Select {
        constructor() {
            this.options = [
                {textContent: '2026年10月', value: '10'},
                {textContent: '2026年11月', value: '11'}
            ];
            this.value = '10';
        }
        get value() { return this.selection; }
        set value(value) { this.selection = value; }
        get selectedOptions() { return this.options.filter(option => option.value === this.value); }
        getClientRects() { return [{}]; }
        getAttribute() { return null; }
        dispatchEvent() {}
    }
    class Reader {
        readAsDataURL(blob) {
            this.done = blob.arrayBuffer().then(data => {
                this.result = 'data:text/csv;base64,' + Buffer.from(data).toString('base64');
                this.onload();
            });
            reads.push(this.done);
        }
    }
    const reads = [];
    const select = new Select();
    const title = '利用実績CSVダウンロード';
    const open = element(title, () => { dialogOpen = true; });
    const download = element('ダウンロードする', () => {
        clicks++;
        const anchor = new Anchor();
        anchor.href = objectUrl.createObjectURL(new Blob([bytes], {type: 'text/csv'}));
        anchor.download = select.value === '11' ? filename : 'ポイント履歴_2026年10月.csv';
        anchor.click();
        objectUrl.revokeObjectURL(anchor.href);
    });
    const objectUrl = {createObjectURL: () => "blob:" + origin + "/test", revokeObjectURL() {}};
    const buttons = 'button,a,[role="button"],[role="tab"]';
    const dialog = {
        ...element(title),
        querySelectorAll: selector => selector === 'select' ? [select] : selector === buttons ? [download] : []
    };
    const document = {
        addEventListener() {},
        querySelectorAll(selector) {
            if (selector === 'input[type="password"]') return loggedOut ? [element('')] : [];
            if (selector.includes('[role="dialog"]')) return dialogOpen ? [dialog] : [];
            return selector === buttons ? [open] : [];
        }
    };
    const window = {};
    const location = {origin, pathname: '/point/point_logs'};
    vm.runInNewContext(script, {
        location,
        window, document, URL: objectUrl, Blob, FileReader: Reader,
        HTMLAnchorElement: Anchor, HTMLSelectElement: Select, Event,
        getComputedStyle: () => ({visibility: 'visible'})
    });
    return {api: window.__aruPayHistory, reads, select, location, clicks: () => clicks, downloads: () => downloads};
}

test('selects the current month and captures the original Blob even when its URL is revoked', async () => {
    const f = fixture();
    assert.equal(f.api.tick(month, 4_000_000).step, 'history');
    assert.equal(f.api.tick(month, 4_000_000).step, 'month');
    assert.equal(f.select.value, '11');
    assert.equal(f.api.tick(month, 4_000_000).step, 'download');
    await Promise.all(f.reads);
    const result = f.api.tick(month, 4_000_000);
    assert.equal(result.name, filename);
    const first = f.api.chunk(0, 16);
    const second = f.api.chunk(16, result.length - 16);
    assert.deepEqual(Buffer.from(first + second, 'base64'), bytes);
    assert.equal(f.clicks(), 1);
    assert.equal(f.downloads(), 0);
});

test('reports login and avoids acting on a different origin', () => {
    const login = fixture({loggedOut: true});
    assert.equal(login.api.tick(month, 4_000_000).step, 'login');
    assert.equal(login.clicks(), 0);
    assert.equal(fixture({origin: host + '.example'}).api, undefined);
});

test('does not export an unavailable month or read an oversized Blob', () => {
    const f = fixture();
    f.api.tick('2026-12', 4_000_000);
    assert.equal(f.api.tick('2026-12', 4_000_000).step, 'month');
    assert.equal(f.clicks(), 0);

    const small = fixture();
    small.api.tick(month, 1);
    small.api.tick(month, 1);
    small.api.tick(month, 1);
    assert.match(small.api.tick(month, 1).error, /4MB/);
    assert.equal(small.reads.length, 0);
});

test('leaving the history route stops CSV access, and returning resumes it', async () => {
    const f = fixture();
    f.location.pathname = '/sign_in';
    assert.equal(f.api.tick(month, 4_000_000).step, 'waiting');
    assert.equal(f.clicks(), 0);
    f.location.pathname = '/point/point_logs';
    f.api.tick(month, 4_000_000);
    f.api.tick(month, 4_000_000);
    f.api.tick(month, 4_000_000);
    await Promise.all(f.reads);
    assert.equal(f.api.tick(month, 4_000_000).name, filename);
    f.location.pathname = '/oauth/resolve_session';
    assert.equal(f.api.tick(month, 4_000_000).step, 'waiting');
    assert.equal(f.api.chunk(0, 16), '');
});
