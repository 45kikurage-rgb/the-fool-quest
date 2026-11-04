package com.aruno.foolquest.widget.next;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure Java calendar and CSV date logic shared by production code and offline tests. */
public final class CalendarDateMath {
    private static final Pattern MONTH=Pattern.compile("20[0-9]{2}-(0[1-9]|1[0-2])");
    private static final Pattern DATE=Pattern.compile("^(20[0-9]{2})([/-])([0-9]{1,2})\\2([0-9]{1,2})(?:[ T](?:[01][0-9]|2[0-3]):[0-5][0-9](?::[0-5][0-9])?)?$");
    private CalendarDateMath(){}
    private static Calendar calendar(String month){
        if(month==null||!MONTH.matcher(month).matches())throw new IllegalArgumentException("Invalid month");
        Calendar c=Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"),Locale.JAPAN);
        c.clear();c.setLenient(false);
        c.set(Integer.parseInt(month.substring(0,4)),Integer.parseInt(month.substring(5,7))-1,1);
        return c;
    }
    public static int daysInMonth(String month){return calendar(month).getActualMaximum(Calendar.DAY_OF_MONTH);}
    public static int firstWeekday(String month){return calendar(month).get(Calendar.DAY_OF_WEEK)-Calendar.SUNDAY;}
    /** Return null for date-free, impossible or unrecognized CSV dates; never impute a day. */
    public static String normalizeCsvDate(String raw){
        if(raw==null)return null;
        Matcher m=DATE.matcher(raw.trim());
        if(!m.matches())return null;
        int year=Integer.parseInt(m.group(1)),month=Integer.parseInt(m.group(3)),day=Integer.parseInt(m.group(4));
        if(month<1||month>12||day<1)return null;
        String monthKey=String.format(Locale.US,"%04d-%02d",year,month);
        if(day>daysInMonth(monthKey))return null;
        return String.format(Locale.US,"%04d-%02d-%02d",year,month,day);
    }
}
