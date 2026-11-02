package com.aruno.foolquest.widget.next;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;

/** Foreground-only updates; activities own the start/stop lifecycle. */
final class DayChangeObserver extends BroadcastReceiver {
    private final Context context;
    private final Runnable update;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean active;
    private final Runnable tick=()->changed();
    DayChangeObserver(Context context,Runnable update){this.context=context;this.update=update;}
    void start(){
        if(active)return;active=true;
        IntentFilter filter=new IntentFilter(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_CHANGED);filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        context.registerReceiver(this,filter);schedule();
    }
    void stop(){
        if(!active)return;active=false;handler.removeCallbacks(tick);context.unregisterReceiver(this);
    }
    private void schedule(){
        handler.removeCallbacks(tick);long now=System.currentTimeMillis();
        handler.postDelayed(tick,Math.max(1,RevenueMath.nextMidnightJst(now)-now));
    }
    private void changed(){if(active){update.run();schedule();}}
    @Override public void onReceive(Context context,Intent intent){changed();}
}
