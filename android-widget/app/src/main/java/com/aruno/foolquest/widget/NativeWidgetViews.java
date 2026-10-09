package com.aruno.foolquest.widget;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

/** Native text takes its width from the actual host, not from a bitmap or orientation map. */
final class NativeWidgetViews {
    static final class Result {RemoteViews views;float font;boolean adjusted;}
    static final int[][] TEXT={
        {R.id.total_label,R.id.total_current,R.id.total_slash,R.id.total_goal,R.id.total_percent},
        {R.id.tiktok_label,R.id.tiktok_current,R.id.tiktok_slash,R.id.tiktok_goal,R.id.tiktok_percent},
        {R.id.coupon_label,R.id.coupon_current,R.id.coupon_slash,R.id.coupon_goal,R.id.coupon_percent}};
    static final int[] ROW={R.id.row_total,R.id.row_tiktok,R.id.row_coupon};
    static final int[] HOLDER={R.id.total_gauge_holder,R.id.tiktok_gauge_holder,R.id.coupon_gauge_holder};
    static final int[] GAUGE={R.layout.gauge_1,R.layout.gauge_2,R.layout.gauge_3,R.layout.gauge_4,R.layout.gauge_5,R.layout.gauge_6,R.layout.gauge_7,R.layout.gauge_8,R.layout.gauge_9,R.layout.gauge_10,R.layout.gauge_11,R.layout.gauge_12};
    private static int px(Context c,float dp){return Math.round(dp*c.getResources().getDisplayMetrics().density);}
    static Result create(Context c,RevenueStore.Data d,DisplaySettings s,int width,int height){
        width=Math.max(120,width);height=Math.max(65,height);
        WidgetRenderer.Render layout=WidgetRenderer.measure(c,d,s,width-16,height-16);
        Result result=new Result();result.font=layout.font;result.adjusted=layout.adjusted;
        RemoteViews rv=new RemoteViews(c.getPackageName(),R.layout.widget);result.views=rv;
        WidgetLayout l=layout.layout;
        rv.setViewPadding(R.id.widget_content,px(c,l.left),px(c,l.top),px(c,(width-16)-l.right),px(c,(height-16)-l.bottom));
        rv.setInt(R.id.widget_panel,"setBackgroundColor",(s.background&0xffffff)|(Math.round(s.opacity*2.55f)<<24));
        for(int edge:new int[]{R.id.edge_top,R.id.edge_bottom,R.id.edge_left,R.id.edge_right})rv.setInt(edge,"setBackgroundColor",s.text);
        int[] colors={s.total,s.tiktok,s.coupon};long[] values=d.values(),goals=d.goals();
        int gauge=Math.max(1,Math.min(s.gauge,Math.round(layout.rowHeight*.2f)));
        for(int i=0;i<3;i++){
            String[] texts={WidgetRenderer.LABELS[i],RevenueMath.money(values[i]),"/",RevenueMath.money(goals[i]),RevenueMath.percent(values[i],goals[i])};
            for(int j=0;j<5;j++){
                rv.setTextViewText(TEXT[i][j],texts[j]);rv.setTextColor(TEXT[i][j],s.text);
                rv.setTextViewTextSize(TEXT[i][j],TypedValue.COMPLEX_UNIT_DIP,result.font);
            }
            rv.setViewPadding(ROW[i],0,0,0,i<2?px(c,Math.min(s.gap,height*.1f)):0);
            RemoteViews bar=new RemoteViews(c.getPackageName(),GAUGE[gauge-1]);
            bar.setImageViewBitmap(R.id.gauge_image,gauge(s.text,colors[i],RevenueMath.progress(values[i],goals[i])));
            rv.removeAllViews(HOLDER[i]);rv.addView(HOLDER[i],bar);
        }
        rv.setTextViewText(R.id.widget_footer,WidgetRenderer.footer(d));rv.setTextColor(R.id.widget_footer,s.text);
        rv.setViewVisibility(R.id.widget_footer,height>=100?View.VISIBLE:View.GONE);
        rv.setContentDescription(R.id.widget_root,WidgetRenderer.description(d));
        rv.setOnClickPendingIntent(R.id.widget_root,PendingIntent.getBroadcast(c,0,new Intent(c,RevenueWidget.class).setAction(RevenueWidget.REFRESH),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        return result;
    }
    private static Bitmap gauge(int text,int color,float progress){
        Bitmap b=Bitmap.createBitmap(512,4,Bitmap.Config.ARGB_8888);b.setDensity(Bitmap.DENSITY_NONE);
        Canvas c=new Canvas(b);c.drawColor(Color.argb(72,Color.red(text),Color.green(text),Color.blue(text)));
        Paint p=new Paint();p.setColor(color);c.drawRect(0,0,512*progress,4,p);return b;
    }
}
