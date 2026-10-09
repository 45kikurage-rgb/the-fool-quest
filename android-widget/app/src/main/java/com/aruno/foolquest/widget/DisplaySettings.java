package com.aruno.foolquest.widget;

import android.content.SharedPreferences;
import android.graphics.Color;

public final class DisplaySettings {
    int text = Color.WHITE, background = Color.BLACK;
    int total = 0xffffe24d, tiktok = 0xffff5252, coupon = 0xff4ade80;
    boolean paceColors = true;
    int opacity = 100, font = 13, left = 8, right = 8, top = 6, bottom = 6, gap = 5, gauge = 4;
    static DisplaySettings load(SharedPreferences p) {
        DisplaySettings s = new DisplaySettings();
        s.paceColors=p.getBoolean("paceColors",true);
        s.text=p.getInt("text",s.text); s.background=p.getInt("background",s.background);
        s.total=p.getInt("colorTotal",s.total); s.tiktok=p.getInt("colorTiktok",s.tiktok); s.coupon=p.getInt("colorCoupon",s.coupon);
        s.opacity=p.getInt("opacity",100); s.font=p.getInt("font",13);
        s.left=p.getInt("left",8); s.right=p.getInt("right",8); s.top=p.getInt("top",6); s.bottom=p.getInt("bottom",6);
        s.gap=p.getInt("gap",5); s.gauge=p.getInt("gauge",4); s.clamp(); return s;
    }
    int gaugeColor(long value,long goal,String month,long now,int fixed) {
        if(!paceColors)return fixed;
        switch(RevenueMath.pace(value,goal,month,now)) {
            case RevenueMath.PACE_ON_TRACK:return 0xff31d158;
            case RevenueMath.PACE_BEHIND:return 0xffffd43b;
            case RevenueMath.PACE_LOW:return 0xffff4545;
            default:return 0xff888888;
        }
    }
    void clamp() {
        opacity=bound(opacity,0,100); font=bound(font,9,24);
        left=bound(left,0,32); right=bound(right,0,32); top=bound(top,0,24); bottom=bound(bottom,0,24);
        gap=bound(gap,0,18); gauge=bound(gauge,1,12);
    }
    private static int bound(int v,int a,int b){ return Math.max(a,Math.min(b,v)); }
    void save(SharedPreferences p) {
        clamp(); p.edit().putBoolean("paceColors",paceColors).putInt("text",text).putInt("background",background).putInt("colorTotal",total)
            .putInt("colorTiktok",tiktok).putInt("colorCoupon",coupon).putInt("opacity",opacity).putInt("font",font)
            .putInt("left",left).putInt("right",right).putInt("top",top).putInt("bottom",bottom).putInt("gap",gap).putInt("gauge",gauge).apply();
    }
}
