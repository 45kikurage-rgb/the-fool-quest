package com.aruno.foolquest.widget;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

/** Native text takes its width from the actual host, not from a bitmap or orientation map. */
final class NativeWidgetViews {
    static final class Result {RemoteViews views;float font;int gauge;boolean adjusted;}
    static final int[][] TEXT={
        {R.id.total_label,R.id.total_current,R.id.total_slash,R.id.total_goal,R.id.total_percent},
        {R.id.tiktok_label,R.id.tiktok_current,R.id.tiktok_slash,R.id.tiktok_goal,R.id.tiktok_percent},
        {R.id.coupon_label,R.id.coupon_current,R.id.coupon_slash,R.id.coupon_goal,R.id.coupon_percent}};
    static final int[] ROW={R.id.row_total,R.id.row_tiktok,R.id.row_coupon};
    static final int[] HOLDER={R.id.total_gauge_holder,R.id.tiktok_gauge_holder,R.id.coupon_gauge_holder};
    private static int px(Context c,float dp){return Math.round(dp*c.getResources().getDisplayMetrics().density);}
    static Result create(Context c,RevenueStore.Data d,DisplaySettings s,int width,int height){
        width=Math.max(120,width);height=Math.max(65,height);
        // The card fills the real host. Text scales within fixed columns, reserving
        // seven-digit money fields even while the displayed amounts are smaller.
        float innerW=width-16,innerH=height-16;
        float left=Math.min(s.left,innerW*.08f),right=Math.min(s.right,innerW*.08f);
        float top=Math.min(s.top,innerH*.12f),bottom=Math.min(s.bottom,innerH*.12f);
        String status=WidgetRenderer.footer(d);
        boolean showStatus=!status.isEmpty()&&height>=100;
        float rowHeight=(innerH-top-bottom-(showStatus?13:0))/3;
        float gap=Math.min(s.gap,Math.max(0,rowHeight*.15f));
        // Baseline is a 126dp-high card with default 6dp top/bottom padding.
        // Gauge height follows vertical resizing independently of the font width cap.
        float preferredGauge=s.gauge*(innerH-top-bottom)/98f;
        int gauge=Math.max(1,Math.min(48,Math.min(Math.round(preferredGauge),Math.round(rowHeight*.2f))));
        float preferred=s.font*(float)Math.sqrt(Math.max(.25f,innerW/324f*innerH/110f));
        float font=preferred,content=innerW-left-right;
        Paint measure=new Paint();measure.setTypeface(Typeface.create("monospace",Typeface.NORMAL));measure.setTextSize(100);
        float[] available={content*.16f-2,content*.29f-3,content*.03f,content*.29f-3,content*.23f-2};
        String[] reserved={"Coupon","¥9,999,999","/","¥9,999,999","9999.99%"};
        long[] values=d.values(),goals=d.goals();
        for(int j=0;j<5;j++)font=Math.min(font,available[j]*100/measure.measureText(reserved[j]));
        for(int i=0;i<3;i++){
            font=Math.min(font,available[1]*100/measure.measureText(RevenueMath.money(values[i])));
            font=Math.min(font,available[3]*100/measure.measureText(RevenueMath.money(goals[i])));
            font=Math.min(font,available[4]*100/measure.measureText(RevenueMath.percent(values[i],goals[i])));
        }
        font=Math.max(1,Math.min(font,(rowHeight-gap-gauge-2)/1.3f));
        Result result=new Result();result.font=font;result.gauge=gauge;
        result.adjusted=font<preferred-.25f||gauge<Math.round(preferredGauge)||gap<s.gap||left<s.left||right<s.right||top<s.top||bottom<s.bottom;
        RemoteViews rv=new RemoteViews(c.getPackageName(),R.layout.widget_pace);result.views=rv;
        rv.setViewPadding(R.id.widget_content,px(c,left),px(c,top),px(c,right),px(c,bottom));
        rv.setInt(R.id.widget_panel,"setBackgroundColor",(s.background&0xffffff)|(Math.round(s.opacity*2.55f)<<24));
        int[] colors={s.total,s.tiktok,s.coupon};
        long now=System.currentTimeMillis();
        for(int i=0;i<3;i++){
            String[] texts={WidgetRenderer.LABELS[i],RevenueMath.money(values[i]),"/",RevenueMath.money(goals[i]),RevenueMath.percent(values[i],goals[i])};
            for(int j=0;j<5;j++){
                rv.setTextViewText(TEXT[i][j],texts[j]);rv.setTextColor(TEXT[i][j],s.text);
                rv.setTextViewTextSize(TEXT[i][j],TypedValue.COMPLEX_UNIT_DIP,result.font);
            }
            rv.setViewPadding(ROW[i],0,0,0,i<2?px(c,gap):0);
            RemoteViews bar=new RemoteViews(c.getPackageName(),R.layout.gauge_adaptive);
            long lap=RevenueMath.lapCount(values[i],goals[i]);
            float progress=RevenueMath.progress(values[i],goals[i]);
            boolean layered=lap>=2 && s.isSecondLap(values[i],goals[i],d.month,now);
            int current=GaugePalette.currentColor(lap,s.paceAchieved,s.secondLap);
            int previous=GaugePalette.previousColor(lap,s.paceAchieved,s.secondLap);
            int base=layered?previous:s.gaugeColor(values[i],goals[i],d.month,now,colors[i]);
            bar.setImageViewBitmap(R.id.gauge_image,gauge(s.text,
                base,layered?1f:progress,layered?current:0,layered?progress:0f,
                layered&&GaugePalette.hasMarker(lap,progress),gauge,
                Math.max(1,Math.round(content-GaugePalette.LAP_LABEL_WIDTH_DP))));
            String suffix=lap>=2?"×"+lap:"";
            bar.setTextViewText(R.id.lap_label,suffix);
            bar.setTextColor(R.id.lap_label,layered?current:GaugePalette.PACE_UNKNOWN);
            bar.setTextViewTextSize(R.id.lap_label,TypedValue.COMPLEX_UNIT_DIP,
                Math.max(5f,Math.min(GaugePalette.LAP_FONT_DP,30f/Math.max(2,suffix.length()))));
            rv.removeAllViews(HOLDER[i]);rv.addView(HOLDER[i],bar);
        }
        rv.setTextViewText(R.id.widget_footer,status);rv.setTextColor(R.id.widget_footer,s.text);
        rv.setViewVisibility(R.id.widget_footer,showStatus?View.VISIBLE:View.GONE);
        rv.setContentDescription(R.id.widget_root,WidgetRenderer.description(d));
        rv.setOnClickPendingIntent(R.id.widget_root,RefreshActivity.tapIntent(c));
        return result;
    }
    private static Bitmap gauge(int text,int color,float progress,int overlayColor,
                                float overlay,boolean marker,int heightDp,int widthDp){
        int over=GaugePalette.MARKER_OVERHANG_DP;
        Bitmap b=Bitmap.createBitmap(widthDp,heightDp+2*over,Bitmap.Config.ARGB_8888);
        b.setDensity(DisplayMetrics.DENSITY_DEFAULT);
        Canvas c=new Canvas(b);
        Paint p=new Paint();p.setColor(Color.argb(72,Color.red(text),Color.green(text),Color.blue(text)));
        c.drawRect(0,over,widthDp,over+heightDp,p);
        p.setColor(color);c.drawRect(0,over,widthDp*progress,over+heightDp,p);
        if(overlay>0f){
            p.setColor(overlayColor);c.drawRect(0,over,widthDp*overlay,over+heightDp,p);
            if(marker){
                float x=widthDp*overlay;
                p.setColor(GaugePalette.MARKER_WHITE);
                float half=GaugePalette.MARKER_WIDTH_DP/2f;
                c.drawRect(x-half,0,x+half,heightDp+2*over,p);
            }
        }
        return b;
    }
}
