package com.aruno.foolquest.widget.next;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Refresh cached display at JST midnight without waiting for a network job. */
public final class DailyTargetReceiver extends BroadcastReceiver {
    private static PendingIntent alarm(Context c) {
        return PendingIntent.getBroadcast(c,20,new Intent(c,DailyTargetReceiver.class)
            .setAction("com.aruno.foolquest.widget.next.DAILY_TARGET"),
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    static boolean exactAllowed(Context c) {
        return Build.VERSION.SDK_INT<31||c.getSystemService(AlarmManager.class).canScheduleExactAlarms();
    }
    static void cancel(Context c){c.getSystemService(AlarmManager.class).cancel(alarm(c));}
    static void schedule(Context c) {
        if(RevenueWidget.ids(c).length==0&&CalendarWidget.ids(c).length==0){cancel(c);return;}
        AlarmManager manager=c.getSystemService(AlarmManager.class);
        long next=RevenueMath.nextMidnightJst(System.currentTimeMillis());
        PendingIntent intent=alarm(c);
        if(exactAllowed(c))try{
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next,intent);return;
        }catch(SecurityException revoked){/* Permission may change after the check. */}
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next,intent);
    }
    @Override public void onReceive(Context c,Intent intent){
        RevenueWidget.renderAll(c);CalendarWidget.renderAll(c);
    }
}
