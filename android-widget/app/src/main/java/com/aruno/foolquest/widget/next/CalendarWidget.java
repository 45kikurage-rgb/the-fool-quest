package com.aruno.foolquest.widget.next;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.util.TypedValue;
import java.util.Arrays;
import android.widget.RemoteViews;

/** Calendar presentation shared by standalone and combined widgets. */
public class CalendarWidget extends AppWidgetProvider {
    static final String CYCLE="com.aruno.foolquest.widget.next.CALENDAR_CYCLE";
    private static final int[] WEEKS={
        R.id.calendar_week_0,R.id.calendar_week_1,R.id.calendar_week_2,
        R.id.calendar_week_3,R.id.calendar_week_4
    };
    private static final String[] SUMMARY_LABELS={"合計","クーポン","TikTok Lite"};
    static int[] ids(Context c){
        int[] calendar=ids(c,CalendarWidget.class);
        int[] combined=ids(c,CombinedWidget.class);
        int[] all=Arrays.copyOf(calendar,calendar.length+combined.length);
        System.arraycopy(combined,0,all,calendar.length,combined.length);
        return all;
    }
    private static int[] ids(Context c,Class<?> component){
        return AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c,component));
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
        AppWidgetManager manager=AppWidgetManager.getInstance(c);
        AppWidgetProviderInfo info=manager.getAppWidgetInfo(id);
        if(info==null)return;
        boolean combined=info.provider.equals(new ComponentName(c,CombinedWidget.class));
        if(!combined&&!info.provider.equals(new ComponentName(c,CalendarWidget.class)))return;
        String month=CalendarWidgetState.month(c,id);
        int filter=CalendarWidgetState.filter(c,id),summary=CalendarWidgetState.summary(c,id);
        CalendarRevenueData.Month data=CalendarRevenueData.read(c,month);
        long now=System.currentTimeMillis();
        Bundle options=manager.getAppWidgetOptions(id);
        RemoteViews views;
        if(combined){
            RevenueStore.Data revenue=RevenueStore.read(c);
            DisplaySettings settings=DisplaySettings.load(RevenueStore.prefs(c));
            views=RevenueWidget.sizedViews(c,options,CombinedWidget.MIN_HEIGHT,460,
                (w,h)->CombinedWidget.buildViews(c,id,month,filter,summary,data,now,revenue,settings,w,h));
        }else{
            views=RevenueWidget.sizedViews(c,options,236,360,
                (w,h)->buildViews(c,id,month,filter,summary,data,now,w,h));
        }
        manager.updateAppWidget(id,views);
    }
    static RemoteViews buildViews(Context c,int id,String month,int filter,int summary,CalendarRevenueData.Month data,long now){
        return buildViews(c,id,month,filter,summary,data,now,280,236);
    }
    static RemoteViews buildViews(Context c,int id,String month,int filter,int summary,CalendarRevenueData.Month data,
                                 long now,int width,int height){
        Paint measure=new Paint();
        measure.setTypeface(c.getResources().getFont(R.font.aru_widget_rounded));
        measure.setTextSize(100);
        float digit=0;
        for(char ch='0';ch<='9';ch++)digit=Math.max(digit,measure.measureText(String.valueOf(ch))/100);
        // Width includes the cell's 2dp side padding and the one-pixel grid line.
        float amountWidth=Math.max(6,(width-8)/7f-5);
        float detailWidth=Math.max(5,amountWidth-9);
        Paint.FontMetrics fm=measure.getFontMetrics();
        float lineHeight=(fm.descent-fm.ascent)/100;
        // Five rows share the body; four text lines share each cell after its padding and gap.
        float rowSpace=Math.max(4,((height-42)/5f-6)/4f);
        float totalSize=Math.min(Math.min(width*22/658f,amountWidth/(6*digit)),
            (rowSpace*1.12f-1)/lineHeight);
        float detailSize=Math.min(Math.min(width*18/658f,detailWidth/(6*digit)),
            (rowSpace*0.90f-1)/lineHeight);
        float dateSize=Math.min(width*30/658f,(rowSpace*1.08f-1)/lineHeight);
        RemoteViews rv=new RemoteViews(c.getPackageName(),R.layout.widget_calendar);
        String title=month.substring(0,4)+"年"+Integer.parseInt(month.substring(5))+"月";
        String[] amounts=new String[SUMMARY_LABELS.length];
        float widestSummary=0;
        for(int i=0;i<amounts.length;i++){
            amounts[i]=CalendarAmountFormatter.month(data.totals[i]);
            float pairWidth=measure.measureText(SUMMARY_LABELS[i])+measure.measureText(amounts[i]);
            widestSummary=Math.max(widestSummary,pairWidth);
        }
        // Leave room for the layout padding, the label/amount gap, and pixel rounding.
        // Reserve the widest pair so switching categories does not resize the header.
        TypedValue summaryScale=new TypedValue();
        c.getResources().getValue(R.dimen.calendar_summary_width_scale,summaryScale,true);
        float headerWidth=width-8;
        float headerSize=Math.min(20f,Math.min(
            100*(headerWidth*4/7-5)/measure.measureText(title),
            100*(headerWidth*3/7-8)/(widestSummary*summaryScale.getFloat())));
        String summaryLabel=SUMMARY_LABELS[summary];
        String summaryAmount=amounts[summary];
        headerText(rv,R.id.calendar_title,title,headerSize);
        headerText(rv,R.id.calendar_summary_label,summaryLabel,headerSize);
        headerText(rv,R.id.calendar_summary_amount,summaryAmount,headerSize);
        rv.setContentDescription(R.id.calendar_summary,summaryLabel+" "+summaryAmount);
        rv.setOnClickPendingIntent(R.id.calendar_summary,cycle(c,id));
        int days=CalendarRevenueData.daysInMonth(month),offset=CalendarRevenueData.firstWeekday(month);
        String current=RevenueMath.month(now);
        int today=Integer.parseInt(RevenueMath.date("dd",now));
        for(int week=0;week<WEEKS.length;week++){
            rv.removeAllViews(WEEKS[week]);
            for(int column=0;column<7;column++){
                if(week==WEEKS.length-1&&column>=5){
                    boolean changeMonth=column==6;
                    RemoteViews button=new RemoteViews(c.getPackageName(),R.layout.calendar_control);
                    button.setTextViewText(R.id.calendar_control,changeMonth?"年月\n表示変更":"カテゴリー\n表示変更");
                    button.setOnClickPendingIntent(R.id.calendar_control,
                        control(c,id,changeMonth?CalendarControlActivity.MONTH:CalendarControlActivity.FILTER));
                    rv.addView(WEEKS[week],button);
                    continue;
                }
                int day=week*7+column+1;
                RemoteViews cell=new RemoteViews(c.getPackageName(),R.layout.calendar_day);
                boolean valid=day<=days;
                boolean isToday=valid&&current.equals(month)&&day==today;
                boolean isPast=valid&&(month.compareTo(current)<0||(current.equals(month)&&day<today));
                int dayOfWeek=(offset+day-1)%7;
                cell.setTextViewText(R.id.calendar_day_number,valid?String.valueOf(day):"");
                cell.setTextViewTextSize(R.id.calendar_day_number,TypedValue.COMPLEX_UNIT_DIP,dateSize);
                cell.setTextViewTextSize(R.id.calendar_coupon_label,TypedValue.COMPLEX_UNIT_DIP,detailSize);
                cell.setTextViewTextSize(R.id.calendar_tiktok_label,TypedValue.COMPLEX_UNIT_DIP,detailSize);
                cell.setTextColor(R.id.calendar_day_number,
                    dayOfWeek==0?0xffff9292:dayOfWeek==6?0xff91baff:0xffeaeaea);
                cell.setInt(R.id.calendar_day_root,"setBackgroundColor",
                    isToday?0xff304154:valid?0xff202020:0xff121212);
                boolean showCoupon=isPast||isToday;
                // A verified CSV can already contain today's TikTok earnings.
                // Do not hide those confirmed values until tomorrow.
                boolean todayTikTokKnown=isToday&&data.days[day][2]>=0;
                boolean showTikTok=isPast||todayTikTokKnown;
                cell.setViewVisibility(R.id.calendar_day_total,showCoupon&&(filter==0||filter==1)?View.VISIBLE:View.INVISIBLE);
                cell.setViewVisibility(R.id.calendar_coupon_row,showCoupon&&(filter==0||filter==2)?View.VISIBLE:View.INVISIBLE);
                cell.setViewVisibility(R.id.calendar_tiktok_row,showTikTok&&(filter==0||filter==3)?View.VISIBLE:View.INVISIBLE);
                if(showCoupon){
                    // Until today's dated TikTok data is available, retain the
                    // existing coupon-only provisional amount without inventing data.
                    long total=isToday&&!todayTikTokKnown?data.days[day][1]:data.days[day][0];
                    amount(cell,R.id.calendar_day_total,CalendarAmountFormatter.day(total),totalSize,amountWidth,measure);
                    amount(cell,R.id.calendar_day_coupon,CalendarAmountFormatter.day(data.days[day][1]),detailSize,detailWidth,measure);
                    if(showTikTok)amount(cell,R.id.calendar_day_tiktok,
                        data.days[day][2]<0?"未取得":CalendarAmountFormatter.day(data.days[day][2]),detailSize,detailWidth,measure);
                    if(total>999999L||data.days[day][1]>999999L||(showTikTok&&data.days[day][2]>999999L)){
                        String date=month+(day<10?"-0":"-")+day;
                        cell.setOnClickPendingIntent(R.id.calendar_day_root,control(c,id,CalendarControlActivity.DAY,date));
                    }
                }
                rv.addView(WEEKS[week],cell);
            }
        }
        return rv;
    }
    private static void headerText(RemoteViews views,int id,String text,float size){
        views.setTextViewText(id,text);
        views.setTextViewTextSize(id,TypedValue.COMPLEX_UNIT_DIP,size);
    }
    private static void amount(RemoteViews cell,int view,String text,float size,float width,Paint measure){
        cell.setTextViewText(view,text);
        // Only exceptional abbreviations can exceed the reserved six-digit width.
        cell.setTextViewTextSize(view,TypedValue.COMPLEX_UNIT_DIP,
            Math.min(size,100*width/measure.measureText(text)));
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
        int[] own=restart?ids(c,getClass()):new int[0];
        if(own.length>0){
            for(int id:own)renderOne(c,id);
            RevenueJob.schedule(c);
            RevenueJob.requestRefresh(c);
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
        if(RevenueWidget.ids(c).length==0&&ids(c).length==0){
            RevenueJob.cancel(c);DailyTargetReceiver.cancel(c);
        }
    }
}
