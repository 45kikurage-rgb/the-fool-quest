package com.aruno.foolquest.widget.next;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Locale;
import java.text.SimpleDateFormat;

/**
 * Local TikTok/goal/history owner. Coupon remains read-only from Ledger.
 * Uses a separate preference key; the existing widget keys and renderer are unchanged.
 */
final class RevenueManager {
    static final String FORMAT = "the-fool-quest-migration";
    private static final String KEY = "nativeRevenueV1";
    private static final String COUPON_HISTORY = "couponRevenueMonthlyV1";
    private static final long MAX = 9007199254740991L;
    private final Context context;
    private final SharedPreferences preferences;
    private JSONObject state;

    RevenueManager(Context context) throws Exception {
        this.context = context.getApplicationContext();
        preferences = RevenueStore.prefs(this.context);
        String raw = preferences.getString(KEY, "");
        if (raw.isEmpty()) {
            state = new JSONObject();
            state.put("format", FORMAT);
            state.put("version", 1);
            state.put("months", new JSONObject());
            state.put("manual", new JSONArray());
            JSONObject goals = new JSONObject();
            long t = preferences.getLong("goalTiktok", -1);
            long c = preferences.getLong("goalCoupon", -1);
            goals.put("tiktok", t > 0 ? t : 500000L);
            goals.put("coupon", c > 0 ? c : 500000L);
            state.put("goals", goals);
            String month=RevenueMath.month(System.currentTimeMillis());
            if(month.equals(preferences.getString("importMonth",""))) {
                long old=preferences.getLong("tiktok",-1);
                if(old>=0) {
                    JSONObject record=new JSONObject();
                    record.put("csv",old);
                    record.put("source","widget-original");
                    state.getJSONObject("months").put(month,record);
                }
            }
            persist();
        } else {
            state = new JSONObject(raw);
            validate(state);
        }
    }

