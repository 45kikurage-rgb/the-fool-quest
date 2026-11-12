package com.aruno.foolquest.widget.next;

import android.content.Context;
import android.widget.RemoteViews;

/** One launcher widget containing the existing gauge and calendar views. */
public final class CombinedWidget extends CalendarWidget {
    static final int MIN_HEIGHT=350;

    static RemoteViews buildViews(Context c,int id,String month,int filter,int summary,CalendarRevenueData.Month days,
                                  long now,RevenueStore.Data revenue,DisplaySettings settings,int width,int height){
        int gaugeHeight=Math.round(height*0.31f);
        RemoteViews views=new RemoteViews(c.getPackageName(),R.layout.widget_combined_v42);
        views.removeAllViews(R.id.combined_gauge);
        CalendarWidget.addStable(views,R.id.combined_gauge,NativeWidgetViews.create(c,revenue,settings,width,gaugeHeight).views,0);
        views.removeAllViews(R.id.combined_calendar);
        CalendarWidget.addStable(views,R.id.combined_calendar,CalendarWidget.buildViews(c,id,month,filter,summary,days,
            now,width,height-gaugeHeight-1),0);
        return views;
    }
}
