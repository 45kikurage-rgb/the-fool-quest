package com.aruno.foolquest.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

public final class RevenueWidget extends AppWidgetProvider {
    private static final String REFRESH="com.aruno.foolquest.widget.REFRESH";
    static int[] ids(Context c){return AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c,RevenueWidget.class));}
    static void renderAll(Context c){
        AppWidgetManager manager=AppWidgetManager.getInstance(c);
        RevenueStore.Data d=RevenueStore.read(c);DisplaySettings s=DisplaySettings.load(RevenueStore.prefs(c));
        for(int id:ids(c))render(c,manager,id,d,s);
    }
    private static void render(Context c,AppWidgetManager m,int id,RevenueStore.Data d,DisplaySettings s){
        Bundle o=m.getAppWidgetOptions(id);
        int w=o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,340),h=o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,130);
        if(w<=0)w=340;if(h<=0)h=130;
        RemoteViews rv=new RemoteViews(c.getPackageName(),R.layout.widget);
        rv.setImageViewBitmap(R.id.widget_image,WidgetRenderer.render(c,d,s,w,h).bitmap);
        rv.setContentDescription(R.id.widget_image,WidgetRenderer.description(d));
        Intent i=new Intent(c,RevenueWidget.class).setAction(REFRESH);
        rv.setOnClickPendingIntent(R.id.widget_root,PendingIntent.getBroadcast(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        m.updateAppWidget(id,rv);
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
