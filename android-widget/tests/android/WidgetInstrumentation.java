package com.aruno.foolquest.widget.next;

import android.app.Instrumentation;
import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
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
    void dailyTargetChecks()throws Exception {
        android.content.SharedPreferences prefs=c.getSharedPreferences("target-test",0);prefs.edit().clear().commit();
        DisplaySettings settings=DisplaySettings.load(prefs);
        ok(settings.targetLineColor==0xffffd43b&&settings.targetLineWidth==2,"old installs receive yellow 2dp target defaults");
        settings.targetLineColor=0xff3dbdff;settings.targetLineWidth=6;settings.font=30;settings.save(prefs);
        settings=DisplaySettings.load(prefs);
        ok(settings.targetLineColor==0xff3dbdff&&settings.targetLineWidth==6&&settings.font==30,"target settings persist alongside existing layout");
        settings.referenceLayout();
        ok(settings.targetLineColor==0xff3dbdff&&settings.targetLineWidth==6,"reference layout preserves target preference");
        settings.targetLineWidth=100;settings.save(prefs);ok(DisplaySettings.load(prefs).targetLineWidth==8,"target width is bounded");
        prefs.edit().clear().commit();
        for(int dpi:new int[]{160,320})for(int thickness:new int[]{1,2,8})for(float progress:new float[]{-1f,0f,.1f,.5f,1f}){
            android.content.res.Configuration config=new android.content.res.Configuration(c.getResources().getConfiguration());config.densityDpi=dpi;
            Context host=c.createConfigurationContext(config);float density=host.getResources().getDisplayMetrics().density;
            android.graphics.Bitmap b=NativeWidgetViews.gauge(host,0xffffffff,0xff00ee00,.1777f,0,0,false,14,300,13);
            android.graphics.Bitmap before=b.copy(android.graphics.Bitmap.Config.ARGB_8888,false);
            android.graphics.Canvas canvas=new android.graphics.Canvas(b);canvas.scale(density,density);
            GaugeDrawing.dailyTarget(canvas,0,GaugePalette.MARKER_OVERHANG_DP,300,14,progress,0xff3dbdff,thickness);
            int changed=0,minX=b.getWidth(),maxX=-1,minY=b.getHeight(),maxY=-1;
            for(int y=0;y<b.getHeight();y++)for(int x=0;x<b.getWidth();x++)if(b.getPixel(x,y)!=before.getPixel(x,y)){
                changed++;minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
                if(b.getPixel(x,y)!=0xff3dbdff)throw new AssertionError("target color mismatch");
            }
            if(progress<0)ok(changed==0,"unknown or stale target leaves pixels unchanged");
            else {
                ok(maxX-minX+1==Math.round(thickness*density),"target keeps chosen width, including month end");
                ok(minY==Math.round(GaugePalette.MARKER_OVERHANG_DP*density)&&maxY-minY+1==Math.round(14*density),"target stays inside gauge height");
                ok(changed==Math.round(thickness*density)*Math.round(14*density),"only target stripe changes");
                if(progress==1)ok(maxX==b.getWidth()-1,"month-end line stays visible at right edge");
            }
        }
        RevenueStore.Data data=new RevenueStore.Data();data.month=RevenueMath.month(System.currentTimeMillis());
        data.tiktok=11700;data.coupon=201500;data.goalTiktok=600000;data.goalCoupon=600000;data.goalTotal=1200000;
        data.importAt=1;data.couponAt=1;data.error="";data.errorCode="";
        DisplaySettings yellow=new DisplaySettings(),blue=new DisplaySettings();blue.targetLineColor=0xff3dbdff;
        runOnMainSync(()->{
            android.view.View first=NativeWidgetViews.create(c,data,yellow,340,126).views.apply(c,null);
            android.view.View second=NativeWidgetViews.create(c,data,blue,340,126).views.apply(c,null);
            for(int row=0;row<3;row++){
                android.widget.ImageView a=first.findViewById(NativeWidgetViews.HOLDER[row]).findViewById(R.id.gauge_image);
                android.widget.ImageView b=second.findViewById(NativeWidgetViews.HOLDER[row]).findViewById(R.id.gauge_image);
                android.graphics.Bitmap aa=((android.graphics.drawable.BitmapDrawable)a.getDrawable()).getBitmap();
                android.graphics.Bitmap bb=((android.graphics.drawable.BitmapDrawable)b.getDrawable()).getBitmap();
                ok(aa.sameAs(bb)==(row!=0),"only Total receives target line color changes");
            }
        });
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
    void calendarLayoutChecks() {
        ok(c.getApplicationInfo().icon==R.drawable.ic_mybest &&
            c.getDrawable(R.drawable.ic_mybest) instanceof android.graphics.drawable.AdaptiveIconDrawable,
            "application uses the MyBest adaptive launcher icon");
        int first=13001, second=13002;
        CalendarWidgetState.month(c,first,"2026-05");
        CalendarWidgetState.filter(c,first,CalendarWidgetState.COUPON);
        CalendarWidgetState.cycleSummary(c,first);
        ok("2026-05".equals(CalendarWidgetState.month(c,first))
            &&CalendarWidgetState.filter(c,first)==CalendarWidgetState.COUPON
            &&CalendarWidgetState.summary(c,first)==2,"the first header tap selects TikTok Lite");
        CalendarWidgetState.cycleSummary(c,first);
        ok(CalendarWidgetState.summary(c,first)==1,"the second header tap selects coupon");
        CalendarWidgetState.cycleSummary(c,first);
        ok(CalendarWidgetState.summary(c,first)==0,"the third header tap returns to total");
        ok(CalendarWidgetState.isFollowingCurrentMonth(c,second)
            &&CalendarWidgetState.filter(c,second)==CalendarWidgetState.ALL
            &&CalendarWidgetState.summary(c,second)==0,"second widget is independent");
        CalendarWidgetState.followCurrentMonth(c,first);
        ok(CalendarWidgetState.isFollowingCurrentMonth(c,first)
            &&RevenueMath.month(System.currentTimeMillis()).equals(CalendarWidgetState.month(c,first)),
            "calendar returns to JST current-month tracking");
        CalendarWidgetState.delete(c,first);
        ok(CalendarWidgetState.filter(c,first)==CalendarWidgetState.ALL,"widget removal clears display preferences");

        CalendarRevenueData.Month data=new CalendarRevenueData.Month();
        data.totals[0]=RevenueMath.MAX_YEN;
        data.days[1][0]=5;
        data.days[1][1]=3;
        data.days[1][2]=2;
        data.days[31][0]=999999;
        data.days[31][1]=350000;
        data.days[31][2]=649999;
        data.dailyLosses[31]=1500;
        data.losses[31][0]=1000;
        data.losses[31][1]=500;
        runOnMainSync(()->{
            for(float fontScale:new float[]{1f,2f}){
                android.content.res.Configuration config=new android.content.res.Configuration(c.getResources().getConfiguration());
                config.fontScale=fontScale;
                Context sized=c.createConfigurationContext(config);
                for(int widthDp:new int[]{280,320,411}){
                    float density=sized.getResources().getDisplayMetrics().density;
                    int width=Math.round(widthDp*density);
                    int height=Math.round(236*density);
                    android.widget.FrameLayout host=new android.widget.FrameLayout(sized);
                    android.view.View calendar=CalendarWidget.buildViews(sized,first,"2026-05",0,0,data,java.time.Instant.parse("2026-06-01T00:00:00Z").toEpochMilli(),widthDp,236).apply(sized,host);
                    host.addView(calendar,new android.widget.FrameLayout.LayoutParams(-1,-1));
                    host.measure(android.view.View.MeasureSpec.makeMeasureSpec(width,android.view.View.MeasureSpec.EXACTLY),
                        android.view.View.MeasureSpec.makeMeasureSpec(height,android.view.View.MeasureSpec.EXACTLY));
                    host.layout(0,0,width,height);
                    android.view.ViewGroup grid=calendar.findViewById(R.id.calendar_grid);
                    ok(grid.getChildCount()==5,"calendar always uses five date rows");
                    android.view.ViewGroup firstRow=(android.view.ViewGroup)grid.getChildAt(0);
                    completeCalendarText(firstRow.getChildAt(0),R.id.calendar_day_number,"1");
                    android.view.ViewGroup lastRow=(android.view.ViewGroup)grid.getChildAt(4);
                    ok(lastRow.getChildCount()==7,"calendar keeps seven columns");
                    android.view.View lastDay=lastRow.getChildAt(2);
                    completeCalendarText(lastDay,R.id.calendar_day_number,"31");
                    completeCalendarText(lastDay,R.id.calendar_day_total,"999999");
                    completeCalendarText(lastDay,R.id.calendar_coupon_label,"C");
                    completeCalendarText(lastDay,R.id.calendar_day_coupon,"350000");
                    completeCalendarText(lastDay,R.id.calendar_tiktok_label,"T");
                    completeCalendarText(lastDay,R.id.calendar_day_tiktok,"649999");
                    completeCalendarText(lastDay,R.id.calendar_day_loss,"−1500");
                    android.widget.TextView loss=lastDay.findViewById(R.id.calendar_day_loss);
                    ok(loss.getCurrentTextColor()==0xffff6464,"loss appears red below TikTok Lite");
                    ok(lastDay.findViewById(R.id.calendar_loss_row).getTop()>=lastDay.findViewById(R.id.calendar_tiktok_row).getBottom(),
                        "loss line is physically below TikTok Lite");
                    android.view.View firstDay=firstRow.getChildAt(0);
                    for(int id:new int[]{R.id.calendar_day_total,R.id.calendar_day_coupon,R.id.calendar_day_tiktok}){
                        android.widget.TextView shortValue=firstDay.findViewById(id);
                        android.widget.TextView longValue=lastDay.findViewById(id);
                        ok(shortValue.getTextSize()==longValue.getTextSize()&&longValue.getTextScaleX()==1f,
                            "daily amounts keep the same natural glyph size from one to six digits");
                    }
                    android.widget.TextView date=lastDay.findViewById(R.id.calendar_day_number);
                    android.widget.TextView total=lastDay.findViewById(R.id.calendar_day_total);
                    android.widget.TextView coupon=lastDay.findViewById(R.id.calendar_day_coupon);
                    ok(total.getTop()-date.getBottom()>=Math.round(2*density),
                        "there is a gap between the date and daily total");
                    ok(total.getTextSize()>coupon.getTextSize(),"daily total uses the free category width");
                    completeCalendarText(lastRow.getChildAt(5),R.id.calendar_control,"カテゴリー\n表示変更");
                    completeCalendarText(lastRow.getChildAt(6),R.id.calendar_control,"年月\n表示変更");
                    checkCalendarHeader(calendar,"2026年5月","合計","¥9,007,199,254,740,991");
                }
            }
            CalendarRevenueData.Month typical=new CalendarRevenueData.Month();
            typical.totals[0]=395601;
            typical.totals[1]=382200;
            typical.totals[2]=13401;
            long headerNow=java.time.Instant.parse("2026-11-05T00:00:00Z").toEpochMilli();
            android.widget.FrameLayout combinedHost=new android.widget.FrameLayout(c);
            android.view.View combined=CombinedWidget.buildViews(c,first,"2026-11",0,2,typical,
                headerNow,RevenueStore.read(c),new DisplaySettings(),411,400).apply(c,combinedHost);
            combinedHost.addView(combined,new android.widget.FrameLayout.LayoutParams(-1,-1));
            float density=c.getResources().getDisplayMetrics().density;
            int width=Math.round(411*density),height=Math.round(400*density);
            combinedHost.measure(android.view.View.MeasureSpec.makeMeasureSpec(width,android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(height,android.view.View.MeasureSpec.EXACTLY));
            combinedHost.layout(0,0,width,height);
            android.view.ViewGroup gauge=combined.findViewById(R.id.combined_gauge);
            android.view.ViewGroup dates=combined.findViewById(R.id.combined_calendar);
            ok(gauge.getChildCount()==1&&gauge.findViewById(R.id.row_coupon)!=null
                &&dates.getChildCount()==1&&dates.findViewById(R.id.calendar_week_4)!=null,
                "combined widget reuses the full gauge and calendar");
            checkCalendarHeader(dates,"2026年11月","TikTok Lite","¥13,401");
            android.widget.TextView title=dates.findViewById(R.id.calendar_title);
            ok(title.getTextSize()>=19*density,"typical monthly values use the large header size");
            long now=java.time.Instant.parse("2026-11-03T15:00:00Z").toEpochMilli();
            CalendarRevenueData.Month current=new CalendarRevenueData.Month();
            current.days[4][0]=58023;
            current.days[4][1]=12345;
            current.days[4][2]=45678;
            android.widget.FrameLayout host=new android.widget.FrameLayout(c);
            android.view.View calendar=CalendarWidget.buildViews(c,first,"2026-11",0,0,current,now).apply(c,host);
            android.view.ViewGroup firstRow=calendar.findViewById(R.id.calendar_week_0);
            android.view.View today=firstRow.getChildAt(3);
            ok("58023".contentEquals(((android.widget.TextView)today.findViewById(R.id.calendar_day_total)).getText())
                &&today.findViewById(R.id.calendar_coupon_row).getVisibility()==android.view.View.VISIBLE
                &&today.findViewById(R.id.calendar_tiktok_row).getVisibility()==android.view.View.VISIBLE
                &&"45678".contentEquals(((android.widget.TextView)today.findViewById(R.id.calendar_day_tiktok)).getText()),
                "today immediately shows verified TikTok and the combined total");
            CalendarRevenueData.Month couponOnly=new CalendarRevenueData.Month();
            couponOnly.days[4][1]=12345;
            android.view.View provisional=CalendarWidget.buildViews(c,first,"2026-11",0,0,couponOnly,now).apply(c,host);
            android.view.View provisionalToday=((android.view.ViewGroup)provisional.findViewById(R.id.calendar_week_0)).getChildAt(3);
            ok("12345".contentEquals(((android.widget.TextView)provisionalToday.findViewById(R.id.calendar_day_total)).getText())
                &&provisionalToday.findViewById(R.id.calendar_tiktok_row).getVisibility()==android.view.View.INVISIBLE,
                "unknown today TikTok stays hidden without inventing zero revenue");
            android.view.View tomorrow=firstRow.getChildAt(4);
            ok(tomorrow.findViewById(R.id.calendar_day_total).getVisibility()==android.view.View.INVISIBLE
                &&tomorrow.findViewById(R.id.calendar_coupon_row).getVisibility()==android.view.View.INVISIBLE,
                "future days show their date but no revenue");
        });
    }

    void checkCalendarHeader(android.view.View root,String month,String category,String amount){
        completeCalendarText(root,R.id.calendar_title,month);
        completeCalendarText(root,R.id.calendar_summary_label,category);
        completeCalendarText(root,R.id.calendar_summary_amount,amount);
        android.widget.TextView title=root.findViewById(R.id.calendar_title);
        android.widget.TextView label=root.findViewById(R.id.calendar_summary_label);
        android.widget.TextView value=root.findViewById(R.id.calendar_summary_amount);
        android.view.View summary=root.findViewById(R.id.calendar_summary);
        ok(title.getTextSize()==label.getTextSize()&&title.getTextSize()==value.getTextSize()
            &&title.getBaseline()==label.getBaseline()&&title.getBaseline()==value.getBaseline(),
            "month and both summary fields share the font size and baseline");
        ok(summary.isClickable()&&summary.hasOnClickListeners()
            &&!label.isClickable()&&!value.isClickable()
            &&label.getLeft()==0&&value.getRight()==summary.getWidth()
            &&label.getHeight()==summary.getHeight()&&value.getHeight()==summary.getHeight(),
            "the full label and amount row shares one click action");
    }

    void completeCalendarText(android.view.View root,int id,String expected){
        android.widget.TextView text=root.findViewById(id);
        android.text.Layout layout=text.getLayout();
        String[] lines=expected.split("\n",-1);
        ok(expected.contentEquals(text.getText())&&layout!=null&&layout.getLineCount()==lines.length
            &&layout.getHeight()<=text.getHeight()-text.getPaddingTop()-text.getPaddingBottom(),
            "unclipped calendar text "+expected);
        int end=0;
        for(int line=0;line<lines.length;line++){
            end+=lines[line].length()+(line<lines.length-1?1:0);
            ok(layout.getLineEnd(line)==end&&layout.getEllipsisCount(line)==0
                &&layout.getLineWidth(line)<=text.getWidth()-text.getPaddingLeft()-text.getPaddingRight()+0.01f,
                "unclipped calendar line "+lines[line]);
        }
    }

    void couponDailyContractChecks() throws Exception {
        long now=java.time.Instant.parse("2026-11-02T15:00:00Z").toEpochMilli();
        String raw="{\"month\":\"2026-11\",\"coupon_revenue\":600,\"through\":\"2026-11-02\","
            +"\"days\":{\"2026-11-01\":300,\"2026-11-02\":0}}";
        org.json.JSONObject snapshot=CouponDailyReader.validate(raw,"2026-11",now);
        ok(snapshot.getLong("total")==600&&snapshot.getLong("sum")==600
            &&snapshot.getJSONObject("days").getLong("2026-11-03")==300,
            "current day equals the reconciled monthly total minus completed days");
        long firstDay=java.time.Instant.parse("2026-10-31T15:00:00Z").toEpochMilli();
        org.json.JSONObject start=CouponDailyReader.validate(
            "{\"month\":\"2026-11\",\"coupon_revenue\":0,\"through\":null,\"days\":{}}","2026-11",firstDay);
        ok(start.getJSONObject("days").getLong("2026-11-01")==0,"verified zero on the first day JST");
        boolean invalid=false;
        try{CouponDailyReader.validate(raw.replace("2026-11-02\":0","2026-11-03\":0"),"2026-11",now);}
        catch(Exception expected){invalid=true;}
        ok(invalid,"daily coverage rejects gaps and future entries");
    }

    void payFetchEntryChecks() throws Exception {
        android.content.pm.PackageManager pm = c.getPackageManager();
        android.content.ComponentName entry = new android.content.ComponentName(c, PayFetchActivity.class);
        ok(!pm.getActivityInfo(entry, 0).exported, "only the app may start the WebView CSV fetch");
        android.content.Intent csv = new android.content.Intent(android.content.Intent.ACTION_VIEW)
            .setDataAndType(android.net.Uri.parse("content://com.android.providers.downloads/test"), "text/csv")
            .setPackage(c.getPackageName());
        ok(pm.queryIntentActivities(csv, 0).isEmpty(), "an external CSV cannot trigger automatic import");
    }

    void nativeRevenueChecks() throws Exception {
        String month=RevenueMath.month(System.currentTimeMillis());
        VatonRevenueCsv.CsvResult parsed=VatonRevenueCsv.parseCsv("ID,チャージポイント\nA,\"1,000\"\nB,-200\n");
        ok(parsed.total==1200 && parsed.count==2,"CSV parser handles quoted values and returns total");
        RevenueManager manager=new RevenueManager(c);
        String csvMonth=month.replace('-','/');
        VatonRevenueCsv.CsvResult vatonFixture=VatonRevenueCsv.parseCsv(
            "\uFEFF※ エクスポート結果です。\n" +
            "実績の説明（テスト用）\n" +
            "※ 円換算額は商品の交換価値です。\n" +
            "記載の金額を保証しません。\n" +
            "日付,種別,ブランド名,商品名,交換ポイント,円換算額,チャージポイント\n" +
            csvMonth+"/01,チャージ,,,0,0,200\n" +
            csvMonth+"/02,交換,例,商品,-5000,5000,0\n" +
            csvMonth+"/02,チャージ,,,0,0,300\n" +
            csvMonth+"/03,チャージ,,,0,0,1700\n" +
            csvMonth+"/04,交換,例,商品,-5000,5000,0\n");
        ok(vatonFixture.datesComplete&&vatonFixture.count==5&&
            vatonFixture.total==2200&&month.equals(vatonFixture.datesMonth)&&
            vatonFixture.days.get(month+"-01")==200L&&
            vatonFixture.days.get(month+"-02")==300L&&
            vatonFixture.days.get(month+"-03")==1700L&&
            vatonFixture.days.get(month+"-04")==0L,
            "Vaton-style BOM/preamble/seven columns/zero-point exchanges are parsed");

        boolean invalidDateRejected=false;
        try{
            VatonRevenueCsv.parseCsv("日付,チャージポイント\n"+csvMonth+"/32,150\n");
        }catch(IllegalArgumentException expected){invalidDateRejected=true;}
        ok(invalidDateRejected,"malformed dated CSV is rejected, not reassigned to current month");
        VatonRevenueCsv.CsvResult bom=VatonRevenueCsv.parseCsv("\uFEFF日付,チャージポイント\n"
            +csvMonth+"/01,150\n");
        ok(bom.datesComplete&&bom.total==150&&bom.count==1&&
            month.equals(bom.datesMonth)&&bom.days.get(month+"-01")==150L,
            "UTF-8 BOM must not invalidate the leading 日付 column");
        VatonRevenueCsv.CsvResult daily=VatonRevenueCsv.parseCsv("日付,チャージポイント\n"
            +csvMonth+"/01,750\n"+csvMonth+"/02,250\n");
        ok(daily.datesComplete&&daily.total==1000&&month.equals(daily.datesMonth),
            "dated CSV parsed into selected month");
        manager.setCsv(month,daily,"dated-fixture.csv");
        CalendarRevenueData.Month calendar=CalendarRevenueData.read(c,month);
        ok(calendar.tiktokDaily&&calendar.days[1][2]==750&&calendar.days[2][2]==250,
            "calendar daily TikTok equals dated CSV");
        ok(calendar.totals[2]==1000,"daily CSV reconciles to monthly baseline");
        manager.addManual(month+"-10",250);
        String id=manager.manual().getJSONObject(manager.manual().length()-1).getString("id");
        ok(manager.tiktok(month)==1250,"native manual amount adds to CSV exactly once");
        manager.setCsv(month,daily,"dated-fixture.csv");
        CalendarRevenueData.Month reimport=CalendarRevenueData.read(c,month);
        ok(reimport.totals[2]==1250&&reimport.days[10][2]==250&&reimport.days[11][2]==-1,
            "repeat import keeps manual income once and missing dates unknown");
        SharedPreferences prefs=RevenueStore.prefs(c);
        prefs.edit().putLong("tiktok",333).commit();
        ok(CalendarRevenueData.read(c,month).totals[2]==1250,
            "legacy site cache cannot replace the dated native calendar");
        String previous=RevenueMath.date("yyyy-MM",System.currentTimeMillis()-35L*24*60*60*1000);
        VatonRevenueCsv.CsvResult older=VatonRevenueCsv.parseCsv("日付,チャージポイント\n"+previous+"-01,500\n");
        manager.addManual(previous+"-10",75);
        CalendarRevenueData.Month manualOnly=CalendarRevenueData.read(c,previous);
        ok(manualOnly.totals[2]==75&&manualOnly.days[10][2]==75&&manualOnly.days[11][2]==-1,
            "manual-only month displays dated entries and leaves other days unknown");
        prefs.edit().putLong("tiktok",333).commit();
        RevenueManager stalePicker=new RevenueManager(c);
        manager.setCsv(previous,older,"older-fixture.csv",previous+"-03");
        CalendarRevenueData.Month covered=CalendarRevenueData.read(c,previous);
        ok(covered.days[2][2]==0&&covered.days[3][2]==0&&covered.days[4][2]==-1,
            "confirmed CSV acquisition date distinguishes zero from unknown");
        ok(RevenueStore.read(c).tiktok==333&&manager.tiktok(month)==1250&&
            CalendarRevenueData.read(c,previous).totals[2]==575,
            "past CSV import preserves the current gauge cache and native month");
        stalePicker.setCsv(month,daily,"dated-fixture.csv");
        ok(CalendarRevenueData.read(c,previous).totals[2]==575,
            "an already open picker cannot overwrite a month saved by another import");
        boolean undatedRefused=false;
        try{manager.setCsv(month,parsed,"legacy-fixture.csv");}
        catch(IllegalArgumentException expected){undatedRefused=true;}
        ok(undatedRefused&&CalendarRevenueData.read(c,month).days[1][2]==750,
            "undated import cannot erase existing verified daily amounts");
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
        manager.addLoss(month+"-01","tiktok",150);
        String tikTokLossId=manager.losses().getJSONObject(manager.losses().length()-1).getString("id");
        manager.addLoss(month+"-01","coupon",80);
        String couponLossId=manager.losses().getJSONObject(manager.losses().length()-1).getString("id");
        ok(manager.netTikTok(month)==1125&&manager.lossTotal(month,"tiktok")==150,
            "TikTok Lite losses reduce current month independently");
        CalendarRevenueData.Month withLoss=CalendarRevenueData.read(c,month);
        ok(withLoss.days[1][2]==600&&withLoss.dailyLosses[1]==230,
            "calendar subtracts TikTok Lite loss once and shows combined red loss separately");
        manager.setCsv(month,daily,"dated-fixture.csv");
        ok(manager.lossTotal(month,"tiktok")==150&&manager.lossTotal(month,"coupon")==80,
            "re-importing CSV cannot reset either loss category");
        manager.updateLoss(tikTokLossId,month+"-01","tiktok",100);
        ok(manager.lossTotal(month,"tiktok")==100&&manager.netTikTok(month)==1175,
            "editing a loss replaces its amount, without double subtraction");
        String lossBackup=manager.backup();
        manager.deleteLoss(tikTokLossId);
        ok(manager.netTikTok(month)==1275,"deleting a loss restores net revenue");
        manager.replaceFromJson(lossBackup);
        ok(manager.netTikTok(month)==1175&&manager.lossTotal(month,"coupon")==80,
            "backups restore both loss categories and original gross revenue");
        boolean invalidLossRejected=false;
        try{manager.addLoss(month+"-01","unknown",100);}
        catch(IllegalArgumentException expected){invalidLossRejected=true;}
        ok(invalidLossRejected,"invalid loss categories are rejected");
        manager.deleteLoss(couponLossId);
        manager.deleteLoss(tikTokLossId);
        ok(manager.lossTotal(month,"coupon")==0&&manager.lossTotal(month,"tiktok")==0,
            "deleting both loss records restores the original values");
        String lastMonth=RevenueMath.date("yyyy-MM",System.currentTimeMillis()-35L*24*60*60*1000);
        RevenueStore.saveCoupon(c,lastMonth,321);
        RevenueStore.saveCoupon(c,month,1000000);
        ok(manager.coupon(lastMonth)==321,"prior month Coupon persists after month refresh");
        ok(new org.json.JSONObject(manager.backup()).getJSONObject("months").getJSONObject(lastMonth).getLong("coupon")==321,
            "Coupon monthly archive is included in user backup");
        Activity home=startActivitySync(new Intent(c,ManagementActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        ok(home!=null,"native revenue management screen launches");
        long targetTime=System.currentTimeMillis();
        String[] expectedTargets={RevenueMath.dailyTargetLabel(month,RevenueMath.total(manager.goal("tiktok"),manager.goal("coupon")),targetTime),
            RevenueMath.dailyTargetLabel(month,manager.goal("tiktok"),targetTime),RevenueMath.dailyTargetLabel(month,manager.goal("coupon"),targetTime)};
        runOnMainSync(()->{
            String[] tags={"target-total","target-tiktok","target-coupon"};
            for(int i=0;i<3;i++){
                android.widget.TextView target=home.getWindow().getDecorView().findViewWithTag(tags[i]);
                ok(target!=null&&target.getText().toString().equals(expectedTargets[i]),"management displays cumulative daily target amount: "+tags[i]);
            }
            ok(button(home.getWindow().getDecorView(),"旧サイトの移行ファイル作成ページ")==null,
                "migration controls are not shown on normal management screen");
            ok(button(home.getWindow().getDecorView(),"バックアップ・復元")!=null,
                "backup recovery remains accessible");
        });
        waitForIdleSync();
        png(getUiAutomation().takeScreenshot(),"native-revenue-daily-targets");
        runOnMainSync(()->button(home.getWindow().getDecorView(),"収益履歴・手動追加の編集").performClick());
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
            dailyTargetChecks();
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
            ok("TIMEOUT".equals(RevenueStore.prefs(c).getString("lastFailureCode","")),"recovery preserves last failure for diagnosis");ok(live.tiktok==-1&&live.values()[0]==RevenueMath.UNKNOWN,"missing TikTok not zero");
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
                ok(button(root,"収益・損失履歴の編集")!=null,"history accessible on tap");
                ok(button(root,"TikTok収益を手動追加")!=null,"manual TikTok accessible on tap");
            ok(button(root,"TikTok Liteの損失を手動追加")!=null,"TikTok losses can be registered");
            ok(button(root,"クーポンの損失を手動追加")!=null,"Coupon losses can be registered");
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
            calendarLayoutChecks();
            couponDailyContractChecks();
            payFetchEntryChecks();
            nativeRevenueChecks();
            result.putString("stream","PASS "+checks+" native Android checks; Coupon "+live.coupon+"; screenshots "+output);
            finish(ActivityResult.OK,result);
        }catch(Throwable e){result.putString("stream","FAIL "+checks+" "+android.util.Log.getStackTraceString(e));finish(ActivityResult.FAIL,result);}
    }
    static final class ActivityResult{static final int OK=-1,FAIL=0;}
}

