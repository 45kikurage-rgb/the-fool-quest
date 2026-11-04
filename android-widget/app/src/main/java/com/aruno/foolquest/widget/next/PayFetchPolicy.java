package com.aruno.foolquest.widget.next;

import java.net.URI;
import java.util.Locale;
import java.util.Map;

/** Source and CSV acceptance rules shared by the WebView fetch and its tests. */
final class PayFetchPolicy {
    private PayFetchPolicy() {}

    static final String VATON_HISTORY_URL = "https://wallet.vaton.jp/point/point_logs";
    static final String VATON_LOGIN_URL = "https://wallet.vaton.jp/sign_in?destination=point_logs";
    static final long SESSION_MS = 10 * 60 * 1000L;
    static final long DOWNLOAD_MS = 120 * 1000L;
    static final long RECEIPT_MS = 60 * 1000L;
    static final int MAX_BYTES = 4_000_000;

    static boolean walletAddress(String address) { return walletUri(address) != null; }

    static boolean historyAddress(String address) {
        URI uri = walletUri(address);
        if (uri == null) return false;
        String path = uri.getRawPath();
        return "/point/point_logs".equals(path) || "/point/point_logs/".equals(path);
    }

    static boolean loginAddress(String address) {
        URI uri = walletUri(address);
        if (uri == null) return false;
        String path = uri.getRawPath();
        return "/sign_in".equals(path) || "/login".equals(path) || path.startsWith("/oauth/")
            || path.startsWith("/sign_up/");
    }

    static boolean canNavigate(String address, boolean loginFlow) {
        return walletAddress(address) || (loginFlow && httpsHost(address) != null);
    }

    static String httpsHost(String address) {
        if (address == null) return null;
        try {
            URI uri = URI.create(address);
            return "https".equals(uri.getScheme()) && uri.getRawUserInfo() == null
                && (uri.getPort() == -1 || uri.getPort() == 443) ? uri.getHost() : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static URI walletUri(String address) {
        return "wallet.vaton.jp".equals(httpsHost(address)) ? URI.create(address) : null;
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

    static boolean validFilename(String name, String expectedMonth) {
        return isHistoryCsv(name) && expectedMonth != null && expectedMonth.equals(monthIn(name));
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
