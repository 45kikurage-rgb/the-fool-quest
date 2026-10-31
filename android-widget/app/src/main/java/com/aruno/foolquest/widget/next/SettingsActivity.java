package com.aruno.foolquest.widget.next;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SettingsActivity extends Activity {
    private DisplaySettings settings;
    private LinearLayout body;
    private android.widget.FrameLayout preview;
    private TextView warning,description,communication;
    private final Map<String,EditText> colors=new LinkedHashMap<>();
    private int widgetId=AppWidgetManager.INVALID_APPWIDGET_ID;
    private boolean colorValid=true;
    private boolean resumed=false;
    private int previewWidgetId=AppWidgetManager.INVALID_APPWIDGET_ID;
    private TextView saveState;
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    @Override public void onCreate(Bundle b){
        super.onCreate(b); setResult(RESULT_CANCELED);
        widgetId=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
        settings=DisplaySettings.load(RevenueStore.prefs(this)); build(); handleImport(getIntent());
        RevenueUpdate.start(this,()->runOnUiThread(this::updatePreview));
    }
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);handleImport(i);}
    @Override protected void onResume(){super.onResume();resumed=true;updatePreview();}
    @Override protected void onPause(){super.onPause();resumed=false;}
    private TextView text(String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextColor(0xff171717);v.setTextSize(size);v.setPadding(0,dp(7),0,dp(7));return v;}
    private Button button(String label,Runnable action){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setOnClickListener(v->action.run());body.addView(b,new LinearLayout.LayoutParams(-1,-2));return b;}
    private void build(){
        colors.clear();
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setFitsSystemWindows(true);
        root.setPadding(dp(18),dp(8),dp(18),dp(8));root.setBackgroundColor(0xfff6f5f2);setContentView(root);
        LinearLayout pinned=new LinearLayout(this);pinned.setOrientation(LinearLayout.VERTICAL);root.addView(pinned,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout header=new LinearLayout(this);header.setOrientation(LinearLayout.HORIZONTAL);pinned.addView(header);
        Button back=new Button(this);back.setText("←");back.setContentDescription("収益管理へ戻る");back.setOnClickListener(v->finish());header.addView(back,new LinearLayout.LayoutParams(dp(48),dp(44)));
        String version="";try{version=" / code "+getPackageManager().getPackageInfo(getPackageName(),0).versionCode;}catch(Exception ignored){}
        header.addView(text("表示設定"+version,16),new LinearLayout.LayoutParams(0,dp(44),1));
        Button saveButton=new Button(this);saveButton.setText("保存");saveButton.setOnClickListener(v->save());header.addView(saveButton,new LinearLayout.LayoutParams(dp(76),dp(44)));
        body=pinned;
        saveState=text("「保存」でホーム画面へ反映",10);saveState.setPadding(0,0,0,0);saveState.setSingleLine(true);body.addView(saveState);
        description=text("",10);description.setPadding(0,0,0,0);description.setSingleLine(true);body.addView(description);
        int[] ids=RevenueWidget.ids(this);
        if(previewWidgetId==AppWidgetManager.INVALID_APPWIDGET_ID)previewWidgetId=widgetId!=AppWidgetManager.INVALID_APPWIDGET_ID?widgetId:ids.length>0?ids[0]:AppWidgetManager.INVALID_APPWIDGET_ID;
        if(ids.length>1){
            android.widget.Spinner target=new android.widget.Spinner(this);
            String[] names=new String[ids.length];int selected=0;
            for(int i=0;i<ids.length;i++){names[i]="ホームのウィジェット "+(i+1);if(ids[i]==previewWidgetId)selected=i;}
            target.setAdapter(new android.widget.ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,names));
            target.setSelection(selected);body.addView(target);
            target.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
                public void onItemSelected(android.widget.AdapterView<?> parent,View v,int pos,long id){previewWidgetId=ids[pos];updatePreview();}
                public void onNothingSelected(android.widget.AdapterView<?> parent){}
            });
        }
        preview=new android.widget.FrameLayout(this);preview.setTag("settings-preview");
        body.addView(preview,new LinearLayout.LayoutParams(-1,dp(126)));
        // Replace preview children after layout; synchronous replacement can leave them unmeasured.
        preview.addOnLayoutChangeListener((v,a,b,c,d,e,f,g,h)->{if(c-a!=g-e)preview.post(this::updatePreview);});
        warning=text("",10);warning.setPadding(0,dp(2),0,dp(2));warning.setMaxLines(2);warning.setTextColor(0xff9b4100);body.addView(warning);
        ScrollView scroll=new ScrollView(this);scroll.setTag("settings-controls");scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(0,dp(8),0,dp(24));scroll.addView(body);
        button("写真2の基準へ戻す",()->{settings.referenceLayout();build();markDirty();});
        body.addView(text("中央が基準。小さく／大きく調整できます。",12));
        if(widgetId==AppWidgetManager.INVALID_APPWIDGET_ID)button("ホーム画面に追加",()->{
            if(!validateColors())return;settings.save(RevenueStore.prefs(this));RevenueWidget.renderAll(this);
            AppWidgetManager m=AppWidgetManager.getInstance(this);
            if(m.isRequestPinAppWidgetSupported())m.requestPinAppWidget(new ComponentName(this,RevenueWidget.class),null,null);
            else message("ホーム画面を長押しして「ウィジェット」から追加してください。");
        });
        body.addView(text("色とレイアウト",18));
        button("黒背景／白背景を反転",()->{
            boolean dark=Color.red(settings.background)+Color.green(settings.background)+Color.blue(settings.background)<384;
            settings.background=dark?Color.WHITE:Color.BLACK;settings.text=dark?Color.BLACK:Color.WHITE;
            colors.get("text").setText(hex(settings.text));colors.get("background").setText(hex(settings.background));updatePreview();
        });
        color("text","文字色",settings.text);color("background","背景色",settings.background);
        slider("背景の濃さ（100%＝不透明）",0,100,settings.opacity,v->settings.opacity=v);
        slider("文字サイズ（基準32）",24,40,settings.font,v->settings.font=v);
        slider("左余白（基準17）",1,33,settings.left,v->settings.left=v);slider("右余白（基準16）",0,32,settings.right,v->settings.right=v);
        slider("上余白",0,24,settings.top,v->settings.top=v);slider("下余白",0,24,settings.bottom,v->settings.bottom=v);
        slider("ステータス行間（基準5）",0,10,settings.gap,v->settings.gap=v);slider("ゲージ太さ（基準14）",8,20,settings.gauge,v->settings.gauge=v);
        slider("文字とゲージの間隔（基準4）",0,8,settings.textGap,v->settings.textGap=v);
        button("設定を保存",this::save);
        android.widget.CheckBox pace=new android.widget.CheckBox(this);pace.setText("進歩カラー（THE FOOL QUESTと同じ）");pace.setChecked(settings.paceColors);body.addView(pace);
        pace.setOnCheckedChangeListener((v,checked)->{settings.paceColors=checked;markDirty();for(String key:new String[]{"total","tiktok","coupon"})colors.get(key).setEnabled(!checked);updatePreview();});
        color("total","Total 固定色（進歩カラーOFF時）",settings.total);color("tiktok","TikTok 固定色（進歩カラーOFF時）",settings.tiktok);color("coupon","Coupon 固定色（進歩カラーOFF時）",settings.coupon);
        for(String key:new String[]{"total","tiktok","coupon"})colors.get(key).setEnabled(!settings.paceColors);
        body.addView(text("日別進歩カラー・2周目カラー",18));
        body.addView(text("3周目（×3）はレインボーで表示します。",13));
        colorPalette("paceAchieved","今日までの目標達成：100%以上",settings.paceAchieved);
        colorPalette("paceBehind","今日までの目標未達：50%以上100%未満",settings.paceBehind);
        colorPalette("paceLow","今日までの目標不足：50%未満",settings.paceLow);
        colorPalette("secondLap","2周目：月間目標100%超",settings.secondLap);
        button("表示設定を初期化",()->new AlertDialog.Builder(this).setMessage("表示設定を初期値に戻します。収益データは保持します。保存でホーム画面へ反映します。")
            .setNegativeButton("キャンセル",null).setPositiveButton("初期化",(d,w)->{settings=new DisplaySettings();build();markDirty();}).show());
        updatePreview();
    }
    private void markDirty(){if(saveState!=null)saveState.setText("未保存の変更があります。上の「保存」で反映");}
    private interface Change{void set(int value);}
    private void slider(String label,int min,int max,int current,Change change){
        TextView title=text(label+"："+current,13);body.addView(title);
        // Keep an existing out-of-range preference until the user explicitly adjusts it.
        int actualMin=Math.min(min,current),actualMax=Math.max(max,current);
        SeekBar bar=new SeekBar(this);bar.setMax(actualMax-actualMin);bar.setProgress(current-actualMin);body.addView(bar);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int v,boolean user){int n=v+actualMin;title.setText(label+"："+n);change.set(n);if(user)markDirty();updatePreview();}
            public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}
        });
    }
    private static String hex(int c){return String.format(java.util.Locale.US,"#%06X",c&0xffffff);}
    private void color(String key,String title,int value){
        body.addView(text(title+"（#RRGGBB）",13));EditText input=new EditText(this);input.setSingleLine(true);input.setText(hex(value));colors.put(key,input);body.addView(input);
        input.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){markDirty();validateColors();updatePreview();}public void afterTextChanged(Editable e){}
        });
    }
    private void colorPalette(String key,String title,int value) {
        color(key,title,value);
        button("● "+title+"：色見本から選ぶ",()->openColorPalette(key,title));
    }
    private void openColorPalette(String key,String title) {
        final int[] samples={GaugePalette.FIRST_GREEN,0xff31d158,GaugePalette.SECOND_LIME,0xffbaff00,
            0xff39ff6a,0xff87ff17,0xffffd43b,0xffff9c24,
            0xffff4545,0xffff78ad,0xff3dbdff,0xff9570ff,
            0xffffffff,0xff147a39,0xff75ffcc,0xffa0a0a0};
        LinearLayout grid=new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(dp(10),dp(12),dp(10),dp(12));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(title).setView(grid)
            .setNegativeButton("キャンセル",null).create();
        for(int row=0;row<samples.length/4;row++){
            LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);
            for(int col=0;col<4;col++){
                final int sample=samples[row*4+col];
                Button swatch=new Button(this);
                swatch.setAllCaps(false);
                swatch.setText(hex(sample).equalsIgnoreCase(colors.get(key).getText().toString())?"✓":"");
                swatch.setTextColor(Color.BLACK);
                swatch.setContentDescription("色 "+hex(sample)+" を選択");
                swatch.setBackgroundColor(sample);
                LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,dp(52),1f);
                params.setMargins(dp(3),dp(3),dp(3),dp(3));
                line.addView(swatch,params);
                swatch.setOnClickListener(v->{colors.get(key).setText(hex(sample));dialog.dismiss();});
            }
            grid.addView(line,new LinearLayout.LayoutParams(-1,-2));
        }
        dialog.show();
    }
    private boolean validateColors(){
        colorValid=true;
        for(Map.Entry<String,EditText> entry:colors.entrySet()){
            String raw=entry.getValue().getText().toString().trim();
            if(!raw.matches("#[a-fA-F0-9]{6}")){entry.getValue().setError("#と6桁の色コード");colorValid=false;continue;}
            int v=Color.parseColor(raw);switch(entry.getKey()){
                case "text":settings.text=v;break;case "background":settings.background=v;break;
                case "total":settings.total=v;break;case "tiktok":settings.tiktok=v;break;case "coupon":settings.coupon=v;break;
                case "paceAchieved":settings.paceAchieved=v;break;
                case "paceBehind":settings.paceBehind=v;break;
                case "paceLow":settings.paceLow=v;break;
                case "secondLap":settings.secondLap=v;break;
            }
        }
        return colorValid;
    }
    private void updatePreview(){
        if(preview==null||isFinishing()||isDestroyed())return;
        float density=getResources().getDisplayMetrics().density;
        int available=preview.getWidth()>0?Math.round(preview.getWidth()/density):340;
        int w=available,h=196;
        boolean actual=previewWidgetId!=AppWidgetManager.INVALID_APPWIDGET_ID;
        if(actual){
            Bundle options=AppWidgetManager.getInstance(this).getAppWidgetOptions(previewWidgetId);
            int[] size=RevenueWidget.previewSize(this,options);w=size[0];h=size[1];
        }
        int maxPreview=Math.max(72,Math.min(180,Math.round(getResources().getDisplayMetrics().heightPixels/density*.25f)));
        float scale=Math.min(1f,Math.min((float)available/w,(float)maxPreview/h));
        android.view.ViewGroup.LayoutParams params=preview.getLayoutParams();
        int previewHeight=Math.round(dp(h)*scale);
        if(params.height!=previewHeight){params.height=previewHeight;preview.setLayoutParams(params);}
        RevenueStore.Data d=RevenueStore.read(this);
        NativeWidgetViews.Result r=NativeWidgetViews.create(this,d,settings,w,h);
        View child=r.views.apply(this,preview);
        child.setPivotX(0);child.setPivotY(0);child.setScaleX(scale);child.setScaleY(scale);child.setTranslationX(dp((available-w*scale)/2f));
        preview.removeAllViews();preview.addView(child,new android.widget.FrameLayout.LayoutParams(dp(w),dp(h)));
        preview.setContentDescription(WidgetRenderer.description(d));
        description.setText((actual?"ホームの枠 ":"見本の枠 ")+w+" × "+h+"dp・全ウィジェット共通");
        String status=!colorValid?"色コードを確認してください。":String.format(java.util.Locale.JAPAN,
            "実際の表示：文字 %.1fdp／行間 %.1fdp／ゲージ %ddp／文字とゲージ %.1fdp。",r.font,r.gap,r.gauge,r.textGap);
        if(r.adjusted)status+=" 枠に収まるよう縮小しています。さらに大きくする場合はホームでウィジェットの幅・高さを広げてください。";
        if(settings.text==settings.background&&settings.opacity==100)status+=" 文字と背景が同じ色です。";
        warning.setText(status);
        if(communication!=null){
            android.content.SharedPreferences p=RevenueStore.prefs(this);
            String last=p.getString("lastFailureCode","");
            communication.setText("通信状態："+(d.loading?"更新中":!d.error.isEmpty()?RevenueFailure.label(d.errorCode):d.couponAt>0?"正常取得":"初回取得待ち")
                +(last.isEmpty()?"":"\n直近の失敗："+RevenueFailure.label(last)+"（"+RevenueMath.date("MM/dd HH:mm",p.getLong("lastFailureAt",0))+"）"));
        }
    }
    private void save(){
        if(!validateColors()){updatePreview();return;}
        settings.save(RevenueStore.prefs(this));RevenueWidget.renderAll(this);
        if(widgetId!=AppWidgetManager.INVALID_APPWIDGET_ID){setResult(RESULT_OK,new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId));finish();}
        else {saveState.setText("保存済み・ホームの "+RevenueWidget.ids(this).length+" 個へ再描画を送信しました。");message("設定を保存し、ホーム画面へ再描画を送信しました。");}
    }
    private void message(String s){new AlertDialog.Builder(this).setMessage(s).setPositiveButton("OK",null).show();}
    private void openExporter(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception e){message("ブラウザでTHE FOOL QUESTの連携ページを開いてください。");}}
    private void handleImport(Intent i){
        if(Intent.ACTION_VIEW.equals(i.getAction())&&i.getData()!=null)receive(i.getData().toString());
        else if(Intent.ACTION_SEND.equals(i.getAction()))receive(i.getStringExtra(Intent.EXTRA_TEXT));
    }
    private void receive(String raw){
        try{
            if(raw==null||raw.length()>4096)throw new IllegalArgumentException("連携データの長さが不正です");
            RevenueStore.Import data=RevenueStore.Import.parse(Uri.parse(raw));
            new AlertDialog.Builder(this).setTitle("サイトの表示値を受け取る")
                .setMessage(data.month+"\nTikTok "+RevenueMath.money(data.tiktok)+"\n目標 Total "+RevenueMath.money(data.goalTotal)+"\nTikTok "+RevenueMath.money(data.goalTiktok)+"\nCoupon "+RevenueMath.money(data.goalCoupon)+"\n\nサイトの表示と一致していることを確認してください。")
                .setNegativeButton("キャンセル",null).setPositiveButton("受け取る",(dialog,which)->{
                    try{data.save(this);RevenueWidget.renderAll(this);updatePreview();RevenueUpdate.start(this,()->runOnUiThread(this::updatePreview));}
                    catch(Exception e){message(e.getMessage());}
                }).show();
        }catch(Exception e){message(e.getMessage()==null?"連携データを確認してください。":e.getMessage());}
    }
}

