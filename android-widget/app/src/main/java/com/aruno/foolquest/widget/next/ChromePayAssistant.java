package com.aruno.foolquest.widget.next;

import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Operates Chrome for one point-history CSV download, only while a fetch
 * session started from the in-app "データ更新" action is active. Ported from
 * ARUNO ASSIST PAY SYNC (ChromePayAssistant). Login, permission dialogs and
 * the app chooser remain user actions; only a native Chrome dialog that
 * names the expected CSV may be acknowledged, and permission dialogs are
 * never touched.
 */
final class ChromePayAssistant {
    private final PayFetchAccessibilityService service;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SyncOverlay overlay;
    private final PayFetchSession session;
    private boolean running;
    private boolean dialogClicked;
    private long downloadClickedAt;
    private boolean openClicked;
    private boolean navigating;
    private boolean downloadConfirmed;
    private long lastAction;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            if (!session.active()) {
                fail("取得を停止しました。データ更新から再実行できます");
                return;
            }
            try {
                step();
            } catch (RuntimeException e) {
                fail("Chromeの取得操作を停止しました。再実行してください");
                return;
            }
            if (running) handler.postDelayed(this, 500);
        }
    };

    ChromePayAssistant(PayFetchAccessibilityService service) {
        this.service = service;
        this.overlay = new SyncOverlay(service);
        this.session = new PayFetchSession(service);
    }

    void start() {
        stop();
        running = true;
        dialogClicked = false;
        downloadClickedAt = 0;
        openClicked = false;
        navigating = false;
        downloadConfirmed = false;
        lastAction = 0;
        handler.post(tick);
    }

    void onAccessibilityEvent(AccessibilityEvent event) {
        if (running && event != null && PayFetchPolicy.CHROME.equals(event.getPackageName() == null
                ? null : event.getPackageName().toString())) {
            handler.removeCallbacks(tick);
            handler.post(tick);
        }
    }

    void stop() {
        running = false;
        handler.removeCallbacks(tick);
        overlay.hide();
    }

    private void fail(String message) {
        stop();
        session.clear();
        try {
            Intent intent = new Intent(service, PayFetchActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(PayFetchActivity.EXTRA_ERROR, message);
            service.startActivity(intent);
        } catch (RuntimeException ignored) {}
    }

    private void step() {
        long now = System.currentTimeMillis();
        if (downloadClickedAt > 0 && now - downloadClickedAt > 120_000) {
            fail("CSVを受け取れませんでした。ChromeのダウンロードからCSVを開くか、手動取込してください");
            return;
        }
        AccessibilityNodeInfo root = service.getRootInActiveWindow();
        if (root == null) return;
        try {
            CharSequence rootPackage = root.getPackageName();
            if (rootPackage == null || !PayFetchPolicy.CHROME.equals(rootPackage.toString())) {
                overlay.hide();
                return;
            }
            if (now - lastAction < 1000) return;
            List<AccessibilityNodeInfo> nodes = new ArrayList<>();
            collect(root, nodes, 0);
            try {
                String address = "";
                for (AccessibilityNodeInfo node : nodes) {
                    if ("com.android.chrome:id/url_bar".equals(node.getViewIdResourceName())) {
                        address = node.getText() == null ? "" : node.getText().toString();
                        break;
                    }
                }
                if (downloadClickedAt > 0) {
                    overlay.show("CSV取得後、Aru-MyBestへ取り込みます\nアプリ選択が出たらAru-MyBestを選んでください");
                    // Chrome may ask to download the same filename again. Only acknowledge
                    // a native dialog naming the expected CSV; never touch permission dialogs.
                    if (!downloadConfirmed) {
                        boolean namesCsv = false;
                        for (AccessibilityNodeInfo node : nodes) {
                            String id = node.getViewIdResourceName();
                            if (id != null && id.startsWith("com.android.chrome:id/")) {
                                String label = label(node);
                                if (label.contains("ポイント履歴") && containsIgnoreCase(label, ".csv")
                                        && session.month().equals(PayFetchPolicy.monthIn(label))) {
                                    namesCsv = true;
                                    break;
                                }
                            }
                        }
                        if (namesCsv) {
                            for (AccessibilityNodeInfo node : nodes) {
                                String id = node.getViewIdResourceName();
                                if (node.isVisibleToUser()
                                        && ("com.android.chrome:id/positive_button".equals(id) || "android:id/button1".equals(id))
                                        && isDownloadLabel(label(node))) {
                                    if (click(node)) {
                                        downloadConfirmed = true;
                                        lastAction = now;
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    if (!openClicked) {
                        // A native Chrome completion message must name this month's CSV.
                        AccessibilityNodeInfo message = null;
                        for (AccessibilityNodeInfo node : nodes) {
                            String id = node.getViewIdResourceName();
                            if (node.isVisibleToUser() && id != null && id.startsWith("com.android.chrome:id/")) {
                                String label = label(node);
                                if (label.contains("ポイント履歴") && containsIgnoreCase(label, ".csv")
                                        && session.month().equals(PayFetchPolicy.monthIn(label))) {
                                    message = node;
                                    break;
                                }
                            }
                        }
                        if (message != null) {
                            AccessibilityNodeInfo parent = message.getParent();
                            for (int depth = 0; depth < 3 && parent != null && !openClicked; depth++) {
                                List<AccessibilityNodeInfo> children = new ArrayList<>();
                                collect(parent, children, 0);
                                StringBuilder nativeText = new StringBuilder();
                                AccessibilityNodeInfo open = null;
                                for (AccessibilityNodeInfo child : children) {
                                    String id = child.getViewIdResourceName();
                                    if (id != null && id.startsWith("com.android.chrome:id/")) {
                                        if (nativeText.length() > 0) nativeText.append('\n');
                                        nativeText.append(label(child));
                                    }
                                    if (open == null && child.isVisibleToUser()
                                            && ("開く".equals(label(child)) || "Open".equals(label(child)) || "OPEN".equals(label(child)))
                                            && id != null && id.startsWith("com.android.chrome:id/")) {
                                        open = child;
                                    }
                                }
                                boolean completed = nativeText.toString().contains("ダウンロード")
                                    || containsIgnoreCase(nativeText.toString(), "download");
                                if (completed && open != null) {
                                    session.expectReceipt();
                                    openClicked = click(open);
                                    if (openClicked) lastAction = now;
                                }
                                for (AccessibilityNodeInfo child : children) child.recycle();
                                AccessibilityNodeInfo next = parent.getParent();
                                parent.recycle();
                                parent = next;
                            }
                            if (parent != null) parent.recycle();
                        }
                    }
                    return;
                }
                boolean onHistory = hasLabel(nodes, "ポイント履歴")
                    && hasLabel(nodes, "利用実績CSVダウンロード");
                if (!PayFetchPolicy.historyAddress(address, onHistory || dialogClicked)) {
                    overlay.show("ChromeでえらべるPayにログインしてください\nログイン後、ポイント履歴から自動取得します");
                    String bare = address.startsWith("https://") ? address.substring("https://".length()) : address;
                    while (bare.endsWith("/")) bare = bare.substring(0, bare.length() - 1);
                    if (!navigating && bare.equals("wallet.vaton.jp/home")) {
                        navigating = true;
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(PayFetchPolicy.VATON_HISTORY_URL));
                            intent.setPackage(PayFetchPolicy.CHROME);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            service.startActivity(intent);
                        } catch (RuntimeException ignored) {}
                        lastAction = now;
                    }
                    if (!navigating && bare.equals("wallet.vaton.jp")) {
                        for (AccessibilityNodeInfo node : nodes) {
                            if (node.isVisibleToUser() && "ポイント履歴".equals(label(node))) {
                                if (click(node)) {
                                    navigating = true;
                                    lastAction = now;
                                }
                                break;
                            }
                        }
                    }
                    return;
                }
                overlay.show("えらべるPay：" + session.month() + "のCSVを取得しています");
                AccessibilityNodeInfo download = null;
                for (AccessibilityNodeInfo node : nodes) {
                    if (node.isVisibleToUser() && "ダウンロードする".equals(label(node))) {
                        download = node;
                        break;
                    }
                }
                if (download != null && dialogClicked) {
                    // The received filename AND parsed month are checked before automatic import.
                    if (click(download)) {
                        downloadClickedAt = now;
                        lastAction = now;
                        session.expectReceipt();
                    }
                    return;
                }
                if (!dialogClicked) {
                    for (AccessibilityNodeInfo node : nodes) {
                        if (node.isVisibleToUser() && "利用実績CSVダウンロード".equals(label(node))) {
                            if (click(node)) {
                                dialogClicked = true;
                                lastAction = now;
                            }
                            break;
                        }
                    }
                }
            } finally {
                for (AccessibilityNodeInfo node : nodes) node.recycle();
            }
        } finally {
            root.recycle();
        }
    }

    private static boolean hasLabel(List<AccessibilityNodeInfo> nodes, String wanted) {
        for (AccessibilityNodeInfo node : nodes) if (wanted.equals(label(node))) return true;
        return false;
    }

    private static boolean isDownloadLabel(String label) {
        return "ダウンロード".equals(label) || "再ダウンロード".equals(label)
            || "Download".equals(label) || "Download again".equals(label);
    }

    private static boolean containsIgnoreCase(String text, String needle) {
        return text.toLowerCase(java.util.Locale.ROOT).contains(needle.toLowerCase(java.util.Locale.ROOT));
    }

    private static String label(AccessibilityNodeInfo node) {
        CharSequence value = node.getText() != null ? node.getText() : node.getContentDescription();
        return value == null ? "" : value.toString().trim();
    }

    private static void collect(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> result, int depth) {
        if (depth > 30 || result.size() >= 1800) return;
        result.add(AccessibilityNodeInfo.obtain(node));
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            collect(child, result, depth + 1);
            child.recycle();
        }
    }

    private static boolean click(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = AccessibilityNodeInfo.obtain(node);
        for (int i = 0; i < 4; i++) {
            if (current == null) return false;
            if (current.isEnabled() && current.isVisibleToUser() && current.isClickable()) {
                boolean result = current.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                current.recycle();
                return result;
            }
            AccessibilityNodeInfo parent = current.getParent();
            current.recycle();
            current = parent;
        }
        if (current != null) current.recycle();
        return false;
    }
}
