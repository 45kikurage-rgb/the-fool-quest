package com.aruno.foolquest.widget.next;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

/** Shared geometry for the native widget and the settings preview, in dp. */
final class GaugeDrawing {
    private GaugeDrawing() {}

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

    static boolean hasDivider(float progress,int percent) {
        return progress<1f && progress>percent/100f;
    }

    static void divisions(Canvas c,float left,float top,float width,float height,
                          float progress,float spToDp) {
        if(progress>=1f)return;
        Paint p=new Paint();p.setAntiAlias(false);p.setColor(GaugePalette.MARKER_WHITE);
        int save=c.save();c.clipRect(left,top,left+width*progress,top+height);
        for(int percent=GaugePalette.DIVIDER_STEP_PERCENT;percent<100;percent+=GaugePalette.DIVIDER_STEP_PERCENT){
            if(!hasDivider(progress,percent))continue;
            float x=left+width*percent/100f,half=GaugePalette.DIVIDER_WIDTH_DP/2f;
            c.drawRect(x-half,top,x+half,top+height,p);
        }
        c.restoreToCount(save);
    }

    /** Current-progress boundary: a right bracket, with caps facing into the fill. */
    static void boundary(Canvas c,float left,float top,float width,float height,
                         float progress,float amountFontDp) {
        Paint digit=textPaint();digit.setTextSize(amountFontDp);
        float digitWidth=digit.measureText("0");
        float x=left+width*progress,half=GaugePalette.MARKER_WIDTH_DP/2f;
        float markerTop=top-GaugePalette.MARKER_OVERHANG_DP;
        float markerBottom=top+height+GaugePalette.MARKER_OVERHANG_DP;
        float capLeft=Math.max(left,x-digitWidth);
        Paint p=new Paint();p.setColor(GaugePalette.MARKER_WHITE);p.setAntiAlias(false);
        int save=c.save();c.clipRect(left,markerTop,left+width,markerBottom);
        c.drawRect(x-half,markerTop,x+half,markerBottom,p);
        c.drawRect(capLeft,top,x+half,top+GaugePalette.MARKER_WIDTH_DP,p);
        c.drawRect(capLeft,top+height-GaugePalette.MARKER_WIDTH_DP,x+half,top+height,p);
        c.restoreToCount(save);
    }
}
