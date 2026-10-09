package com.aruno.foolquest.widget;

import android.app.Instrumentation;
import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import java.util.ArrayList;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Only in the separate test APK, never in the shipped app. */
public class WidgetInstrumentation extends Instrumentation {
    int checks; Context c; File output; Bundle args;
    void ok(boolean x,String name){checks++;if(!x)throw new AssertionError(name);}
    void png(android.graphics.Bitmap b,String name)throws Exception{try(FileOutputStream f=new FileOutputStream(new File(output,name+".png"))){b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,f);}}
    void invalid(String raw,String month)throws Exception{checks++;try{RevenueStore.validateCoupon(raw,month);throw new AssertionError("accepted invalid "+raw);}catch(IllegalArgumentException|org.json.JSONException expected){}}
    @Override public void onCreate(Bundle args){super.onCreate(args);this.args=args;start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            c=getTargetContext();output=new File(c.getExternalFilesDir(null),"verification");output.mkdirs();
            if("offline".equals(args.getString("mode"))){
                RevenueStore.Data before=RevenueStore.read(c);CountDownLatch latch=new CountDownLatch(1);
                RevenueUpdate.start(c,latch::countDown);ok(latch.await(20,TimeUnit.SECONDS),"offline request finished");
                RevenueStore.Data after=RevenueStore.read(c);ok(after.coupon==before.coupon&&after.couponAt==before.couponAt,"offline keeps good amount and timestamp");ok(!after.error.isEmpty(),"offline error is visible");
                png(WidgetRenderer.render(c,after,new DisplaySettings(),360,126).bitmap,"native-offline-retained");
                result.putString("stream","PASS "+checks+" native offline checks");finish(ActivityResult.OK,result);return;
            }
            if("reboot".equals(args.getString("mode"))){
                RevenueStore.Data cached=RevenueStore.read(c);ok(cached.coupon>=0&&cached.couponAt>0,"reboot saved data");
                ok(c.getSystemService(android.app.job.JobScheduler.class).getPendingJob(RevenueJob.PERIODIC)!=null,"reboot persisted periodic job");
                png(WidgetRenderer.render(c,cached,DisplaySettings.load(RevenueStore.prefs(c)),360,126).bitmap,"native-reboot-cache");
                result.putString("stream","PASS "+checks+" native reboot checks");finish(ActivityResult.OK,result);return;
            }
            String month=RevenueMath.month(System.currentTimeMillis());
            ok(RevenueStore.validateCoupon("{\"month\":\""+month+"\",\"coupon_revenue\":302990}",month)==302990,"API numeric contract");
            for(String value:new String[]{"-1","1.5","\"302990\"","9007199254740992","null"})invalid("{\"month\":\""+month+"\",\"coupon_revenue\":"+value+"}",month);
            invalid("{\"month\":\"2000-01\",\"coupon_revenue\":1}",month);invalid("{}",month);
            String link="tfqwidget://snapshot?v=1&month="+month+"&tiktok=1000000&goalTotal=1000000&goalTiktok=1000000&goalCoupon=1000000&at="+System.currentTimeMillis();
            RevenueStore.Import.parse(Uri.parse(link)).save(c);RevenueStore.saveCoupon(c,month,1000000);
            RevenueStore.Data d=RevenueStore.read(c);d.tiktok=1000000;d.coupon=1000000;
            DisplaySettings s=new DisplaySettings();
            Paint p=new Paint();p.setTypeface(Typeface.create("monospace",Typeface.NORMAL));
            for(int width:new int[]{280,320,360,400,500})for(int height:new int[]{100,110,126,150}){
                WidgetRenderer.Render r=WidgetRenderer.render(c,d,s,width,height);WidgetLayout l=new WidgetLayout(width,height,s.left,s.right,s.top,s.bottom,s.gap);p.setTextSize(r.font);
                ok(p.measureText("Coupon")<=l.labelWidth()+.1,"label fits");
                ok(p.measureText("¥9,999,999")<=l.moneyWidth()+.1,"7 digit individual money fields fit");
                ok(p.measureText("9999.99%")<=l.percentWidth()+.1,"percent fits");
                if(height==126)png(r.bitmap,"native-"+width+"x"+height+"-dummy");
            }
            // Screenshot from the physical phone exposed spacing and slash movement.
            d.tiktok=21649;d.coupon=322990;d.goalTotal=1200000;d.goalTiktok=600000;d.goalCoupon=600000;
            WidgetRenderer.Render compact=WidgetRenderer.render(c,d,s,340,196);
            png(compact.bitmap,"native-compact-user-values");
            ok(compact.panelHeight<110,"tall widget stays compact");
            ok(compact.rowHeight<30,"gauge stays immediately below text");
            for(long v:new long[]{1,999,21649,344639,1000000,9999999}){
                d.tiktok=v;d.coupon=0;d.goalTiktok=v;d.goalCoupon=v;
                WidgetRenderer.Render r=WidgetRenderer.render(c,d,s,340,196);
                ok(r.layout.currentEnd==compact.layout.currentEnd&&r.layout.slashCenter==compact.layout.slashCenter&&r.layout.goalEnd==compact.layout.goalEnd,"current slash goal columns do not move with digits");
                ok(r.font==compact.font,"seven digit inputs preserve font cell width");
                png(r.bitmap,"native-fixed-money-"+v);
            }
            d.tiktok=9999999;d.coupon=9999999;d.goalTotal=1000000;d.goalTiktok=1000000;d.goalCoupon=1000000;
            png(WidgetRenderer.render(c,d,s,360,126).bitmap,"native-seven-digits-over100");
            s.background=android.graphics.Color.WHITE;s.text=android.graphics.Color.BLACK;png(WidgetRenderer.render(c,d,s,360,126).bitmap,"native-white");
            s.opacity=30;s.font=24;s.left=32;s.right=32;s.top=24;s.bottom=24;s.gap=18;s.gauge=12;
            WidgetRenderer.Render adjusted=WidgetRenderer.render(c,d,s,280,100);ok(adjusted.adjusted,"unsafe settings adjusted");png(adjusted.bitmap,"native-extreme-settings");
            s.save(RevenueStore.prefs(c));ok(DisplaySettings.load(RevenueStore.prefs(c)).font==24,"settings persist");
            new DisplaySettings().save(RevenueStore.prefs(c));
            RevenueStore.prefs(c).edit().remove("importMonth").remove("tiktok").remove("importAt").apply();
            CountDownLatch fetched=new CountDownLatch(1);RevenueUpdate.start(c,fetched::countDown);ok(fetched.await(20,TimeUnit.SECONDS),"fetch completed");
            RevenueStore.Data live=RevenueStore.read(c);ok(live.coupon>=0&&live.error.isEmpty(),"production API received");ok(live.tiktok==-1&&live.values()[0]==-1,"missing TikTok not zero");
            png(WidgetRenderer.render(c,live,new DisplaySettings(),360,126).bitmap,"native-live-coupon");
            // Actual Android AppWidgetHostView: applies the production RemoteViews and PendingIntent.
            Activity activity=startActivitySync(new Intent(c,SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();Thread.sleep(500);png(getUiAutomation().takeScreenshot(),"native-settings-screen");
            AppWidgetHost host=new AppWidgetHost(c,1234);AppWidgetManager manager=AppWidgetManager.getInstance(c);
            int id=host.allocateAppWidgetId();
            ok(manager.bindAppWidgetIdIfAllowed(id,new ComponentName(c,RevenueWidget.class)),"bind Android widget host");
            host.startListening();
            final AppWidgetHostView[] view={null};
            final int hostWidth=Math.min(360,Math.round(activity.getResources().getDisplayMetrics().widthPixels/activity.getResources().getDisplayMetrics().density)-24);
            runOnMainSync(()->{
                view[0]=host.createView(activity,id,manager.getAppWidgetInfo(id));
                android.widget.FrameLayout frame=new android.widget.FrameLayout(activity);frame.setBackgroundColor(0xff20302c);
                int densityWidth=Math.round(hostWidth*activity.getResources().getDisplayMetrics().density),densityHeight=Math.round(126*activity.getResources().getDisplayMetrics().density);
                android.widget.FrameLayout.LayoutParams lp=new android.widget.FrameLayout.LayoutParams(densityWidth,densityHeight);lp.gravity=android.view.Gravity.CENTER;
                frame.addView(view[0],lp);activity.setContentView(frame);
            });
            Bundle dimensions=new Bundle();dimensions.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,hostWidth);dimensions.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,126);
            ArrayList<android.util.SizeF> exactSizes=new ArrayList<>();exactSizes.add(new android.util.SizeF(hostWidth,126));
            dimensions.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES,exactSizes);manager.updateAppWidgetOptions(id,dimensions);
            RevenueWidget.renderAll(c);waitForIdleSync();Thread.sleep(700);png(getUiAutomation().takeScreenshot(),"native-widget-host-live");
            java.lang.reflect.Field busyField=RevenueUpdate.class.getDeclaredField("BUSY");busyField.setAccessible(true);
            java.util.concurrent.atomic.AtomicBoolean gate=(java.util.concurrent.atomic.AtomicBoolean)busyField.get(null);
            for(int n=0;n<100&&RevenueUpdate.busy();n++)Thread.sleep(100);
            gate.set(true);long beforeDuplicate=RevenueStore.read(c).couponAt;CountDownLatch duplicate=new CountDownLatch(1);RevenueUpdate.start(c,duplicate::countDown);
            ok(duplicate.await(1,TimeUnit.SECONDS)&&RevenueStore.read(c).couponAt==beforeDuplicate,"busy request coalesced without network");gate.set(false);
            long beforeTap=RevenueStore.read(c).couponAt;
            runOnMainSync(()->{ok(view[0].findViewById(R.id.widget_root)!=null,"production RemoteViews applied");view[0].findViewById(R.id.widget_root).performClick();});
            for(int n=0;n<100&&RevenueStore.read(c).couponAt<=beforeTap;n++)Thread.sleep(100);
            ok(RevenueStore.read(c).couponAt>beforeTap,"normal widget tap updates API timestamp");
            dimensions.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,280);dimensions.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,100);exactSizes.clear();exactSizes.add(new android.util.SizeF(280,100));dimensions.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES,exactSizes);manager.updateAppWidgetOptions(id,dimensions);
            runOnMainSync(()->{android.view.ViewGroup.LayoutParams lp=view[0].getLayoutParams();lp.width=Math.round(280*activity.getResources().getDisplayMetrics().density);lp.height=Math.round(100*activity.getResources().getDisplayMetrics().density);view[0].setLayoutParams(lp);});
            Thread.sleep(700);png(getUiAutomation().takeScreenshot(),"native-widget-host-resized");host.stopListening();runOnMainSync(activity::finish);
            result.putString("stream","PASS "+checks+" native Android checks; Coupon "+live.coupon+"; screenshots "+output);
            finish(ActivityResult.OK,result);
        }catch(Throwable e){result.putString("stream","FAIL "+checks+" "+android.util.Log.getStackTraceString(e));finish(ActivityResult.FAIL,result);}
    }
    static final class ActivityResult{static final int OK=-1,FAIL=0;}
}
