package com.aruno.foolquest.widget.next;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.Iterator;

/** Read-only presentation projection. Unknown day values are -1, never fabricated as zero. */
final class CalendarRevenueData {
    static final class Month {
        final long[] totals={-1L,-1L,-1L}; // total, coupon, TikTok
        final long[][] days=new long[32][3];
        boolean tiktokDaily;
        Month(){for(long[] row:days)Arrays.fill(row,-1L);}
    }
    private CalendarRevenueData(){}
    static int daysInMonth(String month){return CalendarDateMath.daysInMonth(month);}
    static int firstWeekday(String month){return CalendarDateMath.firstWeekday(month);}
    static long sum(long a,long b){
        return a<0||b<0||a>RevenueMath.MAX_YEN-b?-1:a+b;
    }
    /** Only fully dated, reconciled CSV snapshots become known daily values. */
    static boolean applyDays(JSONObject map,String month,long expected,long[][] target,int column){
        if(map==null||expected<0)return false;
        long[] candidate=new long[32];Arrays.fill(candidate,-1L);
        long total=0;
        for(Iterator<String> it=map.keys();it.hasNext();){
            String key=it.next();
            if(!key.matches("20[0-9]{2}-(0[1-9]|1[0-2])-([0-2][0-9]|3[01])")||!key.startsWith(month+"-"))return false;
            int day=Integer.parseInt(key.substring(8));
            if(day<1||day>daysInMonth(month))return false;
            Object amount=map.opt(key);
            if(!(amount instanceof Number))return false;
            long value=((Number)amount).longValue();
            if(value<0||value>RevenueMath.MAX_YEN||((Number)amount).doubleValue()!=value)return false;
            total=sum(total,value);
            if(total<0)return false;
            candidate[day]=value;
        }
        if(total!=expected)return false;
        for(int day=1;day<=daysInMonth(month);day++)target[day][column]=candidate[day];
        return true;
    }
    static Month read(Context context,String selected){
        Month result=new Month();
        if(!RevenueMath.validMonth(selected))return result;
        try{
            SharedPreferences p=RevenueStore.prefs(context);
            JSONObject state=new JSONObject(p.getString("nativeRevenueV1","{}"));
            JSONObject months=state.optJSONObject("months");
            JSONObject record=months==null?null:months.optJSONObject(selected);
            long csv=record==null?-1:record.optLong("csv",-1);
            if(csv<0||csv>RevenueMath.MAX_YEN)csv=-1;
            long[] manualByDay=new long[32];long manualTotal=0;
            JSONArray manual=state.optJSONArray("manual");
            if(manual!=null)for(int i=0;i<manual.length();i++){
                JSONObject item=manual.optJSONObject(i);
                if(item==null)continue;
                String date=item.optString("date","");
                if(!date.startsWith(selected+"-")||date.length()!=10)continue;
                int day=Integer.parseInt(date.substring(8));
                if(day<1||day>daysInMonth(selected))continue;
                long amount=item.optLong("amount",-1);
                if(amount<=0)continue;
                manualTotal=sum(manualTotal,amount);
                manualByDay[day]=sum(manualByDay[day],amount);
            }
            // The legacy site snapshot writes a monthly value to the existing widget cache.
            // It is a monthly fallback only: no daily breakdown may be inferred from it.
            long oldTotal=selected.equals(p.getString("importMonth",""))
                ?p.getLong("tiktok",-1):-1;
            long fromHistory=csv>=0?sum(csv,manualTotal):-1;
            // A dated native import owns the calendar for its month. The old site
            // snapshot remains a monthly fallback when no verified daily data exists.
            if(record!=null&&record.optBoolean("csvDailyComplete",false)
                &&applyDays(record.optJSONObject("csvDaily"),selected,csv,result.days,2)){
                long checked=0;
                for(int d=1;d<=daysInMonth(selected);d++){
                    long imported=result.days[d][2];
                    long manualAmount=manualByDay[d];
                    if(imported<0&&manualAmount==0)continue;
                    result.days[d][2]=sum(Math.max(0,imported),manualAmount);
                    checked=sum(checked,result.days[d][2]);
                }
                result.tiktokDaily=checked>=0&&checked==fromHistory;
                if(!result.tiktokDaily)
                    for(int d=1;d<=daysInMonth(selected);d++)result.days[d][2]=-1L;
            }
            long cached=oldTotal>=0&&oldTotal<=RevenueMath.MAX_YEN?oldTotal:-1;
            result.totals[2]=result.tiktokDaily?fromHistory:
                selected.equals(RevenueMath.month(System.currentTimeMillis()))&&cached>=0
                    ?cached:fromHistory>=0?fromHistory:cached;
            if(selected.equals(p.getString("couponMonth","")))result.totals[1]=p.getLong("coupon",-1);
            else{
                JSONObject history=new JSONObject(p.getString("couponRevenueMonthlyV1","{}"));
                if(history.has(selected))result.totals[1]=history.optLong(selected,-1);
                else if(record!=null)result.totals[1]=record.optLong("coupon",-1);
            }
            // Reserved for an approved read-only daily Ledger projection. No consumer
            // is allowed to infer historic Coupon days from the monthly amount.
            JSONObject couponSnapshots=new JSONObject(p.getString("couponRevenueDailyV1","{}"));
            JSONObject couponMonth=couponSnapshots.optJSONObject(selected);
            if(couponMonth!=null&&couponMonth.optBoolean("verified",false)
                &&couponMonth.optLong("total",-1)==result.totals[1]){
                applyDays(couponMonth.optJSONObject("days"),selected,result.totals[1],result.days,1);
            }
            result.totals[0]=sum(result.totals[1],result.totals[2]);
            for(int day=1;day<=daysInMonth(selected);day++)
                result.days[day][0]=sum(result.days[day][1],result.days[day][2]);
        }catch(Exception invalid){
            // A corrupted local snapshot must never turn unknown daily revenue into zero.
            return new Month();
        }
        return result;
    }
}