    static synchronized void archiveCoupon(Context c,String month,long amount) {
        if(!RevenueMath.validMonth(month)||amount<0||amount>MAX)throw new IllegalArgumentException("Coupon月次履歴が不正です");
        SharedPreferences p=RevenueStore.prefs(c);
        try {
            JSONObject months=new JSONObject(p.getString(COUPON_HISTORY,"{}"));
            if(months.has(month)&&months.getLong(month)==amount)return;
            if(months.length()>=240&&!months.has(month))
                throw new IllegalStateException("Coupon月次履歴の保存上限です");
            months.put(month,amount);
            if(!p.edit().putString(COUPON_HISTORY,months.toString()).commit())
                throw new IllegalStateException("Coupon月次履歴を保存できませんでした");
        }catch(org.json.JSONException e){throw new IllegalStateException("Coupon月次履歴が不正です",e);}
    }
    JSONObject couponHistory() throws Exception {
        return new JSONObject(preferences.getString(COUPON_HISTORY,"{}"));
    }
    List<String> archivedCouponMonths() throws Exception {
        List<String> result=new ArrayList<>();
        Iterator<String> keys=couponHistory().keys();
        while(keys.hasNext())result.add(keys.next());
        return result;
    }
    JSONObject snapshot() throws Exception {
        JSONObject copy=new JSONObject(state.toString());
        JSONObject records=copy.getJSONObject("months"), archived=couponHistory();
        Iterator<String> months=archived.keys();
        while(months.hasNext()) {
            String m=months.next();
            if(!RevenueMath.validMonth(m))continue;
            JSONObject record=records.optJSONObject(m);
            if(record==null){record=new JSONObject();records.put(m,record);}
            record.put("coupon",archived.getLong(m));
        }
        return copy;
    }
    JSONObject months() throws Exception { return state.getJSONObject("months"); }
    JSONArray manual() throws Exception { return state.getJSONArray("manual"); }
    JSONObject goals() throws Exception { return state.getJSONObject("goals"); }
    String month() { return RevenueMath.month(System.currentTimeMillis()); }
    static long safe(long n, boolean positive) {
        if(n<0 || n>MAX || (positive && n==0)) throw new IllegalArgumentException("金額の範囲を確認してください");
        return n;
    }
    static long parseAmount(String text,boolean positive) {
        String v=text.replace(",","").replace("¥","").trim();
        if(!v.matches("[0-9]{1,16}"))throw new IllegalArgumentException("金額は0以上の整数で入力してください");
        return safe(Long.parseLong(v), positive);
    }
    static String requireMonth(String s) {
        if(!RevenueMath.validMonth(s)) throw new IllegalArgumentException("対象月が不正です");
        return s;
    }
    static long plus(long a,long b) {
        if(a<0 || b<0 || a>MAX-b)throw new IllegalArgumentException("金額が上限を超えています");
        return a+b;
    }
    long goal(String key) throws Exception {return goals().getLong(key);}
    void setGoals(long t,long c) throws Exception {
        safe(t,true);safe(c,true);plus(t,c);
        goals().put("tiktok",t);goals().put("coupon",c);
        persistAndRender();
    }
    long csvBase(String key) throws Exception {
        JSONObject v=months().optJSONObject(key);
        return v==null?-1:v.optLong("csv",-1);
    }
    long manualTotal(String key) throws Exception {
        long result=0;
        JSONArray rows=manual();
        for(int i=0;i<rows.length();i++) {
            JSONObject row=rows.getJSONObject(i);
            if(key.equals(row.getString("date").substring(0,7)))result=plus(result,row.getLong("amount"));
        }
        return result;
    }
    long tiktok(String key) throws Exception {
        long base=csvBase(key);
        if(base<0) return -1;
        return plus(base,manualTotal(key));
    }
    long coupon(String key) throws Exception {
        if(key.equals(month())) {
            RevenueStore.Data d=RevenueStore.read(context);
            if(key.equals(d.couponMonth))return d.coupon;
        }
        JSONObject archived=couponHistory();
        if(archived.has(key))return archived.getLong(key);
        JSONObject old=months().optJSONObject(key);
        return old==null?-1:old.optLong("coupon",-1);
    }
    void setCsv(String key,long amount,String filename,int count) throws Exception {
        requireMonth(key);safe(amount,false);
        JSONObject record=months().optJSONObject(key);
        if(record==null){record=new JSONObject();months().put(key,record);}
        record.put("csv",amount);
        record.put("file",filename);
        record.put("count",count);
        record.put("source","csv");
        persistAndRender();
    }
    static void checkDate(String date) throws Exception {
        if(!date.matches("20[0-9]{2}-(0[1-9]|1[0-2])-([0-2][0-9]|3[01])"))
            throw new IllegalArgumentException("日付はyyyy-MM-dd形式で入力してください");
        SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd",Locale.US);
        format.setLenient(false);
        if(!date.equals(format.format(format.parse(date))))
            throw new IllegalArgumentException("存在しない日付です");
    }
    void addManual(String date,long amount) throws Exception {
        checkDate(date);
        safe(amount,true);
        if(manual().length()>=10000)throw new IllegalArgumentException("履歴件数の上限です");
        // A manual-only month starts with a zero CSV baseline, without inventing CSV data.
        String key=date.substring(0,7);
        if(csvBase(key)<0) { JSONObject record=new JSONObject();record.put("csv",0);record.put("source","manual-only");months().put(key,record); }
        plus(plus(csvBase(key),manualTotal(key)),amount);
        JSONObject row=new JSONObject();
        row.put("id",java.util.UUID.randomUUID().toString());
        row.put("date",date);
        row.put("amount",amount);
        manual().put(row);
        persistAndRender();
    }
    void deleteManual(String id) throws Exception {
        JSONArray fresh=new JSONArray();boolean found=false;
        for(int i=0;i<manual().length();i++) {
            JSONObject v=manual().getJSONObject(i);
            if(id.equals(v.optString("id")))found=true;
            else fresh.put(v);
        }
        if(!found)throw new IllegalArgumentException("履歴が見つかりません");
        state.put("manual",fresh);persistAndRender();
    }
    void updateManual(String id,String date,long amount) throws Exception {
        checkDate(date);safe(amount,true);
        if(csvBase(date.substring(0,7))<0) { JSONObject record=new JSONObject();record.put("csv",0);record.put("source","manual-only");months().put(date.substring(0,7),record); }
        JSONArray changed=new JSONArray();boolean found=false;
        for(int i=0;i<manual().length();i++) {
            JSONObject entry=manual().getJSONObject(i);
            if(id.equals(entry.optString("id"))) {
                found=true;
                JSONObject edited=new JSONObject(entry.toString());
                edited.put("date",date);edited.put("amount",amount);
                changed.put(edited);
            }else changed.put(entry);
        }
        if(!found)throw new IllegalArgumentException("履歴が見つかりません");
        JSONObject candidate=new JSONObject(state.toString());
        candidate.put("manual",changed);
        validate(candidate);
        JSONObject old=state;state=candidate;
        try{persistAndRender();}catch(Exception e){state=old;throw e;}
    }
    void persist() throws Exception {
        if(!preferences.edit().putString(KEY,state.toString()).commit())
            throw new IllegalStateException("端末に保存できませんでした");
    }
    void persistAndRender() throws Exception {
        persist();
        projectWidget();
    }
    void projectWidget() throws Exception {
        SharedPreferences.Editor ed=preferences.edit();
        JSONObject g=goals();long t=g.getLong("tiktok"),c=g.getLong("coupon");
        ed.putLong("goalTiktok",t).putLong("goalCoupon",c).putLong("goalTotal",plus(t,c));
        long current=tiktok(month());
        if(current>=0) {
            ed.putString("importMonth",month()).putLong("tiktok",current)
                .putLong("importAt",System.currentTimeMillis());
        } else {
            // Do not silently represent unimported TikTok as zero.
            ed.remove("importMonth").remove("tiktok");
        }
        if(!ed.commit())throw new IllegalStateException("ウィジェット保存に失敗しました");
        RevenueWidget.renderAll(context);
    }
    String backup() throws Exception { return snapshot().toString(); }

