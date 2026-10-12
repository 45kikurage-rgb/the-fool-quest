package com.aruno.foolquest.widget;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

/** Shared geometry for the native widget and the settings preview, in dp. */
final class GaugeDrawing {
    private GaugeDrawing() {}

    static Paint textPaint() {
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTypeface(Typeface.create("monospace",Typeface.NORMAL));
        return p;
    }

    static Rect fitTextHeight(Paint p,String text,float height) {
        Rect bounds=new Rect();
        p.setTextSize(GaugePalette.LAP_LABEL_PROBE_SIZE);
        p.getTextBounds(text,0,text.length(),bounds);
        p.setTextSize(GaugePalette.LAP_LABEL_PROBE_SIZE*height/Math.max(1,bounds.height()));
        p.getTextBounds(text,0,text.length(),bounds);
        return bounds;
    }

    static void lapLabel(Canvas c,long lap,int color,float right,float top,float height) {
        if(lap<2)return;
        Paint p=textPaint();String text="×"+lap;
        Rect bounds=fitTextHeight(p,text,height);
        float available=GaugePalette.LAP_LABEL_WIDTH_DP-GaugePalette.LAP_LABEL_GAP_DP;
        // Keep very large lap counts inside the same reserved column.
        if(bounds.width()>available){
            p.setTextScaleX(available/bounds.width());
            p.getTextBounds(text,0,text.length(),bounds);
        }
        p.setColor(color);
        c.drawText(text,right-bounds.right,top+(height-bounds.height())/2f-bounds.top,p);
    }

    static boolean hasDivider(float progress,int percent) {
        return progress<1f && progress>percent/100f;
    }

    static void divisions(Canvas c,float left,float top,float width,float height,
                          float progress,float spToDp) {
        if(progress>=1f)return;
        Paint p=textPaint();Rect bounds=new Rect();
        float outline=GaugePalette.LABEL_OUTLINE_DP;
        p.setTextSize(GaugePalette.DIVIDER_LABEL_SP*spToDp);
        p.getTextBounds("80%",0,3,bounds);
        if(bounds.height()+outline>height)
            fitTextHeight(p,"80%",Math.max(0,height-outline));
        float segment=width*GaugePalette.DIVIDER_STEP_PERCENT/100f;
        int save=c.save();c.clipRect(left,top,left+width*progress,top+height);
        for(int percent=GaugePalette.DIVIDER_STEP_PERCENT;percent<100;percent+=GaugePalette.DIVIDER_STEP_PERCENT){
            if(!hasDivider(progress,percent))continue;
            float x=left+width*percent/100f,half=GaugePalette.DIVIDER_WIDTH_DP/2f;
            p.setStyle(Paint.Style.FILL);p.setColor(GaugePalette.MARKER_WHITE);
            // Rectangles retain an exact 1dp width without antialiasing the divider.
            p.setAntiAlias(false);c.drawRect(x-half,top,x+half,top+height,p);p.setAntiAlias(true);
            String label=percent+"%";p.getTextBounds(label,0,label.length(),bounds);
            float padding=GaugePalette.DIVIDER_LABEL_GAP_DP+half+outline/2f;
            if(bounds.width()+outline+padding>segment || height<=outline)continue;
            float textX=x-padding-bounds.right;
            float baseline=top+height-outline/2f-bounds.bottom;
            p.setStrokeWidth(outline);p.setStyle(Paint.Style.STROKE);p.setColor(GaugePalette.LABEL_OUTLINE_BLACK);
            c.drawText(label,textX,baseline,p);
            p.setStyle(Paint.Style.FILL);p.setColor(GaugePalette.LABEL_WHITE);
            c.drawText(label,textX,baseline,p);
        }
        c.restoreToCount(save);
    }
}
