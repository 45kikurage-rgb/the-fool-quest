package com.aruno.foolquest.widget.next;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/** Small OS dialog launched from the widget; Android widgets cannot open a popup in place. */
public final class CalendarControlActivity extends Activity {
    static final String EXTRA_MODE="controlMode";
    static final String FILTER="filter",MONTH="month";
    private int widgetId;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        widgetId=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
        if(!CalendarWidgetState.exists(this,widgetId)){finish();return;}
        if(FILTER.equals(getIntent().getStringExtra(EXTRA_MODE)))showFilters();
        else showMonthPicker();
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
        Calendar today=Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
        int maxYear=today.get(Calendar.YEAR)+1;
        NumberPicker year=new NumberPicker(this);
        year.setMinValue(2024);year.setMaxValue(Math.max(2024,maxYear));
        year.setValue(Math.max(2024,Math.min(maxYear,Integer.parseInt(month.substring(0,4)))));
        NumberPicker monthPicker=new NumberPicker(this);
        monthPicker.setMinValue(1);monthPicker.setMaxValue(12);
        monthPicker.setValue(Integer.parseInt(month.substring(5,7)));
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER);
        row.setPadding(16,8,16,8);row.addView(year,new LinearLayout.LayoutParams(0,-2,1));
        row.addView(monthPicker,new LinearLayout.LayoutParams(0,-2,1));
        new AlertDialog.Builder(this,android.R.style.Theme_Material_Light_Dialog_Alert).setTitle("表示する年月").setView(row)
            .setPositiveButton("選択月を表示",(dialog,which)->{
                String selected=String.format(Locale.US,"%04d-%02d",year.getValue(),monthPicker.getValue());
                CalendarWidgetState.month(this,widgetId,selected);
                CalendarWidget.renderOne(this,widgetId);
                finish();
            })
            .setNeutralButton("今月に戻す（自動）",(dialog,which)->{
                CalendarWidgetState.followCurrentMonth(this,widgetId);
                CalendarWidget.renderOne(this,widgetId);
                finish();
            })
            .setNegativeButton("閉じる",(d,w)->finish())
            .setOnCancelListener(d->finish()).show();
    }
}
