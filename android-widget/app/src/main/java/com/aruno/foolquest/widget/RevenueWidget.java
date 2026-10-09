package com.aruno.foolquest.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Build;
import android.util.SizeF;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import android.widget.RemoteViews;

public final class RevenueWidget extends AppWidgetProvider {
    static final String REFRESH="com.aruno.foolquest.widget.REFRESH";
    static int[] ids(Context c){return AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c,RevenueWidget.class));}
    static void renderAll(Context c){
        AppWidgetManager manager=AppWidgetManager.getInstance(c);
        RevenueStore.Data d=RevenueStore.read(c);DisplaySettings s=DisplaySettings.load(RevenueStore.prefs(c));
        for(int id:ids(c))render(c,manager,id,d,s);
    }
    private static void render(Context c,AppWidgetManager m,int id,RevenueStore.Data d,DisplaySettings s){
        Bundle o=m.getAppWidgetOptions(id);
        float screen=c.getResources().getDisplayMetrics().widthPixels/c.getResources().getDisplayMetrics().density;
        int w=Math.min(o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,340),Math.round(screen));
        int h=o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,130);
        NativeWidgetViews.Result result=NativeWidgetViews.create(c,d,s,w,h);
        m.updateAppWidget(id,result.views);
    }
    @Override public void onReceive(Context c,Intent i){
        if(REFRESH.equals(i.getAction())){
            PendingResult p=goAsync();RevenueUpdate.start(c,p::finish);return;
        }
        super.onReceive(c,i);
        if(Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())||Intent.ACTION_MY_PACKAGE_REPLACED.equals(i.getAction())){
            renderAll(c);if(ids(c).length>0)RevenueJob.schedule(c);
        }
    }
    @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){
        renderAll(c);RevenueJob.schedule(c);PendingResult p=goAsync();RevenueUpdate.start(c,p::finish);
    }
    @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle o){renderAll(c);}
    @Override public void onEnabled(Context c){RevenueJob.schedule(c);}
    @Override public void onDisabled(Context c){RevenueJob.cancel(c);}
}
