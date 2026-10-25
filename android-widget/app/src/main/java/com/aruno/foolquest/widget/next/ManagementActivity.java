package com.aruno.foolquest.widget.next;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/** Offline-capable native revenue management; Coupon stays sourced from Ledger. */
public final class ManagementActivity extends Activity {
    private static final int OPEN_CSV=1, OPEN_MIGRATION=2, SAVE_BACKUP=3;
    private RevenueManager model;
    private LinearLayout content;
    private TextView status;
    private int background=0xff101010, foreground=0xffeeeeee;
    private interface Task {void run() throws Exception;}
    private void execute(Task t){try{t.run();}catch(Exception e){alert("操作できません",e.getMessage()==null?"保存・入力を確認してください":e.getMessage());}}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        execute(()->{model=new RevenueManager(this);model.projectWidget();showHome();});
    }
    @Override protected void onResume(){super.onResume();if(model!=null)execute(this::showHome);}
    private void alert(String title,String message){
        if(isFinishing())return;
        new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("OK",null).show();
    }
    private void screen(String title) {
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);scroll.setBackgroundColor(background);
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16),dp(24),dp(16),dp(28));scroll.addView(content);setContentView(scroll);
        TextView head=text(title,22);head.setGravity(Gravity.CENTER_VERTICAL);content.addView(head);
    }
    private TextView text(String value,int size) {
        TextView t=new TextView(this);t.setText(value);t.setTextColor(foreground);
        t.setTextSize(size);t.setPadding(dp(4),dp(10),dp(4),dp(10));return t;
    }
    private void info(String value){TextView t=text(value,13);t.setTextColor(0xffbbbbbb);content.addView(t);}
    private Button action(String name,Task task) {
        Button button=new Button(this);button.setAllCaps(false);button.setText(name);
        button.setOnClickListener(v->execute(task));content.addView(button,new LinearLayout.LayoutParams(-1,-2));return button;
    }
    private void heading(String value) {
        TextView t=text(value,18);t.setTextColor(0xffcaff40);content.addView(t);
    }
    private String money(long yen){return RevenueMath.money(yen);}
    private String today(){return RevenueMath.date("yyyy-MM-dd",System.currentTimeMillis());}
    private void showHome() throws Exception {
        screen("THE FOOL QUEST");
        String month=model.month();
        long tiktok=model.tiktok(month),coupon=model.coupon(month);
        long total=RevenueMath.total(tiktok,coupon);
        long tg=model.goal("tiktok"),cg=model.goal("coupon");
        heading(month+" 収益");
        content.addView(text("Total   "+money(total)+" / "+money(tg+cg),19));
        content.addView(text("TikTok   "+money(tiktok)+" / "+money(tg),16));
        content.addView(text("Coupon   "+money(coupon)+" / "+money(cg),16));
        if(coupon<0)info("Couponは未取得です。ネットワーク接続後に更新してください。");
        if(tiktok<0)info("TikTokは未登録です。CSVまたはサイトの旧データを取り込んでください。");
        info("ウィジェットは同じ端末内の収益・目標を表示します。Couponは中央管理台帳の読取専用です。");
        action("CSVを取り込む",()->pick("text/*",OPEN_CSV));
        action("TikTok収益を手動追加",this::showManualAdd);
        action("Couponを今すぐ更新",()->{
            RevenueUpdate.start(this,()->runOnUiThread(()->execute(this::showHome)));
            status.setText("更新中（正常値は維持）");
        });
        status=text("",13);content.addView(status);
        action("月間目標を設定",this::showGoals);
        action("収益履歴・手動追加の編集",this::showHistory);
        action("稼働数・収益を検証",this::showSimulation);
        action("表示設定・進捗ゲージ",()->startActivity(new Intent(this,SettingsActivity.class)));
        action("えらべるPayのポイント履歴をChromeで開く",
            ()->openUrl("https://wallet.vaton.jp/point/point_logs"));
        heading("サイトからの一度限りの移行");
        info("旧サイトと同じChromeで「移行ファイル作成」を開き、保存したJSONをここで取り込んでください。現在のアプリデータを上書きする前に確認します。");
        action("旧サイトの移行ファイル作成ページ",()->
            openUrl("https://the-fool-quest.45kikurage.workers.dev/manage-export.html"));
        action("GitHub Pages版の移行ファイル作成ページ",()->
            openUrl("https://45kikurage-rgb.github.io/the-fool-quest/manage-export.html"));
        action("旧サイトのJSONを取り込む",()->pick("application/json",OPEN_MIGRATION));
        action("アプリの全履歴をJSONでバックアップ",()->{
            Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE,"the-fool-quest-backup-"+today()+".json");
            startActivityForResult(intent,SAVE_BACKUP);
        });
        info("サイト内の元データは削除しません。バックアップも端末外に保存してください。");
    }
    private void openUrl(String url) {
        Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse(url));
        try{startActivity(i);}catch(Exception e){alert("ブラウザを開けません",url);}
    }
    private void pick(String mime,int code) {
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType(mime);i.addCategory(Intent.CATEGORY_OPENABLE);
        if(code==OPEN_MIGRATION)i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/json","text/plain","application/octet-stream"});
        if(code==OPEN_CSV)i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"text/csv","text/plain","text/comma-separated-values","application/vnd.ms-excel","application/octet-stream"});
        startActivityForResult(i,code);
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        execute(()->{
            if(request==SAVE_BACKUP) {
                try(OutputStream stream=getContentResolver().openOutputStream(uri)) {
                    if(stream==null)throw new IllegalArgumentException("出力先が開けません");
                    stream.write(model.backup().getBytes(StandardCharsets.UTF_8));
                }
                alert("バックアップ完了","ファイルを保存しました。既存サイトの記録は変更していません。");
            }else {
                byte[] input=readLimited(uri,request==OPEN_CSV?4_000_000:2_000_000);
                if(request==OPEN_CSV) {
                    String utf8=decodeUtf8(input);
                    RevenueManager.CsvResult parsed;
                    try{parsed=RevenueManager.parseCsv(utf8);}
                    catch(IllegalArgumentException error){
                        String legacy=new String(input,Charset.forName("MS932"));
                        parsed=RevenueManager.parseCsv(legacy);
                    }
                    final RevenueManager.CsvResult resultCsv=parsed;
                    String name=uri.getLastPathSegment();
                    if(name==null)name="選択したCSV";
                    final String file=name;
                    String month=model.month();
                    long old=model.csvBase(month);
                    new AlertDialog.Builder(this).setTitle("CSV反映の確認")
                        .setMessage("対象月："+month+"\n新CSV："+money(parsed.total)+"（"+parsed.count+"件）"
                            +"\n既存CSV："+money(old)
                            +"\n\n既存CSV合計を置き換えます。手動追加分は別に保持するため二重加算しません。")
                        .setNegativeButton("やめる",null)
                        .setPositiveButton("この金額に更新",(d,w)->execute(()->{
                            model.setCsv(month,resultCsv.total,file,resultCsv.count);showHome();
                        })).show();
                } else if(request==OPEN_MIGRATION) {
                    String json=decodeUtf8(input);
                    JSONObject payload=new JSONObject(json);
                    RevenueManager.validate(payload);
                    int n=payload.getJSONObject("months").length(),m=payload.getJSONArray("manual").length();
                    new AlertDialog.Builder(this).setTitle("旧サイトの履歴を移行")
                        .setMessage("対象：月別"+n+"件／手動追加"+m+"件\n\nアプリ内の既存履歴・目標を移行ファイルの内容で置き換えます。既存アプリデータを残す場合、先にJSONバックアップしてください。Couponの当月値は中央管理台帳から取得します。")
                        .setNegativeButton("キャンセル",null)
                        .setPositiveButton("移行する",(d,w)->execute(()->{
                            model.replaceFromJson(json);showHome();
                            alert("移行完了","収益・目標・過去の履歴を取り込みました。実際の金額と照合してください。");
                        })).show();
                }
            }
        });
    }
    private String decodeUtf8(byte[] bytes) throws Exception {
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString();
        }catch(CharacterCodingException e){throw new IllegalArgumentException("UTF-8で読み取れません");}
    }
    private byte[] readLimited(Uri uri,int limit) throws Exception {
        try(InputStream stream=getContentResolver().openInputStream(uri)){
            if(stream==null)throw new IllegalArgumentException("ファイルが開けません");
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int count;
            while((count=stream.read(buffer))!=-1){
                if(out.size()+count>limit)throw new IllegalArgumentException("ファイルサイズが上限を超えています");
                out.write(buffer,0,count);
            }
            return out.toByteArray();
        }
    }
    private EditText field(String initial,boolean number){
        EditText input=new EditText(this);input.setSingleLine(true);input.setText(initial);
        input.setTextColor(0xff222222);
        if(number)input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        return input;
    }
    private LinearLayout form(String[] labels,EditText[] inputs) {
        LinearLayout group=new LinearLayout(this);group.setOrientation(LinearLayout.VERTICAL);
        group.setPadding(dp(16),dp(12),dp(16),dp(8));
        for(int i=0;i<inputs.length;i++){
            TextView label=new TextView(this);label.setText(labels[i]);label.setTextColor(0xff333333);
            group.addView(label);group.addView(inputs[i]);
        }
        return group;
    }
    private void showManualAdd() throws Exception {
        if(model.csvBase(model.month())<0) {
            alert("先にCSVを取り込んでください","移行元のCSV金額がないと、手動追加との二重計上を防げません。");
            return;
        }
        EditText date=field(today(),false),value=field("",true);
        new AlertDialog.Builder(this).setTitle("TikTok収益の手動追加")
            .setView(form(new String[]{"日付（yyyy-MM-dd）","追加金額（円）"},new EditText[]{date,value}))
            .setNegativeButton("やめる",null)
            .setPositiveButton("追加",(d,w)->execute(()->{
                model.addManual(date.getText().toString(),RevenueManager.parseAmount(value.getText().toString(),true));
                showHome();
            })).show();
    }
    private void showGoals() throws Exception {
        EditText t=field(String.valueOf(model.goal("tiktok")),true);
        EditText c=field(String.valueOf(model.goal("coupon")),true);
        new AlertDialog.Builder(this).setTitle("月間目標設定")
            .setView(form(new String[]{"TikTok（円）","Coupon（円）"},new EditText[]{t,c}))
            .setNegativeButton("キャンセル",null)
            .setPositiveButton("保存",(d,w)->execute(()->{
                model.setGoals(RevenueManager.parseAmount(t.getText().toString(),true),
                    RevenueManager.parseAmount(c.getText().toString(),true));
                showHome();
            })).show();
    }
    private void showHistory() throws Exception {
        screen("収益履歴");
        action("← 管理トップへ",this::showHome);
        info("単位：円。過去月のCouponは移行済みの履歴値、当月Couponは中央台帳の取得値です。");
        List<String> months=new ArrayList<>();
        for(Iterator<String> i=model.months().keys();i.hasNext();)months.add(i.next());
        if(!months.contains(model.month()))months.add(model.month());
        Collections.sort(months,Collections.reverseOrder());
        long allTikTok=0,allCoupon=0;
        java.util.Map<String,long[]> yearTotals=new java.util.TreeMap<>(Collections.reverseOrder());
        for(String month:months) {
            long t=model.tiktok(month),c=model.coupon(month);
            long[] y=yearTotals.get(month.substring(0,4));
            if(y==null){y=new long[2];yearTotals.put(month.substring(0,4),y);}
            if(t>=0){allTikTok=RevenueManager.plus(allTikTok,t);y[0]=RevenueManager.plus(y[0],t);}
            if(c>=0){allCoupon=RevenueManager.plus(allCoupon,c);y[1]=RevenueManager.plus(y[1],c);}
        }
        heading("全期間 Total "+money(RevenueManager.plus(allTikTok,allCoupon)));
        info("TikTok "+money(allTikTok)+" / Coupon "+money(allCoupon));
        for(java.util.Map.Entry<String,long[]> y:yearTotals.entrySet()){
            long[] x=y.getValue();heading(y.getKey()+"年 "+money(RevenueManager.plus(x[0],x[1])));
        }
        heading("月別");
        for(String m:months) {
            long t=model.tiktok(m),c=model.coupon(m);
            long total=RevenueMath.total(t,c);
            if(total<0)total=t>=0?t:c;
            Button b=action(m+"　"+money(total)+"\nTikTok "+money(t)+"　Coupon "+money(c),()->showMonthDetails(m));
            b.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);
        }
    }
    private void showMonthDetails(String month) throws Exception {
        screen(month+" 収益");
        action("← 収益履歴へ",this::showHistory);
        content.addView(text("TikTok："+money(model.tiktok(month)),19));
        content.addView(text("Coupon："+money(model.coupon(month)),19));
        heading("手動追加");
        JSONArray rows=model.manual();
        boolean found=false;
        for(int i=0;i<rows.length();i++){
            JSONObject row=rows.getJSONObject(i);
            if(!row.getString("date").startsWith(month))continue;
            found=true;String id=row.getString("id");
            Button b=action(row.getString("date")+"   "+money(row.getLong("amount"))+"  編集／削除",
                ()->showEditManual(id,month));
            b.setGravity(Gravity.LEFT);
        }
        if(!found)info("手動追加はありません");
    }
    private void showEditManual(String id,String month) throws Exception {
        JSONObject entry=null;
        JSONArray rows=model.manual();
        for(int i=0;i<rows.length();i++)if(id.equals(rows.getJSONObject(i).optString("id")))entry=rows.getJSONObject(i);
        if(entry==null){showHistory();return;}
        EditText date=field(entry.getString("date"),false);
        EditText value=field(String.valueOf(entry.getLong("amount")),true);
        new AlertDialog.Builder(this).setTitle("手動追加を編集")
            .setView(form(new String[]{"日付（yyyy-MM-dd）","金額（円）"},new EditText[]{date,value}))
            .setNeutralButton("削除",(d,w)->new AlertDialog.Builder(this).setTitle("履歴を削除しますか？")
                .setMessage("この手動追加だけを削除します。")
                .setNegativeButton("キャンセル",null).setPositiveButton("削除",(x,y)->execute(()->{
                    model.deleteManual(id);showMonthDetails(month);
                })).show())
            .setNegativeButton("戻る",null)
            .setPositiveButton("変更を保存",(d,w)->execute(()->{
                model.updateManual(id,date.getText().toString(),RevenueManager.parseAmount(value.getText().toString(),true));
                showMonthDetails(month);
            })).show();
    }
    private void showSimulation() throws Exception {
        screen("稼働数・収益検証");
        info("既存サイトと同じ14日稼働・休憩日数・保有台数・1台粗利の試算です。実際の収益データは変更しません。");
        JSONObject s=model.simulation();
        EditText rest=field(String.valueOf(s.getLong("restDays")),true);
        EditText owned=field(String.valueOf(s.getLong("owned")),true);
        EditText amount=field(String.valueOf(s.getLong("amount")),true);
        content.addView(form(new String[]{"休憩日数","保有台数","1台あたり粗利（円）"},new EditText[]{rest,owned,amount}));
        action("試算する",()->{
            long r=RevenueManager.parseAmount(rest.getText().toString(),false);
            long o=RevenueManager.parseAmount(owned.getText().toString(),false);
            long a=RevenueManager.parseAmount(amount.getText().toString(),false);
            model.setSimulation(o,r,a);
            // Preserve the site's original integer-floor formula and its two run cadences.
            long starts30=(long)Math.floor((double)o/(14+r));
            long starts20=(long)Math.floor((double)o/(14+r)*30/20);
            long day=RevenueManager.plus(0,starts30*a);
            long monthly=day*30,active=starts30*14;
            alert("検証結果","月30日作業：1日 "+starts30+"台\n月20日作業：1日 "+starts20+"台"
                +"\n1日収益："+money(day)+"\n月間収益："+money(monthly)
                +"\n最大稼働台数："+active+"台");
        });
        action("← 管理トップへ",this::showHome);
    }
}