package com.aruno.foolquest.widget.next;

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
import java.util.function.BiFunction;
import android.widget.RemoteViews;

public final class RevenueWidget extends AppWidgetProvider {
    static final String REFRESH="com.aruno.foolquest.widget.next.REFRESH";
    static int[] ids(Context c){return AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c,RevenueWidget.class));}
    static void renderAll(Context c){
        AppWidgetManager manager=AppWidgetManager.getInstance(c);
        RevenueStore.Data d=RevenueStore.read(c);DisplaySettings s=DisplaySettings.load(RevenueStore.prefs(c));
        for(int id:ids(c))render(c,manager,id,d,s);
        DailyTargetReceiver.schedule(c);
    }
    static int[] previewSize(Context c,Bundle o){
        return previewSize(c,o,196);
    }
    private static int[] previewSize(Context c,Bundle o,int defaultHeight){
        int screen=Math.round(c.getResources().getDisplayMetrics().widthPixels/c.getResources().getDisplayMetrics().density);
        boolean landscape=c.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        int w=o.getInt(landscape?AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH:AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,340);
        int h=o.getInt(landscape?AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT:AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,defaultHeight);
        if(android.os.Build.VERSION.SDK_INT>=31){
            ArrayList<SizeF> sizes=o.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES);
            if(sizes!=null){
                SizeF best=null;float distance=Float.MAX_VALUE;
                for(SizeF size:sizes)if(size.getWidth()>0&&size.getWidth()<=screen+1&&size.getHeight()>0){
                    float delta=Math.abs(size.getWidth()-w)+Math.abs(size.getHeight()-h);
                    if(delta<distance){best=size;distance=delta;}
                }
                if(best!=null){w=Math.round(best.getWidth());h=Math.round(best.getHeight());}
            }
        }
        return new int[]{Math.max(120,Math.min(w>0?w:340,screen)),Math.max(65,h>0?h:defaultHeight)};
    }
    static RemoteViews sizedViews(Context c,Bundle o,RevenueStore.Data d,DisplaySettings s){
        return sizedViews(c,o,65,196,(w,h)->NativeWidgetViews.create(c,d,s,w,h).views);
    }
    static RemoteViews sizedViews(Context c,Bundle o,int minHeight,int defaultHeight,
                                  BiFunction<Integer,Integer,RemoteViews> render){
        int[] size=previewSize(c,o,defaultHeight);
        if(Build.VERSION.SDK_INT>=31){
            ArrayList<SizeF> sizes=o.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES);
            Map<SizeF,RemoteViews> variants=new LinkedHashMap<>();
            float screen=c.getResources().getDisplayMetrics().widthPixels/c.getResources().getDisplayMetrics().density;
            if(sizes!=null)for(SizeF value:sizes){
                if(variants.size()>=8)break;
                if(value.getWidth()<120||value.getWidth()>screen+1||value.getHeight()<minHeight||value.getHeight()>900)continue;
                variants.put(value,render.apply(Math.round(value.getWidth()),Math.round(value.getHeight())));
            }
            if(!variants.isEmpty())return new RemoteViews(variants);
        }
        return render.apply(size[0],Math.max(minHeight,size[1]));
    }
    private static void render(Context c,AppWidgetManager m,int id,RevenueStore.Data d,DisplaySettings s){
        m.updateAppWidget(id,sizedViews(c,m.getAppWidgetOptions(id),d,s));
    }
    @Override public void onReceive(Context c,Intent i){
        if(REFRESH.equals(i.getAction())){
            RevenueJob.requestRefresh(c);return;
        }
        super.onReceive(c,i);
        if(Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())||Intent.ACTION_MY_PACKAGE_REPLACED.equals(i.getAction())){
            renderAll(c);if(ids(c).length>0)RevenueJob.schedule(c);
        }
    }
    @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){
        renderAll(c);RevenueJob.schedule(c);RevenueJob.requestRefresh(c);
    }
    @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle o){m.updateAppWidget(id,sizedViews(c,o,RevenueStore.read(c),DisplaySettings.load(RevenueStore.prefs(c))));}
    @Override public void onEnabled(Context c){RevenueJob.schedule(c);DailyTargetReceiver.schedule(c);}
    @Override public void onDisabled(Context c){
        if(CalendarWidget.ids(c).length==0){RevenueJob.cancel(c);DailyTargetReceiver.cancel(c);}
    }
}

