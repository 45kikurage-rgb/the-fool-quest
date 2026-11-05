package com.aruno.foolquest.widget.next;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import java.util.Locale;

/** Small OS dialog launched from the widget; Android widgets cannot open a popup in place. */
public final class CalendarControlActivity extends Activity {
    static final String EXTRA_MODE="controlMode",EXTRA_DATE="controlDate";
    static final String FILTER="filter",MONTH="month",DAY="day";
    private int widgetId;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        widgetId=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
        if(!CalendarWidgetState.exists(this,widgetId)){finish();return;}
        String mode=getIntent().getStringExtra(EXTRA_MODE);
        if(FILTER.equals(mode))showFilters();
        else if(DAY.equals(mode))showDay(getIntent().getStringExtra(EXTRA_DATE));
        else showMonthPicker();
    }
    private void showDay(String date){
        String normalized=CalendarDateMath.normalizeCsvDate(date);
        String today=RevenueMath.date("yyyy-MM-dd",System.currentTimeMillis());
        if(normalized==null||normalized.compareTo(today)>0){finish();return;}
        String month=normalized.substring(0,7);
        int day=Integer.parseInt(normalized.substring(8));
        CalendarRevenueData.Month data=CalendarRevenueData.read(this,month);
        long[] amounts=data.days[day];
        String coupon=CalendarAmountFormatter.monthNet(amounts[1],data.known[day][1]);
        String detail="合計（損失控除後）  "
            +CalendarAmountFormatter.monthNet(amounts[0],data.known[day][0])
            +"\nTikTok Lite（獲得額）  "
            +CalendarAmountFormatter.monthNet(amounts[2],data.known[day][2])
            +"\nクーポン（獲得額）  "+coupon
            +(!data.known[day][2]?"\n※ TikTok Liteの当日収益は未取得":"");
        detail+="\n損失合計  "+CalendarAmountFormatter.dailyLoss(data.dailyLosses[day])
            +"\n  TikTok Lite  "+CalendarAmountFormatter.dailyLoss(data.losses[day][0])
            +"\n  クーポン  "+CalendarAmountFormatter.dailyLoss(data.losses[day][1]);
        new AlertDialog.Builder(this,android.R.style.Theme_Material_Light_Dialog_Alert)
            .setTitle(normalized).setMessage(detail).setPositiveButton("閉じる",(d,w)->finish())
            .setOnCancelListener(d->finish()).show();
    }
    private void showFilters(){
        final String[] entries={"3種類表示","合計のみ","クーポンのみ","TikTok Liteのみ"};
        new AlertDialog.Builder(this,android.R.style.Theme_Material_Light_Dialog_Alert).setTitle("日別の表示").setSingleChoiceItems(
            entries,CalendarWidgetState.filter(this,widgetId),(dialog,which)->{
                CalendarWidgetState.filter(this,widgetId,which);
                CalendarWidget.renderOne(this,widgetId);
                dialog.dismiss();finish();
            }).setNegativeButton("閉じる",(d,w)->finish()).setOnCancelListener(d->finish()).show();
    }
    private void showMonthPicker(){
        String month=CalendarWidgetState.month(this,widgetId);
        int selectedYear=Integer.parseInt(month.substring(0,4));
        int currentYear=Integer.parseInt(RevenueMath.date("yyyy",System.currentTimeMillis()));
        NumberPicker year=new NumberPicker(this);
        year.setMinValue(2000);
        year.setMaxValue(Math.min(2099,Math.max(selectedYear,currentYear+1)));
        year.setValue(selectedYear);
        NumberPicker monthPicker=new NumberPicker(this);
        monthPicker.setMinValue(1);monthPicker.setMaxValue(12);
        monthPicker.setValue(Integer.parseInt(month.substring(5,7)));
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER);
        row.setPadding(16,8,16,8);row.addView(year,new LinearLayout.LayoutParams(0,-2,1));
        row.addView(monthPicker,new LinearLayout.LayoutParams(0,-2,1));
        new AlertDialog.Builder(this,android.R.style.Theme_Material_Light_Dialog_Alert).setTitle("表示する年月").setView(row)
            .setPositiveButton("選択月を表示",(dialog,which)->{
                String selected=String.format(Locale.US,"%04d-%02d",year.getValue(),monthPicker.getValue());
                if(selected.equals(RevenueMath.month(System.currentTimeMillis())))
                    CalendarWidgetState.followCurrentMonth(this,widgetId);
                else CalendarWidgetState.month(this,widgetId,selected);
                CalendarWidget.renderOne(this,widgetId);
                RevenueJob.requestRefresh(this);
                finish();
            })
            .setNeutralButton("今月に戻す（自動）",(dialog,which)->{
                CalendarWidgetState.followCurrentMonth(this,widgetId);
                CalendarWidget.renderOne(this,widgetId);
                RevenueJob.requestRefresh(this);
                finish();
            })
            .setNegativeButton("閉じる",(d,w)->finish())
            .setOnCancelListener(d->finish()).show();
    }
}
