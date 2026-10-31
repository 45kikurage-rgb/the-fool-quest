package com.aruno.foolquest.widget.next;

/** Fit the complete vertical stack once; keep requested spacing whenever it fits. */
public final class WidgetSpacing {
    public final float top, bottom, gap, textGap, font;
    public final int gauge;
    public final boolean adjusted;
    public WidgetSpacing(float height,float wantedTop,float wantedBottom,float wantedGap,
                         float wantedTextGap,float wantedFont,float wantedGauge,float footer) {
        // Reserve marker overhang, the minimum one-dp track and rounding per row.
        float fixed=footer+3*6,extraGauge=Math.max(0f,wantedGauge-1f);
        float wanted=wantedTop+wantedBottom+2*wantedGap+3*(wantedTextGap+1.3f*wantedFont+extraGauge);
        float scale=Math.min(1f,Math.max(0f,height-fixed)/Math.max(1f,wanted));
        float readable=Math.min(wantedFont,Math.min(9f,Math.max(1f,(height-fixed)/3f/1.3f)));
        font=Math.max(readable,wantedFont*scale);
        float other=wantedTop+wantedBottom+2*wantedGap+3*(wantedTextGap+extraGauge);
        float spacingScale=Math.min(1f,Math.max(0f,height-fixed-3*1.3f*font)/Math.max(1f,other));
        top=wantedTop*spacingScale; bottom=wantedBottom*spacingScale; gap=wantedGap*spacingScale;
        textGap=wantedTextGap*spacingScale;
        gauge=Math.max(1,Math.min(48,1+Math.round(extraGauge*spacingScale)));
        adjusted=scale<.999f;
    }
}
