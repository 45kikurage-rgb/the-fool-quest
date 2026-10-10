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
    int checks; Context c; File output; Bundle args; Activity nativeFixture;
    java.util.Map<String,Float> measuredGaugeHeights=new java.util.HashMap<>();
    void ok(boolean x,String name){checks++;if(!x)throw new AssertionError(name);}
    void png(android.graphics.Bitmap b,String name)throws Exception{try(FileOutputStream f=new FileOutputStream(new File(output,name+".png"))){b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,f);}}
    void pointerTap(float x,float y)throws Exception {
        long time=android.os.SystemClock.uptimeMillis();
        android.view.MotionEvent down=android.view.MotionEvent.obtain(time,time,android.view.MotionEvent.ACTION_DOWN,x,y,0);
        down.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
        boolean sentDown=getUiAutomation().injectInputEvent(down,true);Thread.sleep(40);
        android.view.MotionEvent up=android.view.MotionEvent.obtain(time,android.os.SystemClock.uptimeMillis(),android.view.MotionEvent.ACTION_UP,x,y,0);
        up.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
        boolean sentUp=getUiAutomation().injectInputEvent(up,true);down.recycle();up.recycle();waitForIdleSync();Thread.sleep(100);
        ok(sentDown&&sentUp,"actual pointer events injected");
    }
    void invalid(String raw,String month)throws Exception{checks++;try{RevenueStore.validateCoupon(raw,month);throw new AssertionError("accepted invalid "+raw);}catch(IllegalArgumentException|org.json.JSONException expected){}}
    void nativeTextLayout(RevenueStore.Data data,DisplaySettings settings,int width,int height,int dpi,String name)throws Exception {
        android.content.res.Configuration config=new android.content.res.Configuration(c.getResources().getConfiguration());config.densityDpi=dpi;
        Context hostContext=c.createConfigurationContext(config);float density=hostContext.getResources().getDisplayMetrics().density;
        final android.view.View[] tree={null};final NativeWidgetViews.Result[] result={null};
        runOnMainSync(()->{
            result[0]=NativeWidgetViews.create(hostContext,data,settings,width,height);
            tree[0]=result[0].views.apply(hostContext,null);
            nativeFixture.setContentView(tree[0],new android.view.ViewGroup.LayoutParams(Math.round(width*density),Math.round(height*density)));
        });
        waitForIdleSync();Thread.sleep(120);
        runOnMainSync(()->{
            for(int row=0;row<3;row++)for(int col=0;col<5;col++){
                android.widget.TextView text=tree[0].findViewById(NativeWidgetViews.TEXT[row][col]);
                ok(Math.abs(text.getTextSize()-result[0].font*density)<.1,"host density preserves requested native font");
                ok(text.getScrollX()==0,"fixed text columns never scroll horizontally");
                ok(text.getPaint().measureText(text.getText().toString())<=text.getWidth()-text.getPaddingLeft()-text.getPaddingRight()+1,"actual native column has no text clipping");
                if(row>0){android.widget.TextView first=tree[0].findViewById(NativeWidgetViews.TEXT[0][col]);ok(text.getLeft()==first.getLeft()&&text.getRight()==first.getRight(),"all native rows share the same column bounds");}
            }
            ok(tree[0].getMeasuredHeight()<=height*density+1,"native content fits host height");
            android.view.View panel=tree[0].findViewById(R.id.widget_panel);
            ok(Math.abs(panel.getHeight()-(height-16)*density)<2,"card background follows resized host height");
            android.view.View firstRow=tree[0].findViewById(R.id.row_total),lastRow=tree[0].findViewById(R.id.row_coupon);
            ok(Math.abs(firstRow.getHeight()-lastRow.getHeight())<=2,"three rows share resized height equally");
            android.widget.TextView footer=tree[0].findViewById(R.id.widget_footer);
            ok(!footer.getText().toString().matches(".*[0-9]{2}:[0-9]{2}.*"),"widget never displays update times");
            if(data.importAt>0&&data.couponAt>0&&!data.loading&&data.error.isEmpty())ok(footer.getVisibility()==android.view.View.GONE,"normal display has no footer");
            if(height>=196)ok(lastRow.getTop()-firstRow.getTop()>height*density*.4f,"taller card distributes rows across its height");
            for(int row=0;row<3;row++){
                android.view.ViewGroup holder=tree[0].findViewById(NativeWidgetViews.HOLDER[row]);
                android.view.View bar=holder.getChildAt(0);
                ok(bar.getHeight()>0&&bar.getHeight()<tree[0].findViewById(NativeWidgetViews.ROW[row]).getHeight(),"gauge has visible height and fits its row");
                ok(Math.abs(bar.getHeight()/density-result[0].gauge)<1,"intrinsic bitmap gauge height respects host density");
                android.widget.TextView amount=tree[0].findViewById(NativeWidgetViews.TEXT[row][1]);
                int[] textPos=new int[2],barPos=new int[2];amount.getLocationInWindow(textPos);bar.getLocationInWindow(barPos);
                ok(barPos[1]>=textPos[1]+amount.getHeight(),"adaptive gauge does not overlap numbers");
                if(row==0)measuredGaugeHeights.put(name,bar.getHeight()/density);
                ok(bar instanceof android.widget.ImageView,"v0.1.4 gauge has no embedded target text");
                android.graphics.Bitmap fill=((android.graphics.drawable.BitmapDrawable)((android.widget.ImageView)bar).getDrawable()).getBitmap();
                int[] fixed={settings.total,settings.tiktok,settings.coupon};
                int expected=settings.gaugeColor(data.values()[row],data.goals()[row],data.month,System.currentTimeMillis(),fixed[row]);
                float progress=RevenueMath.progress(data.values()[row],data.goals()[row]);
                if(progress>0)ok(fill.getPixel(0,0)==expected,"actual native gauge pixel uses daily pace palette");
                ok(result[0].gauge<=48,"v0.1.4 gauge keeps its original height limit");
            }
        });
        android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(tree[0].getWidth(),tree[0].getHeight(),android.graphics.Bitmap.Config.ARGB_8888);
        runOnMainSync(()->tree[0].draw(new android.graphics.Canvas(bitmap)));
        png(bitmap,name);
        android.view.View panel=tree[0].findViewById(R.id.widget_panel);
        int background=(settings.background&0xffffff)|(Math.round(settings.opacity*2.55f)<<24);
        ok(bitmap.getPixel(panel.getLeft(),panel.getTop())==background,"top corner has background without widget border");
        ok(bitmap.getPixel(panel.getRight()-1,panel.getBottom()-1)==background,"bottom corner has background without widget border");
        // A width check alone can pass when an unattached TextView has not drawn its text.
        for(int col:new int[]{1,3,4}){
            android.widget.TextView text=tree[0].findViewById(NativeWidgetViews.TEXT[0][col]);
            int[] origin=new int[2],position=new int[2];tree[0].getLocationInWindow(origin);text.getLocationInWindow(position);
            int ink=0;for(int y=Math.max(0,position[1]-origin[1]);y<Math.min(bitmap.getHeight(),position[1]-origin[1]+text.getHeight());y++)
                for(int x=Math.max(0,position[0]-origin[0]+2);x<Math.min(bitmap.getWidth(),position[0]-origin[0]+text.getWidth()-2);x++){
                    int color=bitmap.getPixel(x,y);if(android.graphics.Color.alpha(color)>0&&Math.abs(android.graphics.Color.red(color)-android.graphics.Color.red(settings.text))<50&&Math.abs(android.graphics.Color.green(color)-android.graphics.Color.green(settings.text))<50&&Math.abs(android.graphics.Color.blue(color)-android.graphics.Color.blue(settings.text))<50)ink++;
                }
            ok(ink>4,"current goal percent glyphs are actually drawn: "+name+" col="+col+" ink="+ink+" bounds="+java.util.Arrays.toString(position)+" size="+text.getWidth()+"x"+text.getHeight()+" baseline="+text.getBaseline()+" scroll="+text.getScrollX()+","+text.getScrollY());
        }
        png(bitmap,name);
    }
    java.util.concurrent.ExecutorService executor()throws Exception {
        java.lang.reflect.Field field=RevenueUpdate.class.getDeclaredField("EXECUTOR");field.setAccessible(true);
        return (java.util.concurrent.ExecutorService)field.get(null);
    }
    @Override public void onCreate(Bundle args){super.onCreate(args);this.args=args;start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            c=getTargetContext();output=new File(c.getExternalFilesDir(null),"verification");output.mkdirs();
            if("offline".equals(args.getString("mode"))){
                android.net.ConnectivityManager connectivity=c.getSystemService(android.net.ConnectivityManager.class);
                for(int n=0;n<100&&connectivity.getActiveNetwork()!=null;n++)Thread.sleep(200);
                ok(connectivity.getActiveNetwork()==null,"offline fixture has no active network");
                for(int n=0;n<50&&RevenueUpdate.busy();n++)Thread.sleep(100);
                ok(!RevenueUpdate.busy(),"previous update finished before offline snapshot");
                RevenueStore.Data before=RevenueStore.read(c);CountDownLatch latch=new CountDownLatch(1);
                RevenueUpdate.start(c,latch::countDown);ok(latch.await(35,TimeUnit.SECONDS),"offline request finished");
                RevenueStore.Data after=RevenueStore.read(c);ok(after.coupon==before.coupon&&after.couponAt==before.couponAt,"offline keeps good amount and timestamp");ok(!after.error.isEmpty(),"offline error is visible");
                ok("OFFLINE".equals(after.errorCode),"offline failure is classified");
                ok("OFFLINE".equals(RevenueStore.prefs(c).getString("lastFailureCode","")),"failure history retained locally");
                ok(RevenueStore.prefs(c).getLong("lastFailureAt",0)>0,"failure history has a timestamp outside the widget");
                Activity offlineHost=startActivitySync(new Intent(c,SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                waitForIdleSync();
                for(int n=0;n<100&&RevenueUpdate.busy();n++)Thread.sleep(100);
                ActivityMonitor offlineMonitor=addMonitor(RefreshActivity.class.getName(),null,false);
                runOnMainSync(()->{try{RefreshActivity.tapIntent(c).send();}catch(android.app.PendingIntent.CanceledException e){throw new AssertionError(e);}});
                Activity refresh=waitForMonitorWithTimeout(offlineMonitor,5000);
                ok(refresh!=null,"offline tap path opens foreground refresh");
                for(int n=0;n<100&&!refresh.isDestroyed();n++)Thread.sleep(100);
                ok(refresh.isDestroyed(),"offline refresh closes automatically");
                RevenueStore.Data tapped=RevenueStore.read(c);
                ok(tapped.coupon==before.coupon&&tapped.couponAt==before.couponAt,"offline foreground refresh keeps good cache");
                ok("OFFLINE".equals(tapped.errorCode),"offline foreground refresh reports reason");
                removeMonitor(offlineMonitor);runOnMainSync(offlineHost::finish);
                png(WidgetRenderer.render(c,tapped,new DisplaySettings(),360,126).bitmap,"native-offline-retained");
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
            RevenueStore.Data nativeData=RevenueStore.read(c);nativeData.tiktok=1000000;nativeData.coupon=500000;nativeData.goalTotal=1000000;nativeData.goalTiktok=9999999;nativeData.goalCoupon=9999999;
            nativeData.importAt=System.currentTimeMillis();nativeData.couponAt=nativeData.importAt;nativeData.loading=false;nativeData.error="";
            nativeFixture=startActivitySync(new Intent(c,SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            for(int width:new int[]{280,320,360,400})for(int height:new int[]{100,196})for(int dpi:new int[]{160,320})
                nativeTextLayout(nativeData,new DisplaySettings(),width,height,dpi,"native-text-"+width+"x"+height+"-dpi"+dpi);
            DisplaySettings nativeWhite=new DisplaySettings();nativeWhite.background=android.graphics.Color.WHITE;nativeWhite.text=android.graphics.Color.BLACK;
            nativeTextLayout(nativeData,nativeWhite,360,126,320,"native-text-white");
            DisplaySettings extremeNative=new DisplaySettings();extremeNative.font=24;extremeNative.left=32;extremeNative.right=32;extremeNative.top=24;extremeNative.bottom=24;extremeNative.gap=18;extremeNative.gauge=12;
            nativeTextLayout(nativeData,extremeNative,280,100,320,"native-text-extreme-settings");
            nativeTextLayout(nativeData,new DisplaySettings(),360,300,320,"native-text-tall-card");
            ok(measuredGaugeHeights.get("native-text-360x100-dpi320")<measuredGaugeHeights.get("native-text-360x196-dpi320"),"gauge thickens when card height increases");
            ok(measuredGaugeHeights.get("native-text-360x196-dpi320")<measuredGaugeHeights.get("native-text-tall-card"),"gauge continues to scale above two-row height");
            ok(measuredGaugeHeights.get("native-text-white")==4,"default-size gauge restores v0.1.4 four-dp thickness");
            ok(NativeWidgetViews.create(c,nativeData,new DisplaySettings(),360,196).font>NativeWidgetViews.create(c,nativeData,new DisplaySettings(),360,100).font,"larger card increases font within safe column limits");
            // Screenshot and explicit palette assertions for green / yellow / red in the same card.
            java.util.Calendar date=java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Tokyo"));
            int days=date.getActualMaximum(java.util.Calendar.DAY_OF_MONTH),today=date.get(java.util.Calendar.DAY_OF_MONTH);
            RevenueStore.Data paceData=RevenueStore.read(c);paceData.tiktok=today*75000L;paceData.coupon=today*25000L;
            paceData.goalTotal=days*50000L;paceData.goalTiktok=days*100000L;paceData.goalCoupon=days*100000L;
            paceData.month=month;paceData.importAt=System.currentTimeMillis();paceData.couponAt=paceData.importAt;paceData.error="";paceData.loading=false;
            DisplaySettings palette=new DisplaySettings();
            ok(palette.paceColors,"new and saved v0.1.4 settings use progress colors by default");
            ok(palette.gaugeColor(paceData.values()[0],paceData.goalTotal,month,System.currentTimeMillis(),palette.total)==0xff31d158,"site green at or above daily target");
            ok(palette.gaugeColor(paceData.tiktok,paceData.goalTiktok,month,System.currentTimeMillis(),palette.tiktok)==0xffffd43b,"site yellow between half and daily target");
            ok(palette.gaugeColor(paceData.coupon,paceData.goalCoupon,month,System.currentTimeMillis(),palette.coupon)==0xffff4545,"site red below half daily target");
            nativeTextLayout(paceData,palette,360,126,320,"native-pace-three-colors");
            // The percentage stays cumulative, while the colored fill restarts at 15%.
            RevenueStore.Data secondLap=RevenueStore.read(c);
            secondLap.month=month;secondLap.tiktok=1150000;secondLap.coupon=0;
            secondLap.goalTotal=1000000;secondLap.goalTiktok=1000000;secondLap.goalCoupon=1000000;
            secondLap.importAt=System.currentTimeMillis();secondLap.couponAt=secondLap.importAt;
            secondLap.loading=false;secondLap.error="";
            ok(RevenueMath.percent(secondLap.values()[0],secondLap.goals()[0]).equals("115.00%"),"native text keeps cumulative 115%");
            ok(RevenueMath.progress(secondLap.values()[0],secondLap.goals()[0])==.15f,"native gauge starts second lap at 15%");
            ok(palette.gaugeColor(secondLap.values()[0],secondLap.goals()[0],month,System.currentTimeMillis(),palette.total)==DisplaySettings.OVER_GOAL_GREEN,"over 100% uses dark green");
            nativeTextLayout(secondLap,palette,360,126,320,"native-second-lap-115-percent");
            secondLap.tiktok=2000000;
            ok(RevenueMath.percent(secondLap.values()[0],secondLap.goals()[0]).equals("200.00%"),"native text keeps cumulative 200%");
            ok(RevenueMath.progress(secondLap.values()[0],secondLap.goals()[0])==1f,"completed 200% lap shows full gauge");
            secondLap.month="2000-01";
            ok(palette.gaugeColor(secondLap.values()[0],secondLap.goals()[0],secondLap.month,System.currentTimeMillis(),palette.total)==0xff888888,"stale overflow does not falsely display dark green");
            palette.paceColors=false;palette.total=0xff112233;palette.tiktok=0xff445566;palette.coupon=0xff778899;
            nativeTextLayout(paceData,palette,360,126,320,"native-pace-off-custom");
            palette.save(RevenueStore.prefs(c));ok(!DisplaySettings.load(RevenueStore.prefs(c)).paceColors,"progress-color preference persists");
            palette.paceColors=true;palette.save(RevenueStore.prefs(c));ok(DisplaySettings.load(RevenueStore.prefs(c)).paceColors,"progress colors can be reenabled without losing fixed colors");
            ok(DisplaySettings.load(RevenueStore.prefs(c)).total==0xff112233,"fixed custom color retained after toggling progress colors");
            new DisplaySettings().save(RevenueStore.prefs(c));
            paceData.month="2000-01";
            ok(palette.gaugeColor(paceData.tiktok,paceData.goalTiktok,paceData.month,System.currentTimeMillis(),palette.tiktok)==0xff888888,"stale month has neutral color instead of false achievement");
            nativeTextLayout(paceData,palette,360,126,320,"native-pace-stale-month");
            runOnMainSync(nativeFixture::finish);nativeFixture=null;
            s.background=android.graphics.Color.WHITE;s.text=android.graphics.Color.BLACK;png(WidgetRenderer.render(c,d,s,360,126).bitmap,"native-white");
            s.opacity=30;s.font=24;s.left=32;s.right=32;s.top=24;s.bottom=24;s.gap=18;s.gauge=12;
            WidgetRenderer.Render adjusted=WidgetRenderer.render(c,d,s,280,100);ok(adjusted.adjusted,"unsafe settings adjusted");png(adjusted.bitmap,"native-extreme-settings");
            s.save(RevenueStore.prefs(c));ok(DisplaySettings.load(RevenueStore.prefs(c)).font==24,"settings persist");
            new DisplaySettings().save(RevenueStore.prefs(c));
            RevenueStore.prefs(c).edit().remove("importMonth").remove("tiktok").remove("importAt").apply();
            CountDownLatch fetched=new CountDownLatch(1);RevenueUpdate.start(c,fetched::countDown);ok(fetched.await(35,TimeUnit.SECONDS),"fetch completed");
            RevenueStore.Data live=RevenueStore.read(c);ok(live.coupon>=0&&live.error.isEmpty(),"production API received");
            RevenueStore.saveFailure(c,"TIMEOUT");RevenueStore.Data timedOut=RevenueStore.read(c);
            ok(timedOut.coupon==live.coupon&&timedOut.couponAt==live.couponAt,"timeout preserves last good amount and timestamp");
            ok(WidgetRenderer.footer(timedOut).startsWith("通信時間切れ"),"widget exposes controlled failure reason");
            RevenueStore.saveCoupon(c,month,live.coupon);RevenueStore.Data restored=RevenueStore.read(c);
            ok(restored.error.isEmpty()&&restored.errorCode.isEmpty(),"successful recovery clears current failure");
            ok("TIMEOUT".equals(RevenueStore.prefs(c).getString("lastFailureCode","")),"recovery preserves last failure for diagnosis");ok(live.tiktok==-1&&live.values()[0]==-1,"missing TikTok not zero");
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
            ArrayList<android.util.SizeF> exactSizes=new ArrayList<>();exactSizes.add(new android.util.SizeF(900,100));
            dimensions.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES,exactSizes);manager.updateAppWidgetOptions(id,dimensions);
            RevenueWidget.renderAll(c);waitForIdleSync();Thread.sleep(700);png(getUiAutomation().takeScreenshot(),"native-widget-host-live");
            runOnMainSync(()->{
                android.widget.TextView label=view[0].findViewById(R.id.total_label);
                ok(label!=null,"production widget uses native TextView");
                ok(label.getTextSize()/activity.getResources().getDisplayMetrics().density>=9,"oversized reported layout cannot shrink native text");
                ok(label.getLeft()>=0&&label.getPaint().measureText("Total")<=label.getWidth(),"launcher host title not clipped");
            });
            for(int n=0;n<350&&RevenueUpdate.busy();n++)Thread.sleep(100);
            ok(!RevenueUpdate.busy(),"earlier requests finish before foreground tap test");
            java.util.concurrent.ExecutorService executor=executor();
            CountDownLatch blockEntered=new CountDownLatch(1),releaseBlock=new CountDownLatch(1);
            executor.execute(()->{blockEntered.countDown();try{releaseBlock.await(40,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}});
            ok(blockEntered.await(2,TimeUnit.SECONDS),"controlled network executor blocker active");
            long beforeDuplicate=RevenueStore.read(c).couponAt;
            CountDownLatch firstDone=new CountDownLatch(1),secondDone=new CountDownLatch(1);
            RevenueUpdate.start(c,firstDone::countDown);RevenueUpdate.start(c,secondDone::countDown);
            ok(RevenueUpdate.busy(),"coalesced requests share in-flight update");
            ok(!secondDone.await(100,TimeUnit.MILLISECONDS),"second caller waits for actual completion");
            ok(RevenueStore.read(c).couponAt==beforeDuplicate,"no premature cache update while waiting");
            releaseBlock.countDown();
            ok(firstDone.await(35,TimeUnit.SECONDS)&&secondDone.await(1,TimeUnit.SECONDS),"both callers notified after fetch completes");
            ok(RevenueStore.read(c).couponAt>beforeDuplicate&&!RevenueUpdate.busy(),"coalesced fetch completes and releases gate");

            android.app.job.JobScheduler scheduler=c.getSystemService(android.app.job.JobScheduler.class);
            scheduler.cancel(RevenueJob.MANUAL);
            scheduler.schedule(new android.app.job.JobInfo.Builder(RevenueJob.MANUAL,new ComponentName(c,RevenueJob.class)).setMinimumLatency(60000).setOverrideDeadline(120000).build());
            ok(scheduler.getPendingJob(RevenueJob.MANUAL)!=null,"delayed background job fixture queued");
            CountDownLatch tapBlockEntered=new CountDownLatch(1),releaseTap=new CountDownLatch(1);
            executor.execute(()->{tapBlockEntered.countDown();try{releaseTap.await(120,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}});
            ok(tapBlockEntered.await(2,TimeUnit.SECONDS),"tap executor fixture active");
            ActivityMonitor tapMonitor=addMonitor(RefreshActivity.class.getName(),null,false);
            final android.view.View touchSurface=((android.view.ViewGroup)activity.findViewById(android.R.id.content)).getChildAt(0);
            final java.util.concurrent.atomic.AtomicInteger touches=new java.util.concurrent.atomic.AtomicInteger();
            runOnMainSync(()->{touchSurface.setClickable(true);touchSurface.setOnTouchListener((v,event)->{if(event.getAction()==android.view.MotionEvent.ACTION_UP)touches.incrementAndGet();return true;});});
            pointerTap(40,400);
            ok(touches.get()==1,"host pointer fixture receives input before refresh");touches.set(0);
            long beforeTap=RevenueStore.read(c).couponAt;
            long tapTestStarted=android.os.SystemClock.uptimeMillis();
            runOnMainSync(()->{ok(view[0].findViewById(R.id.widget_root)!=null,"production RemoteViews applied");ok(view[0].findViewById(R.id.widget_root).performClick(),"initial production widget tap is handled");});
            Activity refresh=waitForMonitorWithTimeout(tapMonitor,5000);
            ok(refresh!=null,"actual widget PendingIntent opens foreground refresh activity");
            waitForIdleSync();
            Thread.sleep(600);
            android.view.WindowManager.LayoutParams quiet=refresh.getWindow().getAttributes();
            ok(quiet.alpha==0f,"refresh window fully transparent rather than just its content");
            ok(quiet.width==1&&quiet.height==1,"refresh window does not cover the widget or home icons");
            ok((quiet.flags&android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)!=0,"refresh window cannot intercept home touches");
            ok((quiet.flags&android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)!=0,"refresh window does not steal key input");
            ok(!refresh.hasWindowFocus(),"quiet refresh does not take focus from widget host");
            ok(activity.hasWindowFocus(),"behind launch preserves the user's host focus");
            // Starting a separate task may pause the host Activity even though its
            // window remains visible. Verify compositor pixels and routed input
            // instead of mistaking Activity focus for launcher touch delivery.
            final android.graphics.Bitmap[] expectedWidget={null};final int[] origin=new int[2];
            runOnMainSync(()->{
                view[0].getLocationOnScreen(origin);
                expectedWidget[0]=android.graphics.Bitmap.createBitmap(view[0].getWidth(),view[0].getHeight(),android.graphics.Bitmap.Config.ARGB_8888);
                view[0].draw(new android.graphics.Canvas(expectedWidget[0]));
            });
            android.graphics.Bitmap display=getUiAutomation().takeScreenshot();
            int compared=0,matching=0;
            for(int y=0;y<expectedWidget[0].getHeight();y+=3)for(int x=0;x<expectedWidget[0].getWidth();x+=3){
                int expected=expectedWidget[0].getPixel(x,y);
                if(android.graphics.Color.alpha(expected)<255)continue;
                int actual=display.getPixel(origin[0]+x,origin[1]+y);compared++;
                if(Math.abs(android.graphics.Color.red(expected)-android.graphics.Color.red(actual))<30
                    &&Math.abs(android.graphics.Color.green(expected)-android.graphics.Color.green(actual))<30
                    &&Math.abs(android.graphics.Color.blue(expected)-android.graphics.Color.blue(actual))<30)matching++;
            }
            ok(compared>1000&&matching>compared*.98,"real widget remains visible without an obscuring float");
            final String[] footer={null};
            runOnMainSync(()->footer[0]=((android.widget.TextView)view[0].findViewById(R.id.widget_footer)).getText().toString());
            ok("更新中".equals(footer[0]),"widget itself displays updating status");
            ok(RevenueUpdate.busy(),"widget tap immediately starts direct fetch without a scheduled job");
            ok(scheduler.getPendingJob(RevenueJob.MANUAL)==null,"foreground tap cancels previously queued manual job");
            ok(refresh.getTaskId()!=activity.getTaskId(),"refresh uses a separate task from settings");
            android.content.pm.ActivityInfo info=c.getPackageManager().getActivityInfo(new ComponentName(c,RefreshActivity.class),0);
            ok(!info.exported,"refresh activity is private");
            ok((info.flags&android.content.pm.ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS)!=0,"refresh excluded from recent apps");
            ok(RefreshActivity.tapIntent(c).isActivity()&&RefreshActivity.tapIntent(c).isImmutable(),"widget tap uses immutable activity PendingIntent");
            png(getUiAutomation().takeScreenshot(),"native-tap-updating");
            pointerTap(40,400);
            ok(touches.get()==1,"underlying home host receives actual touches during refresh; observed="+touches.get()+"; focus="+activity.hasWindowFocus());
            runOnMainSync(()->touchSurface.setOnTouchListener(null));
            removeMonitor(tapMonitor);
            ActivityMonitor repeatMonitor=addMonitor(RefreshActivity.class.getName(),null,false);
            // Use the actual RemoteViews click path again; a raw PendingIntent
            // send does not carry the launcher's background-start options.
            runOnMainSync(()->ok(view[0].findViewById(R.id.widget_root).performClick(),"repeated production widget tap is handled"));
            Activity repeated=waitForMonitorWithTimeout(repeatMonitor,500);
            // Android may suppress an additional behind launch while the first
            // request is active. Ignoring that tap is valid duplicate prevention.
            waitForIdleSync();Thread.sleep(150);
            // Android may replace/destroy an already-background Activity when
            // another task launch arrives. The application-scoped fetch must
            // remain active, keep the cache unchanged until completion, and
            // deliver the result regardless of that UI lifecycle.
            ok(RevenueUpdate.busy()&&RevenueStore.read(c).couponAt==beforeTap,"repeated tap retains the single in-flight fetch and last-good cache; elapsed="+(android.os.SystemClock.uptimeMillis()-tapTestStarted));
            ok(activity.hasWindowFocus()&&(repeated==null||!repeated.hasWindowFocus()),"repeated behind launch also preserves host focus");
            CountDownLatch tapDone=new CountDownLatch(1);RevenueUpdate.start(c,tapDone::countDown);
            ok(!tapDone.await(100,TimeUnit.MILLISECONDS),"observer waits for same tap request");
            releaseTap.countDown();
            ok(tapDone.await(35,TimeUnit.SECONDS),"quiet foreground tap fetch completes");
            for(int n=0;n<100&&!refresh.isDestroyed();n++)Thread.sleep(100);
            ok(refresh.isDestroyed(),"refresh task closes automatically after fetch");
            if(repeated!=null)for(int n=0;n<100&&!repeated.isDestroyed();n++)Thread.sleep(100);
            ok(repeated==null||repeated.isDestroyed(),"any coalesced behind task also closes after fetch");
            ok(!activity.isFinishing()&&!activity.isDestroyed(),"settings host task survives refresh completion");
            ok(RevenueStore.read(c).couponAt>beforeTap,"normal widget tap updates API timestamp through foreground path");
            ok(RevenueStore.read(c).error.isEmpty(),"foreground refresh finishes without error");
            ok(scheduler.getPendingJob(RevenueJob.MANUAL)==null,"tap completion leaves no duplicate manual job");
            waitForIdleSync();Thread.sleep(200);png(getUiAutomation().takeScreenshot(),"native-tap-complete");
            removeMonitor(repeatMonitor);
            ActivityMonitor nextMonitor=addMonitor(RefreshActivity.class.getName(),null,false);
            // A tap after completion must start a fresh request, not be stuck on
            // the retired behind task. Exercise the production click path again.
            long beforeNextTap=RevenueStore.read(c).couponAt;
            runOnMainSync(()->ok(view[0].findViewById(R.id.widget_root).performClick(),"next production widget tap is handled"));
            Activity next=waitForMonitorWithTimeout(nextMonitor,5000);
            ok(next!=null&&next!=refresh,"next normal widget tap opens a fresh behind task");
            CountDownLatch nextDone=new CountDownLatch(1);RevenueUpdate.start(c,nextDone::countDown);
            ok(nextDone.await(35,TimeUnit.SECONDS),"next normal widget tap completes");
            for(int n=0;n<100&&!next.isDestroyed();n++)Thread.sleep(100);
            ok(next.isDestroyed()&&RevenueStore.read(c).couponAt>beforeNextTap&&RevenueStore.read(c).error.isEmpty(),"fresh widget tap advances API timestamp and closes");
            ok(activity.hasWindowFocus(),"home host retains focus after successive completed updates");
            removeMonitor(nextMonitor);
            dimensions.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,280);dimensions.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,100);exactSizes.clear();exactSizes.add(new android.util.SizeF(280,100));dimensions.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES,exactSizes);manager.updateAppWidgetOptions(id,dimensions);
            runOnMainSync(()->{android.view.ViewGroup.LayoutParams lp=view[0].getLayoutParams();lp.width=Math.round(280*activity.getResources().getDisplayMetrics().density);lp.height=Math.round(100*activity.getResources().getDisplayMetrics().density);view[0].setLayoutParams(lp);});
            Thread.sleep(700);png(getUiAutomation().takeScreenshot(),"native-widget-host-resized");host.stopListening();runOnMainSync(activity::finish);
            result.putString("stream","PASS "+checks+" native Android checks; Coupon "+live.coupon+"; screenshots "+output);
            finish(ActivityResult.OK,result);
        }catch(Throwable e){result.putString("stream","FAIL "+checks+" "+android.util.Log.getStackTraceString(e));finish(ActivityResult.FAIL,result);}
    }
    static final class ActivityResult{static final int OK=-1,FAIL=0;}
}
