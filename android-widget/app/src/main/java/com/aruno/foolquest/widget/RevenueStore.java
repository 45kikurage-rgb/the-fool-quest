package com.aruno.foolquest.widget;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import org.json.JSONObject;

/** Last-good display projection only. Never writes to Ledger or the web source. */
final class RevenueStore {
    static final String API = "https://aruno-consolidated-ledger-api.45kikurage.workers.dev/api/v1/revenue/monthly?month=";
    static SharedPreferences prefs(Context c){ return c.getSharedPreferences("widget",Context.MODE_PRIVATE); }
    static final class Data {
        String month, importMonth, couponMonth, error, errorCode;
        long tiktok=-1,coupon=-1,goalTotal=-1,goalTiktok=-1,goalCoupon=-1,couponAt,importAt;
        boolean loading;
        long[] values(){ return new long[]{RevenueMath.total(tiktok,coupon),tiktok,coupon}; }
        long[] goals(){ return new long[]{goalTotal,goalTiktok,goalCoupon}; }
    }
    static Data read(Context c) {
        SharedPreferences p=prefs(c); Data d=new Data(); d.month=RevenueMath.month(System.currentTimeMillis());
        d.importMonth=p.getString("importMonth",""); d.couponMonth=p.getString("couponMonth","");
        d.goalTotal=p.getLong("goalTotal",-1); d.goalTiktok=p.getLong("goalTiktok",-1); d.goalCoupon=p.getLong("goalCoupon",-1);
        d.couponAt=p.getLong("couponAt",0); d.importAt=p.getLong("importAt",0);
        // Do not mix different months or turn missing TikTok into zero.
        if(d.importMonth.equals(d.month)) d.tiktok=p.getLong("tiktok",-1);
        if(d.couponMonth.equals(d.month)) d.coupon=p.getLong("coupon",-1);
        // If the month rolled over and the new fetch fails, keep the previous coherent display.
        if(d.coupon<0 && d.couponAt>0) {
            d.month=d.couponMonth; d.coupon=p.getLong("coupon",-1);
            d.tiktok=d.importMonth.equals(d.month)?p.getLong("tiktok",-1):-1;
        }
        d.loading=RevenueUpdate.busy(); d.error=p.getString("error",""); d.errorCode=p.getString("errorCode",""); return d;
    }
    static long validateCoupon(String raw,String expected) throws Exception {
        JSONObject j=new JSONObject(raw);
        if(!expected.equals(j.optString("month"))) throw new IllegalArgumentException("APIの対象月が一致しません");
        Object value=j.get("coupon_revenue");
        if(!(value instanceof Number)) throw new IllegalArgumentException("APIの収益型が不正です");
        double v=((Number)value).doubleValue();
        if(!Double.isFinite(v)||v<0||v>RevenueMath.MAX_YEN||v!=Math.floor(v)) throw new IllegalArgumentException("APIの収益が不正です");
        return (long)v;
    }
    static void saveCoupon(Context c,String month,long value) {
        prefs(c).edit().putString("couponMonth",month).putLong("coupon",value).putLong("couponAt",System.currentTimeMillis()).remove("error").remove("errorCode").apply();
    }
    static void saveFailure(Context c,String code) {
        long now=System.currentTimeMillis();
        prefs(c).edit().putString("error",RevenueFailure.label(code)).putString("errorCode",code)
            .putString("lastFailureCode",code).putLong("lastFailureAt",now).apply();
    }
    static final class Import {
        String month; long tiktok,goalTotal,goalTiktok,goalCoupon,at;
        static Import parse(Uri uri) {
            if(uri==null||!"tfqwidget".equals(uri.getScheme())||!"snapshot".equals(uri.getHost())||!"1".equals(uri.getQueryParameter("v")))
                throw new IllegalArgumentException("THE FOOL QUESTの連携データを選んでください");
            Import i=new Import(); i.month=uri.getQueryParameter("month");
            if(!RevenueMath.validMonth(i.month)||!i.month.equals(RevenueMath.month(System.currentTimeMillis()))) throw new IllegalArgumentException("当月のデータだけ受け取れます");
            i.tiktok=RevenueMath.yen(uri.getQueryParameter("tiktok"),false);
            i.goalTotal=RevenueMath.yen(uri.getQueryParameter("goalTotal"),true);
            i.goalTiktok=RevenueMath.yen(uri.getQueryParameter("goalTiktok"),true);
            i.goalCoupon=RevenueMath.yen(uri.getQueryParameter("goalCoupon"),true);
            i.at=RevenueMath.yen(uri.getQueryParameter("at"),true);
            if(i.at>System.currentTimeMillis()+300000L || i.at<System.currentTimeMillis()-86400000L) throw new IllegalArgumentException("連携データが古いです。サイトで再作成してください");
            return i;
        }
        void save(Context c) {
            SharedPreferences p=prefs(c);
            if(at<p.getLong("importAt",0)) throw new IllegalArgumentException("新しいデータが既に保存されています");
            p.edit().putString("importMonth",month).putLong("tiktok",tiktok).putLong("goalTotal",goalTotal)
                .putLong("goalTiktok",goalTiktok).putLong("goalCoupon",goalCoupon).putLong("importAt",at).apply();
        }
    }
}
