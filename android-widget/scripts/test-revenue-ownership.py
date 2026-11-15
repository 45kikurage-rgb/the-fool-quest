"""Offline regression for legacy/native ownership and loss aggregate bounds.

Run with: python3 scripts/test-revenue-ownership.py path/to/json.jar
Compiles the real revenue classes with explicit Android doubles. The supplied
org.json dependency is reused; this script does not download dependencies.
"""
from pathlib import Path
import os
import shutil
import subprocess
import sys
import tempfile


source = Path(__file__).resolve().parents[1] / "app/src/main/java"
jsonjar = Path(sys.argv[1]).resolve()
assert jsonjar.is_file(), jsonjar
package = "com/aruno/foolquest/widget/next"
stubs = {
    "android/content/Context.java": """
package android.content;
public class Context {public Context getApplicationContext(){return this;}}
""",
    "android/content/SharedPreferences.java": """
package android.content;
import java.util.Map;
public interface SharedPreferences {
  Map<String,?> getAll();
  String getString(String key,String fallback);
  long getLong(String key,long fallback);
  Editor edit();
  interface Editor {
    Editor putString(String key,String value);
    Editor putLong(String key,long value);
    Editor remove(String key);
    boolean commit();
    void apply();
  }
}
""",
    "android/net/Uri.java": """
package android.net;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
public class Uri {
  private final URI value;
  private Uri(String raw){value=URI.create(raw);}
  public static Uri parse(String raw){return new Uri(raw);}
  public String getScheme(){return value.getScheme();}
  public String getHost(){return value.getHost();}
  public String getQueryParameter(String key){
    String query=value.getRawQuery();
    if(query==null)return null;
    for(String field:query.split("&")){
      String[] parts=field.split("=",2);
      if(key.equals(URLDecoder.decode(parts[0],StandardCharsets.UTF_8)))
        return parts.length==1?"":URLDecoder.decode(parts[1],StandardCharsets.UTF_8);
    }
    return null;
  }
}
""",
    "android/util/Log.java": """
package android.util;
public class Log {
  public static int e(String tag,String message,Throwable error){return 0;}
  public static int w(String tag,String message,Throwable error){return 0;}
}
""",
    package + "/FixtureDependencies.java": """
package com.aruno.foolquest.widget.next;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

final class BackupCoordinator {
  static final Object LOCK=new Object();
  private static final Map<Context,BackupCoordinator> instances=new HashMap<>();
  final Memory preferences=new Memory();
  static BackupCoordinator get(Context context){
    synchronized(LOCK){return instances.computeIfAbsent(context,key->new BackupCoordinator());}
  }
}
final class Memory implements SharedPreferences {
  private Map<String,Object> values=new HashMap<>();
  public Map<String,?> getAll(){return new HashMap<>(values);}
  public String getString(String key,String fallback){return (String)values.getOrDefault(key,fallback);}
  public long getLong(String key,long fallback){return (Long)values.getOrDefault(key,fallback);}
  public Editor edit(){return new Edit();}
  final class Edit implements Editor {
    private final Map<String,Object> changes=new HashMap<>();
    public Editor putString(String key,String value){changes.put(key,value);return this;}
    public Editor putLong(String key,long value){changes.put(key,value);return this;}
    public Editor remove(String key){changes.put(key,null);return this;}
    public boolean commit(){
      Map<String,Object> after=new HashMap<>(values);
      for(Map.Entry<String,Object> change:changes.entrySet()){
        if(change.getValue()==null)after.remove(change.getKey());
        else after.put(change.getKey(),change.getValue());
      }
      // Match the production preference boundary's real pre-persistence validation.
      try{BackupFormat.encode(after);}catch(Exception invalid){return false;}
      values=after;return true;
    }
    public void apply(){if(!commit())throw new IllegalStateException("fixture save rejected");}
  }
}
final class RevenueWidget {static void renderAll(Context context){}}
final class CalendarWidget {static void renderAll(Context context){}}
final class RevenueUpdate {static boolean busy(){return false;}}
final class RevenueFailure {static String label(String code){return code;}}
final class CouponDailyReader {
  static JSONObject snapshot(SharedPreferences preferences,String month){return null;}
}
""",
    package + "/RevenueOwnershipFixture.java": """
package com.aruno.foolquest.widget.next;
import android.content.Context;
import android.net.Uri;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

public class RevenueOwnershipFixture {
  interface Task {void run()throws Exception;}
  static void ok(boolean valid,String label){
    if(!valid)throw new AssertionError(label);
    System.out.println("PASS: "+label);
  }
  static void rejects(Task task,String label)throws Exception {
    try{task.run();}catch(IllegalArgumentException expected){ok(true,label);return;}
    throw new AssertionError("not rejected: "+label);
  }
  static String snapshotUri(String month,long now){
    return "tfqwidget://snapshot?v=1&month="+month+
      "&tiktok=2000&goalTiktok=800000&goalCoupon=900000&goalTotal=1700000&at="+now;
  }
  public static void main(String[] ignored)throws Exception {
    String month=RevenueMath.month(System.currentTimeMillis());
    String uri=snapshotUri(month,System.currentTimeMillis());
    RevenueStore.Import legacy=RevenueStore.Import.parse(Uri.parse(uri));
    Context context=new Context();
    RevenueStore.Import.checkAvailable(context);
    legacy.save(context);
    ok(RevenueStore.read(context).tiktok==2000,"first legacy snapshot remains supported");
    RevenueManager manager=new RevenueManager(context);
    ok(manager.tiktok(month)==2000&&manager.goal("tiktok")==800000,
      "first native owner preserves imported revenue and goals");
    manager.setCsv(month,1000,"fixture.csv",1);
    manager.setGoals(500000,500000);
    manager.addManual(month+"-01",100);
    manager.addLoss(month+"-01","tiktok",10);
    Map<String,?> before=RevenueStore.prefs(context).getAll();
    rejects(()->RevenueStore.Import.checkAvailable(context),"native owner blocks confirmation");
    rejects(()->legacy.save(context),"native owner blocks save");
    ok(before.equals(RevenueStore.prefs(context).getAll()),
      "refused legacy import preserves CSV, manual revenue, loss, goals and all preferences");
    new RevenueManager(context).projectWidget();
    ok(RevenueStore.read(context).tiktok==1100&&RevenueStore.read(context).goalTiktok==500000,
      "normal projection stays coherent after refusal");

    Context openedBeforeNative=new Context();
    RevenueStore.Import.checkAvailable(openedBeforeNative);
    new RevenueManager(openedBeforeNative);
    Map<String,?> atConfirmation=RevenueStore.prefs(openedBeforeNative).getAll();
    rejects(()->legacy.save(openedBeforeNative),"native created after confirmation still blocks save");
    ok(atConfirmation.equals(RevenueStore.prefs(openedBeforeNative).getAll()),
      "confirmation race does not alter newly created native state");

    rejects(()->RevenueStore.Import.parse(Uri.parse(uri.replace("tiktok=2000","tiktok=-1"))),
      "legacy negative revenue validation remains active");
    rejects(()->RevenueStore.Import.parse(Uri.parse(uri.replace("goalTiktok=800000","goalTiktok=-1"))),
      "legacy negative goal validation remains active");
    rejects(()->RevenueStore.Import.parse(Uri.parse(uri.replace("goalCoupon=900000","goalCoupon=0"))),
      "legacy zero goal validation remains active");
    rejects(()->RevenueStore.Import.parse(Uri.parse(snapshotUri(month,System.currentTimeMillis()-172800000L))),
      "legacy stale snapshot validation remains active");
    rejects(()->RevenueStore.Import.parse(Uri.parse(uri.replace("v=1","v=2"))),
      "legacy format version validation remains active");

    JSONObject candidate=new RevenueManager(context).snapshot();
    String absent=month.equals("2000-01")?"2000-02":"2000-01";
    candidate.put("losses",new JSONArray()
      .put(new JSONObject().put("id","loss1").put("date",absent+"-01")
        .put("category","tiktok").put("amount",RevenueMath.MAX_YEN))
      .put(new JSONObject().put("id","loss2").put("date",absent+"-01")
        .put("category","tiktok").put("amount",1)));
    rejects(()->RevenueManager.validate(candidate),"loss-only month MAX+1 aggregate rejected");
    candidate.getJSONArray("losses").getJSONObject(1).put("category","coupon");
    rejects(()->RevenueManager.validate(candidate),"same-day cross-category MAX+1 aggregate rejected");
    candidate.getJSONArray("losses").getJSONObject(1).put("date",absent+"-02");
    RevenueManager.validate(candidate);
    ok(true,"independent categories at bounds on separate days remain valid");
  }
}
""",
}

with tempfile.TemporaryDirectory(prefix="aruno-revenue-ownership-") as folder:
    root = Path(folder)
    sources = []
    for relative, contents in stubs.items():
        path = root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(contents, encoding="utf-8")
        sources.append(path)
    for name in (
        "RevenueStore", "RevenueManager", "RevenueMath", "CalendarRevenueData",
        "CalendarDateMath", "VatonRevenueCsv", "BackupFormat",
    ):
        relative = Path(package) / (name + ".java")
        path = root / relative
        shutil.copyfile(source / relative, path)
        sources.append(path)
    output = root / "classes"
    subprocess.run([
        "java", "--module", "jdk.compiler/com.sun.tools.javac.Main", "-encoding", "UTF-8",
        "-cp", str(jsonjar), "-d", str(output), *map(str, sources),
    ], check=True)
    subprocess.run([
        "java", "-cp", os.pathsep.join((str(output), str(jsonjar))),
        "com.aruno.foolquest.widget.next.RevenueOwnershipFixture",
    ], check=True)