    void replaceFromJson(String json) throws Exception {
        if(json.length()>2_000_000)throw new IllegalArgumentException("ファイルが大きすぎます");
        JSONObject incoming=new JSONObject(json);validate(incoming);
        // Stage validated data first; never erase the previous state for a malformed import.
        JSONObject old=state;state=incoming;
        try{persistAndRender();}catch(Exception e){state=old;throw e;}
    }
    static long strictNumber(Object raw,boolean positive) {
        if(!(raw instanceof Number))throw new IllegalArgumentException("金額は数値で指定してください");
        double n=((Number)raw).doubleValue();
        if(!Double.isFinite(n)||n!=Math.floor(n)||n<0||n>MAX||(positive&&n==0))
            throw new IllegalArgumentException("金額に不正な値が含まれています");
        return (long)n;
    }
    static void validate(JSONObject root) throws Exception {
        if(!FORMAT.equals(root.optString("format"))||root.optInt("version")!=1)
            throw new IllegalArgumentException("THE FOOL QUESTの移行ファイルではありません");
        JSONObject months=root.getJSONObject("months");
        if(months.length()>240)throw new IllegalArgumentException("月別履歴が多すぎます");
        Iterator<String> keys=months.keys();
        while(keys.hasNext()) {
            String k=requireMonth(keys.next());JSONObject v=months.getJSONObject(k);
            if(v.has("csv"))strictNumber(v.get("csv"),false);
            if(v.has("coupon"))strictNumber(v.get("coupon"),false);
        }
        JSONArray manual=root.getJSONArray("manual");
        if(manual.length()>10000)throw new IllegalArgumentException("手動追加履歴が多すぎます");
        java.util.HashSet<String> ids=new java.util.HashSet<>();
        for(int i=0;i<manual.length();i++) {
            JSONObject row=manual.getJSONObject(i);
            String date=row.getString("date");
            checkDate(date);
            String id=row.getString("id");
            if(!ids.add(id)||id.length()>100||id.isEmpty())throw new IllegalArgumentException("履歴IDが重複しています");
            strictNumber(row.get("amount"),true);
            JSONObject base=months.optJSONObject(date.substring(0,7));
            if(base==null||!base.has("csv"))throw new IllegalArgumentException("手動追加の月にCSV元データがありません");
        }
        JSONObject goals=root.getJSONObject("goals");
        plus(strictNumber(goals.get("tiktok"),true),strictNumber(goals.get("coupon"),true));
        // Ensure totals can be projected without overflow.
        for(Iterator<String> it=months.keys();it.hasNext();) {
            String m=it.next();long base=months.getJSONObject(m).optLong("csv",-1);
            if(base>=0) {
                long total=0;
                for(int i=0;i<manual.length();i++) {
                    JSONObject row=manual.getJSONObject(i);
                    if(row.getString("date").startsWith(m))total=plus(total,row.getLong("amount"));
                }
                plus(base,total);
            }
        }
    }
    static final class CsvResult {
        final long total;final int count;
        CsvResult(long total,int count){this.total=total;this.count=count;}
    }
    static CsvResult parseCsv(String text) {
        if(text.length()>4_000_000)throw new IllegalArgumentException("CSVが大きすぎます");
        List<List<String>> rows=new ArrayList<>();
        List<String> row=new ArrayList<>();StringBuilder cell=new StringBuilder();
        boolean quoted=false;
        for(int i=0;i<text.length();i++) {
            char ch=text.charAt(i);
            if(i==0&&ch=='\uFEFF')continue;
            if(ch=='"') {
                if(quoted && i+1<text.length()&&text.charAt(i+1)=='"'){cell.append('"');i++;}
                else quoted=!quoted;
            }else if(!quoted&&(ch==','||ch=='\n')){
                row.add(cell.toString().replace("\r",""));cell.setLength(0);
                if(ch=='\n'){rows.add(row);row=new ArrayList<>();}
            }else cell.append(ch);
        }
        if(quoted)throw new IllegalArgumentException("CSVの引用符が閉じていません");
        if(cell.length()>0||!row.isEmpty()){row.add(cell.toString().replace("\r",""));rows.add(row);}
        int head=-1,index=-1;
        for(int i=0;i<rows.size();i++) {
            int c=rows.get(i).indexOf("チャージポイント");
            if(c>=0){head=i;index=c;break;}
        }
        if(head<0)throw new IllegalArgumentException("「チャージポイント」列が見つかりません");
        long total=0;int count=0;
        for(int i=head+1;i<rows.size();i++) {
            List<String> record=rows.get(i);
            if(record.size()<=index)continue;
            String raw=record.get(index).trim().replace(",","");
            if(raw.isEmpty() || !raw.matches("-?[0-9]+"))continue;
            long amount;
            try{amount=Long.parseLong(raw);}catch(NumberFormatException e){throw new IllegalArgumentException("チャージ金額が大きすぎます");}
            if(amount==Long.MIN_VALUE)throw new IllegalArgumentException("CSV金額が不正です");
            total=plus(total,Math.abs(amount));count++;
        }
        return new CsvResult(total,count);
    }
}