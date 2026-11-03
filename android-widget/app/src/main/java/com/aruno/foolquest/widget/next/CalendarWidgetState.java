package com.aruno.foolquest.widget.next;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;

/** Display-only, per-widget preferences. No revenue or source mutations. */
final class CalendarWidgetState {
    static final int ALL=0, TOTAL=1, COUPON=2, TIKTOK=3;
    private CalendarWidgetState(){}
    private static SharedPreferences prefs(Context c){ return RevenueStore.prefs(c); }
    static String month(Context c,int id){
        String saved=prefs(c).getString("calendar.month."+id,"");
        return RevenueMath.validMonth(saved)?saved:RevenueMath.month(System.currentTimeMillis());
    }
    static int filter(Context c,int id){
        int saved=prefs(c).getInt("calendar.filter."+id,ALL);
        return saved>=ALL&&saved<=TIKTOK?saved:ALL;
    }
    static int summary(Context c,int id){
        int saved=prefs(c).getInt("calendar.summary."+id,0);
        return saved>=0&&saved<=2?saved:0;
    }
    static void month(Context c,int id,String value){
        if(!RevenueMath.validMonth(value))throw new IllegalArgumentException("Invalid month");
        prefs(c).edit().putString("calendar.month."+id,value).apply();
    }
    static void filter(Context c,int id,int value){
        if(value<ALL||value>TIKTOK)throw new IllegalArgumentException("Invalid filter");
        prefs(c).edit().putInt("calendar.filter."+id,value).apply();
    }
    static void cycleSummary(Context c,int id){
        prefs(c).edit().putInt("calendar.summary."+id,(summary(c,id)+1)%3).apply();
    }
    static void delete(Context c,int id){
        prefs(c).edit().remove("calendar.month."+id).remove("calendar.filter."+id)
            .remove("calendar.summary."+id).apply();
    }
    static boolean exists(Context c,int id){
        if(id==AppWidgetManager.INVALID_APPWIDGET_ID)return false;
        for(int present:CalendarWidget.ids(c))if(present==id)return true;
        return false;
    }
}
