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
    private static final String REFRESH="com.aruno.foolquest.widget.REFRESH";
    static int[] ids(Context c){return AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c,RevenueWidget.class));}
    static void renderAll(Context c){
        AppWidgetManager manager=AppWidgetManager.getInstance(c);
        RevenueStore.Data d=RevenueStore.read(c);DisplaySettings s=DisplaySettings.load(RevenueStore.prefs(c));
        for(int id:ids(c))render(c,manager,id,d,s);
    }
    private static void render(Context c,AppWidgetManager m,int id,RevenueStore.Data d,DisplaySettings s){
        Bundle o=m.getAppWidgetOptions(id);
        if(Build.VERSION.SDK_INT>=31) {
            ArrayList<SizeF> sizes=o.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES);
            if(sizes!=null&&!sizes.isEmpty()) {
                Map<SizeF,RemoteViews> views=new LinkedHashMap<>();
                for(SizeF size:sizes) {
                    if(size.getWidth()>0&&size.getHeight()>0&&views.size()<4)
                        views.put(size,views(c,d,s,Math.round(size.getWidth()),Math.round(size.getHeight())));
                }
                if(!views.isEmpty()){m.updateAppWidget(id,new RemoteViews(views));return;}
            }
        }
        int pw=o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,340),ph=o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,130);
        int lw=o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,pw),lh=o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,ph);
        RemoteViews portrait=views(c,d,s,pw,ph);
        m.updateAppWidget(id,lw>0&&lh>0&&(lw!=pw||lh!=ph)?new RemoteViews(views(c,d,s,lw,lh),portrait):portrait);
    }
    private static RemoteViews views(Context c,RevenueStore.Data d,DisplaySettings s,int w,int h){
        if(w<=0)w=340;if(h<=0)h=130;
        RemoteViews rv=new RemoteViews(c.getPackageName(),R.layout.widget);
        WidgetRenderer.Render rendered=WidgetRenderer.render(c,d,s,w,h);
        rv.setImageViewBitmap(R.id.widget_image,rendered.bitmap);
        if(Build.VERSION.SDK_INT>=31)rv.setViewLayoutHeight(R.id.widget_image,(float)Math.ceil(rendered.panelHeight),android.util.TypedValue.COMPLEX_UNIT_DIP);
        rv.setContentDescription(R.id.widget_image,WidgetRenderer.description(d));
        Intent i=new Intent(c,RevenueWidget.class).setAction(REFRESH);
        rv.setOnClickPendingIntent(R.id.widget_root,PendingIntent.getBroadcast(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        return rv;
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
