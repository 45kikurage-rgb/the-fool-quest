package com.aruno.foolquest.widget.next;

import java.util.Locale;

/** UI-only money display: six-digit daily revenue is never rounded or abbreviated. */
public final class CalendarAmountFormatter {
    private CalendarAmountFormatter() {}
    public static String day(long yen) {
        if(yen<0)return "—";
        if(yen<=999999L)return Long.toString(yen); // exact 0..999999, no ¥ or commas in narrow day cells
        if(yen<100000000L) {
            long units=yen/10000L;
            if(yen%10000L==0L)return units+"万";
            return "≈"+Math.round(yen/10000d)+"万";
        }
        if(yen%100000000L==0L)return (yen/100000000L)+"億";
        return "≈"+Math.round(yen/100000000d)+"億";
    }
    public static String month(long yen) {
        return yen<0?"—":String.format(Locale.JAPAN,"¥%,d",yen);
    }
    public static String dayNet(long value,boolean known) {
        if(!known||value==RevenueMath.UNKNOWN)return "—";
        return value<0?"−"+day(-value):day(value);
    }
    public static String monthNet(long value,boolean known) {
        return known?RevenueMath.moneyNet(value):"—";
    }
    /** A loss is always displayed as an explicit negative, even if source income is unknown. */
    public static String dailyLoss(long positiveYen) {
        return positiveYen<=0?"":"−"+day(positiveYen);
    }
    public static String dayWithPrefix(String prefix,long yen) {
        return prefix+" "+day(yen);
    }
}
