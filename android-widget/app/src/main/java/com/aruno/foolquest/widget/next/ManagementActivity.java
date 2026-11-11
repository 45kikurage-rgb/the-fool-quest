package com.aruno.foolquest.widget.next;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.AlertDialog;
import android.content.Context;
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
import android.util.TypedValue;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/** Offline-capable native revenue management; Coupon stays sourced from Ledger. */
public final class ManagementActivity extends Activity {
    private static final int OPEN_CSV=1, OPEN_MIGRATION=2, SAVE_BACKUP=3;
    static final String EXTRA_WIDGET_TAP="com.aruno.foolquest.widget.next.OPEN_REVENUE";
    static PendingIntent tapIntent(Context context) {
        Intent intent=new Intent(context,ManagementActivity.class)
            .putExtra(EXTRA_WIDGET_TAP,true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(context,2,intent,
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private RevenueManager model;
    private boolean showingHome;
    private LinearLayout content;
    private TextView status;
    private int background=0xff101010, foreground=0xffeeeeee;
    private final DayChangeObserver dayChanges=new DayChangeObserver(this,()->{
        if(model!=null&&showingHome)execute(this::showHome);
    });
    private interface Task {void run() throws Exception;}
    private void execute(Task t){try{t.run();}catch(Exception e){alert("操作できません",e.getMessage()==null?"保存・入力を確認してください":e.getMessage());}}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        execute(()->{model=new RevenueManager(this);model.projectWidget();showHome();});
        if(getIntent().getBooleanExtra(EXTRA_WIDGET_TAP,false))refreshFromWidget();
    }
    @Override protected void onResume(){super.onResume();if(model!=null)execute(()->{model=new RevenueManager(this);showHome();});dayChanges.start();}
    @Override protected void onPause(){dayChanges.stop();super.onPause();}
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);setIntent(intent);
        if(intent!=null && intent.getBooleanExtra(EXTRA_WIDGET_TAP,false)) {
            if(model!=null)execute(this::showHome);
            refreshFromWidget();
        }
    }
    private void refreshFromWidget() {
        if(model==null)return;
        RevenueJob.cancelQueuedManual(this);
        RevenueUpdate.start(this,()->runOnUiThread(()->{
            if(!isFinishing()&&!isDestroyed()&&showingHome)execute(this::showHome);
        }));
        if(showingHome&&status!=null)status.setText("更新中（正常値は維持）");
    }
    private void alert(String title,String message){
        if(isFinishing())return;
        new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("OK",null).setNeutralButton("バックアップ・復旧",(d,w)->startActivity(new Intent(this,BackupActivity.class))).show();
    }
    private void screen(String title) {
        showingHome=false;
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
    private String netMoney(long yen){return RevenueMath.moneyNet(yen);}
    private String lossLabel(String category){return "coupon".equals(category)?"クーポン":"TikTok Lite";}
    private String today(){return RevenueMath.date("yyyy-MM-dd",System.currentTimeMillis());}
    private void dailyGoal(String tag,String month,long goal,long now){
        TextView target=text(RevenueMath.dailyTargetLabel(month,goal,now),13);
        target.setTag(tag);target.setTextColor(0xffffd43b);target.setPadding(dp(4),0,dp(4),dp(8));
        content.addView(target);
    }
    private void showCouponDailyConnection(){
        android.widget.EditText key=new android.widget.EditText(this);
        key.setSingleLine(true);
        key.setImportantForAutofill(android.view.View.IMPORTANT_FOR_AUTOFILL_NO);
        key.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        key.setTypeface(getResources().getFont(R.font.aru_widget_rounded));
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20),0,dp(20),0);
        Button help=new Button(this);help.setText("専用キーの確認ページを開く");help.setAllCaps(false);
        help.setOnClickListener(v->openUrl("https://aruno-consolidated-ledger-api.45kikurage.workers.dev/api/v1/admin/mybest-reader-key"));
        form.addView(help);form.addView(key);
        new AlertDialog.Builder(this).setTitle("クーポン日別の接続設定")
            .setMessage("通常のパスワードではなく、日別収益の読取専用キーを入力します。確認ページで管理者認証後に表示されるキーをコピーし、ここへ貼り付けて保存してください。")
            .setView(form).setNegativeButton("戻る",null)
            .setNeutralButton("接続解除",(d,w)->execute(()->{
                CouponDailyReader.setToken(this,"");CalendarWidget.renderAll(this);
            }))
            .setPositiveButton("保存",(d,w)->execute(()->{
                String token=key.getText().toString().trim();
                if(token.isEmpty())throw new IllegalArgumentException("読取専用キーを入力してください");
                CouponDailyReader.setToken(this,token);
                RevenueJob.requestRefresh(this);
            })).show();
    }
    private void showHome() throws Exception {
        screen(getString(R.string.app_name));showingHome=true;
        String month=model.month();
        long tiktok=model.tiktok(month),coupon=model.coupon(month);
        long tLoss=model.lossTotal(month,"tiktok"),cLoss=model.lossTotal(month,"coupon");
        long tNet=model.netTikTok(month),cNet=model.netCoupon(month);
        long total=RevenueMath.netTotal(tNet,cNet);
        long tg=model.goal("tiktok"),cg=model.goal("coupon");
        long now=System.currentTimeMillis();
        heading(month+" 収益");
        content.addView(text("Total   "+netMoney(total)+" / "+money(tg+cg),19));
        dailyGoal("target-total",month,RevenueMath.total(tg,cg),now);
        content.addView(text("TikTok   "+netMoney(tNet)+" / "+money(tg),16));
        info("収益 "+money(tiktok)+" − 損失 "+money(tLoss));
        dailyGoal("target-tiktok",month,tg,now);
        content.addView(text("Coupon   "+netMoney(cNet)+" / "+money(cg),16));
        info("収益 "+money(coupon)+" − 損失 "+money(cLoss));
        dailyGoal("target-coupon",month,cg,now);
        if(coupon<0)info("Couponは未取得です。ネットワーク接続後に更新してください。");
        if(tiktok<0)info("TikTokは未登録です。CSVまたはサイトの旧データを取り込んでください。");
        info("ウィジェットは同じ端末内の収益・目標を表示します。Couponは中央管理台帳の読取専用です。");
        action("えらべるPayのポイント履歴をChromeで開く",
            ()->openUrl("https://wallet.vaton.jp/point/point_logs"));
        action("CSVを取り込む",()->pick("text/*",OPEN_CSV));
        action("データ更新（ポイント履歴を自動取得）",
            ()->startActivity(new Intent(this,PayFetchActivity.class)));
        action("TikTok収益を手動追加",this::showManualAdd);
        action("TikTok Liteの損失を手動追加",()->showLossAdd("tiktok"));
        action("クーポンの損失を手動追加",()->showLossAdd("coupon"));
        action("Couponを今すぐ更新",()->{
            RevenueUpdate.start(this,()->runOnUiThread(()->execute(this::showHome)));
            status.setText("更新中（正常値は維持）");
        });
        status=text(RevenueUpdate.busy()?"更新中（正常値は維持）":"",13);content.addView(status);
        action("月間目標を設定",this::showGoals);
        action("収益・損失履歴の編集",this::showHistory);
        action("表示設定・進捗ゲージ",()->startActivity(new Intent(this,SettingsActivity.class)));
        action("クーポン日別の接続設定",this::showCouponDailyConnection);
        action("バックアップ・復元",this::showBackupMenu);
        if(!BackupCoordinator.get(this).meta.getString("error","").isEmpty() || BackupCoordinator.get(this).meta.getString("tree","").isEmpty()){
            info(BackupCoordinator.get(this).status());
            action("自動バックアップの保存先を設定・確認",()->startActivity(new Intent(this,BackupActivity.class)));
        }
    }
    private void showBackupMenu() throws Exception {
        screen("バックアップ・復元");
        action("← 収益管理へ戻る",this::showHome);
        action("自動バックアップ・復旧を開く",()->startActivity(new Intent(this,BackupActivity.class)));
        info(BackupCoordinator.get(this).status());
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
        Intent browser=new Intent(Intent.ACTION_VIEW,Uri.parse(url));
        browser.setPackage("com.android.chrome");
        try { startActivity(browser); }
        catch(Exception e) {
            browser.setPackage(null);
            try { startActivity(browser); }
            catch(Exception unavailable) { alert("ブラウザを開けません",url); }
        }
    }
    private void pick(String mime,int code) {
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType(mime);i.addCategory(Intent.CATEGORY_OPENABLE);
        if(code==OPEN_MIGRATION)i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/json","text/plain","application/octet-stream"});
        if(code==OPEN_CSV)i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"text/csv","text/plain","text/comma-separated-values","application/vnd.ms-excel","application/octet-stream"});
        startActivityForResult(i,code);
    }
    private String csvAcquisitionDate(Uri uri, VatonRevenueCsv.CsvResult parsed) {
        if (!parsed.datesComplete) return "";
        String column=android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED;
        try (android.database.Cursor cursor=getContentResolver().query(uri,new String[]{column},null,null,null)) {
            if(cursor==null||!cursor.moveToFirst())return "";
            int index=cursor.getColumnIndex(column);
            if(index<0||cursor.isNull(index))return "";
            return VatonRevenueCsv.suggestAcquiredDate(parsed,cursor.getLong(index),System.currentTimeMillis());
        } catch(IllegalArgumentException|SecurityException ignored) {
            return "";
        }
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
                    VatonRevenueCsv.CsvResult parsed=VatonRevenueCsv.parseCsv(input);
                    String name=uri.getLastPathSegment();
                    if(name==null)name="選択したCSV";
                    final String file=name;
                    String month=parsed.datesComplete ? parsed.datesMonth : model.month();
                    long old=model.csvBase(month);
                    EditText acquired=new EditText(this);
                    acquired.setSingleLine(true);
                    acquired.setHint("取得日（任意） yyyy-MM-dd");
                    String candidate=csvAcquisitionDate(uri,parsed);
                    if(!candidate.isEmpty())acquired.setText(candidate);
                    AlertDialog.Builder confirm=new AlertDialog.Builder(this).setTitle("CSV反映の確認")
                        .setMessage("対象月："+month+"\n新CSV："+money(parsed.total)+"（"+parsed.count+"件）"
                            +"\n日別："+(parsed.datesComplete?"保存可能":"日付を確認できないため月計のみ")
                            +"\n既存CSV："+money(old)
                            +"\n\n既存CSV合計を置き換えます。手動追加分は別に保持します。"
                            +(parsed.datesComplete?"\n取得日まで明細のない日は0円で表示します。":"")
                            +(!candidate.isEmpty()?"\nファイルの最終更新日時から取得日を入力しました。違う場合は修正してください。":""))
                        .setNegativeButton("やめる",null)
                        .setPositiveButton("この金額に更新",(d,w)->execute(()->{
                            model.setCsv(month,parsed,file,acquired.getText().toString().trim());showHome();
                        }));
                    if(parsed.datesComplete)confirm.setView(acquired);
                    confirm.show();
                } else if(request==OPEN_MIGRATION) {
                    String json=decodeUtf8(input);
                    JSONObject payload=new JSONObject(json);
                    RevenueManager.validate(payload);
                    int n=payload.getJSONObject("months").length(),m=payload.getJSONArray("manual").length();
                    new AlertDialog.Builder(this).setTitle("旧サイトの履歴を移行")
                        .setMessage("対象：月別"+n+"件／手動追加"+m+"件\n\nアプリ内の既存履歴・目標を移行ファイルの内容で置き換えます。既存アプリデータを残す場合、先にJSONバックアップしてください。Couponの当月値は中央管理台帳から取得します。")
                        .setNegativeButton("キャンセル",null)
                        .setPositiveButton("移行する",(d,w)->execute(()->{
                            BackupCoordinator.get(this).capture("safety",true);
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
        EditText date=field(today(),false),value=field("",true);
        new AlertDialog.Builder(this).setTitle("TikTok収益の手動追加")
            .setView(form(new String[]{"日付（yyyy-MM-dd）","追加金額（円）"},new EditText[]{date,value}))
            .setNegativeButton("やめる",null)
            .setPositiveButton("追加",(d,w)->execute(()->{
                model.addManual(date.getText().toString(),RevenueManager.parseAmount(value.getText().toString(),true));
                showHome();
            })).show();
    }
    private void showLossAdd(String category) throws Exception {
        RevenueManager.requireCategory(category);
        EditText date=field(today(),false),value=field("",true);
        new AlertDialog.Builder(this).setTitle(lossLabel(category)+"の損失を手動追加")
            .setView(form(new String[]{"損失日（yyyy-MM-dd）","損失金額（円）"},new EditText[]{date,value}))
            .setNegativeButton("やめる",null)
            .setPositiveButton("損失を登録",(d,w)->execute(()->{
                model.addLoss(date.getText().toString(),category,
                    RevenueManager.parseAmount(value.getText().toString(),true));
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
    private String tableAmount(long yen) {
        return yen==RevenueMath.UNKNOWN?"—":
            (yen<0?"−":"")+String.format(Locale.JAPAN,"%,d",Math.abs(yen));
    }
    private TextView historyCell(String label,int position,boolean header,boolean strong) {
        TextView cell=new TextView(this);
        cell.setText(label);
        cell.setTextColor(0xfff6f6f6);
        cell.setSingleLine(true);
        cell.setGravity((position==0?Gravity.START:Gravity.END)|Gravity.CENTER_VERTICAL);
        cell.setPadding(dp(position==0?4:2),dp(3),dp(position==0?1:3),dp(3));
        if(header||strong)cell.setTypeface(getResources().getFont(R.font.aru_widget_rounded));
        cell.setAutoSizeTextTypeUniformWithConfiguration(9,header?12:13,1,TypedValue.COMPLEX_UNIT_SP);
        android.graphics.drawable.GradientDrawable border=new android.graphics.drawable.GradientDrawable();
        border.setColor(0xff090909);
        border.setStroke(dp(1),0xffeeeeee);
        cell.setBackground(border);
        cell.setContentDescription(label);
        return cell;
    }
    private LinearLayout historyRow(LinearLayout table,String[] data,boolean header,boolean strong,String description,Task tap) {
        LinearLayout row=new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        float[] weights={0.19f,0.27f,0.27f,0.27f};
        for(int i=0;i<4;i++){
            TextView cell=historyCell(data[i],i,header,strong);
            row.addView(cell,new LinearLayout.LayoutParams(0,dp(header?44:50),weights[i]));
        }
        if(tap!=null){
            row.setClickable(true);
            row.setFocusable(true);
            row.setContentDescription(description+"　詳細を開く");
            row.setOnClickListener(v->execute(tap));
        }else row.setContentDescription(description);
        table.addView(row,new LinearLayout.LayoutParams(-1,-2));
        return row;
    }
    private void showHistory() throws Exception {
        screen("収益履歴");
        action("← 管理トップへ",this::showHome);
        info("単位：円　損失控除後の収支を表示。月を押すと収益・損失の編集／削除ができます。");
        // Present all known source months, including Coupon-only archived months.
        List<String> months=new ArrayList<>();
        for(Iterator<String> it=model.months().keys();it.hasNext();)months.add(it.next());
        for(String m:model.archivedCouponMonths())if(!months.contains(m))months.add(m);
        JSONArray lossRows=model.losses();
        for(int i=0;i<lossRows.length();i++){
            String m=lossRows.getJSONObject(i).getString("date").substring(0,7);
            if(!months.contains(m))months.add(m);
        }
        if(!months.contains(model.month()))months.add(model.month());
        Collections.sort(months,Collections.reverseOrder());
        long allTikTok=0,allCoupon=0;
        java.util.Map<String,long[]> years=new java.util.LinkedHashMap<>();
        java.util.Map<String,long[]> monthValues=new java.util.LinkedHashMap<>();
        for(String month:months){
            long t=model.netTikTok(month),c=model.netCoupon(month);
            monthValues.put(month,new long[]{t,c});
            String year=month.substring(0,4);
            long[] totals=years.get(year);
            if(totals==null){totals=new long[2];years.put(year,totals);}
            if(t!=RevenueMath.UNKNOWN){totals[0]=RevenueMath.netTotal(totals[0],t);allTikTok=RevenueMath.netTotal(allTikTok,t);}
            if(c!=RevenueMath.UNKNOWN){totals[1]=RevenueMath.netTotal(totals[1],c);allCoupon=RevenueMath.netTotal(allCoupon,c);}
        }
        // The entire table uses the same four columns and narrow borders as the former site.
        LinearLayout table=new LinearLayout(this);
        table.setOrientation(LinearLayout.VERTICAL);
        table.setBackgroundColor(0xff090909);
        content.addView(table,new LinearLayout.LayoutParams(-1,-2));
        historyRow(table,new String[]{"","合計","TikTok","Coupon"},true,true,"収益履歴の項目",null);
        historyRow(table,new String[]{"全期間",tableAmount(RevenueMath.netTotal(allTikTok,allCoupon)),tableAmount(allTikTok),tableAmount(allCoupon)},
            false,true,"全期間収益",null);
        for(java.util.Map.Entry<String,long[]> year:years.entrySet()) {
            String y=year.getKey();long[] values=year.getValue();
            historyRow(table,new String[]{y+"年",tableAmount(RevenueMath.netTotal(values[0],values[1])),
                tableAmount(values[0]),tableAmount(values[1])},false,true,y+"年収益",null);
            for(String m:months){
                if(!m.startsWith(y+"-"))continue;
                long[] numbers=monthValues.get(m);
                long total=RevenueMath.netTotal(numbers[0],numbers[1]);
                if(total==RevenueMath.UNKNOWN)
                    total=numbers[0]!=RevenueMath.UNKNOWN?numbers[0]:numbers[1];
                String label=String.valueOf(Integer.parseInt(m.substring(5)))+"月";
                historyRow(table,new String[]{label,tableAmount(total),tableAmount(numbers[0]),tableAmount(numbers[1])},
                    false,false,m+"収益",()->showMonthDetails(m));
            }
        }
    }
    private void showMonthDetails(String month) throws Exception {
        screen(month+" 収益");
        action("← 収益履歴へ",this::showHistory);
        content.addView(text("TikTok："+netMoney(model.netTikTok(month))+
            "（損失 "+money(model.lossTotal(month,"tiktok"))+"）",18));
        content.addView(text("Coupon："+netMoney(model.netCoupon(month))+
            "（損失 "+money(model.lossTotal(month,"coupon"))+"）",18));
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
        if(!found)info("収益の手動追加はありません");
        heading("損失の手動追加");
        JSONArray losses=model.losses();
        boolean hasLoss=false;
        for(int i=0;i<losses.length();i++){
            JSONObject row=losses.getJSONObject(i);
            if(!row.getString("date").startsWith(month))continue;
            hasLoss=true;String id=row.getString("id");
            Button b=action(row.getString("date")+" "+lossLabel(row.getString("category"))+
                " −"+money(row.getLong("amount"))+"  編集／削除",
                ()->showEditLoss(id,month));
            b.setGravity(Gravity.LEFT);
        }
        if(!hasLoss)info("損失の手動追加はありません");
    }
    private void showEditLoss(String id,String month) throws Exception {
        JSONObject entry=null;
        JSONArray rows=model.losses();
        for(int i=0;i<rows.length();i++)
            if(id.equals(rows.getJSONObject(i).optString("id")))entry=rows.getJSONObject(i);
        if(entry==null){showMonthDetails(month);return;}
        String category=entry.getString("category");
        EditText date=field(entry.getString("date"),false);
        EditText value=field(String.valueOf(entry.getLong("amount")),true);
        new AlertDialog.Builder(this).setTitle(lossLabel(category)+"の損失を編集")
            .setView(form(new String[]{"損失日（yyyy-MM-dd）","損失金額（円）"},new EditText[]{date,value}))
            .setNeutralButton("削除",(d,w)->new AlertDialog.Builder(this)
                .setTitle("損失履歴を削除しますか？")
                .setMessage("この損失記録だけを削除します。元の収益は変更しません。")
                .setNegativeButton("キャンセル",null)
                .setPositiveButton("削除",(x,y)->execute(()->{
                    model.deleteLoss(id);showMonthDetails(month);
                })).show())
            .setNegativeButton("戻る",null)
            .setPositiveButton("変更を保存",(d,w)->execute(()->{
                model.updateLoss(id,date.getText().toString(),category,
                    RevenueManager.parseAmount(value.getText().toString(),true));
                showMonthDetails(month);
            })).show();
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
}
