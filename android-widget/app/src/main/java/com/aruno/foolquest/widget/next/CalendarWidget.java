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
    static final String FORECAST="com.aruno.foolquest.widget.next.CALENDAR_FORECAST";
    static final String CYCLE="com.aruno.foolquest.widget.next.CALENDAR_CYCLE";
    private static final int[] WEEKS={
        R.id.calendar_week_0,R.id.calendar_week_1,R.id.calendar_week_2,
        R.id.calendar_week_3,R.id.calendar_week_4
    };
    private static final float DAILY_TOTAL_UNIT_SCALE=0.43f;
    private static final int DAILY_TOTAL_FLAGS=Paint.ANTI_ALIAS_FLAG|Paint.FAKE_BOLD_TEXT_FLAG|Paint.SUBPIXEL_TEXT_FLAG;
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
    private static PendingIntent cycle(Context c,int id){return action(c,id,CYCLE);}
    private static PendingIntent action(Context c,int id,String action){
        Intent intent=new Intent(c,CalendarWidget.class).setAction(action);
        intent.setData(Uri.parse("tfq-calendar://action/"+action+"/"+id));
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
        // Five weeks, with an independent, compact fifth line reserved for daily losses.
        float rowSpace=Math.max(4,((height-42)/5f-6)/4.90f);
        float unitWidth=measure.measureText("万")/100*0.55f;
        float positiveWidth=3*digit+measure.measureText(".")/100+unitWidth;
        float signedWidth=positiveWidth+measure.measureText("−")/100;
        float totalSize=Math.min(Math.min(width*30/658f,amountWidth/signedWidth),
            (rowSpace*1.12f-1)/lineHeight);
        float detailSize=Math.min(Math.min(width*26/658f,detailWidth/positiveWidth),
            (rowSpace*0.90f-1)/lineHeight);
        float dateSize=Math.min(width*30/658f,(rowSpace*1.08f-1)/lineHeight);
        float lossSize=Math.min(detailSize,Math.min(amountWidth/signedWidth,(rowSpace*0.64f-0.35f)/lineHeight));
        RemoteViews rv=new RemoteViews(c.getPackageName(),R.layout.widget_calendar);
        String title=month.substring(0,4)+"年"+Integer.parseInt(month.substring(5))+"月";
        String[] amounts=new String[SUMMARY_LABELS.length];
        float widestSummary=0;
        for(int i=0;i<amounts.length;i++){
            amounts[i]=CalendarAmountFormatter.monthNet(data.totals[i],data.totalKnown[i]||data.totals[i]>=0);
            float pairWidth=measure.measureText(SUMMARY_LABELS[i])+measure.measureText(amounts[i]);
            widestSummary=Math.max(widestSummary,pairWidth);
        }
        // Leave room for the layout padding, the label/amount gap, and pixel rounding.
        // Reserve the widest pair so switching categories does not resize the header.
        float headerWidth=width-8;
        float headerSize=Math.min(26f,Math.min(
            100*(headerWidth*3/7-5)/measure.measureText(title),
            100*(headerWidth*4/7-8)/(widestSummary*0.8f)));
        // Preserve the hierarchy even for exceptional large month totals.
        totalSize=Math.min(totalSize,headerSize/1.25f);
        Paint totalMeasure=new Paint(measure);
        totalMeasure.setFlags(DAILY_TOTAL_FLAGS);
        TypedValue totalScale=new TypedValue();
        c.getResources().getValue(R.dimen.calendar_daily_total_width_scale,totalScale,true);
        totalMeasure.setTextScaleX(totalScale.getFloat());
        float boldDigit=0;
        for(char ch='0';ch<='9';ch++)boldDigit=Math.max(boldDigit,totalMeasure.measureText(String.valueOf(ch))/100);
        float boldWidth=3*boldDigit+totalMeasure.measureText("−.")/100
            +totalMeasure.measureText("万")/100*DAILY_TOTAL_UNIT_SCALE;
        // Use the existing total row's space while leaving every other row in place.
        float dailyTotalSize=Math.min(Math.min(width*34/658f,amountWidth/boldWidth),
            Math.min((rowSpace*1.12f-1.25f)/lineHeight,headerSize/1.20f));
        detailSize=Math.min(detailSize,totalSize*0.90f);
        lossSize=Math.min(lossSize,detailSize);
        String summaryLabel=SUMMARY_LABELS[summary];
        String summaryAmount=amounts[summary];
        // Preserve code38's glyph height. Share one horizontal scale across the
        // entire header instead of condensing only category/amount to 80%.
        // Reserve the widest category pair so tapping never changes typography.
        float titleWidth=measure.measureText(title)*headerSize/100f;
        float headerScale=Math.min(1f,(headerWidth-12)/(titleWidth+widestSummary*headerSize/100f));
        headerText(rv,R.id.calendar_title,title,headerSize,headerScale);
        headerText(rv,R.id.calendar_summary_label,summaryLabel,headerSize,headerScale);
        headerText(rv,R.id.calendar_summary_amount,summaryAmount,headerSize,headerScale);
        float density=c.getResources().getDisplayMetrics().density;
        rv.setInt(R.id.calendar_title,"setWidth",(int)Math.ceil((titleWidth*headerScale+7)*density));
        rv.setContentDescription(R.id.calendar_summary,summaryLabel+" "+summaryAmount);
        rv.setOnClickPendingIntent(R.id.calendar_summary,cycle(c,id));
        int days=CalendarRevenueData.daysInMonth(month),offset=CalendarRevenueData.firstWeekday(month);
        String current=RevenueMath.month(now);
        int today=Integer.parseInt(RevenueMath.date("dd",now));
        for(int week=0;week<WEEKS.length;week++){
            rv.removeAllViews(WEEKS[week]);
            for(int column=0;column<7;column++){
                if(week==WEEKS.length-1&&column==4){
                    RemoteViews forecast=new RemoteViews(c.getPackageName(),R.layout.calendar_forecast);
                    int elapsed=month.compareTo(current)<0?days:month.equals(current)?today:0;
                    forecast.setTextViewText(R.id.calendar_forecast_title,"月末予想");
                    int[] values={R.id.calendar_forecast_total,R.id.calendar_forecast_coupon,R.id.calendar_forecast_tiktok};
                    String[] projected=new String[3];
                    for(int i=0;i<3;i++){
                        projected[i]=CalendarAmountFormatter.forecast(data.totals[i],data.totalKnown[i]||data.totals[i]>=0,elapsed,days);
                        amount(forecast,values[i],projected[i],i==0?totalSize:detailSize,i==0?amountWidth:detailWidth,measure);
                    }
                    forecast.setTextViewTextSize(R.id.calendar_forecast_c_label,TypedValue.COMPLEX_UNIT_DIP,detailSize);
                    forecast.setTextViewTextSize(R.id.calendar_forecast_t_label,TypedValue.COMPLEX_UNIT_DIP,detailSize);
                    boolean visible=CalendarWidgetState.forecastVisible(c,id);
                    forecast.setViewVisibility(R.id.calendar_forecast_values,visible?View.VISIBLE:View.INVISIBLE);
                    forecast.setContentDescription(R.id.calendar_forecast,visible
                        ?"月末予想 合計 "+projected[0]+" クーポン "+projected[1]+" TikTok "+projected[2]+"。タップで非表示"
                        :"月末予想。タップで表示");
                    forecast.setOnClickPendingIntent(R.id.calendar_forecast,action(c,id,FORECAST));
                    rv.addView(WEEKS[week],forecast);
                    continue;
                }
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
                cell.setTextViewTextSize(R.id.calendar_day_loss,TypedValue.COMPLEX_UNIT_DIP,lossSize);
                cell.setTextColor(R.id.calendar_day_number,
                    dayOfWeek==0?0xffff9292:dayOfWeek==6?0xff91baff:0xffeaeaea);
                cell.setInt(R.id.calendar_day_root,"setBackgroundColor",
                    isToday?0xff304154:valid?0xff202020:0xff121212);
                boolean showCoupon=isPast||isToday;
                // A verified CSV can already contain today's TikTok earnings.
                // Do not hide those confirmed values until tomorrow.
                boolean todayTikTokKnown=isToday&&(data.known[day][2]||data.days[day][2]>=0);
                boolean showTikTok=isPast||todayTikTokKnown;
                cell.setViewVisibility(R.id.calendar_day_total,showCoupon&&(filter==0||filter==1)?View.VISIBLE:View.INVISIBLE);
                cell.setViewVisibility(R.id.calendar_coupon_row,showCoupon&&(filter==0||filter==2)?View.VISIBLE:View.INVISIBLE);
                cell.setViewVisibility(R.id.calendar_tiktok_row,showTikTok&&(filter==0||filter==3)?View.VISIBLE:View.INVISIBLE);
                // Reserve the loss line even when zero, so every date keeps the same glyph heights.
                boolean showLoss=showCoupon&&(filter==CalendarWidgetState.ALL||filter==CalendarWidgetState.LOSS)
                    &&data.dailyLosses[day]>0;
                cell.setViewVisibility(R.id.calendar_loss_row,showLoss?View.VISIBLE:View.INVISIBLE);
                if(showLoss)amount(cell,R.id.calendar_day_loss,
                    CalendarAmountFormatter.compactLoss(data.dailyLosses[day]),lossSize,amountWidth,measure);
                if(showCoupon){
                    // Until today's dated TikTok data is available, retain the
                    // existing coupon-only provisional amount without inventing data.
                    boolean provisional=isToday&&!todayTikTokKnown&&!data.known[day][0];
                    long total=provisional?data.days[day][1]:data.days[day][0];
                    boolean totalKnown=provisional
                        ?(data.known[day][1]||data.days[day][1]>=0)
                        :(data.known[day][0]||data.days[day][0]>=0);
                    cell.setInt(R.id.calendar_day_total,"setPaintFlags",DAILY_TOTAL_FLAGS);
                    cell.setTextColor(R.id.calendar_day_total,
                        totalKnown&&total<0&&total!=RevenueMath.UNKNOWN?0xffff9292:0xffffffff);
                    amount(cell,R.id.calendar_day_total,CalendarAmountFormatter.compact(total,totalKnown),
                        dailyTotalSize,amountWidth,totalMeasure,DAILY_TOTAL_UNIT_SCALE);
                    amount(cell,R.id.calendar_day_coupon,CalendarAmountFormatter.compact(data.days[day][1],(data.known[day][1]||data.days[day][1]>=0)),detailSize,detailWidth,measure);
                    if(showTikTok)amount(cell,R.id.calendar_day_tiktok,
                        !(data.known[day][2]||data.days[day][2]>=0)?"未取得":CalendarAmountFormatter.compact(data.days[day][2],true),detailSize,detailWidth,measure);
                    // Rounded amounts always offer the original exact yen values on tap.
                    {
                        String date=month+(day<10?"-0":"-")+day;
                        cell.setOnClickPendingIntent(R.id.calendar_day_root,control(c,id,CalendarControlActivity.DAY,date));
                    }
                }
                rv.addView(WEEKS[week],cell);
            }
        }
        return rv;
    }
    private static void headerText(RemoteViews views,int id,String text,float size,float scale){
        views.setTextViewText(id,text);
        views.setTextViewTextSize(id,TypedValue.COMPLEX_UNIT_DIP,size);
        views.setFloat(id,"setTextScaleX",scale);
    }
    private static void amount(RemoteViews cell,int view,String text,float size,float width,Paint measure){
        amount(cell,view,text,size,width,measure,0.55f);
    }
    private static void amount(RemoteViews cell,int view,String text,float size,float width,Paint measure,float unitScale){
        android.text.SpannableString styled=new android.text.SpannableString(text);
        int unit=text.indexOf('万');
        float measured=measure.measureText(text);
        if(unit>=0){
            styled.setSpan(new android.text.style.RelativeSizeSpan(unitScale),unit,unit+1,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            measured-=measure.measureText("万")*(1-unitScale);
        }
        cell.setTextViewText(view,styled);
        cell.setTextViewTextSize(view,TypedValue.COMPLEX_UNIT_DIP,
            Math.min(size,100*width/Math.max(1,measured)));
    }

    @Override public void onReceive(Context c,Intent intent){
        if(CYCLE.equals(intent.getAction())||FORECAST.equals(intent.getAction())){
            int id=intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
            if(CalendarWidgetState.exists(c,id)){
                if(FORECAST.equals(intent.getAction()))CalendarWidgetState.toggleForecast(c,id);
                else CalendarWidgetState.cycleSummary(c,id);
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

