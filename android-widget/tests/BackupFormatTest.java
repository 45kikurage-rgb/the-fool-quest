package com.aruno.foolquest.widget.next;
import java.util.*;
import org.json.*;
public final class BackupFormatTest {
    static int checks;
    static void ok(boolean condition,String why){checks++;if(!condition)throw new AssertionError(why);}
    interface Operation{void run()throws Exception;}
    static void rejects(Operation operation,String why)throws Exception{boolean no=false;try{operation.run();}catch(Exception expected){no=true;}ok(no,why);}
    public static void main(String[] args)throws Exception {
        JSONObject nativeData=new JSONObject().put("format",RevenueManager.FORMAT).put("version",1)
            .put("months",new JSONObject().put("2026-11",new JSONObject().put("csv",300).put("csvDailyComplete",true).put("csvDaily",new JSONObject().put("2026-11-01",300))))
            .put("manual",new JSONArray().put(new JSONObject().put("id","manual-1").put("date","2026-11-01").put("amount",25)))
            .put("losses",new JSONArray().put(new JSONObject().put("id","loss-1").put("date","2026-11-01").put("amount",7).put("category","coupon")))
            .put("goals",new JSONObject().put("tiktok",123000).put("coupon",456000));
        Map<String,Object> p=new HashMap<>();p.put("nativeRevenueV1",nativeData.toString());p.put("font",37);p.put("goalTotal",579000L);
        p.put("calendar.forecastHidden.3",1);p.put("calendar.filter.3",2);p.put("paceColors",true);p.put("myBestCouponDailyCredentialV1","secret-value");p.put("password","not-for-backup");
        p.put("calendarHeaderMonthAlign",0);p.put("calendarHeaderCategoryAlign",1);p.put("calendarHeaderAmountAlign",2);
        String json=BackupFormat.create(p,"change",1794096000000L,35);
        JSONObject parsed=BackupFormat.parse(json);Map<String,Object> out=BackupFormat.decode(parsed.getJSONObject("preferences"));
        ok(!json.contains("secret-value")&&!json.contains("not-for-backup"),"credential values excluded");
        ok(out.get("calendarHeaderMonthAlign").equals(0)&&out.get("calendarHeaderCategoryAlign").equals(1)&&out.get("calendarHeaderAmountAlign").equals(2),"three independent header positions roundtrip");
        ok(out.get("font") instanceof Integer&&out.get("goalTotal") instanceof Long,"integer versus long types preserved");
        ok(out.get("calendar.forecastHidden.3").equals(1),"forecast visibility preserved");
        ok(out.get("paceColors") instanceof Boolean,"boolean preserved");
        ok(new JSONObject((String)out.get("nativeRevenueV1")).getJSONArray("losses").getJSONObject(0).getLong("amount")==7,"category loss preserved");
        ok(BackupFormat.records(out)==3,"monthly manual loss count verified");
        rejects(()->BackupFormat.parse(json.replace("579000","579001")),"corrupted payload checksum refused");
        JSONObject env=new JSONObject(json),body=new JSONObject(env.getString("body"));body.put("version",2);
        String bodyText=body.toString();String unsupported=new JSONObject().put("body",bodyText).put("sha256",BackupFormat.sha(bodyText)).toString();
        rejects(()->BackupFormat.parse(unsupported),"future format fails safely even with a valid hash");
        Map<String,Object> broken=new HashMap<>(p);broken.put("font",37L);rejects(()->BackupFormat.create(broken,"change",1794096000000L,35),"wrong stored setting type refused");
        JSONObject wrong=new JSONObject(nativeData.toString());wrong.getJSONObject("months").getJSONObject("2026-11").put("csv",301);broken.put("font",37);broken.put("nativeRevenueV1",wrong.toString());
        rejects(()->BackupFormat.create(broken,"change",1794096000000L,35),"CSV day sum mismatch refused");
        p.put("couponRevenueDailyV2",new JSONObject().put("2026-11",new JSONObject().put("total",50).put("sum",50).put("days",new JSONObject().put("2026-11-01",49))).toString());
        rejects(()->BackupFormat.create(p,"daily",1794096000000L,35),"invalid read-only daily cache refused");
        p.remove("couponRevenueDailyV2");
        ok(BackupFormat.canonical(BackupFormat.encode(p)).equals(BackupFormat.canonical(BackupFormat.encode(out))),"round-trip matches full typed settings and data");
        ok(BackupFormat.summary(parsed).contains("SHA-256"),"preview reports integrity");
        System.out.println("PASS "+checks+" backup format checks");
    }
}
