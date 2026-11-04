package com.aruno.foolquest.widget.next;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Console and receipt point for the point-history CSV auto-fetch.
 *
 * The fetch itself runs in Chrome, operated by PayFetchAccessibilityService
 * only after the user taps "データ更新". The downloaded CSV arrives here via
 * the system app chooser. The automatic import saves only when every check
 * in PayFetchPolicy passes, including the daily breakdown; otherwise the
 * stored values are kept and the user is guided to the manual CSV import.
 */
public class PayFetchActivity extends Activity {
    static final String EXTRA_ERROR = "com.aruno.foolquest.widget.next.PAY_FETCH_ERROR";

    private TextView status;
    private PayFetchSession session;
    private boolean importInProgress;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new PayFetchSession(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xff101010);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(24), dp(16), dp(28));
        scroll.addView(content);
        setContentView(scroll);

        TextView head = text("データ更新", 22);
        content.addView(head);
        status = text("", 15);
        content.addView(status);
        if (isInternalEntry()) {
            content.addView(action("ChromeでCSV取得を開始／再実行", () -> startFetch()));
            content.addView(action("ユーザー補助の設定を開く",
                () -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))));
            content.addView(action("取得を中止", () -> {
                PayFetchAccessibilityService service = PayFetchAccessibilityService.instance;
                if (service != null) service.stopPayFetch();
                session.clear();
                status.setText("取得を中止しました。保存済みのデータは変更していません");
            }));
        }
        content.addView(action("収益管理へ戻る", () -> {
            startActivity(new Intent(this, ManagementActivity.class));
            finish();
        }));
        handleIntent(getIntent(), savedInstanceState == null);
    }

    boolean isInternalEntry() { return false; }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent, true);
    }

    private void handleIntent(Intent source, boolean fresh) {
        if (source == null || importInProgress) return;
        if (isInternalEntry()) {
            String error = source.getStringExtra(EXTRA_ERROR);
            if (error != null) status.setText(error);
            else if (fresh) startFetch();
            else status.setText("取得中です。Chromeの案内を確認してください");
            return;
        }
        Uri uri = Intent.ACTION_VIEW.equals(source.getAction()) ? source.getData() : null;
        if (uri != null && "content".equals(uri.getScheme())) {
            processReceipt(uri);
        } else {
            status.setText("管理画面の「データ更新」から取得を開始してください");
        }
    }

    private void startFetch() {
        if (importInProgress) return;
        PayFetchAccessibilityService service = PayFetchAccessibilityService.instance;
        if (service == null) {
            status.setText("初回は「ユーザー補助の設定を開く」から「Aru-MyBest データ更新」を有効にして、もう一度開始してください");
            return;
        }
        if (session.active()) {
            status.setText("取得中です。再実行する場合は、いったん中止してください");
            return;
        }
        if (!session.begin()) {
            status.setText("取得を開始できません。端末の保存状態を確認してください");
            return;
        }
        status.setText("データ更新中\nChromeでえらべるPayを開きます。ログインが必要な場合はログインしてください。\nCSVのアプリ選択が出たらAru-MyBestを選んでください");
        try {
            Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(PayFetchPolicy.VATON_HISTORY_URL));
            browser.setPackage(PayFetchPolicy.CHROME);
            startActivity(browser);
            service.startPayFetch();
        } catch (RuntimeException e) {
            session.clear();
            service.stopPayFetch();
            status.setText("Chromeを起動できません。Chromeのインストール・有効化を確認してください");
        }
    }

    private void processReceipt(final Uri uri) {
        final long started = session.started();
        final long opened = session.opened();
        final String expectedMonth = session.month();
        if (!PayFetchPolicy.canReadReceipt(started, opened, System.currentTimeMillis(), uri.getAuthority())) {
            status.setText(receiptError("今回の自動取得に対応するCSVを確認できませんでした"));
            return;
        }
        importInProgress = true;
        status.setText("データ更新中\nCSVを確認しています…");
        new Thread(() -> {
            try {
                final String name = displayName(uri);
                if (!PayFetchPolicy.isHistoryCsv(name) || !expectedMonth.equals(PayFetchPolicy.monthIn(name)))
                    throw new IllegalArgumentException("対象月のポイント履歴CSVではありません");
                final VatonRevenueCsv.CsvResult parsed = VatonRevenueCsv.parseCsv(readLimited(uri));
                if (!PayFetchPolicy.canAutoImport(started, opened, System.currentTimeMillis(),
                        uri.getAuthority(), name, parsed.datesMonth, expectedMonth))
                    throw new IllegalArgumentException("受取期限と対象月を確認できませんでした");
                runOnUiThread(() -> finishReceipt(started, opened, expectedMonth, name, parsed));
            } catch (Exception e) {
                final String message = e.getMessage() == null ? "CSVを確認してください" : e.getMessage();
                runOnUiThread(() -> {
                    importInProgress = false;
                    status.setText(receiptError(message));
                });
            }
        }).start();
    }

    private void finishReceipt(long started, long opened, String month, String name,
                               VatonRevenueCsv.CsvResult parsed) {
        importInProgress = false;
        if (isFinishing() || isDestroyed()) return;
        if (!PayFetchPolicy.dailyVerified(parsed, month)) {
            status.setText(receiptError("CSVの日付と日別合計を確認できませんでした"));
            return;
        }
        if (!session.consume(started, opened, month)) {
            status.setText(receiptError("取得が中止されたか、受取期限が切れました"));
            return;
        }
        PayFetchAccessibilityService service = PayFetchAccessibilityService.instance;
        if (service != null) service.stopPayFetch();
        try {
            String acquired = RevenueMath.date("yyyy-MM-dd", opened);
            new RevenueManager(this).setCsv(month, parsed, name, acquired);
            status.setText(month + " のCSVを自動で取り込みました\n"
                + RevenueMath.money(parsed.total) + "（" + parsed.count + "件・日別あり）");
        } catch (Exception e) {
            status.setText(receiptError(e.getMessage() == null ? "CSVを確認してください" : e.getMessage()));
        }
    }

    private static String receiptError(String message) {
        return message + "\n保存済みのデータは変更していません。管理画面の「CSVを取り込む」から手動で取り込めます";
    }

    private String displayName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0 && !cursor.isNull(index)) return cursor.getString(index);
            }
        } catch (RuntimeException ignored) {}
        String segment = uri.getLastPathSegment();
        return segment == null ? null : segment.substring(segment.lastIndexOf('/') + 1);
    }

    private byte[] readLimited(Uri uri) throws Exception {
        try (InputStream stream = getContentResolver().openInputStream(uri)) {
            if (stream == null) throw new IllegalArgumentException("CSVを開けません");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = stream.read(buffer)) != -1) {
                if (out.size() + count > PayFetchPolicy.MAX_BYTES)
                    throw new IllegalArgumentException("CSVデータが大きすぎます");
                out.write(buffer, 0, count);
            }
            return out.toByteArray();
        }
    }

    private TextView text(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(0xffeeeeee);
        t.setTextSize(size);
        t.setPadding(dp(4), dp(10), dp(4), dp(10));
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    private Button action(String name, Runnable task) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(name);
        button.setOnClickListener(v -> task.run());
        button.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return button;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
