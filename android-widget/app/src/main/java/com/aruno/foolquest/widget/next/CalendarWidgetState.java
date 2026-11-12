package com.aruno.foolquest.widget.next;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;

/** Display-only, per-widget preferences. No revenue or source mutations. */
final class CalendarWidgetState {
    static final int ALL=0, TOTAL=1, COUPON=2, TIKTOK=3, LOSS=4;
    private CalendarWidgetState(){}
    private static final Object SUMMARY_LOCK=new Object();
    private static final java.util.Map<Integer,Integer> pending=new java.util.HashMap<>();
    private static final java.util.List<android.content.BroadcastReceiver.PendingResult> broadcasts=new java.util.ArrayList<>();
    private static final android.os.Handler main=new android.os.Handler(android.os.Looper.getMainLooper());
    private static final java.util.concurrent.ExecutorService saves=java.util.concurrent.Executors.newSingleThreadExecutor(task->{
        Thread thread=new Thread(task,"calendar-summary-save");thread.setDaemon(true);return thread;
    });
    private static Context saveContext;
    private static final Runnable flush=()->{
        final java.util.Map<Integer,Integer> snapshot;
        final java.util.List<android.content.BroadcastReceiver.PendingResult> results;
        final Context context;
        synchronized(SUMMARY_LOCK){snapshot=new java.util.HashMap<>(pending);results=new java.util.ArrayList<>(broadcasts);broadcasts.clear();context=saveContext;}
        saves.execute(()->{
            try{
                synchronized(BackupCoordinator.LOCK){
                    java.util.Map<Integer,Integer> current=new java.util.HashMap<>();
                    synchronized(SUMMARY_LOCK){for(java.util.Map.Entry<Integer,Integer> entry:snapshot.entrySet())if(entry.getValue().equals(pending.get(entry.getKey())))current.put(entry.getKey(),entry.getValue());}
                    if(!current.isEmpty()){
                        SharedPreferences.Editor editor=prefs(context).edit();
                        for(java.util.Map.Entry<Integer,Integer> entry:current.entrySet())editor.putInt("calendar.summary."+entry.getKey(),entry.getValue());
                        // Keep the existing write-ahead backup/validation boundary.
                        // Only its work is coalesced and moved off the UI thread.
                        if(editor.commit())synchronized(SUMMARY_LOCK){for(java.util.Map.Entry<Integer,Integer> entry:current.entrySet())if(entry.getValue().equals(pending.get(entry.getKey())))pending.remove(entry.getKey());}
                    }
                }
            }finally{for(android.content.BroadcastReceiver.PendingResult result:results)result.finish();}
        });
    };
    static void discardPendingSummaries(){
        synchronized(SUMMARY_LOCK){pending.clear();main.removeCallbacks(flush);for(android.content.BroadcastReceiver.PendingResult result:broadcasts)result.finish();broadcasts.clear();}
    }

    private static SharedPreferences prefs(Context c){ return RevenueStore.prefs(c); }
    static String month(Context c,int id){
        String saved=prefs(c).getString("calendar.month."+id,"");
        return RevenueMath.validMonth(saved)?saved:RevenueMath.month(System.currentTimeMillis());
    }
    static int filter(Context c,int id){
        int saved=prefs(c).getInt("calendar.filter."+id,ALL);
        return saved>=ALL&&saved<=LOSS?saved:ALL;
    }
    static int summary(Context c,int id){
        synchronized(SUMMARY_LOCK){Integer value=pending.get(id);if(value!=null)return value;}
        int saved=prefs(c).getInt("calendar.summary."+id,0);
        return saved>=0&&saved<=2?saved:0;
    }
    static void followCurrentMonth(Context c,int id){
        // No saved month means always follow JST current month at redraw.
        prefs(c).edit().remove("calendar.month."+id).apply();
    }
    static boolean isFollowingCurrentMonth(Context c,int id){
        return !RevenueMath.validMonth(prefs(c).getString("calendar.month."+id,""));
    }
    static void month(Context c,int id,String value){
        if(!RevenueMath.validMonth(value))throw new IllegalArgumentException("Invalid month");
        prefs(c).edit().putString("calendar.month."+id,value).apply();
    }
    static void filter(Context c,int id,int value){
        if(value<ALL||value>LOSS)throw new IllegalArgumentException("Invalid filter");
        prefs(c).edit().putInt("calendar.filter."+id,value).apply();
    }
    static void cycleSummary(Context c,int id){cycleSummary(c,id,null);}
    static void cycleSummary(Context c,int id,android.content.BroadcastReceiver.PendingResult result){
        // Obtain the coordinator before SUMMARY_LOCK. Workers/restore use
        // BackupCoordinator.LOCK -> SUMMARY_LOCK, so never invert that order.
        SharedPreferences stored=prefs(c);
        synchronized(SUMMARY_LOCK){
            Integer queued=pending.get(id);int value=queued==null?stored.getInt("calendar.summary."+id,0):queued;
            if(value<0||value>2)value=0;
            pending.put(id,(value+2)%3);saveContext=c.getApplicationContext();
            if(result!=null)broadcasts.add(result);
            main.removeCallbacks(flush);main.postDelayed(flush,16);
        }
    }
    static boolean forecastVisible(Context c,int id){
        return prefs(c).getInt("calendar.forecastHidden."+id,0)!=1;
    }
    static void toggleForecast(Context c,int id){
        prefs(c).edit().putInt("calendar.forecastHidden."+id,forecastVisible(c,id)?1:0).apply();
    }
    static void delete(Context c,int id){
        synchronized(BackupCoordinator.LOCK){
            synchronized(SUMMARY_LOCK){pending.remove(id);}
            prefs(c).edit().remove("calendar.month."+id).remove("calendar.filter."+id)
                .remove("calendar.summary."+id).remove("calendar.forecastHidden."+id).apply();
        }
    }
    static boolean exists(Context c,int id){
        if(id==AppWidgetManager.INVALID_APPWIDGET_ID)return false;
        for(int present:CalendarWidget.ids(c))if(present==id)return true;
        return false;
    }
}
