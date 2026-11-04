(function () {
    if (location.origin !== 'https://wallet.vaton.jp'
        || !/^\/point\/point_logs\/?$/.test(location.pathname)
        || window.__aruPayHistory) return;

    const state = {historyTabOpened: false, opened: false, armed: false, reading: false};
    const blobUrls = new Map();
    const text = el => (el.innerText || el.textContent || '').replace(/\s+/g, ' ').trim();
    const visible = el => el.getClientRects().length > 0 && getComputedStyle(el).visibility === 'visible';
    const enabled = el => visible(el) && !el.disabled && el.getAttribute('aria-disabled') !== 'true';
    const control = (root, label) => {
        for (const selector of ['button,a,[role="button"],[role="tab"]', 'div,span,li,p']) {
            const element = Array.from(root.querySelectorAll(selector)).find(
                el => enabled(el) && text(el) === label);
            if (element) return element;
        }
        return null;
    };

    const createObjectURL = URL.createObjectURL;
    URL.createObjectURL = function (value) {
        const url = createObjectURL.call(this, value);
        if (state.armed && value instanceof Blob && blobUrls.size < 8) blobUrls.set(url, value);
        return url;
    };

    function capture(link) {
        if (!state.armed || !link.hasAttribute('download')) return false;
        const url = link.href;
        const name = link.download;
        if (!/^ポイント履歴[^/\\]*\.csv$/i.test(name)
            || (!url.startsWith('blob:' + location.origin + '/') && !url.startsWith('data:'))) return false;
        if (state.reading) return true;
        state.reading = true;
        state.name = name;

        function readBlob(blob) {
            if (blob.size < 1 || blob.size > state.limit) {
                state.error = 'CSVの容量を確認できませんでした（上限4MB）';
                return;
            }
            const reader = new FileReader();
            reader.onload = () => { state.data = String(reader.result).split(',')[1]; };
            reader.onerror = reader.onabort = () => { state.error = 'CSVを読み取れませんでした'; };
            reader.readAsDataURL(blob);
        }

        const blob = blobUrls.get(url);
        blobUrls.clear();
        if (blob) readBlob(blob);
        else if (url.startsWith('data:') && url.length > Math.ceil(state.limit / 3) * 4 + 256) {
            state.error = 'CSVの容量を確認できませんでした（上限4MB）';
        } else {
            fetch(url).then(response => response.blob()).then(readBlob)
                .catch(() => { state.error = 'CSVを読み取れませんでした'; });
        }
        return true;
    }

    const click = HTMLAnchorElement.prototype.click;
    HTMLAnchorElement.prototype.click = function () {
        if (!capture(this)) click.call(this);
    };
    document.addEventListener('click', event => {
        const link = event.target.closest && event.target.closest('a[download]');
        if (link && capture(link)) {
            event.preventDefault();
            event.stopImmediatePropagation();
        }
    }, true);

    const monthIn = value => {
        const match = /(20\d{2})\s*年\s*(\d{1,2})\s*月/.exec(value);
        return match ? match[1] + '-' + String(Number(match[2])).padStart(2, '0') : '';
    };

    function selectMonth(root, expected) {
        for (const select of root.querySelectorAll('select')) {
            if (!enabled(select)) continue;
            const options = Array.from(select.options);
            if (!options.some(option => monthIn(text(option)))) continue;
            if (select.selectedOptions.length && monthIn(text(select.selectedOptions[0])) === expected) return true;
            const option = options.find(item => !item.disabled && monthIn(text(item)) === expected);
            if (option) {
                Object.getOwnPropertyDescriptor(HTMLSelectElement.prototype, 'value').set.call(select, option.value);
                select.dispatchEvent(new Event('input', {bubbles: true}));
                select.dispatchEvent(new Event('change', {bubbles: true}));
            }
            return false;
        }
        const combo = Array.from(root.querySelectorAll('[role="combobox"]')).find(enabled);
        if (combo) {
            if (monthIn(text(combo)) === expected || monthIn(combo.value) === expected) return true;
            const option = Array.from(document.querySelectorAll('[role="option"]')).find(
                item => enabled(item) && monthIn(text(item)) === expected);
            if (option) option.click();
            else if (combo.getAttribute('aria-expanded') !== 'true') {
                combo.dispatchEvent(new MouseEvent('mousedown', {bubbles: true, cancelable: true, button: 0, view: window}));
                if (combo.getAttribute('aria-expanded') !== 'true') combo.click();
            }
            return false;
        }
        // A dialog without a month control can still supply its default month.
        // The original file name and every CSV date are validated by Android.
        return true;
    }

    window.__aruPayHistory = {
        tick(expected, limit) {
            if (state.error) return {error: state.error};
            if (state.data) return {name: state.name, length: state.data.length};
            if (state.armed) return {step: 'download'};
            if (Array.from(document.querySelectorAll('input[type="password"]')).some(visible)) return {step: 'login'};
            const title = '利用実績CSVダウンロード';
            const dialog = Array.from(document.querySelectorAll('[role="dialog"],[aria-modal="true"],.modal'))
                .find(el => visible(el) && text(el).includes(title));
            const download = control(dialog || document, 'ダウンロードする');
            if (download && (dialog || (state.opened && text(document.body).includes(title)))) {
                if (!selectMonth(dialog || document, expected)) return {step: 'month'};
                state.armed = true;
                state.limit = limit;
                download.click();
                return {step: 'download'};
            }
            const open = control(document, title);
            if (open && !state.opened) {
                state.opened = true;
                open.click();
            } else if (!open && !state.historyTabOpened) {
                const historyTab = control(document, 'ポイント履歴');
                if (historyTab) {
                    state.historyTabOpened = true;
                    historyTab.click();
                }
            }
            return {step: 'history'};
        },
        chunk(offset, length) {
            return state.data ? state.data.slice(offset, offset + length) : '';
        }
    };
})();
