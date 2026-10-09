package com.aruno.foolquest.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

final class WidgetRenderer {
    static final String[] LABELS={"Total","TikTok","Coupon"};
    static final class Render { Bitmap bitmap; float font; boolean adjusted; float rowHeight,panelHeight; WidgetLayout layout; }
    static Render render(Context context,RevenueStore.Data data,DisplaySettings s,int w,int h) {return layout(context,data,s,w,h,true);}
    static Render measure(Context context,RevenueStore.Data data,DisplaySettings s,int w,int h) {return layout(context,data,s,w,h,false);}
    private static Render layout(Context context,RevenueStore.Data data,DisplaySettings s,int w,int h,boolean draw) {
        w=Math.max(120,Math.min(900,w)); h=Math.max(65,Math.min(600,h));
        // Render at twice dp resolution for crisp text, with a bounded RemoteViews payload.
        Render result=new Render();
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); p.setTypeface(Typeface.create("monospace",Typeface.NORMAL));
        WidgetLayout l=new WidgetLayout(w,h,s.left,s.right,s.top,s.bottom,s.gap);
        long[] values=data.values(),goals=data.goals(); String[] current=new String[3],targets=new String[3],percents=new String[3];
        float font=s.font;
        for(int i=0;i<3;i++){ current[i]=RevenueMath.money(values[i]); targets[i]=RevenueMath.money(goals[i]); percents[i]=RevenueMath.percent(values[i],goals[i]); }
        // Reserve the same seven-digit money and four-digit percent widths even for small values.
        p.setTextSize(font);
        font=Math.min(font,s.font*l.labelWidth()/p.measureText("Coupon"));
        font=Math.min(font,s.font*l.moneyWidth()/p.measureText("¥9,999,999"));
        font=Math.min(font,s.font*l.percentWidth()/p.measureText("9999.99%"));
        for(int i=0;i<3;i++) {
            font=Math.min(font,s.font*l.moneyWidth()/p.measureText(current[i]));
            font=Math.min(font,s.font*l.moneyWidth()/p.measureText(targets[i]));
            font=Math.min(font,s.font*l.percentWidth()/p.measureText(percents[i]));
        }
        float gauge=Math.min(s.gauge,Math.max(1,l.rowHeight*.2f));
        font=Math.max(1,Math.min(font,(l.rowHeight-gauge-3)/1.3f));
        result.font=font; result.adjusted=font<s.font-.25f||gauge<s.gauge||l.adjustedPadding||s.gap>h*.1f;
        p.setTextSize(font); Paint.FontMetrics fm=p.getFontMetrics();
        int[] colors={s.total,s.tiktok,s.coupon}; float gap=Math.min(s.gap,h*.1f);
        // Height follows content, never the launcher's spare vertical area.
        float rowHeight=Math.min(l.rowHeight,fm.bottom-fm.top+2+gauge);
        float panelHeight=l.top+rowHeight*3+gap*2+l.footerHeight+(h-l.bottom);
        result.rowHeight=rowHeight; result.panelHeight=panelHeight; result.layout=l;
        if(!draw)return result;
        result.bitmap=Bitmap.createBitmap(w*2,(int)Math.ceil(panelHeight*2),Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(result.bitmap); c.scale(2,2);
        p.setColor((s.background&0x00ffffff)|(Math.round(s.opacity*2.55f)<<24));
        c.drawRoundRect(new RectF(.5f,.5f,w-.5f,panelHeight-.5f),5,5,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.7f);p.setColor(s.text);
        c.drawRoundRect(new RectF(.5f,.5f,w-.5f,panelHeight-.5f),5,5,p);p.setStyle(Paint.Style.FILL);
        for(int i=0;i<3;i++) {
            float y=l.top+i*(rowHeight+gap), baseline=y-fm.top;
            p.setColor(s.text);p.setTextAlign(Paint.Align.LEFT);c.drawText(LABELS[i],l.left,baseline,p);
            p.setTextAlign(Paint.Align.RIGHT);c.drawText(current[i],l.currentEnd,baseline,p);c.drawText(targets[i],l.goalEnd,baseline,p);
            c.drawText(percents[i],l.percentEnd,baseline,p);
            p.setTextAlign(Paint.Align.CENTER);c.drawText("/",l.slashCenter,baseline,p);
            float gy=y+rowHeight-gauge;
            p.setColor(Color.argb(72,Color.red(s.text),Color.green(s.text),Color.blue(s.text)));
            c.drawRoundRect(new RectF(l.left,gy,l.right,gy+gauge),gauge/2,gauge/2,p);
            float fill=RevenueMath.progress(values[i],goals[i]);
            if(fill>0){p.setColor(colors[i]);c.drawRoundRect(new RectF(l.left,gy,l.left+(l.right-l.left)*fill,gy+gauge),gauge/2,gauge/2,p);}
        }
        if(l.footerHeight>0){
            p.setTextSize(8);p.setTextAlign(Paint.Align.LEFT);p.setColor(s.text);
            String info=footer(data);
            while(p.measureText(info)>l.right-l.left && p.getTextSize()>5) p.setTextSize(p.getTextSize()-.25f);
            c.drawText(info,l.left,panelHeight-(h-l.bottom)-1,p);
        }
        return result;
    }
    static String footer(RevenueStore.Data d){
        if(d.loading) return "更新中";
        if(!d.error.isEmpty()) return d.month+" 通信失敗・前回値を表示";
        if(d.couponAt==0) return "初回取得待ち · TikTok・目標はサイトから連携";
        String coupon="C "+RevenueMath.date("MM/dd HH:mm",d.couponAt);
        return d.importAt==0 ? coupon+" · TikTok・目標未連携" : coupon+" · T "+RevenueMath.date("MM/dd HH:mm",d.importAt);
    }
    static String description(RevenueStore.Data d){
        StringBuilder b=new StringBuilder(d.month+" ");long[] v=d.values(),g=d.goals();
        for(int i=0;i<3;i++)b.append(LABELS[i]).append(' ').append(RevenueMath.money(v[i])).append('/').append(RevenueMath.money(g[i])).append(' ').append(RevenueMath.percent(v[i],g[i])).append("。 ");
        return b.append(footer(d)).toString();
    }
}
