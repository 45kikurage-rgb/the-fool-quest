package com.aruno.foolquest.widget;

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
    private boolean secondLapPreview=false;
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
        colors.clear();ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setFitsSystemWindows(true);
        body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(18),dp(24),dp(18),dp(28));body.setBackgroundColor(0xfff6f5f2);scroll.addView(body);setContentView(scroll);
        body.addView(text("THE FOOL QUEST",24));body.addView(text("収益ウィジェット 0.1.12",14));
        description=text("",12);body.addView(description);
        preview=new android.widget.FrameLayout(this);
        body.addView(preview,new LinearLayout.LayoutParams(-1,dp(126)));
        preview.addOnLayoutChangeListener((v,a,b,c,d,e,f,g,h)->{if(c-a!=g-e)updatePreview();});
        body.addView(text("日別の進歩カラー：達成・未達・不足の3色。月間目標を超えると前の周を満タンで残し、2周目はライム色・3周目は緑色を交互に重ねます。周数と白い境界線を表示し、達成率の数字は累計表示です。",12));
        button("2周目 182.09% サンプル表示 ON/OFF",()->{secondLapPreview=!secondLapPreview;updatePreview();});
        warning=text("",12);warning.setTextColor(0xff9b4100);body.addView(warning);
        body.addView(text("プレビューは即時反映。ホーム画面には「設定を保存」で反映します。設定はすべての同種ウィジェットに共通です。",12));
        button("設定を保存",this::save);
        if(widgetId==AppWidgetManager.INVALID_APPWIDGET_ID)button("ホーム画面に追加",()->{
            if(!validateColors())return;settings.save(RevenueStore.prefs(this));RevenueWidget.renderAll(this);
            AppWidgetManager m=AppWidgetManager.getInstance(this);
            if(m.isRequestPinAppWidgetSupported())m.requestPinAppWidget(new ComponentName(this,RevenueWidget.class),null,null);
            else message("ホーム画面を長押しして「ウィジェット」から追加してください。");
        });
        body.addView(text("データ連携",18));
        body.addView(text("Couponは30分ごとの自動更新とタップ更新に対応。TikTok・目標は、普段使うサイトから受け取った表示用データです。サイトの値を変更したら再度連携してください。",13));
        button("THE FOOL QUESTから連携（通常サイト）",()->openExporter("https://the-fool-quest.45kikurage.workers.dev/widget-export.html"));
        button("GitHub Pages版から連携",()->openExporter("https://45kikurage-rgb.github.io/the-fool-quest/widget-export.html"));
        button("連携データを貼り付け",()->{
            EditText input=new EditText(this);input.setHint("サイトでコピーした連携データ");input.setMaxLines(5);
            new AlertDialog.Builder(this).setTitle("連携データ").setView(input).setNegativeButton("キャンセル",null)
                .setPositiveButton("確認",(d,w)->receive(input.getText().toString().trim())).show();
        });
        communication=text("",12);body.addView(communication);
        body.addView(text("表示設定",18));
        button("黒背景／白背景を反転",()->{
            boolean dark=Color.red(settings.background)+Color.green(settings.background)+Color.blue(settings.background)<384;
            settings.background=dark?Color.WHITE:Color.BLACK;settings.text=dark?Color.BLACK:Color.WHITE;
            colors.get("text").setText(hex(settings.text));colors.get("background").setText(hex(settings.background));updatePreview();
        });
        color("text","文字色",settings.text);color("background","背景色",settings.background);
        slider("背景の濃さ（100%＝不透明）",0,100,settings.opacity,v->settings.opacity=v);
        slider("文字サイズ（基準・枠に合わせて調整）",9,24,settings.font,v->settings.font=v);
        slider("左余白",0,32,settings.left,v->settings.left=v);slider("右余白",0,32,settings.right,v->settings.right=v);
        slider("上余白",0,24,settings.top,v->settings.top=v);slider("下余白",0,24,settings.bottom,v->settings.bottom=v);
        slider("ステータス行間",0,18,settings.gap,v->settings.gap=v);slider("ゲージ太さ（基準・高さに合わせて調整）",1,12,settings.gauge,v->settings.gauge=v);
        android.widget.CheckBox pace=new android.widget.CheckBox(this);pace.setText("進歩カラー（THE FOOL QUESTと同じ）");pace.setChecked(settings.paceColors);body.addView(pace);
        pace.setOnCheckedChangeListener((v,checked)->{settings.paceColors=checked;for(String key:new String[]{"total","tiktok","coupon"})colors.get(key).setEnabled(!checked);updatePreview();});
        color("total","Total 固定色（進歩カラーOFF時）",settings.total);color("tiktok","TikTok 固定色（進歩カラーOFF時）",settings.tiktok);color("coupon","Coupon 固定色（進歩カラーOFF時）",settings.coupon);
        for(String key:new String[]{"total","tiktok","coupon"})colors.get(key).setEnabled(!settings.paceColors);
        body.addView(text("日別進歩カラー・2周目カラー（色見本から変更できます）",18));
        colorPalette("paceAchieved","今日までの目標達成：100%以上",settings.paceAchieved);
        colorPalette("paceBehind","今日までの目標未達：50%以上100%未満",settings.paceBehind);
        colorPalette("paceLow","今日までの目標不足：50%未満",settings.paceLow);
        colorPalette("secondLap","2周目：月間目標100%超",settings.secondLap);
        button("表示設定を初期化",()->new AlertDialog.Builder(this).setMessage("表示設定を初期値に戻します。収益データは保持します。保存でホーム画面へ反映します。")
            .setNegativeButton("キャンセル",null).setPositiveButton("初期化",(d,w)->{settings=new DisplaySettings();build();}).show());
        body.addView(text("操作：ウィジェット全体をタップして更新。長押しでサイズ変更。表示設定のメニューはホームアプリによって異なります。このアプリからも設定できます。",12));
        body.addView(text("省電力中は自動更新が遅れる場合があります。通信失敗時は前回正常値を表示します。月をまたぐデータは合算しません。TikTok未連携時の「—」は0円ではありません。",12));
        updatePreview();
    }
    private interface Change{void set(int value);}
    private void slider(String label,int min,int max,int current,Change change){
        TextView title=text(label+"："+current,13);body.addView(title);
        SeekBar bar=new SeekBar(this);bar.setMax(max-min);bar.setProgress(current-min);body.addView(bar);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int v,boolean user){int n=v+min;title.setText(label+"："+n);change.set(n);updatePreview();}
            public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}
        });
    }
    private static String hex(int c){return String.format(java.util.Locale.US,"#%06X",c&0xffffff);}
    private void color(String key,String title,int value){
        body.addView(text(title+"（#RRGGBB）",13));EditText input=new EditText(this);input.setSingleLine(true);input.setText(hex(value));colors.put(key,input);body.addView(input);
        input.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){validateColors();updatePreview();}public void afterTextChanged(Editable e){}
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
        int w=preview.getWidth()>0?Math.round(preview.getWidth()/getResources().getDisplayMetrics().density):340;
        RevenueStore.Data d=RevenueStore.read(this);
        if(secondLapPreview){
            d.month=RevenueMath.month(System.currentTimeMillis());
            d.tiktok=0;d.coupon=546270;d.goalTotal=300000;d.goalTiktok=300000;d.goalCoupon=300000;
            d.importAt=System.currentTimeMillis();d.couponAt=d.importAt;d.loading=false;d.error="";
        }
        NativeWidgetViews.Result r=NativeWidgetViews.create(this,d,settings,w,126);
        preview.removeAllViews();preview.addView(r.views.apply(this,preview));
        preview.setContentDescription(WidgetRenderer.description(d));
        description.setText(secondLapPreview?"サンプルデータ：Coupon 182.09%（保存収益には影響しません）":WidgetRenderer.footer(d));
        String status=!colorValid?"色コードを確認してください。":r.adjusted?"この枠では文字・ゲージ・余白を安全に調整します（文字 "+String.format(java.util.Locale.JAPAN,"%.1f",r.font)+"dp）。":"数値列の位置を固定して表示します。ホーム画面の角で欠けないよう外周8dpを確保します。";
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
        else message("設定を保存しました。");
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
