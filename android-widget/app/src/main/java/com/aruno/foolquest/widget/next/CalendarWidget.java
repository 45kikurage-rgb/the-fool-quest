package com.aruno.foolquest.widget.next;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

/** Independent monthly revenue calendar. Does not modify the existing three-gauge widget. */
public final class CalendarWidget extends AppWidgetProvider {
    static final String CYCLE="com.aruno.foolquest.widget.next.CALENDAR_CYCLE";
    private static final int[] WEEKS={
        R.id.calendar_week_0,R.id.calendar_week_1,R.id.calendar_week_2,
        R.id.calendar_week_3,R.id.calendar_week_4,R.id.calendar_week_5
    };
    static int[] ids(Context c){
        return AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c,CalendarWidget.class));
    }
    static void renderAll(Context c){
        for(int id:ids(c))renderOne(c,id);
    }
    private static PendingIntent control(Context c,int id,String mode){
        return control(c,id,mode,"");
    }
    private static PendingIntent control(Context c,int id,String mode,String date){
        Intent intent=new Intent(c,CalendarControlActivity.class);
        intent.setData(Uri.parse("tfq-calendar://control/"+id+"/"+mode+"/"+date));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);
        intent.putExtra(CalendarControlActivity.EXTRA_MODE,mode);
        intent.putExtra(CalendarControlActivity.EXTRA_DATE,date);
        return PendingIntent.getActivity(c,0,intent,
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private static PendingIntent cycle(Context c,int id){
        Intent intent=new Intent(c,CalendarWidget.class).setAction(CYCLE);
        intent.setData(Uri.parse("tfq-calendar://cycle/"+id));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);
        return PendingIntent.getBroadcast(c,6000+id,intent,
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    static void renderOne(Context c,int id){
        if(!CalendarWidgetState.exists(c,id))return;
        String month=CalendarWidgetState.month(c,id);
        RemoteViews views=buildViews(c,id,month,CalendarWidgetState.filter(c,id),
            CalendarWidgetState.summary(c,id),CalendarRevenueData.read(c,month));
        AppWidgetManager.getInstance(c).updateAppWidget(id,views);
    }
    static RemoteViews buildViews(Context c,int id,String month,int filter,int summary,CalendarRevenueData.Month data){
        RemoteViews rv=new RemoteViews(c.getPackageName(),R.layout.widget_calendar);
        rv.setContentDescription(R.id.calendar_filter,"表示："+(filter==0?"3種類":filter==1?"合計":filter==2?"クーポン":"TikTok Lite"));
        rv.setTextViewText(R.id.calendar_month,month.substring(0,4)+"年"+Integer.parseInt(month.substring(5))+"月 ▾");
        String summaryLabel=(summary==0?"合計":summary==1?"クーポン":"TikTok Lite")+" ↻";
        String summaryAmount=CalendarAmountFormatter.month(data.totals[summary]);
        rv.setTextViewText(R.id.calendar_summary_label,summaryLabel);
        rv.setTextViewText(R.id.calendar_summary_amount,summaryAmount);
        rv.setContentDescription(R.id.calendar_summary,summaryLabel+" "+summaryAmount);
        rv.setOnClickPendingIntent(R.id.calendar_filter,control(c,id,CalendarControlActivity.FILTER));
        rv.setOnClickPendingIntent(R.id.calendar_month,control(c,id,CalendarControlActivity.MONTH));
        rv.setOnClickPendingIntent(R.id.calendar_summary,cycle(c,id));
        int days=CalendarRevenueData.daysInMonth(month),offset=CalendarRevenueData.firstWeekday(month);
        long now=System.currentTimeMillis();
        String current=RevenueMath.month(now);
        int today=Integer.parseInt(RevenueMath.date("dd",now));
        for(int week=0;week<6;week++){
            rv.removeAllViews(WEEKS[week]);
            for(int dayOfWeek=0;dayOfWeek<7;dayOfWeek++){
                int day=week*7+dayOfWeek-offset+1;
                RemoteViews cell=new RemoteViews(c.getPackageName(),R.layout.calendar_day);
                boolean valid=day>=1&&day<=days;
                cell.setTextViewText(R.id.calendar_day_number,valid?String.valueOf(day):"");
                cell.setTextColor(R.id.calendar_day_number,
                    dayOfWeek==0?0xffff9292:dayOfWeek==6?0xff91baff:0xffeaeaea);
                cell.setInt(R.id.calendar_day_root,"setBackgroundColor",
                    valid&&current.equals(month)&&day==today?0xff304154:valid?0xff202020:0xff121212);
                cell.setViewVisibility(R.id.calendar_day_total,valid&&(filter==0||filter==1)?View.VISIBLE:View.GONE);
                cell.setViewVisibility(R.id.calendar_day_coupon,valid&&(filter==0||filter==2)?View.VISIBLE:View.GONE);
                cell.setViewVisibility(R.id.calendar_day_tiktok,valid&&(filter==0||filter==3)?View.VISIBLE:View.GONE);
                if(valid){
                    cell.setTextViewText(R.id.calendar_day_total,CalendarAmountFormatter.day(data.days[day][0]));
                    cell.setTextViewText(R.id.calendar_day_coupon,CalendarAmountFormatter.dayWithPrefix("C",data.days[day][1]));
                    cell.setTextViewText(R.id.calendar_day_tiktok,CalendarAmountFormatter.dayWithPrefix("T",data.days[day][2]));
                    if(data.days[day][0]>999999L||data.days[day][1]>999999L||data.days[day][2]>999999L){
                        String date=month+(day<10?"-0":"-")+day;
                        cell.setOnClickPendingIntent(R.id.calendar_day_root,control(c,id,CalendarControlActivity.DAY,date));
                    }
                }
                rv.addView(WEEKS[week],cell);
            }
        }
        rv.setTextViewText(R.id.calendar_note,"C＝クーポン  T＝TikTok Lite  —＝未取得  ≈＝概算");
        return rv;
    }
    @Override public void onReceive(Context c,Intent intent){
        if(CYCLE.equals(intent.getAction())){
            int id=intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
            if(CalendarWidgetState.exists(c,id)){
                CalendarWidgetState.cycleSummary(c,id);
                renderOne(c,id);
            }
            return;
        }
        super.onReceive(c,intent);
        boolean restart=Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())||
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction());
        if(restart&&ids(c).length>0){
            renderAll(c);
            RevenueJob.schedule(c);
            DailyTargetReceiver.schedule(c);
        }
    }
    @Override public void onUpdate(Context c,AppWidgetManager manager,int[] ids){
        for(int id:ids)renderOne(c,id);
        RevenueJob.schedule(c);
        RevenueJob.requestRefresh(c);
    }
    @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle options){
        renderOne(c,id);
    }
    @Override public void onDeleted(Context c,int[] ids){
        for(int id:ids)CalendarWidgetState.delete(c,id);
    }
    @Override public void onEnabled(Context c){
        RevenueJob.schedule(c);DailyTargetReceiver.schedule(c);
    }
    @Override public void onDisabled(Context c){
        if(RevenueWidget.ids(c).length==0){
            RevenueJob.cancel(c);DailyTargetReceiver.cancel(c);
        }
    }
}
