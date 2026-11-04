package com.aruno.foolquest.widget.next;

import java.nio.charset.StandardCharsets;

/** Offline checks for the auto-fetch acceptance rules (PayFetchPolicy). */
public class PayFetchPolicyTest {
    private static int checks;

    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) throw new AssertionError(description);
    }

    public static void main(String[] args) {
        long started = 1_000_000L;
        check(PayFetchPolicy.active(started, started), "session starts active");
        check(PayFetchPolicy.active(started, started + PayFetchPolicy.SESSION_MS), "session active at limit");
        check(!PayFetchPolicy.active(started, started + PayFetchPolicy.SESSION_MS + 1), "session expires");
        check(!PayFetchPolicy.active(0, started), "no session is inactive");

        check(PayFetchPolicy.historyAddress("wallet.vaton.jp/point/point_logs", false), "history url");
        check(PayFetchPolicy.historyAddress("https://wallet.vaton.jp/point/point_logs?x=1", false), "history url with query");
        check(PayFetchPolicy.historyAddress("wallet.vaton.jp/point/point_logs/extra", false), "history url with suffix");
        check(PayFetchPolicy.historyAddress("wallet.vaton.jp", true), "bare domain with history controls");
        check(!PayFetchPolicy.historyAddress("wallet.vaton.jp", false), "bare domain without controls");
        check(!PayFetchPolicy.historyAddress("wallet.vaton.jp/home", true), "home is not history");

        check(PayFetchPolicy.isHistoryCsv("ポイント履歴_2026年11月.csv"), "history csv name");
        check(PayFetchPolicy.isHistoryCsv("ポイント履歴_2026年11月 (3).csv"), "history csv copy name");
        check(!PayFetchPolicy.isHistoryCsv("other.csv"), "other csv rejected");
        check(!PayFetchPolicy.isHistoryCsv("ポイント履歴_2026年11月.csv.exe"), "double extension rejected");
        check(!PayFetchPolicy.isHistoryCsv("a/ポイント履歴.csv"), "path in name rejected");

        check("2026-11".equals(PayFetchPolicy.monthIn("ポイント履歴_2026年11月.csv")), "month from file name");
        check("2026-03".equals(PayFetchPolicy.monthIn("report 2026/3")), "month from slash form");
        check(PayFetchPolicy.monthIn("ポイント履歴_2026年13月.csv") == null, "month 13 rejected");
        check(PayFetchPolicy.monthIn("no month here") == null, "no month found");

        long opened = started + 5_000L;
        long now = opened + 10_000L;
        check(PayFetchPolicy.canAutoImport(started, opened, now,
            "com.android.chrome.FileProvider", "ポイント履歴_2026年11月.csv", "2026-11", "2026-11"),
            "valid auto import");
        check(PayFetchPolicy.canAutoImport(started, opened, now,
            "com.android.providers.downloads.documents", "ポイント履歴_2026年11月.csv", "2026-11", "2026-11"),
            "downloads provider accepted");
        check(!PayFetchPolicy.canAutoImport(started, opened, now,
            "com.example.other", "ポイント履歴_2026年11月.csv", "2026-11", "2026-11"),
            "unknown authority rejected");
        check(!PayFetchPolicy.canAutoImport(started, opened - 120_000L, now,
            "com.android.chrome.FileProvider", "ポイント履歴_2026年11月.csv", "2026-11", "2026-11"),
            "receipt opened before session rejected");
        check(!PayFetchPolicy.canAutoImport(started, opened, opened + PayFetchPolicy.RECEIPT_MS + 1,
            "com.android.chrome.FileProvider", "ポイント履歴_2026年11月.csv", "2026-11", "2026-11"),
            "late receipt rejected");
        check(!PayFetchPolicy.canAutoImport(started, opened, now,
            "com.android.chrome.FileProvider", "ポイント履歴_2026年10月.csv", "2026-10", "2026-11"),
            "previous month rejected");
        check(!PayFetchPolicy.canAutoImport(started, opened, started + PayFetchPolicy.SESSION_MS + 1,
            "com.android.chrome.FileProvider", "ポイント履歴_2026年11月.csv", "2026-11", "2026-11"),
            "expired session rejected");

        String header = "日付,種別,ブランド名,商品名,交換ポイント,円換算額,チャージポイント\n";
        String dated = header
            + "2026/11/01,チャージ,,,0,100,100\n"
            + "2026/11/02,チャージ,,,0,300,300\n"
            + "2026/11/02,交換,,Kyash,-3000,0,0\n";
        VatonRevenueCsv.CsvResult parsed = VatonRevenueCsv.parseCsv(dated.getBytes(StandardCharsets.UTF_8));
        check(parsed.datesComplete && parsed.total == 400 && parsed.count == 3, "dated fixture parses");
        check(PayFetchPolicy.dailyVerified(parsed, "2026-11"), "daily verified for fetch month");
        check(!PayFetchPolicy.dailyVerified(parsed, "2026-10"), "daily rejected for other month");

        String undated = "チャージポイント,説明\n100,a\n300,b\n";
        VatonRevenueCsv.CsvResult noDates = VatonRevenueCsv.parseCsv(undated.getBytes(StandardCharsets.UTF_8));
        check(!noDates.datesComplete, "undated fixture has no dates");
        check(!PayFetchPolicy.dailyVerified(noDates, "2026-11"), "undated csv is not auto-saved");

        String october = header + "2026/10/31,チャージ,,,0,50,50\n";
        VatonRevenueCsv.CsvResult octoberParsed = VatonRevenueCsv.parseCsv(october.getBytes(StandardCharsets.UTF_8));
        check(!PayFetchPolicy.dailyVerified(octoberParsed, "2026-11"), "other-month csv is not auto-saved");
        check(PayFetchPolicy.dailyVerified(octoberParsed, "2026-10"), "other-month csv verifies for its own month");
        check(!PayFetchPolicy.dailyVerified(null, "2026-11"), "null result rejected");

        System.out.println("PayFetchPolicyTest: " + checks + " checks passed");
    }
}
