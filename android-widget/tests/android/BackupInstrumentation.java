package com.aruno.foolquest.widget.next;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Isolated emulator fixture only. Never installed or run against a user's phone. */
public final class BackupInstrumentation extends Instrumentation {
    private Context c;private BackupCoordinator b;private int checks;private Bundle args;
    @Override public void onCreate(Bundle a){super.onCreate(a);args=a;start();}
    private void ok(boolean v,String message){checks++;if(!v)throw new AssertionError(message);}
    private void drained()throws Exception {for(int i=0;i<3;i++)b.worker.submit(()->{}).get(40,TimeUnit.SECONDS);}
    private String current()throws Exception{return BackupFormat.canonical(BackupFormat.encode(b.raw.getAll()));}
    private void pickerClick(String... labels)throws Exception {
        for(int tries=0;tries<60;tries++){
            AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();
            if(root!=null)for(String label:labels)for(AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText(label)){
                if(!node.isEnabled())continue;
                AccessibilityNodeInfo target=node;
                while(target!=null&&!target.isClickable())target=target.getParent();
                if(target!=null&&target.performAction(AccessibilityNodeInfo.ACTION_CLICK)){Thread.sleep(400);return;}
            }
            Thread.sleep(150);
        }throw new AssertionError("picker action unavailable "+Arrays.toString(labels));
    }
    private void findButton(android.view.View view,String text){
        if(view instanceof android.widget.Button&&text.equals(((android.widget.Button)view).getText().toString())){view.performClick();return;}
        if(view instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)view;for(int i=0;i<g.getChildCount();i++)findButton(g.getChildAt(i),text);}
    }
    @Override public void onStart(){Bundle result=new Bundle();try {
        c=getTargetContext();b=BackupCoordinator.get(c);
        String month=RevenueMath.month(System.currentTimeMillis());
        if("reboot".equals(args.getString("mode"))){
            drained();ok(b.meta.getLong("backupTestExpected",0)==new RevenueManager(c).tiktok(month),"reboot retains native revenue");
            ok(!b.meta.getString("tree","").isEmpty(),"reboot retains selected tree");
            b.capture("safety",true);b.flush();ok(b.meta.getLong("lastSuccess",0)>0,"persisted grant supports automatic writes after reboot");
        }else {
            Activity screen=startActivitySync(new Intent(c,BackupActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            runOnMainSync(()->findButton(screen.getWindow().getDecorView(),"保存先を選択・権限を再設定"));
            pickerClick("USE THIS FOLDER","Use this folder");pickerClick("ALLOW","Allow");
            for(int i=0;i<100&&b.meta.getString("tree","").isEmpty();i++)Thread.sleep(100);
            ok(!b.meta.getString("tree","").isEmpty(),"real Android SAF selection persisted");drained();
            runOnMainSync(screen::finish);
            RevenueManager m=new RevenueManager(c);m.setCsv(month,1000,"fixture.csv",1);
            m.addManual(month+"-01",250);drained();
            ok(new RevenueManager(c).tiktok(month)==1250,"manual addition persists");
            List<File> versions=b.local();boolean manualSaved=false;
            for(File f:versions){JSONObject body=BackupFormat.parse(BackupCoordinator.read(f));Map<String,Object> p=BackupFormat.decode(body.getJSONObject("preferences"));if(p.containsKey("nativeRevenueV1")&&new JSONObject((String)p.get("nativeRevenueV1")).getJSONArray("manual").length()>0)manualSaved=true;}
            ok(manualSaved,"manual addition automatically creates readable backup");
            m.addLoss(month+"-01","tiktok",80);m.addLoss(month+"-01","coupon",50);
            String lossId=m.losses().getJSONObject(m.losses().length()-2).getString("id");
            DisplaySettings settings=DisplaySettings.load(RevenueStore.prefs(c));settings.font=37;settings.gap=8;settings.save(RevenueStore.prefs(c));
            CalendarWidgetState.month(c,12345,month);CalendarWidgetState.filter(c,12345,3);
            RevenueManager.archiveCoupon(c,"2026-09",777);
            JSONObject oldDaily=new JSONObject().put("2026-09",new JSONObject().put("total",777).put("sum",777).put("days",new JSONObject().put("2026-09-01",777)));
            RevenueStore.prefs(c).edit().putString("couponRevenueDailyV2",oldDaily.toString()).apply();
            String restore=BackupFormat.create(b.raw.getAll(),"safety",System.currentTimeMillis(),35);
            RevenueStore.saveCoupon(c,month,888);
            RevenueStore.prefs(c).edit().putString("couponRevenueMonthlyV1",new JSONObject().put(month,888).toString())
                .putString("couponRevenueDailyV2",new JSONObject().put(month,new JSONObject().put("total",888).put("sum",888).put("days",new JSONObject().put(month+"-01",888))).toString()).apply();
            m.updateLoss(lossId,month+"-02","tiktok",99);m.deleteLoss(lossId);
            settings.font=18;settings.save(RevenueStore.prefs(c));
            b.restore(restore,current());m=new RevenueManager(c);
            ok(m.lossTotal(month,"tiktok")==80&&m.lossTotal(month,"coupon")==50,"restore recovers edited/deleted category losses");
            ok(DisplaySettings.load(RevenueStore.prefs(c)).font==37&&DisplaySettings.load(RevenueStore.prefs(c)).gap==8,"restore recovers typed display settings");
            ok(CalendarWidgetState.filter(c,12345)==3,"restore recovers widget configuration");
            ok(m.couponHistory().getLong("2026-09")==777&&m.couponHistory().getLong(month)==888,"restore fills missing historical Coupon months without replacing current projection");
            JSONObject combinedDaily=new JSONObject(RevenueStore.prefs(c).getString("couponRevenueDailyV2","{}"));
            ok(combinedDaily.getJSONObject("2026-09").getLong("total")==777&&combinedDaily.getJSONObject(month).getLong("total")==888,"daily projection merge preserves old history and current authority");
            ok(RevenueStore.read(c).coupon==888,"restore does not roll back the current server display cache");
            File alreadySent=new File(b.directory,"aruno-result-change-1000000000000-00000000-0000-0000-0000-000000000000.json");
            File marker=new File(b.directory,alreadySent.getName()+".sent");
            try{
                try(RandomAccessFile sparse=new RandomAccessFile(alreadySent,"rw")){sparse.setLength(33L*1024*1024);}
                BackupCoordinator.write(marker,"ok");
                java.lang.reflect.Method capacity=BackupCoordinator.class.getDeclaredMethod("capacity");capacity.setAccessible(true);capacity.invoke(b);
                ok(true,"exported generations do not consume the pending-outbox capacity");
            }finally{alreadySent.delete();marker.delete();}
            VatonRevenueCsv.CsvResult csv=VatonRevenueCsv.parseCsv("日付,チャージポイント\n"+month+"-01,300\n"+month+"-02,700\n");
            m.setCsv(month,csv,"dated.csv");drained();
            ok(CalendarRevenueData.read(c,month).totals[2]==1170,"CSV and manual minus loss remain coherent");
            String good=current();boolean blank=RevenueStore.prefs(c).edit().remove("nativeRevenueV1").remove("couponRevenueMonthlyV1").remove("couponRevenueDailyV2").remove("tiktok").remove("coupon").commit();
            ok(!blank&&current().equals(good),"empty reset rejected before normal data changes");
            boolean corrupt=false;try{b.restore(restore.replace("sha256\":\"","sha256\":\"bad"),current());}catch(Exception expected){corrupt=true;}
            ok(corrupt&&current().equals(good),"corrupt SHA is refused without changing live data");
            // Stale in-memory owners must not overwrite each other's confirmed changes.
            RevenueManager first=new RevenueManager(c),second=new RevenueManager(c);
            first.addManual(month+"-03",11);second.addManual(month+"-03",13);
            ok(new RevenueManager(c).manualTotal(month)==274,"concurrent owners preserve both manual additions");
            ExecutorService rapid=Executors.newFixedThreadPool(3);List<Future<?>> work=new ArrayList<>();
            for(int i=0;i<15;i++)work.add(rapid.submit(()->{try{new RevenueManager(c).addManual(month+"-04",1);}catch(Exception e){throw new RuntimeException(e);}}));
            for(Future<?> future:work)future.get(40,TimeUnit.SECONDS);rapid.shutdown();drained();
            ok(new RevenueManager(c).manualTotal(month)==289,"rapid concurrent changes are not lost");
            b.flush();Map<String,Uri> docs=new BackupFiles(c,Uri.parse(b.meta.getString("tree",""))).documents();Map<String,Integer> counts=new HashMap<>();
            for(Map.Entry<String,Uri> e:docs.entrySet())if(e.getKey().endsWith(".json")){
                JSONObject body=BackupFormat.parse(BackupFiles.read(c,e.getValue()));String kind=body.getString("kind");counts.put(kind,counts.getOrDefault(kind,0)+1);
                ok(!BackupFiles.read(c,e.getValue()).contains("myBestCouponDailyCredentialV1"),"backup excludes authorization secrets");
            }
            ok(counts.getOrDefault("change",0)==10,"external recent generations retain exactly ten");
            ok(counts.getOrDefault("daily",0)>=1,"daily anchor survives rapid writes");
            ok(counts.getOrDefault("safety",0)>=1,"pre-restore anchor remains independent");
            // Real URI grant revocation must leave the normal values and already-good files intact.
            Uri tree=Uri.parse(b.meta.getString("tree",""));
            c.getContentResolver().releasePersistableUriPermission(tree,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            new RevenueManager(c).addManual(month+"-05",7);drained();
            ok(!b.meta.getString("error","").isEmpty(),"revoked authorization produces persistent user-visible failure");
            ok(new RevenueManager(c).manualTotal(month)==296,"failed external copy keeps committed normal data and durable outbox");
            // Pending replay and prior-generation recovery remain available after permission loss.
            ok(b.local().size()>=10,"local outbox survives failed external copy");
            Activity again=startActivitySync(new Intent(c,BackupActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            runOnMainSync(()->findButton(again.getWindow().getDecorView(),"保存先を選択・権限を再設定"));
            pickerClick("USE THIS FOLDER","Use this folder");pickerClick("ALLOW","Allow");drained();runOnMainSync(again::finish);
            b.flush();ok(b.meta.getString("error","").isEmpty(),"reselection drains pending snapshots safely");
            b.meta.edit().putLong("backupTestExpected",new RevenueManager(c).tiktok(month)).commit();
            String before=current();
            b.restore(restore,before);ok(new RevenueManager(c).manualTotal(month)==250,"older valid generation remains recoverable after more than ten writes");
            b.meta.edit().putLong("backupTestExpected",new RevenueManager(c).tiktok(month)).commit();drained();
            ok(RevenueWidget.ids(c).length>=0&&CalendarWidget.ids(c).length>=0,"all widget providers remain queryable");
        }
        result.putString("stream","PASS "+checks+" backup Android checks");finish(-1,result);
    }catch(Throwable e){result.putString("stream","FAIL "+checks+" "+android.util.Log.getStackTraceString(e));finish(0,result);}}
}
