package com.aruno.foolquest.widget.next;

/** Fit the complete vertical stack once; keep requested spacing whenever it fits. */
public final class WidgetSpacing {
    public final float top, bottom, gap, textGap, font;
    public final int gauge;
    public final boolean adjusted;
    public WidgetSpacing(float height,float wantedTop,float wantedBottom,float wantedGap,
                         float wantedTextGap,float wantedFont,float wantedGauge,float footer) {
        // Marker overhang and rounding are real pixels in each of the three rows.
        float fixed=footer+3*5;
        float wanted=wantedTop+wantedBottom+2*wantedGap+3*(wantedTextGap+1.3f*wantedFont+wantedGauge);
        float scale=Math.min(1f,Math.max(0f,height-fixed)/Math.max(1f,wanted));
        top=wantedTop*scale; bottom=wantedBottom*scale; gap=wantedGap*scale;
        textGap=wantedTextGap*scale; font=Math.max(1f,wantedFont*scale);
        gauge=Math.max(1,Math.min(48,Math.round(wantedGauge*scale)));
        adjusted=scale<.999f;
    }
}
