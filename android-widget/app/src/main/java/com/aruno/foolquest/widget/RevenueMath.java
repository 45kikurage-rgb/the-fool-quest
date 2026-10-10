package com.aruno.foolquest.widget;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Calendar;

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
    /** A completed first lap is full; values above 100% start another lap. */
    public static boolean beyondGoal(long value,long goal) {
        return goal > 0 && value > goal && value <= MAX_YEN;
    }
    public static float progress(long value, long goal) {
        if (value < 0 || goal <= 0) return 0f;
        if (value <= goal) return (float)((double)value / goal);
        // At exact multiples (200%, 300%, ...), show the completed lap full.
        // Use remainder arithmetic to avoid overflow for large yen amounts.
        long remainder = value % goal;
        return remainder == 0 ? 1f : (float)((double)remainder / goal);
    }
    /** Inclusive JST day, ceiling to yen; stale-month data has no current-day target. */
    public static long targetThroughToday(String displayedMonth,long monthlyGoal,long time) {
        if(monthlyGoal<=0||monthlyGoal>MAX_YEN||!month(time).equals(displayedMonth))return -1;
        Calendar day=Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"),Locale.JAPAN);
        day.setTimeInMillis(time);
        int elapsed=day.get(Calendar.DAY_OF_MONTH),days=day.getActualMaximum(Calendar.DAY_OF_MONTH);
        return monthlyGoal/days*elapsed+((monthlyGoal%days)*elapsed+days-1)/days;
    }
    public static final int PACE_UNKNOWN=0, PACE_LOW=1, PACE_BEHIND=2, PACE_ON_TRACK=3;
    /** Same inclusive JST daily pace thresholds as THE FOOL QUEST renderMetric. */
    public static int pace(long value,long monthlyGoal,String displayedMonth,long time) {
        long daily=targetThroughToday(displayedMonth,monthlyGoal,time);
        if(value<0||value>MAX_YEN||daily<0)return PACE_UNKNOWN;
        if(value>=daily)return PACE_ON_TRACK;
        Calendar day=Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"),Locale.JAPAN);
        day.setTimeInMillis(time);
        int elapsed=day.get(Calendar.DAY_OF_MONTH),divisor=day.getActualMaximum(Calendar.DAY_OF_MONTH)*2;
        long half=monthlyGoal/divisor*elapsed+((monthlyGoal%divisor)*elapsed+divisor-1)/divisor;
        return value>=half?PACE_BEHIND:PACE_LOW;
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
