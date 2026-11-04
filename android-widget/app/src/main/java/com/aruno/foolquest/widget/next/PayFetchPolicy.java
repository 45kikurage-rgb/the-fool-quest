package com.aruno.foolquest.widget.next;

import java.util.Locale;
import java.util.Map;

/**
 * Pure rules for the in-app point-history CSV fetch. Ported from ARUNO ASSIST
 * PAY SYNC (ChromePayPolicy) with the Aru-MyBest limits: the automatic import
 * is accepted only inside a short-lived fetch session, only from Chrome or
 * the system downloads provider, only for the current month's history CSV,
 * and (per the 2026-11-04 consolidation decision) only when the CSV verifies
 * down to the daily breakdown.
 */
final class PayFetchPolicy {
    private PayFetchPolicy() {}

    static final String CHROME = "com.android.chrome";
    static final String VATON_HISTORY_URL = "https://wallet.vaton.jp/point/point_logs";
    static final long SESSION_MS = 10 * 60 * 1000L;
    static final long RECEIPT_MS = 60 * 1000L;
    static final long MAX_BYTES = 4_000_000L;

    static boolean active(long started, long now) {
        return started > 0 && now - started >= 0 && now - started <= SESSION_MS;
    }

    static boolean historyAddress(String address, boolean historyControlsVisible) {
        String normalized = address.trim();
        if (normalized.startsWith("https://")) normalized = normalized.substring("https://".length());
        return (normalized.equals("wallet.vaton.jp") && historyControlsVisible)
            || normalized.equals("wallet.vaton.jp/point/point_logs")
            || normalized.startsWith("wallet.vaton.jp/point/point_logs?")
            || normalized.startsWith("wallet.vaton.jp/point/point_logs/");
    }

    static boolean isHistoryCsv(String name) {
        return name != null
            && name.startsWith("ポイント履歴")
            && name.toLowerCase(Locale.ROOT).endsWith(".csv")
            && name.indexOf('/') < 0
            && name.indexOf('\\') < 0;
    }

    /** Extracts "yyyy-MM" from text such as a history CSV file name, or null. */
    static String monthIn(String text) {
        if (text == null) return null;
        java.util.regex.Matcher match =
            java.util.regex.Pattern.compile("(20\\d{2})(?:年\\s*|[-/])(\\d{1,2})(?:月|(?=[^0-9]|$))").matcher(text);
        if (!match.find()) return null;
        int month = Integer.parseInt(match.group(2));
        if (month < 1 || month > 12) return null;
        return String.format(Locale.ROOT, "%s-%02d", match.group(1), month);
    }

    static boolean canAutoImport(long started, long opened, long now, String authority,
                                 String name, String month, String expectedMonth) {
        if (!active(started, now)) return false;
        if (opened < started || now - opened < 0 || now - opened > RECEIPT_MS) return false;
        if (!("com.android.chrome.FileProvider".equals(authority)
            || "com.android.providers.downloads".equals(authority)
            || "com.android.providers.downloads.documents".equals(authority)
            || "downloads".equals(authority))) return false;
        return isHistoryCsv(name) && expectedMonth != null
            && expectedMonth.equals(monthIn(name)) && expectedMonth.equals(month);
    }

    /**
     * The automatic import saves only when the parsed CSV is dated, belongs
     * entirely to the expected month, and its daily amounts add up exactly
     * to its monthly total. Anything else keeps the stored values and falls
     * back to the manual import entry point.
     */
    static boolean dailyVerified(VatonRevenueCsv.CsvResult result, String expectedMonth) {
        if (result == null || expectedMonth == null) return false;
        if (!result.datesComplete || !expectedMonth.equals(result.datesMonth)) return false;
        if (result.count <= 0 || result.days.isEmpty()) return false;
        long sum = 0;
        for (Map.Entry<String, Long> day : result.days.entrySet()) {
            if (day.getKey() == null || !day.getKey().startsWith(expectedMonth + "-")) return false;
            long value = day.getValue();
            if (value < 0 || value > result.total - sum) return false;
            sum += value;
        }
        return sum == result.total;
    }
}
