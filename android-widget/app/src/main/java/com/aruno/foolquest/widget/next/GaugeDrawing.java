package com.aruno.foolquest.widget.next;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

/** Shared geometry for the native widget and the settings preview, in dp. */
final class GaugeDrawing {
    private GaugeDrawing() {}

    /** Half-size target triangle, centered on the track's upper edge. */
    static void targetMarker(Canvas c,float left,float top,float width,float height,
                             float progress,int color) {
        if(progress<0||progress>1||width<=0||height<=0)return;
        float half=Math.min(4,width/2),rise=Math.min(2,height/2);
        float x=Math.max(left+half,Math.min(left+width-half,left+width*progress));
        Path triangle=new Path();triangle.moveTo(x-half,top-rise);
        triangle.lineTo(x+half,top-rise);triangle.lineTo(x,top+rise);triangle.close();
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color|0xff000000);
        c.drawPath(triangle,p);p.setColor(0xff000000);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.5f);
        c.drawPath(triangle,p);
    }

    /** Same two-line label in every metric without increasing the track height. */
    static void targetLabel(Canvas c,float left,float top,float width,float height,long target,Typeface typeface) {
        if(width<=0||height<=0)return;
        String title="現目標値",amount=target<0?"—":java.text.NumberFormat.getIntegerInstance(java.util.Locale.JAPAN).format(target);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setTypeface(typeface);p.setTextSize(100);
        Paint.FontMetrics probe=p.getFontMetrics();
        float size=Math.min(9f,Math.max(.5f,(height-1)/2)*100/(probe.descent-probe.ascent));
        size=Math.min(size,Math.max(1,width-4)*100/Math.max(p.measureText(title),p.measureText(amount)));
        p.setTextSize(Math.max(.5f,size));Paint.FontMetrics fm=p.getFontMetrics();
        float line=fm.descent-fm.ascent,box=Math.min(width,Math.max(p.measureText(title),p.measureText(amount))+4);
        int save=c.save();c.clipRect(left,top,left+width,top+height);
        p.setColor(0xcc000000);c.drawRect(left,top,left+box,top+height,p);
        p.setColor(0xffffffff);float baseline=top+(height-line*2)/2-fm.ascent;
        c.drawText(title,left+2,baseline,p);c.drawText(amount,left+2,baseline+line,p);
        c.restoreToCount(save);
    }

    static Paint textPaint() {
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTypeface(Typeface.create("monospace",Typeface.NORMAL));
        return p;
    }

    static RectF textBounds(Paint p,String text) {
        Path path=new Path();RectF bounds=new RectF();
        p.getTextPath(text,0,text.length(),0,0,path);path.computeBounds(bounds,true);
        return bounds;
    }

    static RectF fitTextHeight(Paint p,String text,float height) {
        p.setTextSize(GaugePalette.LAP_LABEL_PROBE_SIZE);
        RectF bounds=textBounds(p,text);
        p.setTextSize(GaugePalette.LAP_LABEL_PROBE_SIZE*height/Math.max(1,bounds.height()));
        return textBounds(p,text);
    }

    static void lapLabel(Canvas c,long lap,int color,float right,float top,float height) {
        if(lap<2)return;
        Paint p=textPaint();String text="×"+lap;
        RectF bounds=fitTextHeight(p,text,height);
        float available=GaugePalette.LAP_LABEL_WIDTH_DP-GaugePalette.LAP_LABEL_GAP_DP;
        // Preserve gauge-height glyphs while fitting the legacy 34dp column.
        if(bounds.width()>available){
            p.setTextScaleX(available/bounds.width());
            bounds=textBounds(p,text);
        }
        p.setColor(color);
        int save=c.save();c.clipRect(right-GaugePalette.LAP_LABEL_WIDTH_DP,top,right,top+height);
        c.drawText(text,right-bounds.right,top+(height-bounds.height())/2f-bounds.top,p);
        c.restoreToCount(save);
    }

    static void rainbow(Canvas c,Paint p,float left,float top,float width,float height){
        if(width<=0||height<=0)return;
        p.setShader(new android.graphics.LinearGradient(left,top,left+Math.max(1,width),top,
            new int[]{0xffff4545,0xffff9c24,0xffffe24d,0xff33ff66,0xff35ddff,0xff466bff,0xffc15bff},
            null,android.graphics.Shader.TileMode.CLAMP));
        c.drawRect(left,top,left+width,top+height,p);p.setShader(null);
    }
    static boolean rainbowLap(long lap){return lap>=3;}

    static void divisions(Canvas c,float left,float top,float width,float height,
                          float heightFraction,float density) {
        if(width<=0||height<=0)return;
        Paint p=new Paint();p.setAntiAlias(false);p.setColor(GaugePalette.MARKER_WHITE);
        // Canvas coordinates are dp; the requested width is exactly two device pixels.
        float half=1f/Math.max(.1f,density),tickTop=top+height*(1-heightFraction);
        int save=c.save();c.clipRect(left,top,left+width,top+height);
        for(int percent=GaugePalette.DIVIDER_STEP_PERCENT;percent<100;percent+=GaugePalette.DIVIDER_STEP_PERCENT){
            float x=left+width*percent/100f;
            c.drawRect(x-half,tickTop,x+half,top+height,p);
        }
        c.restoreToCount(save);
    }
}

