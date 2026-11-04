package com.aruno.foolquest.widget.next;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Acquires the site's own CSV inside an app-only WebView. */
public final class PayFetchActivity extends Activity {
    // Keep evaluateJavascript replies comfortably below Android's IPC limit.
    private static final int CHUNK_SIZE = 128 * 1024;
    private static final int MAX_ENCODED = ((PayFetchPolicy.MAX_BYTES + 2) / 3) * 4;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable pollTask = this::poll;
    private TextView status;
    private FrameLayout page;
    private Button retry;
    private WebView web;
    private String script;
    private String month;
    private long started;
    private long historyStarted;
    private long receiptStarted;
    private int navigation;
    private boolean foreground;
    private boolean loaded;
    private boolean scriptInstalled;
    private boolean evaluating;

    @Override public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12), dp(12), dp(12), dp(12));
        content.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(dp(12) + insets.getSystemWindowInsetLeft(),
                dp(12) + insets.getSystemWindowInsetTop(), dp(12) + insets.getSystemWindowInsetRight(),
                dp(12) + insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(content);

        status = new TextView(this);
        status.setTextSize(16);
        status.setPadding(dp(4), dp(8), dp(4), dp(8));
        content.addView(status);
        retry = button("ポイント履歴を開き直す", this::startFetch);
        content.addView(retry);
        page = new FrameLayout(this);
        content.addView(page, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        content.addView(button("収益管理へ戻る", this::finish));
        if (savedState == null) startFetch();
        else fail("画面が再作成されました。ポイント履歴を開き直してください");
    }

    @Override protected void onStart() {
        super.onStart();
        foreground = true;
        schedulePoll();
    }

    @Override protected void onStop() {
        foreground = false;
        handler.removeCallbacks(pollTask);
        super.onStop();
    }

    @Override protected void onDestroy() {
        closeWeb();
        super.onDestroy();
    }

    private void startFetch() {
        closeWeb();
        retry.setVisibility(View.GONE);
        status.setText("データ更新中…");
        started = SystemClock.elapsedRealtime();
        historyStarted = started;
        month = RevenueMath.month(System.currentTimeMillis());
        try {
            if (script == null) script = readScript();
            WebView view = new WebView(this);
            web = view;
            view.getSettings().setJavaScriptEnabled(true);
            view.getSettings().setDomStorageEnabled(true);
            view.getSettings().setAllowFileAccess(false);
            view.getSettings().setAllowContentAccess(false);
            CookieManager.getInstance().setAcceptCookie(true);
            view.setWebViewClient(new WebViewClient() {
                @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    if (!request.isForMainFrame() || PayFetchPolicy.walletAddress(request.getUrl().toString()))
                        return false;
                    if (view == web) fail("えらべるPayの外部ページへ移動しました");
                    return true;
                }

                @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap icon) {
                    if (view != web) return;
                    navigation++;
                    evaluating = false;
                    loaded = false;
                    scriptInstalled = false;
                    if (!PayFetchPolicy.walletAddress(url)) fail("取得元を確認できませんでした");
                }

                @Override public void onPageFinished(WebView view, String url) {
                    if (view != web || !url.equals(view.getUrl())) return;
                    loaded = true;
                    CookieManager.getInstance().flush();
                    schedulePoll();
                }

                @Override public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                    if (view == web) fail("アプリ内ブラウザが停止しました");
                    return true;
                }

                @Override public void onReceivedError(WebView view, WebResourceRequest request,
                                                       WebResourceError error) {
                    if (view == web && request.isForMainFrame()) fail("えらべるPayに接続できませんでした");
                }

                @Override public void onReceivedHttpError(WebView view, WebResourceRequest request,
                                                           WebResourceResponse error) {
                    if (view == web && request.isForMainFrame()) fail("えらべるPayを開けませんでした");
                }
            });
            page.addView(view, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            view.setVisibility(View.INVISIBLE);
            view.loadUrl(PayFetchPolicy.VATON_HISTORY_URL);
            schedulePoll();
        } catch (RuntimeException | IOException e) {
            fail("アプリ内ブラウザを開けませんでした");
        }
    }

    private void schedulePoll() {
        handler.removeCallbacks(pollTask);
        if (foreground && web != null) handler.postDelayed(pollTask, 400);
    }

    private void poll() {
        WebView view = web;
        if (view == null) return;
        long now = SystemClock.elapsedRealtime();
        if (receiptStarted > 0 && now - receiptStarted >= PayFetchPolicy.RECEIPT_MS) {
            fail("CSVの受取期限が切れました");
            return;
        }
        if (now - started >= PayFetchPolicy.SESSION_MS
            || (receiptStarted == 0 && historyStarted > 0 && now - historyStarted >= PayFetchPolicy.DOWNLOAD_MS)) {
            fail("CSVの取得が時間内に完了しませんでした");
            return;
        }
        if (!loaded || evaluating || receiptStarted > 0) {
            schedulePoll();
            return;
        }
        if (!PayFetchPolicy.historyAddress(view.getUrl())) {
            showLogin(view);
            schedulePoll();
            return;
        }
        final int document = navigation;
        evaluating = true;
        String tick = "window.__aruPayHistory && window.__aruPayHistory.tick("
            + JSONObject.quote(month) + "," + PayFetchPolicy.MAX_BYTES + ")";
        String code = scriptInstalled ? tick : script + "\n" + tick;
        view.evaluateJavascript(code, value -> {
            if (view != web || navigation != document) return;
            evaluating = false;
            if (!PayFetchPolicy.historyAddress(view.getUrl())) {
                schedulePoll();
                return;
            }
            JSONObject result;
            try {
                result = new JSONObject(value);
            } catch (Exception e) {
                scriptInstalled = false;
                if (historyStarted == 0) historyStarted = SystemClock.elapsedRealtime();
                schedulePoll();
                return;
            }
            scriptInstalled = true;
            String error = result.optString("error");
            String step = result.optString("step");
            if (!error.isEmpty()) {
                fail(error);
                return;
            }
            if ("login".equals(step)) {
                showLogin(view);
            } else {
                if (historyStarted == 0) historyStarted = SystemClock.elapsedRealtime();
                view.setVisibility(View.INVISIBLE);
                retry.setVisibility(View.GONE);
                status.setText("データ更新中… " + month + " のポイント履歴を確認しています");
            }
            if (result.has("name")) {
                String name = result.optString("name");
                int length = result.optInt("length");
                if (!PayFetchPolicy.validFilename(name, month) || length <= 0 || length > MAX_ENCODED) {
                    fail("対象月のポイント履歴CSVを確認できませんでした");
                    return;
                }
                receiptStarted = SystemClock.elapsedRealtime();
                readChunk(view, document, name, new StringBuilder(length), length, receiptStarted);
                schedulePoll();
                return;
            }
            schedulePoll();
        });
        schedulePoll();
    }

    private void showLogin(WebView view) {
        historyStarted = 0;
        view.setVisibility(View.VISIBLE);
        retry.setVisibility(View.VISIBLE);
        status.setText("えらべるPayにログインしてください。履歴画面に戻ると自動取得を再開します");
    }

    private void readChunk(WebView view, int document, String name, StringBuilder data,
                           int length, long received) {
        if (view != web) return;
        if (!validReceipt(view, document, received)) {
            fail("CSV受取中にページが変わったか、受取期限が切れました");
            return;
        }
        int count = Math.min(CHUNK_SIZE, length - data.length());
        String call = "window.__aruPayHistory && window.__aruPayHistory.chunk("
            + data.length() + "," + count + ")";
        view.evaluateJavascript(call, value -> {
            if (view != web) return;
            if (!validReceipt(view, document, received)) {
                fail("CSV受取中にページが変わったか、受取期限が切れました");
                return;
            }
            try {
                String chunk = new JSONArray("[" + value + "]").getString(0);
                if (chunk.length() != count) throw new IllegalArgumentException();
                data.append(chunk);
            } catch (Exception e) {
                fail("CSVを読み取れませんでした");
                return;
            }
            if (data.length() < length) readChunk(view, document, name, data, length, received);
            else parseReceipt(view, document, name, data, received);
        });
    }

    private boolean validReceipt(WebView view, int document, long received) {
        long now = SystemClock.elapsedRealtime();
        return view == web && document == navigation && PayFetchPolicy.historyAddress(view.getUrl())
            && now - started < PayFetchPolicy.SESSION_MS && now - received < PayFetchPolicy.RECEIPT_MS;
    }

    private void parseReceipt(WebView view, int document, String name, StringBuilder data, long received) {
        String expectedMonth = month;
        status.setText("データ更新中… CSVを検証しています");
        new Thread(() -> {
            try {
                byte[] bytes = java.util.Base64.getDecoder().decode(data.toString());
                if (bytes.length > PayFetchPolicy.MAX_BYTES) throw new IllegalArgumentException("CSVが大きすぎます");
                VatonRevenueCsv.CsvResult parsed = VatonRevenueCsv.parseCsv(bytes);
                if (!PayFetchPolicy.dailyVerified(parsed, expectedMonth))
                    throw new IllegalArgumentException("CSVの日付と日別合計を確認できませんでした");
                runOnUiThread(() -> saveReceipt(view, document, name, expectedMonth, parsed, received));
            } catch (Exception e) {
                String reason = e.getMessage();
                runOnUiThread(() -> {
                    if (view == web) fail(reason == null ? "CSVを確認できませんでした" : reason);
                });
            }
        }, "mybest-pay-csv").start();
    }

    private void saveReceipt(WebView view, int document, String name, String expectedMonth,
                             VatonRevenueCsv.CsvResult parsed, long received) {
        if (view != web || isFinishing() || isDestroyed()) return;
        long now = System.currentTimeMillis();
        if (!validReceipt(view, document, received) || !expectedMonth.equals(RevenueMath.month(now))) {
            fail("受取期限または対象月を確認できませんでした");
            return;
        }
        closeWeb();
        try {
            new RevenueManager(this).setCsv(expectedMonth, parsed, name, RevenueMath.date("yyyy-MM-dd", now));
            status.setText(expectedMonth + " のCSVを取り込みました\n"
                + RevenueMath.money(parsed.total) + "（" + parsed.count + "件・日別あり）");
        } catch (Exception e) {
            fail("CSVを保存できませんでした");
        }
    }

    private void fail(String reason) {
        closeWeb();
        status.setText(reason + "\n管理画面の「CSVを取り込む」から手動で取り込めます");
        retry.setVisibility(View.VISIBLE);
    }

    private void closeWeb() {
        handler.removeCallbacks(pollTask);
        WebView view = web;
        web = null;
        loaded = false;
        historyStarted = 0;
        receiptStarted = 0;
        scriptInstalled = false;
        evaluating = false;
        if (view == null) return;
        view.stopLoading();
        page.removeView(view);
        view.destroy();
    }

    private String readScript() throws IOException {
        try (InputStream stream = getResources().openRawResource(R.raw.pay_history_fetch);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            for (int count; (count = stream.read(buffer)) != -1;) out.write(buffer, 0, count);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    private Button button(String label, Runnable action) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
