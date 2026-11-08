package com.aruno.foolquest.widget.next;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.widget.*;
import org.json.JSONObject;
import java.io.File;
import java.util.*;

/** Recovery stays accessible even when the live revenue store is unreadable. */
public final class BackupActivity extends Activity {
    private static final int FOLDER=81,OPEN=82;
    private BackupCoordinator backups;private LinearLayout layout;private TextView status;
    private interface Task{void run()throws Exception;}
    @Override public void onCreate(Bundle b){super.onCreate(b);backups=BackupCoordinator.get(this);home();}
    private void text(String s){TextView t=new TextView(this);t.setText(s);t.setTextSize(15);t.setPadding(8,12,8,12);layout.addView(t);}
    private void button(String s,Runnable r){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setOnClickListener(v->r.run());layout.addView(b);}
    private void alert(String title,String message){if(!isFinishing())new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("OK",null).show();}
    private void work(Task task,Runnable done){
        status.setText("確認・保存中…");
        backups.worker.execute(()->{try{task.run();runOnUiThread(()->{if(!isFinishing()){home();if(done!=null)done.run();}});}catch(Exception e){backups.failure(e);runOnUiThread(()->{if(!isFinishing()){home();alert("バックアップを確認してください",e.getMessage()==null?"保存先またはファイルを確認してください":e.getMessage());}});}});
    }
    private void home(){
        ScrollView scroll=new ScrollView(this);layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);layout.setPadding(24,32,24,32);scroll.addView(layout);setContentView(scroll);
        text("自動バックアップ・復旧");status=new TextView(this);status.setText(backups.status());layout.addView(status);
        text("端末内のDocuments等に「Aruno-Result」専用フォルダーを作成して選んでください。初回以降は変更確定時に自動保存します。通常10世代＋日別10日分＋更新・復旧前10世代を別々に保持します。");
        button("保存先を選択・権限を再設定",()->{
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
            i.putExtra(DocumentsContract.EXTRA_INITIAL_URI,Uri.parse("content://com.android.externalstorage.documents/document/primary:Documents"));startActivityForResult(i,FOLDER);
        });
        button("今のデータを保全・保存を再試行",()->work(()->{backups.capture("safety",true);backups.flush();},null));
        button("保存済みバックアップの一覧",this::list);
        button("JSONファイルを選んで復旧",()->startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json"),OPEN));
        text("認証トークン・パスワードは保存しません。アプリデータ初期化後は保存先を再選択し、日別連携キーを再設定してください。ホームのウィジェット配置はAndroidが管理しており、消えた場合は再配置が必要です。保存済みの表示設定は復旧できます。\nCouponは読取用キャッシュとして保存します。復旧からサーバーへ書き込むことはありません。");
        button("収益管理へ戻る",this::finish);
    }
    private void list(){
        status.setText("バックアップを検証中…");backups.worker.execute(()->{
            List<String> labels=new ArrayList<>();List<String> copies=new ArrayList<>();int corrupt=0;
            try {
                String tree=backups.meta.getString("tree","");
                if(!tree.isEmpty()){
                    BackupFiles files=new BackupFiles(this,Uri.parse(tree));Map<String,Uri> docs=files.documents();
                    List<String> names=new ArrayList<>(docs.keySet());Collections.sort(names,Collections.reverseOrder());
                    for(String n:names)if(n.startsWith("aruno-result-")&&n.endsWith(".json"))try{
                        String json=BackupFiles.read(this,docs.get(n));JSONObject b=BackupFormat.parse(json);copies.add(json);labels.add(label(b,"端末フォルダー"));
                    }catch(Exception invalid){corrupt++;}
                }
                for(File f:backups.local())try{String json=BackupCoordinator.read(f);JSONObject b=BackupFormat.parse(json);if(!copies.contains(json)){copies.add(json);labels.add(label(b,"アプリ内退避"));}}catch(Exception invalid){corrupt++;}
                final int bad=corrupt;runOnUiThread(()->{
                    if(isFinishing())return;home();
                    if(labels.isEmpty()){alert("保存済みバックアップ", "正常なバックアップは見つかりませんでした。破損・未対応："+bad+"件");return;}
                    new AlertDialog.Builder(this).setTitle("復旧対象を選択（正常 "+labels.size()+"／破損・未対応 "+bad+"）")
                        .setItems(labels.toArray(new String[0]),(d,w)->preview(copies.get(w))).setNegativeButton("戻る",null).show();
                });
            }catch(Exception e){backups.failure(e);runOnUiThread(()->{home();alert("一覧を取得できません", "保存先の権限を確認してください。JSONを直接選択する方法でも復旧できます。");});}
        });
    }
    private String label(JSONObject b,String where)throws Exception {
        String kind=b.getString("kind");return RevenueMath.date("MM/dd HH:mm:ss",b.getLong("savedAt"))+"　"+b.getInt("records")+"件　"+("daily".equals(kind)?"日別":"safety".equals(kind)?"保全":"変更")+"（"+where+"）";
    }
    private void preview(String json){
        try {
            JSONObject b=BackupFormat.parse(json);
            String expected; synchronized(BackupCoordinator.LOCK){expected=BackupFormat.canonical(BackupFormat.encode(backups.raw.getAll()));}
            String message=BackupFormat.summary(b)+"\n\n現在の収益・手動追加・損失・目標・表示設定を置き換えます。確認後に変更があった場合は復旧を中止します。\n復旧前の現状を独立フォルダーへ保全できた場合だけ適用します。\nCouponの現在の取得済みキャッシュと認証設定は維持し、サーバーは変更しません。";
            new AlertDialog.Builder(this).setTitle("復旧内容の確認").setMessage(message).setNegativeButton("キャンセル",null)
                .setPositiveButton("確認して復旧する",(d,w)->work(()->backups.restore(json,expected),()->alert("復旧完了","復旧後の金額・目標・表示設定を確認してください。ウィジェットへ再描画を送信しました。"))).show();
        }catch(Exception e){alert("復旧できません",e.getMessage()==null?"破損・未対応のファイルです。現在のデータは変更しません。":e.getMessage());}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);
        if(result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        if(request==FOLDER)work(()->{backups.select(uri,data.getFlags());backups.flush();},null);
        else if(request==OPEN){status.setText("ファイルを検証中…");backups.worker.execute(()->{
            try{String json=BackupFiles.read(this,uri);BackupFormat.parse(json);runOnUiThread(()->{home();preview(json);});}
            catch(Exception e){runOnUiThread(()->{home();alert("復旧できません","破損・未対応のファイルです。現在のデータは変更しません。");});}
        });}
    }
}
