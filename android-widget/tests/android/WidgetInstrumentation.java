package com.aruno.foolquest.widget.next;

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
    android.widget.Button button(android.view.View view,String text){
        if(view instanceof android.widget.Button&&((android.widget.Button)view).getText().toString().equals(text))return (android.widget.Button)view;
        if(view instanceof android.view.ViewGroup){
            android.view.ViewGroup group=(android.view.ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++){android.widget.Button found=button(group.getChildAt(i),text);if(found!=null)return found;}
        }
        return null;
    }
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
            ok(Math.abs(firstRow.getHeight()-lastRow.getHeight())<=2,"all three rows receive the actual host height equally");
            ok(lastRow.getBottom()<=tree[0].findViewById(R.id.widget_content).getHeight()+1,"last row stays inside content bounds");
            android.widget.TextView footer=tree[0].findViewById(R.id.widget_footer);
            ok(!footer.getText().toString().matches(".*[0-9]{2}:[0-9]{2}.*"),"widget never displays update times");
            if(data.importAt>0&&data.couponAt>0&&!data.loading&&data.error.isEmpty())ok(footer.getVisibility()==android.view.View.GONE,"normal display has no footer");
            android.view.View secondRow=tree[0].findViewById(R.id.row_tiktok);
            ok(secondRow.getTop()-firstRow.getBottom()==Math.round(result[0].gap*density),"first exact spacer keeps the configured gap");
            ok(lastRow.getTop()-secondRow.getBottom()==Math.round(result[0].gap*density),"second exact spacer keeps the configured gap");
            for(int row=0;row<3;row++){
                android.view.ViewGroup holder=tree[0].findViewById(NativeWidgetViews.HOLDER[row]);
                android.view.View bar=holder.findViewById(R.id.gauge_image);
                android.widget.ImageView lapLabel=holder.findViewById(R.id.lap_label);
                long lap=RevenueMath.lapCount(data.values()[row],data.goals()[row]);
                ok(lapLabel.getContentDescription().toString().equals(lap>=2?"×"+lap:""),"lap suffix correct for every row");
                ok(Math.abs(lapLabel.getWidth()/density-GaugePalette.LAP_LABEL_WIDTH_DP)<1,"every row reserves the same suffix width");
                ok(bar.getHeight()>0&&bar.getHeight()<tree[0].findViewById(NativeWidgetViews.ROW[row]).getHeight(),"gauge has visible height and fits its row");
                ok(bar.getHeight()/density>=result[0].gauge-1,"native image retains at least the requested bar height");
                android.widget.TextView amount=tree[0].findViewById(NativeWidgetViews.TEXT[row][1]);
                int[] textPos=new int[2],barPos=new int[2];amount.getLocationInWindow(textPos);bar.getLocationInWindow(barPos);
                ok(barPos[1]>=textPos[1]+amount.getHeight(),"adaptive gauge does not overlap numbers");
                if(row==0)measuredGaugeHeights.put(name,bar.getHeight()/density);
                ok(bar instanceof android.widget.ImageView,"v0.1.4 gauge has no embedded target text");
                android.graphics.Bitmap fill=((android.graphics.drawable.BitmapDrawable)((android.widget.ImageView)bar).getDrawable()).getBitmap();
                ok(fill.getHeight()==Math.round((result[0].gauge+2*GaugePalette.MARKER_OVERHANG_DP)*density),
                    "gauge bitmap includes precisely 2dp of top and bottom marker overhang");
                int[] fixed={settings.total,settings.tiktok,settings.coupon};
                int expected=settings.gaugeColor(data.values()[row],data.goals()[row],data.month,System.currentTimeMillis(),fixed[row]);
                float progress=RevenueMath.progress(data.values()[row],data.goals()[row]);
                int cy=fill.getHeight()/2;
                boolean layered=lap>=2&&settings.isSecondLap(data.values()[row],data.goals()[row],data.month,System.currentTimeMillis());
                if(progress>0){
                    int first=layered?GaugePalette.currentColor(lap,settings.paceAchieved,settings.secondLap):expected;
                    if(layered&&GaugeDrawing.rainbowLap(lap))ok(android.graphics.Color.red(fill.getPixel(0,cy))>240&&android.graphics.Color.green(fill.getPixel(0,cy))<130,"third-lap rainbow starts with red");
                    else ok(fill.getPixel(0,cy)==first,"native gauge uses the active lap color");
                }
                if(layered){
                    int near=Math.min(fill.getWidth()-1,Math.max(0,(int)(fill.getWidth()*Math.max(0,progress-.1f))));
                    int right=fill.getWidth()-2;
                    int base=GaugePalette.previousColor(lap,settings.paceAchieved,settings.secondLap);
                    if(progress<.9f&&!GaugeDrawing.rainbowLap(lap-1))ok(fill.getPixel(right,cy)==base,"full previous lap remains visible");
                    if(GaugePalette.hasMarker(lap,progress)){
                        int edge=Math.round(fill.getWidth()*progress);
                        boolean white=false;
                        for(int dx=-2;dx<=2;dx++){
                            int x=Math.max(0,Math.min(fill.getWidth()-1,edge+dx));
                            if(fill.getPixel(x,0)==GaugePalette.MARKER_WHITE)white=true;
                        }
                        ok(white,"white 2dp marker protrudes above the active overlay");
                    }else ok(lap>=2&&progress==1f,"exact completed lap has no marker");
                }
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
                    int color=bitmap.getPixel(x,y);
                    // At 160dpi, small antialiased glyphs may have no near-solid
                    // white pixels. Count visible foreground contrast instead.
                    int fg=Math.abs(android.graphics.Color.red(color)-android.graphics.Color.red(settings.text))+Math.abs(android.graphics.Color.green(color)-android.graphics.Color.green(settings.text))+Math.abs(android.graphics.Color.blue(color)-android.graphics.Color.blue(settings.text));
                    int bg=Math.abs(android.graphics.Color.red(color)-android.graphics.Color.red(settings.background))+Math.abs(android.graphics.Color.green(color)-android.graphics.Color.green(settings.background))+Math.abs(android.graphics.Color.blue(color)-android.graphics.Color.blue(settings.background));
                    if(android.graphics.Color.alpha(color)>0&&bg>80&&fg<bg)ink++;
                }
            ok(ink>4,"current goal percent glyphs are actually drawn: "+name+" col="+col+" ink="+ink+" bounds="+java.util.Arrays.toString(position)+" size="+text.getWidth()+"x"+text.getHeight()+" baseline="+text.getBaseline()+" scroll="+text.getScrollX()+","+text.getScrollY());
        }
        png(bitmap,name);
    }
    void pinnedSettingsPreview()throws Exception {
        waitForIdleSync();Thread.sleep(150);
        final int[] before=new int[2],after=new int[2];
        runOnMainSync(()->{
            android.view.View root=nativeFixture.getWindow().getDecorView();
            android.view.View preview=root.findViewWithTag("settings-preview");
            android.widget.ScrollView controls=root.findViewWithTag("settings-controls");
            ok(preview!=null&&controls!=null,"preview is separate from scrolling controls");
            preview.getLocationOnScreen(before);controls.fullScroll(android.view.View.FOCUS_DOWN);
        });
        waitForIdleSync();Thread.sleep(150);
        runOnMainSync(()->{
            android.view.View root=nativeFixture.getWindow().getDecorView();
            android.view.View preview=root.findViewWithTag("settings-preview");
            android.widget.ScrollView controls=root.findViewWithTag("settings-controls");
            preview.getLocationOnScreen(after);
            ok(controls.getScrollY()>0,"controls really scrolled");
            ok(java.util.Arrays.equals(before,after),"preview remains fixed after scrolling to the bottom");
            ok(button(root,"保存")!=null,"save stays accessible above the scroll area");
        });
        png(getUiAutomation().takeScreenshot(),"settings-pinned-preview-scrolled");
    }
    void constrainedHost(RevenueStore.Data data,DisplaySettings settings,int hint,int actual)throws Exception {
        final android.view.View[] tree={null};float density=c.getResources().getDisplayMetrics().density;
        runOnMainSync(()->{
            tree[0]=NativeWidgetViews.create(c,data,settings,360,hint).views.apply(c,null);
            nativeFixture.setContentView(tree[0],new android.view.ViewGroup.LayoutParams(Math.round(360*density),Math.round(actual*density)));
        });
        waitForIdleSync();Thread.sleep(120);
        runOnMainSync(()->{
            android.view.View content=tree[0].findViewById(R.id.widget_content);
            for(int i=0;i<3;i++){
                android.view.View row=tree[0].findViewById(NativeWidgetViews.ROW[i]);
                android.view.View holder=tree[0].findViewById(NativeWidgetViews.HOLDER[i]);
                android.view.View bar=holder.findViewById(R.id.gauge_image);
                ok(row.getTop()>=0&&row.getBottom()<=content.getHeight(),"short host retains all row bounds");
                ok(bar.getHeight()>0,"short host retains every gauge including Coupon");
                int[] a=new int[2],b=new int[2];bar.getLocationInWindow(a);row.getLocationInWindow(b);
                ok(a[1]+bar.getHeight()<=b[1]+row.getHeight()+1,"gauge remains inside its real row");
            }
        });
        png(getUiAutomation().takeScreenshot(),"host-hint-"+hint+"-actual-"+actual);
    }
    void rainbowThirdLap()throws Exception {
        ok(!GaugeDrawing.rainbowLap(1)&&!GaugeDrawing.rainbowLap(2)&&GaugeDrawing.rainbowLap(3)&&!GaugeDrawing.rainbowLap(4),"rainbow is the third lap, not the third metric");
        for(float progress:new float[]{.1162f,.5f,1f}){
            android.graphics.Bitmap b=NativeWidgetViews.gauge(c,0xffffffff,GaugePalette.SECOND_LIME,1f,
                GaugePalette.FIRST_GREEN,progress,progress<1f,14,300,14,false,true);
            int width=Math.round(b.getWidth()*progress),y=b.getHeight()/2;
            java.util.Set<Integer> colors=new java.util.HashSet<>();
            for(float x:new float[]{.08f,.25f,.42f,.58f,.75f,.92f})colors.add(b.getPixel(Math.max(0,Math.min(b.getWidth()-1,Math.round(width*x))),y));
            ok(colors.size()>=5,"the entire rainbow fits even the short 11.62-percent overlay");
            if(progress<1f)ok(b.getPixel(b.getWidth()-2,y)==GaugePalette.SECOND_LIME,"completed second lap remains lime behind rainbow");
            png(b,"third-lap-rainbow-"+Math.round(progress*10000));
        }
    }
    void gaugeAnnotations()throws Exception {
        android.content.res.Configuration config=new android.content.res.Configuration(c.getResources().getConfiguration());config.densityDpi=320;
        Context host=c.createConfigurationContext(config);float density=host.getResources().getDisplayMetrics().density;
        for(float progress:new float[]{0f,.1188f,.2f,.4f,.6f,.8f,.8859f,.5f,1f}){
            android.graphics.Bitmap b=NativeWidgetViews.gauge(host,0xffffffff,GaugePalette.FIRST_GREEN,1f,
                GaugePalette.SECOND_LIME,progress,progress>0&&progress<1,16,300,13);
            int top=Math.round(GaugePalette.MARKER_OVERHANG_DP*density),bottom=top+Math.round(16*density);
            for(int percent:new int[]{20,40,60,80}){
                int x=Math.round(300*density*percent/100f);
                if(progress>percent/100f && progress<1f){
                    ok(b.getPixel(x,top)==GaugePalette.MARKER_WHITE,"reached divider is white: "+progress+" / "+percent);
                    ok(android.graphics.Color.alpha(b.getPixel(x,0))==0,"divider never protrudes above gauge");
                    boolean black=false;
                    for(int y=top;y<bottom;y++)for(int xx=Math.max(0,x-Math.round(60*density));xx<x-Math.round(3*density);xx++)
                        black|=b.getPixel(xx,y)==GaugePalette.LABEL_OUTLINE_BLACK;
                    ok(!black,"percent annotation text is removed from gauge");
                }else if(progress!=percent/100f){
                    ok(b.getPixel(x,top)!=GaugePalette.MARKER_WHITE,"unreached or completed lap has no divider");
                }
            }
            png(b,"gauge-divisions-"+Math.round(progress*10000));
        }
        ok(!GaugeDrawing.hasDivider(.2f,20)&&!GaugeDrawing.hasDivider(.4f,40)&&
            !GaugeDrawing.hasDivider(.6f,60)&&!GaugeDrawing.hasDivider(.8f,80),"exact thresholds do not double-draw marker");
        ok(!GaugeDrawing.hasDivider(1f,20),"completed lap suppresses all divisions");
        for(int height:new int[]{4,12,48})for(long lap:new long[]{1,2,3,9}){
            android.graphics.Bitmap label=android.graphics.Bitmap.createBitmap(GaugePalette.LAP_LABEL_WIDTH_DP*2,(height+4)*2,android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas=new android.graphics.Canvas(label);canvas.scale(2,2);
            GaugeDrawing.lapLabel(canvas,lap,GaugePalette.SECOND_LIME,GaugePalette.LAP_LABEL_WIDTH_DP,2,height);
            int minY=label.getHeight(),maxY=-1,minX=label.getWidth();
            for(int y=0;y<label.getHeight();y++)for(int x=0;x<label.getWidth();x++)if(android.graphics.Color.alpha(label.getPixel(x,y))>128){minY=Math.min(minY,y);maxY=Math.max(maxY,y);minX=Math.min(minX,x);}
            if(lap==1)ok(maxY==-1,"first lap leaves reserved suffix empty");
            else {
                ok(Math.abs((maxY-minY+1)/2f-height)<=1,"lap glyph height follows track: "+height+" / "+lap);
                ok(minY>=3&&maxY<=(height+2)*2,"lap glyph vertically aligns with track");
                ok(minX>0,"lap label fits legacy 34dp column while retaining full track height");
            }
            png(label,"lap-label-"+height+"-"+lap);
        }
        android.graphics.Bitmap narrow=NativeWidgetViews.gauge(host,0xffffffff,GaugePalette.FIRST_GREEN,1f,GaugePalette.SECOND_LIME,.8859f,true,16,30,13);
        boolean black=false;for(int y=0;y<narrow.getHeight();y++)for(int x=0;x<narrow.getWidth();x++)black|=narrow.getPixel(x,y)==GaugePalette.LABEL_OUTLINE_BLACK;
        ok(!black,"narrow gauge also has no percent annotations");
        ok(GaugePalette.LAP_LABEL_WIDTH_DP==34,"legacy track width is restored by reserving only 34dp");
        for(int font:new int[]{9,13,24}){
            android.graphics.Bitmap b=NativeWidgetViews.gauge(host,0xffffffff,GaugePalette.FIRST_GREEN,1f,GaugePalette.SECOND_LIME,.5f,true,16,300,font);
            Paint digit=GaugeDrawing.textPaint();digit.setTextSize(font);
            int edge=Math.round(150*density),cap=Math.round(digit.measureText("0")*density);
            int top=Math.round(GaugePalette.MARKER_OVERHANG_DP*density),bottom=top+Math.round(16*density);
            ok(b.getPixel(edge-cap+2,top)==GaugePalette.SECOND_LIME,"upper horizontal cap removed at font "+font);
            ok(b.getPixel(edge-cap+2,bottom-1)==GaugePalette.SECOND_LIME,"lower horizontal cap removed at font "+font);
            ok(b.getPixel(edge,0)==GaugePalette.MARKER_WHITE,"original vertical marker protrudes above track");
            ok(b.getPixel(edge,b.getHeight()-1)==GaugePalette.MARKER_WHITE,"original vertical marker protrudes below track");
            ok(android.graphics.Color.alpha(b.getPixel(edge-cap+2,0))==0,"vertical marker has no upper side stroke");
            ok(android.graphics.Color.alpha(b.getPixel(edge+Math.round(3*density),0))==0,"vertical marker retains original width");
            ok(b.getPixel(edge-cap+2,b.getHeight()/2)==GaugePalette.SECOND_LIME,"track color remains intact");
            png(b,"gauge-vertical-font-"+font);
        }
    }
    java.util.concurrent.ExecutorService executor()throws Exception {
        java.lang.reflect.Field field=RevenueUpdate.class.getDeclaredField("EXECUTOR");field.setAccessible(true);
        return (java.util.concurrent.ExecutorService)field.get(null);
    }

    android.widget.LinearLayout historyRow(android.view.View view,String period) {
        if(view instanceof android.widget.LinearLayout) {
            android.widget.LinearLayout line=(android.widget.LinearLayout)view;
            if(line.getChildCount()==4 && line.getChildAt(0) instanceof android.widget.TextView) {
                if(period.equals(((android.widget.TextView)line.getChildAt(0)).getText().toString()))
                    return line;
            }
        }
        if(view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group=(android.view.ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++){
                android.widget.LinearLayout found=historyRow(group.getChildAt(i),period);
                if(found!=null)return found;
            }
        }
        return null;
    }
    void nativeRevenueChecks() throws Exception {
        String month=RevenueMath.month(System.currentTimeMillis());
        RevenueManager.CsvResult parsed=RevenueManager.parseCsv("ID,チャージポイント\nA,\"1,000\"\nB,-200\n");
        ok(parsed.total==1200 && parsed.count==2,"CSV parser handles quoted values and returns total");
        RevenueManager manager=new RevenueManager(c);
        manager.setCsv(month,1000,"fixture.csv",1);
        manager.addManual(month+"-10",250);
        ok(manager.tiktok(month)==1250,"native manual amount adds to CSV exactly once");
        String id=manager.manual().getJSONObject(manager.manual().length()-1).getString("id");
        manager.updateManual(id,month+"-10",275);
        ok(manager.tiktok(month)==1275,"editing manual entry replaces amount exactly once");
        ok(RevenueStore.read(c).tiktok==1275,"widget receives native value instantly");
        String saved=manager.backup();
        boolean refused=false;
        try{manager.replaceFromJson("{\"format\":\"invalid\",\"version\":1}");}
        catch(Exception expected){refused=true;}
        ok(refused && manager.tiktok(month)==1275,"invalid history migration preserves current records");
        manager.deleteManual(id);
        ok(manager.tiktok(month)==1000,"deleting manual entry restores CSV baseline");
        manager.replaceFromJson(saved);
        ok(manager.tiktok(month)==1275,"valid backup restores manual and CSV history");
        String lastMonth=RevenueMath.date("yyyy-MM",System.currentTimeMillis()-35L*24*60*60*1000);
        RevenueStore.saveCoupon(c,lastMonth,321);
        RevenueStore.saveCoupon(c,month,1000000);
        ok(manager.coupon(lastMonth)==321,"prior month Coupon persists after month refresh");
        ok(new org.json.JSONObject(manager.backup()).getJSONObject("months").getJSONObject(lastMonth).getLong("coupon")==321,
            "Coupon monthly archive is included in user backup");
        Activity home=startActivitySync(new Intent(c,ManagementActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        ok(home!=null,"native revenue management screen launches");
        runOnMainSync(()->{
            ok(button(home.getWindow().getDecorView(),"旧サイトの移行ファイル作成ページ")==null,
                "migration controls are not shown on normal management screen");
            ok(button(home.getWindow().getDecorView(),"バックアップ・復元")!=null,
                "backup recovery remains accessible");
            button(home.getWindow().getDecorView(),"収益履歴・手動追加の編集").performClick();
        });
        waitForIdleSync();
        runOnMainSync(()->{
            android.view.View root=home.getWindow().getDecorView();
            android.widget.LinearLayout headings=historyRow(root,"");
            android.widget.LinearLayout all=historyRow(root,"全期間");
            android.widget.LinearLayout year=historyRow(root,month.substring(0,4)+"年");
            ok(headings!=null&&all!=null&&year!=null,"history uses four aligned columns for labels, all-time and year");
            ok("合計".equals(((android.widget.TextView)headings.getChildAt(1)).getText().toString()),
                "four-column revenue headers preserved");
            ok("TikTok".equals(((android.widget.TextView)headings.getChildAt(2)).getText().toString()),
                "TikTok column preserved");
            ok("Coupon".equals(((android.widget.TextView)headings.getChildAt(3)).getText().toString()),
                "Coupon column preserved");
            ok(historyRow(root,String.valueOf(Integer.parseInt(month.substring(5)))+"月")!=null,
                "current month appears as compact row");
        });
        png(getUiAutomation().takeScreenshot(),"native-revenue-history-4column");
        runOnMainSync(home::finish);
    }

    @Override public void onCreate(Bundle args){super.onCreate(args);this.args=args;start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            c=getTargetContext();output=new File(c.getExternalFilesDir(null),"verification");output.mkdirs();
            gaugeAnnotations();
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
            ok(compact.panelHeight<=196,"reference bitmap stays within its host");
            ok(compact.rowHeight<=compact.layout.rowHeight,"reference row fits its allocation");
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
            pinnedSettingsPreview();
            rainbowThirdLap();
            constrainedHost(nativeData,new DisplaySettings(),196,100);
            constrainedHost(nativeData,new DisplaySettings(),300,126);
            for(int width:new int[]{280,320,360,400})for(int height:new int[]{65,100,126,196})for(int dpi:new int[]{160,320})
                nativeTextLayout(nativeData,new DisplaySettings(),width,height,dpi,"native-text-"+width+"x"+height+"-dpi"+dpi);
            DisplaySettings relativeFont=new DisplaySettings();
            float centerFont=NativeWidgetViews.create(c,nativeData,relativeFont,360,196).font;
            relativeFont.font=24;ok(NativeWidgetViews.create(c,nativeData,relativeFont,360,196).font<centerFont,"font below midpoint visibly gets smaller");
            nativeTextLayout(nativeData,relativeFont,360,196,320,"native-reference-smaller");
            relativeFont.font=40;ok(NativeWidgetViews.create(c,nativeData,relativeFont,360,196).font>centerFont,"font above midpoint visibly gets larger");
            nativeTextLayout(nativeData,relativeFont,360,196,320,"native-reference-larger");
            DisplaySettings nativeWhite=new DisplaySettings();nativeWhite.background=android.graphics.Color.WHITE;nativeWhite.text=android.graphics.Color.BLACK;
            nativeTextLayout(nativeData,nativeWhite,360,126,320,"native-text-white");
            DisplaySettings extremeNative=new DisplaySettings();extremeNative.font=24;extremeNative.left=32;extremeNative.right=32;extremeNative.top=24;extremeNative.bottom=24;extremeNative.gap=18;extremeNative.gauge=12;
            nativeTextLayout(nativeData,extremeNative,280,100,320,"native-text-extreme-settings");
            nativeTextLayout(nativeData,new DisplaySettings(),360,300,320,"native-text-tall-card");
            DisplaySettings expanded=new DisplaySettings();expanded.font=64;expanded.gap=96;expanded.textGap=48;expanded.gauge=48;expanded.left=128;expanded.right=128;expanded.top=128;expanded.bottom=128;
            nativeTextLayout(nativeData,expanded,280,100,320,"native-expanded-small-host");
            nativeTextLayout(nativeData,expanded,900,600,160,"native-expanded-large-host");
            expanded.save(RevenueStore.prefs(c));DisplaySettings expandedRestored=DisplaySettings.load(RevenueStore.prefs(c));
            ok(expandedRestored.font==64&&expandedRestored.gap==96&&expandedRestored.textGap==48&&expandedRestored.gauge==48&&expandedRestored.left==128&&expandedRestored.top==128,"expanded settings survive save and reload without old clamp");
            new DisplaySettings().save(RevenueStore.prefs(c));
            DisplaySettings moreGap=new DisplaySettings();moreGap.gap=60;
            ok(NativeWidgetViews.create(c,nativeData,moreGap,360,600).gap==60,"tall host honors 60dp row gap without the old fifteen-percent limit");
            nativeTextLayout(nativeData,moreGap,360,600,160,"native-row-gap-60");
            ok(measuredGaugeHeights.get("native-text-360x100-dpi320")<measuredGaugeHeights.get("native-text-360x196-dpi320"),"gauge thickens when card height increases");
            ok(measuredGaugeHeights.get("native-text-360x196-dpi320")<measuredGaugeHeights.get("native-text-tall-card"),"gauge continues to scale above two-row height");
            ok(new DisplaySettings().font==32&&new DisplaySettings().gauge==14&&new DisplaySettings().textGap==4,
                "photo-two reference is the default adjustment midpoint");
            ok(NativeWidgetViews.create(c,nativeData,new DisplaySettings(),360,196).font>NativeWidgetViews.create(c,nativeData,new DisplaySettings(),360,100).font,"larger card increases font within safe column limits");
            // Screenshot and explicit palette assertions for green / yellow / red in the same card.
            java.util.Calendar date=java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Tokyo"));
            int days=date.getActualMaximum(java.util.Calendar.DAY_OF_MONTH),today=date.get(java.util.Calendar.DAY_OF_MONTH);
            RevenueStore.Data paceData=RevenueStore.read(c);paceData.tiktok=today*75000L;paceData.coupon=today*25000L;
            paceData.goalTotal=days*100000L;paceData.goalTiktok=days*100000L;paceData.goalCoupon=days*100000L;
            paceData.month=month;paceData.importAt=System.currentTimeMillis();paceData.couponAt=paceData.importAt;paceData.error="";paceData.loading=false;
            DisplaySettings palette=new DisplaySettings();
            ok(palette.paceColors,"new and saved v0.1.4 settings use progress colors by default");
            ok(palette.gaugeColor(paceData.values()[0],paceData.goalTotal,month,System.currentTimeMillis(),palette.total)==GaugePalette.FIRST_GREEN,"configured green at or above daily target");
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
            ok(palette.gaugeColor(secondLap.values()[0],secondLap.goals()[0],month,System.currentTimeMillis(),palette.total)==palette.secondLap,"over 100% uses configured second-lap color");
            nativeTextLayout(secondLap,palette,360,126,320,"native-second-lap-115-percent");
            for(long[] spec:new long[][]{{0L,1L},{631000L,1L},{1000000L,1L},{1118800L,2L},{1200000L,2L},{1820900L,2L},{1885900L,2L},{2500000L,3L},{3000000L,3L}}){
                secondLap.tiktok=spec[0];secondLap.coupon=0;
                ok(RevenueMath.lapCount(secondLap.values()[0],secondLap.goalTotal)==spec[1],"native lap calculation on boundary samples");
                nativeTextLayout(secondLap,palette,360,126,320,"native-lap-"+spec[0]);
            }
            secondLap.tiktok=1150000;
            palette.paceAchieved=0xff116622;palette.secondLap=0xffbaff00;
            nativeTextLayout(secondLap,palette,360,126,320,"native-second-lap-custom-colors");
            palette.paceAchieved=GaugePalette.FIRST_GREEN;
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
            palette.paceAchieved=0xff112233;palette.paceBehind=0xff224466;palette.paceLow=0xffaa2244;palette.secondLap=0xffbaff00;
            palette.save(RevenueStore.prefs(c));
            DisplaySettings persisted=DisplaySettings.load(RevenueStore.prefs(c));
            ok(persisted.paceAchieved==0xff112233&&persisted.paceBehind==0xff224466&&persisted.paceLow==0xffaa2244&&persisted.secondLap==0xffbaff00,
                "all four selected palette colors survive settings save and load");
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
            runOnMainSync(()->{
                android.view.View root=activity.getWindow().getDecorView();
                ok(button(root,"THE FOOL QUESTから連携（通常サイト）")==null,
                    "obsolete website linking is hidden from widget display settings");
                ok(button(root,"連携データを貼り付け")==null,
                    "obsolete pasted link is hidden");
                ok(button(root,"2周目 182.09% サンプル表示 ON/OFF")==null,
                    "sample-only action is removed from production settings");
                ok(button(root,"設定を保存")!=null,"widget save settings remains available");
                ok(button(root,"ホーム画面に追加")!=null,"widget placement remains available");
            });
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
            ok(scheduler.getPendingJob(RevenueJob.MANUAL)!=null,"delayed manual job queued");
            CountDownLatch tapBlockEntered=new CountDownLatch(1),releaseTap=new CountDownLatch(1);
            executor.execute(()->{tapBlockEntered.countDown();try{releaseTap.await(120,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}});
            ok(tapBlockEntered.await(2,TimeUnit.SECONDS),"tap executor blocker active");
            ActivityMonitor tapMonitor=addMonitor(ManagementActivity.class.getName(),null,false);
            long beforeTap=RevenueStore.read(c).couponAt;
            ok(ManagementActivity.tapIntent(c).isActivity()&&ManagementActivity.tapIntent(c).isImmutable(),"immutable revenue management PendingIntent");
            runOnMainSync(()->{
                ok(view[0].findViewById(R.id.widget_root)!=null,"production widget RemoteViews");
                ok(view[0].findViewById(R.id.widget_root).performClick(),"production widget tapped");
            });
            Activity management=waitForMonitorWithTimeout(tapMonitor,5000);
            ok(management!=null,"widget opens revenue management");
            waitForIdleSync();
            ok(!management.isFinishing()&&!management.isDestroyed(),"management stays open");
            runOnMainSync(()->{
                android.view.View root=management.getWindow().getDecorView();
                ok(button(root,"収益履歴・手動追加の編集")!=null,"history accessible on tap");
                ok(button(root,"TikTok収益を手動追加")!=null,"manual TikTok accessible on tap");
                ok(button(root,"Couponを今すぐ更新")!=null,"Coupon refresh accessible on tap");
            });
            ok(RevenueUpdate.busy(),"tap starts direct Coupon refresh");
            ok(RevenueStore.read(c).couponAt==beforeTap,"last-good cache survives while updating");
            ok(scheduler.getPendingJob(RevenueJob.MANUAL)==null,"queued manual job cancelled");
            runOnMainSync(()->ok(view[0].findViewById(R.id.widget_root).performClick(),"repeat widget tap handled"));
            waitForIdleSync();
            ok(!management.isFinishing()&&!management.isDestroyed(),"repeated tap keeps management open");
            ok(RevenueUpdate.busy()&&RevenueStore.read(c).couponAt==beforeTap,"repeated tap coalesces active fetch");
            CountDownLatch tapDone=new CountDownLatch(1);RevenueUpdate.start(c,tapDone::countDown);
            ok(!tapDone.await(100,TimeUnit.MILLISECONDS),"coalesced observer waits");
            releaseTap.countDown();
            ok(tapDone.await(35,TimeUnit.SECONDS),"Coupon refresh completes");
            waitForIdleSync();
            ok(!management.isDestroyed(),"management remains open after refresh");
            ok(RevenueStore.read(c).couponAt>beforeTap&&RevenueStore.read(c).error.isEmpty(),"widget tap updates Coupon");
            ok(scheduler.getPendingJob(RevenueJob.MANUAL)==null,"no duplicate manual job");
            png(getUiAutomation().takeScreenshot(),"native-widget-opens-revenue");
            removeMonitor(tapMonitor);runOnMainSync(management::finish);
            dimensions.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,280);dimensions.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,100);exactSizes.clear();exactSizes.add(new android.util.SizeF(280,100));dimensions.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES,exactSizes);manager.updateAppWidgetOptions(id,dimensions);
            runOnMainSync(()->{android.view.ViewGroup.LayoutParams lp=view[0].getLayoutParams();lp.width=Math.round(280*activity.getResources().getDisplayMetrics().density);lp.height=Math.round(100*activity.getResources().getDisplayMetrics().density);view[0].setLayoutParams(lp);});
            Thread.sleep(700);png(getUiAutomation().takeScreenshot(),"native-widget-host-resized");
            runOnMainSync(()->{
                android.view.View last=view[0].findViewById(R.id.row_coupon),content=view[0].findViewById(R.id.widget_content);
                ok(last.getBottom()<=content.getHeight(),"real host resize retains the entire Coupon row");
                ok(view[0].findViewById(R.id.coupon_gauge_holder).getHeight()>0,"real host resize retains the third gauge");
            });
            host.stopListening();runOnMainSync(activity::finish);
            nativeRevenueChecks();
            result.putString("stream","PASS "+checks+" native Android checks; Coupon "+live.coupon+"; screenshots "+output);
            finish(ActivityResult.OK,result);
        }catch(Throwable e){result.putString("stream","FAIL "+checks+" "+android.util.Log.getStackTraceString(e));finish(ActivityResult.FAIL,result);}
    }
    static final class ActivityResult{static final int OK=-1,FAIL=0;}
}

