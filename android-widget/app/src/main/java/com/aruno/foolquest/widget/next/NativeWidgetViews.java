package com.aruno.foolquest.widget.next;

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
    static final class Result {RemoteViews views;float font;int gauge;float gap,textGap;boolean adjusted;}
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
        float paddingScale=Math.min(1f,Math.max(0f,innerW-Math.min(innerW,160f))/Math.max(1f,s.left+s.right));
        float left=s.left*paddingScale,right=s.right*paddingScale;
        String status=WidgetRenderer.footer(d);
        boolean showStatus=!status.isEmpty()&&height>=100;
        float preferredGauge=Math.min(48,s.gauge*Math.max(.25f,(innerH-s.top-s.bottom)/98f));
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
        WidgetSpacing spacing=new WidgetSpacing(innerH,s.top,s.bottom,s.gap,s.textGap,font,preferredGauge,showStatus?13:0);
        font=spacing.font;
        float top=spacing.top,bottom=spacing.bottom,gap=spacing.gap;
        int gauge=spacing.gauge;
        Result result=new Result();result.font=font;result.gauge=gauge;result.gap=gap;result.textGap=spacing.textGap;
        result.adjusted=font<preferred-.25f||spacing.adjusted||paddingScale<1f;
        RemoteViews rv=new RemoteViews(c.getPackageName(),R.layout.widget_pace);result.views=rv;
        rv.setViewPadding(R.id.widget_content,px(c,left),px(c,top),px(c,right),px(c,bottom));
        rv.setInt(R.id.widget_panel,"setBackgroundColor",(s.background&0xffffff)|(Math.round(s.opacity*2.55f)<<24));
        rv.setInt(R.id.row_gap_one,"setMinimumHeight",px(c,gap));
        rv.setInt(R.id.row_gap_two,"setMinimumHeight",px(c,gap));
        int[] colors={s.total,s.tiktok,s.coupon};
        long now=System.currentTimeMillis();
        for(int i=0;i<3;i++){
            String[] texts={WidgetRenderer.LABELS[i],RevenueMath.money(values[i]),"/",RevenueMath.money(goals[i]),RevenueMath.percent(values[i],goals[i])};
            for(int j=0;j<5;j++){
                rv.setTextViewText(TEXT[i][j],texts[j]);rv.setTextColor(TEXT[i][j],s.text);
                rv.setTextViewTextSize(TEXT[i][j],TypedValue.COMPLEX_UNIT_DIP,result.font);
            }
            rv.setViewPadding(ROW[i],0,0,0,0);
            rv.setViewPadding(HOLDER[i],0,px(c,spacing.textGap),0,0);
            RemoteViews bar=new RemoteViews(c.getPackageName(),R.layout.gauge_adaptive);
            long lap=RevenueMath.lapCount(values[i],goals[i]);
            float progress=RevenueMath.progress(values[i],goals[i]);
            boolean layered=lap>=2 && s.isSecondLap(values[i],goals[i],d.month,now);
            int current=GaugePalette.currentColor(lap,s.paceAchieved,s.secondLap);
            int previous=GaugePalette.previousColor(lap,s.paceAchieved,s.secondLap);
            int base=layered?previous:s.gaugeColor(values[i],goals[i],d.month,now,colors[i]);
            bar.setImageViewBitmap(R.id.gauge_image,gauge(c,s.text,
                base,layered?1f:progress,layered?current:0,layered?progress:0f,
                layered&&GaugePalette.hasMarker(lap,progress),gauge,
                Math.max(1,Math.round(content-GaugePalette.LAP_LABEL_WIDTH_DP)),result.font,
                layered&&GaugeDrawing.rainbowLap(lap-1),layered&&GaugeDrawing.rainbowLap(lap)));
            String suffix=lap>=2?"×"+lap:"";
            Bitmap label=bitmap(c,GaugePalette.LAP_LABEL_WIDTH_DP,gauge+2*GaugePalette.MARKER_OVERHANG_DP);
            Canvas labelCanvas=dpCanvas(c,label);
            GaugeDrawing.lapLabel(labelCanvas,lap,layered?current:GaugePalette.PACE_UNKNOWN,
                GaugePalette.LAP_LABEL_WIDTH_DP,GaugePalette.MARKER_OVERHANG_DP,gauge);
            bar.setImageViewBitmap(R.id.lap_label,label);
            bar.setContentDescription(R.id.lap_label,suffix);
            rv.removeAllViews(HOLDER[i]);rv.addView(HOLDER[i],bar);
        }
        rv.setTextViewText(R.id.widget_footer,status);rv.setTextColor(R.id.widget_footer,s.text);
        rv.setViewVisibility(R.id.widget_footer,showStatus?View.VISIBLE:View.GONE);
        rv.setContentDescription(R.id.widget_root,WidgetRenderer.description(d));
        rv.setOnClickPendingIntent(R.id.widget_root,ManagementActivity.tapIntent(c));
        return result;
    }
    private static Bitmap bitmap(Context context,int widthDp,int heightDp){
        Bitmap b=Bitmap.createBitmap(Math.max(1,px(context,widthDp)),Math.max(1,px(context,heightDp)),Bitmap.Config.ARGB_8888);
        b.setDensity(context.getResources().getDisplayMetrics().densityDpi);
        return b;
    }
    private static Canvas dpCanvas(Context context,Bitmap bitmap){
        Canvas c=new Canvas(bitmap);float density=context.getResources().getDisplayMetrics().density;
        c.scale(density,density);return c;
    }
    static Bitmap gauge(Context context,int text,int color,float progress,int overlayColor,
                                float overlay,boolean marker,int heightDp,int widthDp,float amountFontDp){
        return gauge(context,text,color,progress,overlayColor,overlay,marker,heightDp,widthDp,amountFontDp,false,false);
    }
    static Bitmap gauge(Context context,int text,int color,float progress,int overlayColor,
                        float overlay,boolean marker,int heightDp,int widthDp,float amountFontDp,
                        boolean baseRainbow,boolean overlayRainbow){
        int over=GaugePalette.MARKER_OVERHANG_DP;
        Bitmap b=bitmap(context,widthDp,heightDp+2*over);
        Canvas c=dpCanvas(context,b);
        Paint p=new Paint();p.setColor(Color.argb(72,Color.red(text),Color.green(text),Color.blue(text)));
        c.drawRect(0,over,widthDp,over+heightDp,p);
        p.setColor(color);if(baseRainbow)GaugeDrawing.rainbow(c,p,0,over,widthDp*progress,heightDp);else c.drawRect(0,over,widthDp*progress,over+heightDp,p);
        if(overlay>0f){
            p.setColor(overlayColor);if(overlayRainbow)GaugeDrawing.rainbow(c,p,0,over,widthDp*overlay,heightDp);else c.drawRect(0,over,widthDp*overlay,over+heightDp,p);
            DisplayMetrics metrics=context.getResources().getDisplayMetrics();
            GaugeDrawing.divisions(c,0,over,widthDp,heightDp,overlay,metrics.scaledDensity/metrics.density);
            if(marker)GaugeDrawing.boundary(c,0,over,widthDp,heightDp,overlay,amountFontDp);
        }
        return b;
    }
}

