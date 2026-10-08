package com.aruno.foolquest.widget;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Pure display calculations. No source records or revenue database. */
public final class RevenueMath {
    public static final long MAX_YEN = 9007199254740991L;
    private RevenueMath() {}
    public static String month(long time) { return date("yyyy-MM", time); }
    public static String date(String pattern, long time) {
        SimpleDateFormat f = new SimpleDateFormat(pattern, Locale.JAPAN);
        f.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo")); return f.format(new Date(time));
    }
    public static String money(long n) { return n < 0 ? "—" : "¥" + String.format(Locale.US, "%,d", n); }
    public static String percent(long value, long goal) {
        return value < 0 || goal <= 0 ? "—%" : String.format(Locale.US, "%.2f%%", (double)value / goal * 100);
    }
    public static float progress(long value, long goal) {
        return value < 0 || goal <= 0 ? 0 : (float)Math.min(1d, (double)value / goal);
    }
    public static long total(long a, long b) {
        if (a < 0 || b < 0) return -1;
        if (a > MAX_YEN - b) return -1;
        return a + b;
    }
    public static long yen(String raw, boolean positive) {
        if (raw == null || !raw.matches("[0-9]{1,16}")) throw new IllegalArgumentException("金額の形式が不正です");
        long n = Long.parseLong(raw);
        if (n > MAX_YEN || (positive && n < 1)) throw new IllegalArgumentException("金額が範囲外です");
        return n;
    }
    public static boolean validMonth(String m) { return m != null && m.matches("20[0-9]{2}-(0[1-9]|1[0-2])"); }
}
