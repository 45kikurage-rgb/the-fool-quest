package com.aruno.foolquest.widget.next;

import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Versioned, typed and allowlisted local recovery format. No credentials. */
final class BackupFormat {
    static final String PACKAGE="com.aruno.foolquest.widget.next", FORMAT="aruno-result-backup";
    static final int LIMIT=4_000_000;
    static final Set<String> INTS=new HashSet<>(Arrays.asList(
        "text","background","colorTotal","colorTiktok","colorCoupon","colorTargetLine","targetLineWidth",
        "colorTargetMarker","gaugeTickDivisor","colorPaceAchieved","colorPaceBehind","colorPaceLow","colorSecondLap",
        "opacity","font","left","right","top","bottom","gap","gauge","textGap"));
    static final Set<String> LONGS=new HashSet<>(Arrays.asList("goalTotal","goalTiktok","goalCoupon","tiktok","coupon","importAt","couponAt"));
    static final Set<String> STRINGS=new HashSet<>(Arrays.asList("nativeRevenueV1","couponRevenueMonthlyV1","couponRevenueDailyV2","importMonth","couponMonth"));
    static String type(String k){
        if(INTS.contains(k)||k.matches("calendar\\.(filter|summary|forecastHidden)\\.[0-9]+"))return "int";
        if(LONGS.contains(k))return "long";
        if(STRINGS.contains(k)||k.matches("calendar\\.month\\.[0-9]+"))return "string";
        return "paceColors".equals(k)?"boolean":null;
    }
    static Map<String,Object> select(Map<String,?> source){
        Map<String,Object> out=new TreeMap<>();
        for(Map.Entry<String,?> e:source.entrySet())if(type(e.getKey())!=null)out.put(e.getKey(),e.getValue());
        return out;
    }
    static JSONObject encode(Map<String,?> source)throws Exception {
        JSONObject out=new JSONObject();
        for(Map.Entry<String,Object> e:select(source).entrySet()){
            String t=type(e.getKey());Object v=e.getValue();
            if((t.equals("int")&&!(v instanceof Integer))||(t.equals("long")&&!(v instanceof Long))||
               (t.equals("boolean")&&!(v instanceof Boolean))||(t.equals("string")&&!(v instanceof String)))
                throw new IllegalArgumentException("保存データの型が不正です："+e.getKey());
            out.put(e.getKey(),new JSONObject().put("type",t).put("value",v));
        }
        decode(out);return out;
    }
    static Map<String,Object> decode(JSONObject data)throws Exception {
        if(data.length()>2048)throw new IllegalArgumentException("設定件数が多すぎます");
        Map<String,Object> out=new TreeMap<>();
        for(Iterator<String> it=data.keys();it.hasNext();){
            String k=it.next(),t=type(k);JSONObject v=data.getJSONObject(k);
            if(t==null||!t.equals(v.getString("type")))throw new IllegalArgumentException("未対応の設定です："+k);
            Object value=v.get("value");
            if(t.equals("string")){if(!(value instanceof String))throw new IllegalArgumentException("文字設定が不正です");}
            else if(t.equals("boolean")){if(!(value instanceof Boolean))throw new IllegalArgumentException("設定が不正です");}
            else {
                if(!(value instanceof Number))throw new IllegalArgumentException("数値設定が不正です");
                double n=((Number)value).doubleValue();long l=((Number)value).longValue();
                if(!Double.isFinite(n)||n!=l||Math.abs(n)>9007199254740991L)throw new IllegalArgumentException("数値設定が範囲外です");
                if(t.equals("int")){if(l<Integer.MIN_VALUE||l>Integer.MAX_VALUE)throw new IllegalArgumentException("整数設定が範囲外です");value=Integer.valueOf((int)l);}
                else value=Long.valueOf(l);
            }
            out.put(k,value);
        }
        Object nativeData=out.get("nativeRevenueV1");
        if(nativeData!=null)RevenueManager.validate(new JSONObject((String)nativeData));
        for(String k:Arrays.asList("importMonth","couponMonth"))if(out.containsKey(k)&&!RevenueMath.validMonth((String)out.get(k)))throw new IllegalArgumentException("保存月が不正です");
        for(String k:LONGS)if(out.containsKey(k)&&((Long)out.get(k))<0)throw new IllegalArgumentException("保存金額が不正です");
        if(out.containsKey("couponRevenueMonthlyV1")){
            JSONObject months=new JSONObject((String)out.get("couponRevenueMonthlyV1"));
            if(months.length()>240)throw new IllegalArgumentException("月別履歴が多すぎます");
            for(Iterator<String> it=months.keys();it.hasNext();){String m=it.next();RevenueManager.requireMonth(m);RevenueManager.strictNumber(months.get(m),false);}
        }
        if(out.containsKey("couponRevenueDailyV2")){
            JSONObject months=new JSONObject((String)out.get("couponRevenueDailyV2"));
            if(months.length()>240)throw new IllegalArgumentException("日別履歴が多すぎます");
            for(Iterator<String> it=months.keys();it.hasNext();){
                String m=it.next();RevenueManager.requireMonth(m);JSONObject row=months.getJSONObject(m),days=row.getJSONObject("days");
                long total=RevenueManager.strictNumber(row.get("total"),false),sum=0;
                if(days.length()>31)throw new IllegalArgumentException("日別履歴が不正です");
                for(Iterator<String> d=days.keys();d.hasNext();){String day=d.next();RevenueManager.checkDate(day);if(!day.startsWith(m+"-"))throw new IllegalArgumentException("日別の月が不正です");sum=RevenueManager.plus(sum,RevenueManager.strictNumber(days.get(day),false));}
                if(sum!=RevenueManager.strictNumber(row.get("sum"),false)||sum>total)throw new IllegalArgumentException("日別合計が一致しません");
            }
        }
        for(String k:out.keySet())if(k.startsWith("calendar.")){
            if(k.startsWith("calendar.month."))RevenueManager.requireMonth((String)out.get(k));
            else {int n=(Integer)out.get(k);if(n<0||n>(k.startsWith("calendar.filter.")?4:k.startsWith("calendar.forecastHidden.")?1:2))throw new IllegalArgumentException("カレンダー設定が不正です");}
        }
        return out;
    }
    static int records(Map<String,?> p)throws Exception {
        int count=0;
        if(p.containsKey("nativeRevenueV1")){
            JSONObject n=new JSONObject((String)p.get("nativeRevenueV1"));
            count+=n.getJSONObject("months").length()+n.getJSONArray("manual").length();
            JSONArray losses=n.optJSONArray("losses");if(losses!=null)count+=losses.length();
        }
        for(String k:Arrays.asList("couponRevenueMonthlyV1","couponRevenueDailyV2"))if(p.containsKey(k))count+=new JSONObject((String)p.get(k)).length();
        if(p.containsKey("tiktok"))count++;if(p.containsKey("coupon"))count++;
        return count;
    }
    static String canonical(Object v)throws Exception {
        if(v instanceof JSONObject){JSONObject o=(JSONObject)v;List<String> keys=new ArrayList<>();for(Iterator<String> it=o.keys();it.hasNext();)keys.add(it.next());Collections.sort(keys);StringBuilder b=new StringBuilder("{");for(String k:keys){if(b.length()>1)b.append(',');b.append(JSONObject.quote(k)).append(':').append(canonical(o.get(k)));}return b.append('}').toString();}
        if(v instanceof JSONArray){JSONArray a=(JSONArray)v;StringBuilder b=new StringBuilder("[");for(int i=0;i<a.length();i++){if(i>0)b.append(',');b.append(canonical(a.get(i)));}return b.append(']').toString();}
        return v instanceof String?JSONObject.quote((String)v):String.valueOf(v);
    }
    static String sha(String value)throws Exception {
        byte[] hash=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder b=new StringBuilder();for(byte h:hash)b.append(String.format(Locale.US,"%02x",h&255));return b.toString();
    }
    static String create(Map<String,?> p,String kind,long time,int version)throws Exception {
        JSONObject data=encode(p);
        String body=canonical(new JSONObject().put("format",FORMAT).put("version",1).put("package",PACKAGE)
            .put("savedAt",time).put("appVersion",version).put("kind",kind).put("records",records(select(p)))
            .put("preferences",data).put("credentialsIncluded",false).put("couponAuthority","Ledger-read-only"));
        String json=new JSONObject().put("body",body).put("sha256",sha(body)).toString();
        if(json.getBytes(StandardCharsets.UTF_8).length>LIMIT)throw new IllegalArgumentException("バックアップが4MBを超えています");return json;
    }
    static JSONObject parse(String json)throws Exception {
        if(json.getBytes(StandardCharsets.UTF_8).length>LIMIT)throw new IllegalArgumentException("バックアップが大きすぎます");
        JSONObject envelope=new JSONObject(json);String raw=envelope.getString("body");
        if(!sha(raw).equals(envelope.getString("sha256")))throw new IllegalArgumentException("バックアップのSHA-256が一致しません");
        JSONObject b=new JSONObject(raw);
        if(!FORMAT.equals(b.getString("format"))||b.getInt("version")!=1||!PACKAGE.equals(b.getString("package"))||b.getLong("savedAt")<=0||b.getBoolean("credentialsIncluded"))throw new IllegalArgumentException("未対応のバックアップです");
        String kind=b.getString("kind");if(!Arrays.asList("change","daily","safety").contains(kind))throw new IllegalArgumentException("保存種別が不正です");
        Map<String,Object> p=decode(b.getJSONObject("preferences"));
        if(records(p)!=b.getInt("records"))throw new IllegalArgumentException("保存件数が一致しません");return b;
    }
    static String summary(JSONObject b)throws Exception {
        Map<String,Object> p=decode(b.getJSONObject("preferences"));StringBuilder out=new StringBuilder();
        out.append("保存日時：").append(RevenueMath.date("yyyy-MM-dd HH:mm:ss",b.getLong("savedAt"))).append("\n保存件数：").append(b.getInt("records")).append("\n設定項目：").append(p.size()).append("\nSHA-256：正常");
        if(p.containsKey("nativeRevenueV1")){
            JSONObject n=new JSONObject((String)p.get("nativeRevenueV1"));
            out.append("\n月別：").append(n.getJSONObject("months").length()).append("／手動：").append(n.getJSONArray("manual").length()).append("／損失：").append(n.optJSONArray("losses")==null?0:n.getJSONArray("losses").length());
            out.append("\n目標 TikTok：").append(n.getJSONObject("goals").getLong("tiktok")).append("／Coupon：").append(n.getJSONObject("goals").getLong("coupon"));
        }
        return out.toString();
    }
}
