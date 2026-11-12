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
    private static final String[] SUMMARY_LABELS={"Total","Coupon","TikTok"};
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
    static synchronized void renderOne(Context c,int id){
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
        rememberHeader(c,id,month,data);
        fullRenderCount++;

    }
    static void addStable(RemoteViews parent,int holder,RemoteViews child,int stableId){
        if(android.os.Build.VERSION.SDK_INT>=31)parent.addStableView(holder,child,stableId);
        else parent.addView(holder,child);
    }
    static int fullRenderCount,partialRenderCount;
    private static android.content.SharedPreferences headerCache(Context c){return c.getSharedPreferences("calendar_header_cache_v42",Context.MODE_PRIVATE);}
    private static void rememberHeader(Context c,int id,String month,CalendarRevenueData.Month data){
        try{
            org.json.JSONObject cache=new org.json.JSONObject().put("month",month).put("date",RevenueMath.date("yyyy-MM-dd",System.currentTimeMillis()));
            org.json.JSONArray totals=new org.json.JSONArray(),known=new org.json.JSONArray();
            for(int i=0;i<3;i++){totals.put(data.totals[i]);known.put(data.totalKnown[i]);}
            cache.put("totals",totals).put("known",known);headerCache(c).edit().putString(String.valueOf(id),cache.toString()).apply();
        }catch(org.json.JSONException impossible){throw new IllegalStateException(impossible);}
    }
    static synchronized boolean renderSummaryOnly(Context c,int id){
        try{
            org.json.JSONObject cache=new org.json.JSONObject(headerCache(c).getString(String.valueOf(id),""));
            String month=CalendarWidgetState.month(c,id);
            if(!month.equals(cache.getString("month"))||!RevenueMath.date("yyyy-MM-dd",System.currentTimeMillis()).equals(cache.getString("date")))return false;
            AppWidgetManager manager=AppWidgetManager.getInstance(c);AppWidgetProviderInfo info=manager.getAppWidgetInfo(id);
            if(info==null)return false;
            boolean combined=info.provider.equals(new ComponentName(c,CombinedWidget.class));
            CalendarRevenueData.Month data=new CalendarRevenueData.Month();
            for(int i=0;i<3;i++){data.totals[i]=cache.getJSONArray("totals").getLong(i);data.totalKnown[i]=cache.getJSONArray("known").getBoolean(i);}
            int summary=CalendarWidgetState.summary(c,id);
            RemoteViews patch=RevenueWidget.sizedViews(c,manager.getAppWidgetOptions(id),combined?CombinedWidget.MIN_HEIGHT:236,combined?460:360,(w,h)->{
                RemoteViews views=new RemoteViews(c.getPackageName(),combined?R.layout.widget_combined_v42:R.layout.widget_calendar_v42);
                applyHeader(c,views,id,month,summary,data,w);return views;
            });
            manager.partiallyUpdateAppWidget(id,patch);partialRenderCount++;return true;
        }catch(org.json.JSONException stale){return false;}
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
        RemoteViews rv=new RemoteViews(c.getPackageName(),R.layout.widget_calendar_v42);
        String title=month.substring(0,4)+"年"+Integer.parseInt(month.substring(5))+"月\u3000";
        String[] amounts=new String[SUMMARY_LABELS.length];
        for(int i=0;i<amounts.length;i++){
            amounts[i]=CalendarAmountFormatter.monthNet(data.totals[i],data.totalKnown[i]||data.totals[i]>=0);
        }
        // Keep every header field at the same glyph height. Fit the available
        // width with one shared horizontal scale, including unusually large sums.
        float headerSize=Math.min(26f,32f/lineHeight);
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
        // The three numerical row gaps each donate one physical pixel to the
        // total row. Convert the recovered glyph height to dp, independently
        // of display density and the user's system font scale.
        float dailyDensity=c.getResources().getDisplayMetrics().density;
        float previousDailyTotalSize=Math.min(Math.min(width*34/658f,amountWidth/boldWidth),
            Math.min((rowSpace*1.12f-1.25f)/lineHeight,headerSize/1.20f));
        float dailyTotalSize=Math.min(previousDailyTotalSize+3f/(dailyDensity*lineHeight),
            headerSize/1.20f);
        dailyTotalSize+=6f/(dailyDensity*glyphRatio(totalMeasure));
        // Keep every day's total at one size, including −99.9万. A shared
        // horizontal scale accommodates the taller digits instead of shrinking
        // individual negative days. Reserve one physical pixel for hinting.
        Paint dailyPixelMeasure=new Paint(totalMeasure);
        dailyPixelMeasure.setTextSize(dailyTotalSize*dailyDensity);
        float pixelDigit=0;
        for(char ch='0';ch<='9';ch++)pixelDigit=Math.max(pixelDigit,
            dailyPixelMeasure.measureText(String.valueOf(ch)));
        float pixelWidth=3*pixelDigit+dailyPixelMeasure.measureText("−.")
            +dailyPixelMeasure.measureText("万")*DAILY_TOTAL_UNIT_SCALE;
        float dailyTotalScale=totalScale.getFloat()*Math.min(1f,
            Math.max(1f,amountWidth*dailyDensity-1f)/Math.max(pixelWidth,
                dailyTotalSize*dailyDensity*boldWidth));
        totalMeasure.setTextScaleX(dailyTotalScale);
        detailSize=Math.min(detailSize,totalSize*0.90f);
        lossSize=Math.min(lossSize,detailSize);
        detailSize=Math.max(1f,detailSize-2f/(dailyDensity*glyphRatio(measure)));
        lossSize=Math.max(1f,lossSize-2f/(dailyDensity*glyphRatio(measure)));
        float lossLabelWidth=lossSize*measure.measureText("損失")/100+1f/dailyDensity;
        float lossAmountWidth=Math.max(2f,amountWidth-lossLabelWidth);
        float lossScale=Math.min(1f,lossAmountWidth/(lossSize*signedWidth));
        Paint lossMeasure=new Paint(measure);lossMeasure.setTextScaleX(lossScale);
        applyHeader(c,rv,id,month,summary,data,width);
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
                    forecast.setTextViewTextSize(R.id.calendar_forecast_title,TypedValue.COMPLEX_UNIT_DIP,dateSize);
                    forecast.setFloat(R.id.calendar_forecast_title,"setTextScaleX",Math.min(1f,100*amountWidth/(dateSize*measure.measureText("月末予想"))));
                    forecast.setInt(R.id.calendar_forecast_total,"setPaintFlags",DAILY_TOTAL_FLAGS);
                    forecast.setFloat(R.id.calendar_forecast_total,"setTextScaleX",dailyTotalScale);
                    forecast.setTextColor(R.id.calendar_forecast_total,data.totalKnown[0]&&data.totals[0]<0?0xffff9292:0xffffffff);
                    int[] values={R.id.calendar_forecast_total,R.id.calendar_forecast_coupon,R.id.calendar_forecast_tiktok};
                    String[] projected=new String[3];
                    for(int i=0;i<3;i++){
                        projected[i]=CalendarAmountFormatter.forecast(data.totals[i],data.totalKnown[i]||data.totals[i]>=0,elapsed,days);
                        amount(forecast,values[i],projected[i],i==0?dailyTotalSize:detailSize,i==0?amountWidth:detailWidth,i==0?totalMeasure:measure,i==0?DAILY_TOTAL_UNIT_SCALE:0.55f);
                    }
                    forecast.setTextViewTextSize(R.id.calendar_forecast_c_label,TypedValue.COMPLEX_UNIT_DIP,detailSize);
                    forecast.setTextViewTextSize(R.id.calendar_forecast_t_label,TypedValue.COMPLEX_UNIT_DIP,detailSize);
                    boolean visible=CalendarWidgetState.forecastVisible(c,id);
                    forecast.setViewVisibility(R.id.calendar_forecast_values,visible?View.VISIBLE:View.INVISIBLE);
                    forecast.setContentDescription(R.id.calendar_forecast,visible
                        ?"月末予想 合計 "+projected[0]+" クーポン "+projected[1]+" TikTok "+projected[2]+"。タップで非表示"
                        :"月末予想。タップで表示");
                    forecast.setOnClickPendingIntent(R.id.calendar_forecast,action(c,id,FORECAST));
                    addStable(rv,WEEKS[week],forecast,column);
                    continue;
                }
                if(week==WEEKS.length-1&&column>=5){
                    boolean changeMonth=column==6;
                    RemoteViews button=new RemoteViews(c.getPackageName(),R.layout.calendar_control);
                    button.setTextViewText(R.id.calendar_control,changeMonth?"年月\n表示変更":"カテゴリー\n表示変更");
                    button.setOnClickPendingIntent(R.id.calendar_control,
                        control(c,id,changeMonth?CalendarControlActivity.MONTH:CalendarControlActivity.FILTER));
                    addStable(rv,WEEKS[week],button,column);
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
                cell.setTextViewTextSize(R.id.calendar_loss_label,TypedValue.COMPLEX_UNIT_DIP,lossSize);
                cell.setInt(R.id.calendar_loss_label,"setWidth",(int)Math.ceil(lossLabelWidth*dailyDensity));
                cell.setFloat(R.id.calendar_day_loss,"setTextScaleX",lossScale);
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
                    CalendarAmountFormatter.compactLoss(data.dailyLosses[day]),lossSize,lossAmountWidth,lossMeasure);
                if(showCoupon){
                    // Until today's dated TikTok data is available, retain the
                    // existing coupon-only provisional amount without inventing data.
                    boolean provisional=isToday&&!todayTikTokKnown&&!data.known[day][0];
                    long total=provisional?data.days[day][1]:data.days[day][0];
                    boolean totalKnown=provisional
                        ?(data.known[day][1]||data.days[day][1]>=0)
                        :(data.known[day][0]||data.days[day][0]>=0);
                    cell.setInt(R.id.calendar_day_total,"setPaintFlags",DAILY_TOTAL_FLAGS);
                    cell.setFloat(R.id.calendar_day_total,"setTextScaleX",dailyTotalScale);
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
                addStable(rv,WEEKS[week],cell,column);
            }
        }
        return rv;
    }
    static void applyHeader(Context c,RemoteViews rv,int id,String month,int summary,CalendarRevenueData.Month data,int width){
        Paint measure=new Paint(Paint.ANTI_ALIAS_FLAG);measure.setTypeface(c.getResources().getFont(R.font.aru_widget_rounded));measure.setTextSize(100);
        float lineHeight=(measure.getFontMetrics().descent-measure.getFontMetrics().ascent)/100;
        float headerSize=Math.min(26f,32f/lineHeight);
        String title=month.substring(0,4)+"年"+Integer.parseInt(month.substring(5))+"月\u3000";
        String[] amounts=new String[3];
        for(int i=0;i<3;i++)amounts[i]=CalendarAmountFormatter.monthNet(data.totals[i],data.totalKnown[i]||data.totals[i]>=0);
        DisplaySettings positions=DisplaySettings.load(RevenueStore.prefs(c));
        String summaryLabel=SUMMARY_LABELS[summary];
        String summaryAmount=amounts[summary];
        float density=c.getResources().getDisplayMetrics().density;
        Paint headerMeasure=new Paint(measure);
        headerMeasure.setTextSize(headerSize*density);
        // Match the actual pixel padding in widget_calendar.xml: root 4dp on
        // each side, row 1px, title 3dp, and amount 4dp + 2dp. Reserve 8dp
        // for TextView shaping/hinting and rounding across all three fields;
        // the weighted category must not wrap on narrow high-density hosts.
        float headerWidthPx=Math.round(width*density)-2*Math.round(4*density)-1;
        int titlePaddingPx=Math.round(3*density);
        int amountPaddingPx=Math.round(4*density)+Math.round(2*density);
        float textWidthPx=Math.max(1,headerWidthPx-titlePaddingPx-amountPaddingPx-Math.round(8*density));
        float titleWidth=headerMeasure.measureText(title);
        float labelWidth=0,amountReserve=0,headerDigit=0;
        for(char ch='0';ch<='9';ch++)headerDigit=Math.max(headerDigit,
            headerMeasure.measureText(String.valueOf(ch)));
        // Seven numerical digits, yen sign and thousands separators. Larger or
        // negative actual amounts expand the reservation; digits are never cut.
        amountReserve=7*headerDigit+headerMeasure.measureText("¥,,");
        for(int i=0;i<amounts.length;i++){
            labelWidth=Math.max(labelWidth,headerMeasure.measureText(SUMMARY_LABELS[i]));
            amountReserve=Math.max(amountReserve,headerMeasure.measureText(amounts[i]));
        }
        float headerScale=Math.min(1f,textWidthPx/(titleWidth+labelWidth+amountReserve));
        // Measure again at the applied scale, because font hinting operates at
        // actual pixels rather than the 100px sizing probe used for daily rows.
        headerMeasure.setTextScaleX(headerScale);
        titleWidth=headerMeasure.measureText(title);
        labelWidth=0;
        for(String label:SUMMARY_LABELS)labelWidth=Math.max(labelWidth,headerMeasure.measureText(label));
        headerText(c,rv,R.id.calendar_title,title,headerSize,headerScale,headerMeasure,positions.calendarHeaderMonthAlign);
        headerText(c,rv,R.id.calendar_summary_label,summaryLabel,headerSize,headerScale,headerMeasure,positions.calendarHeaderCategoryAlign);
        headerText(c,rv,R.id.calendar_summary_amount,summaryAmount,headerSize,headerScale,headerMeasure,positions.calendarHeaderAmountAlign);
        rv.setInt(R.id.calendar_title,"setWidth",(int)Math.ceil(titleWidth)+titlePaddingPx+2);
        // Fix the amount column to seven digits plus yen/separators. The label
        // fills the remaining width and ends at the same boundary for every
        // category and every value up to seven digits, including reapply.
        headerDigit=0;
        for(char ch='0';ch<='9';ch++)headerDigit=Math.max(headerDigit,
            headerMeasure.measureText(String.valueOf(ch)));
        amountReserve=7*headerDigit+headerMeasure.measureText("¥,,");
        for(String value:amounts)amountReserve=Math.max(amountReserve,headerMeasure.measureText(value));
        rv.setInt(R.id.calendar_summary_amount,"setWidth",(int)Math.ceil(amountReserve)+amountPaddingPx+2);
        rv.setContentDescription(R.id.calendar_summary,summaryLabel+" "+summaryAmount);
        rv.setOnClickPendingIntent(R.id.calendar_summary,cycle(c,id));
    }
    private static void headerText(Context c,RemoteViews views,int id,String text,float size,float scale,Paint paint,int position){
        views.setTextViewText(id,text);
        views.setTextViewTextSize(id,TypedValue.COMPLEX_UNIT_DIP,size);
        views.setFloat(id,"setTextScaleX",scale);
        int vertical=position==0?android.view.Gravity.TOP:position==1?android.view.Gravity.BOTTOM:android.view.Gravity.CENTER_VERTICAL;
        views.setInt(id,"setGravity",(id==R.id.calendar_title?android.view.Gravity.START:android.view.Gravity.END)|vertical);
        // Native glyph raster bounds, rather than font/line boxes, define the
        // selected edge/center. Cache these tiny measurements; no bitmap is sent
        // in a partial RemoteViews update or retained in the system widget cache.
        String key=position+":"+Float.floatToIntBits(paint.getTextSize())+":"+Float.floatToIntBits(scale)+":"+c.getResources().getDisplayMetrics().densityDpi+":"+text;
        Float cached=headerOffsets.get(key);
        if(cached!=null){views.setFloat(id,"setTranslationY",cached);return;}
        RemoteViews probeViews=new RemoteViews(c.getPackageName(),R.layout.header_alignment_probe42);
        probeViews.setTextViewText(R.id.calendar_alignment_probe,text);
        android.widget.TextView probe=(android.widget.TextView)probeViews.apply(c,null);
        probe.setTypeface(paint.getTypeface());probe.setIncludeFontPadding(false);
        if(android.os.Build.VERSION.SDK_INT>=28)probe.setFallbackLineSpacing(false);
        probe.setElegantTextHeight(false);probe.setMaxLines(1);probe.setPadding(0,0,0,0);
        probe.setTextColor(android.graphics.Color.WHITE);probe.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        probe.setTextSize(TypedValue.COMPLEX_UNIT_PX,paint.getTextSize());probe.setTextScaleX(scale);
        probe.setGravity(android.view.Gravity.START|vertical);probe.setText(text);
        int height=Math.round(38*c.getResources().getDisplayMetrics().density);
        int width=Math.max(1,(int)Math.ceil(paint.measureText(text))+4);
        probe.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));
        probe.layout(0,0,width,height);
        android.graphics.Bitmap raster=android.graphics.Bitmap.createBitmap(width,height,android.graphics.Bitmap.Config.ARGB_8888);
        probe.draw(new android.graphics.Canvas(raster));
        int top=height,bottom=-1;
        for(int y=0;y<height;y++)for(int x=0;x<width;x++)if(android.graphics.Color.alpha(raster.getPixel(x,y))>=160){top=Math.min(top,y);bottom=Math.max(bottom,y);}
        raster.recycle();
        float offset=0;
        if(bottom>=top){float inkHeight=bottom-top+1;offset=(position==0?0:position==1?height-inkHeight:(height-inkHeight)/2f)-top;}
        headerOffsets.put(key,offset);views.setFloat(id,"setTranslationY",offset);
    }
    private static final android.util.LruCache<String,Float> headerOffsets=new android.util.LruCache<>(96);
    private static final float[] digitRatios=new float[2];
    private static float glyphRatio(Paint paint){
        int key=paint.isFakeBoldText()?1:0;if(digitRatios[key]>0)return digitRatios[key];
        Paint.FontMetricsInt metrics=paint.getFontMetricsInt();
        int width=(int)Math.ceil(paint.measureText("0123456789"))+8,height=metrics.bottom-metrics.top+8;
        android.graphics.Bitmap raster=android.graphics.Bitmap.createBitmap(width,height,android.graphics.Bitmap.Config.ARGB_8888);
        Paint visible=new Paint(paint);visible.setColor(android.graphics.Color.WHITE);
        new android.graphics.Canvas(raster).drawText("0123456789",4,4-metrics.top,visible);
        int top=height,bottom=-1;
        for(int y=0;y<height;y++)for(int x=0;x<width;x++)if(android.graphics.Color.alpha(raster.getPixel(x,y))>=160){top=Math.min(top,y);bottom=Math.max(bottom,y);}
        raster.recycle();return digitRatios[key]=Math.max(.1f,(bottom-top+1)/paint.getTextSize());
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
                if(FORECAST.equals(intent.getAction())||!renderSummaryOnly(c,id))renderOne(c,id);
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
        for(int id:ids){CalendarWidgetState.delete(c,id);headerCache(c).edit().remove(String.valueOf(id)).apply();}
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

