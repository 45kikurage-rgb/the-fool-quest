package com.aruno.foolquest.widget.next;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.util.AtomicFile;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Independent append-only journal + durable outbox to a user-selected device folder. */
final class BackupCoordinator {
    static final Object LOCK=new Object();
    private static BackupCoordinator singleton;
    final Context context;final SharedPreferences raw,meta;final BackupPreferences preferences;
    final File directory;final ExecutorService worker=Executors.newSingleThreadExecutor();
    private boolean scheduled;private String lastToast="";
    static BackupCoordinator get(Context c){synchronized(LOCK){
        if(singleton==null){singleton=new BackupCoordinator(c.getApplicationContext());singleton.start();}return singleton;
    }}
    BackupCoordinator(Context c){
        context=c;raw=c.getSharedPreferences("widget",Context.MODE_PRIVATE);meta=c.getSharedPreferences("resultBackupV1",Context.MODE_PRIVATE);
        directory=new File(c.getFilesDir(),"result-backups-v1");directory.mkdirs();preferences=new BackupPreferences(raw,this);
    }
    int version(){try{return context.getPackageManager().getPackageInfo(context.getPackageName(),0).versionCode;}catch(Exception e){return 0;}}
    static String read(File file)throws Exception {
        byte[] bytes=new AtomicFile(file).readFully();if(bytes.length>BackupFormat.LIMIT)throw new IOException("バックアップが大きすぎます");
        return new String(bytes,StandardCharsets.UTF_8);
    }
    static void write(File file,String json)throws Exception {
        AtomicFile atomic=new AtomicFile(file);FileOutputStream stream=null;
        try{stream=atomic.startWrite();stream.write(json.getBytes(StandardCharsets.UTF_8));atomic.finishWrite(stream);stream=null;
            if(!json.equals(read(file)))throw new IOException("端末の保存後照合に失敗しました");
        }catch(Exception e){if(stream!=null)atomic.failWrite(stream);throw e;}
    }
    List<File> local(){
        File[] files=directory.listFiles((d,n)->n.matches("aruno-result-(change|daily|safety)-[0-9]+-[a-f0-9-]+\\.json"));
        List<File> out=new ArrayList<>(files==null?Collections.emptyList():Arrays.asList(files));
        Collections.sort(out,(a,b)->b.getName().compareTo(a.getName()));return out;
    }
    private File append(String json)throws Exception {
        JSONObject b=BackupFormat.parse(json);String name="aruno-result-"+b.getString("kind")+"-"+b.getLong("savedAt")+"-"+UUID.randomUUID()+".json";
        File out=new File(directory,name);write(out,json);return out;
    }
    private void capacity()throws Exception {
        long bytes=0;for(File file:local())bytes+=file.length();
        if(bytes>32L*1024*1024)throw new IOException("未保存のバックアップが32MBを超えました。保存先の権限を確認して再試行してください");
    }
    private void recoverTransaction(Map<String,?> current)throws Exception {
        File tx=new File(directory,"transaction.json");
        if(tx.exists()||new File(directory,"transaction.json.bak").exists()){
            String staged=read(tx);JSONObject b=BackupFormat.parse(staged);
            if(BackupFormat.canonical(BackupFormat.encode(current)).equals(BackupFormat.canonical(b.getJSONObject("preferences"))))append(staged);
            else write(new File(directory,"uncertain-"+System.currentTimeMillis()+"-"+UUID.randomUUID()+".json"),staged);
            new AtomicFile(tx).delete();
        }
    }
    private void start(){
        try {
            recoverTransaction(raw.getAll());
            Map<String,Object> current=BackupFormat.select(raw.getAll());
            if(!current.isEmpty()){
                BackupFormat.encode(current);
                int previous=meta.getInt("appVersion",0);
                if(previous!=version())capture("safety",true);
                else capture("change",false);
            }
            meta.edit().putInt("appVersion",version()).commit();
        }catch(Exception e){failure(e);}
        retry();
    }
    /** Caller holds LOCK. Write-ahead record exists before touching normal data. */
    boolean commit(Map<String,Object> before,Map<String,Object> after)throws Exception {
        Map<String,Object> old=BackupFormat.select(before),next=BackupFormat.select(after);
        if(old.equals(next))return BackupPreferences.replace(raw,after);
        recoverTransaction(before);
        BackupFormat.encode(next);
        if(old.containsKey("nativeRevenueV1")&&!next.containsKey("nativeRevenueV1"))throw new IOException("収益履歴の初期化を防止しました。復旧画面を確認してください");
        int oldRecords=BackupFormat.records(old),newRecords=BackupFormat.records(next);
        if(newRecords==0&&(oldRecords>0||meta.getInt("lastNonemptyRecords",0)>0))throw new IOException("空データによる置換を防止しました。バックアップから復旧してください");
        capacity();
        String json=BackupFormat.create(after,"change",System.currentTimeMillis(),version());
        File tx=new File(directory,"transaction.json");write(tx,json);
        if(!BackupPreferences.replace(raw,after)){
            BackupPreferences.replace(raw,before);throw new IOException("通常データを保存できませんでした");
        }
        if(!after.equals(raw.getAll())){BackupPreferences.replace(raw,before);throw new IOException("通常データと確定内容が一致しません");}
        try {
            append(json);new AtomicFile(tx).delete();
            if(newRecords>0)meta.edit().putInt("lastNonemptyRecords",newRecords).commit();
            daily(after);pruneLocal();
        }catch(Exception e){failure(e);/* durable write-ahead record remains for startup recovery */}
        retry();return true;
    }
    void capture(String kind,boolean force)throws Exception {
        synchronized(LOCK){
            Map<String,Object> current=BackupFormat.select(raw.getAll());
            if(current.isEmpty())return;
            int n=BackupFormat.records(current);
            if(n==0&&meta.getInt("lastNonemptyRecords",0)>0)throw new IOException("収益が空です。正常バックアップを保護しています");
            String payload=BackupFormat.canonical(BackupFormat.encode(current));
            if(!force)for(File f:local())try{JSONObject b=BackupFormat.parse(read(f));if(payload.equals(BackupFormat.canonical(b.getJSONObject("preferences")))){daily(current);return;}}catch(Exception ignored){}
            capacity();append(BackupFormat.create(current,kind,System.currentTimeMillis(),version()));
            if(n>0)meta.edit().putInt("lastNonemptyRecords",n).commit();
            daily(current);pruneLocal();
        }
    }
    private void daily(Map<String,?> current)throws Exception {
        if(BackupFormat.records(BackupFormat.select(current))==0)return;
        String today=RevenueMath.date("yyyy-MM-dd",System.currentTimeMillis());
        if(today.equals(meta.getString("localDay","")))return;
        append(BackupFormat.create(current,"daily",System.currentTimeMillis(),version()));
        if(!meta.edit().putString("localDay",today).commit())throw new IOException("日別保存状態を記録できません");
    }
    private void pruneLocal(){
        Map<String,Integer> counts=new HashMap<>();
        for(File f:local())try {
            String kind=BackupFormat.parse(read(f)).getString("kind");int n=counts.getOrDefault(kind,0)+1;counts.put(kind,n);
            // Never discard an unexported confirmed change.
            if(n>10&&new File(directory,f.getName()+".sent").exists()){
                new AtomicFile(f).delete();new File(directory,f.getName()+".sent").delete();
            }
        }catch(Exception invalid){/* retain corruption for inspection */}
    }
    void failure(Exception e){
        String message=e instanceof SecurityException?"保存先の権限が失われました。フォルダーを再選択してください":e.getMessage();
        if(message==null)message="保存先・空き容量・バックアップを確認してください";
        meta.edit().putString("error",message).commit();
        final String notice=message;
        new Handler(Looper.getMainLooper()).post(()->{
            if(!notice.equals(lastToast)){lastToast=notice;Toast.makeText(context,"自動バックアップ："+notice,Toast.LENGTH_LONG).show();}
        });
        // No income, filenames, credentials or exception payloads in logs/notifications.
    }
    String status(){
        String folder=meta.getString("folderLabel","");long at=meta.getLong("lastSuccess",0);
        return "保存先："+(folder.isEmpty()?"未選択（独立ファイル保存は未開始）":folder)
            +"\n最終成功："+(at==0?"未完了":RevenueMath.date("yyyy-MM-dd HH:mm:ss",at))
            +"\n保存件数："+meta.getInt("lastRecords",0)+"／正常性："+(at==0?"未確認":"保存後SHA-256照合済み")
            +(meta.getString("error","").isEmpty()?"":"\n要確認："+meta.getString("error",""));
    }
    void select(Uri tree,int flags)throws Exception {
        if(!"com.android.externalstorage.documents".equals(tree.getAuthority()))throw new IOException("端末内のDocuments等に専用フォルダーを作成してください（クラウド保存先は対象外です）");
        int grant=flags&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if(grant!=(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION))throw new IOException("読取・保存の両権限が必要です");
        context.getContentResolver().takePersistableUriPermission(tree,grant);
        BackupFiles files=new BackupFiles(context,tree);int max=0;Map<String,Uri> docs=files.documents();
        for(String name:docs.keySet())if(name.startsWith("aruno-result-")&&name.endsWith(".json"))try{max=Math.max(max,BackupFormat.parse(BackupFiles.read(context,docs.get(name))).getInt("records"));}catch(Exception corrupt){}
        synchronized(LOCK){
            if(!meta.edit().putString("tree",tree.toString()).putString("folderLabel",android.provider.DocumentsContract.getTreeDocumentId(tree)).putInt("lastNonemptyRecords",Math.max(max,meta.getInt("lastNonemptyRecords",0))).commit())throw new IOException("保存先を記録できません");
            if(max>0&&BackupFormat.records(BackupFormat.select(raw.getAll()))==0){failure(new IOException("収益が空です。既存バックアップを復旧対象として確認してください"));return;}
            // Markers are specific to a destination; new destination must receive retained generations again.
            File[] marks=directory.listFiles((d,n)->n.endsWith(".sent"));if(marks!=null)for(File f:marks)f.delete();
            capture("safety",true);
        }retry();
    }
    private boolean unsent()throws Exception {
        if(meta.getString("tree","").isEmpty())return false;
        for(File f:local())if(!new File(directory,f.getName()+".sent").exists()){
            JSONObject b=BackupFormat.parse(read(f));if(b.getInt("records")>0||meta.getInt("lastNonemptyRecords",0)==0)return true;
        }return false;
    }
    void retry(){synchronized(LOCK){if(scheduled)return;scheduled=true;}
        worker.execute(()->{
            boolean success=false;
            try{flush();success=true;}catch(Exception e){failure(e);}
            finally{synchronized(LOCK){scheduled=false;if(success)try{if(unsent())retry();}catch(Exception e){failure(e);}}}
        });
    }
    void flush()throws Exception {
        String value=meta.getString("tree","");if(value.isEmpty())return;
        BackupFiles files=new BackupFiles(context,Uri.parse(value));
        Map<String,Uri> docs=files.documents();List<File> pending;
        synchronized(LOCK){pending=local();}Collections.reverse(pending);
        for(File local:pending){
            if(new File(directory,local.getName()+".sent").exists())continue;
            String json=read(local);JSONObject b=BackupFormat.parse(json);
            if(b.getInt("records")==0&&meta.getInt("lastNonemptyRecords",0)>0)continue;
            Uri present=docs.get(local.getName());
            if(present==null)files.append(local.getName(),json);
            else if(!json.equals(BackupFiles.read(context,present)))throw new IOException("保存済みファイルが破損しています。以前の正常世代は保持します");
            write(new File(directory,local.getName()+".sent"),"ok");
            meta.edit().putLong("lastSuccess",Math.max(b.getLong("savedAt"),meta.getLong("lastSuccess",0))).putInt("lastRecords",b.getInt("records")).remove("error").commit();lastToast="";
        }
        files.prune();synchronized(LOCK){pruneLocal();}
    }
    void restore(String json,String expectedCurrent)throws Exception {
        JSONObject body=BackupFormat.parse(json);Map<String,Object> incoming=BackupFormat.decode(body.getJSONObject("preferences"));
        synchronized(LOCK){
            String current=BackupFormat.canonical(BackupFormat.encode(raw.getAll()));
            if(!current.equals(expectedCurrent))throw new IOException("確認後にデータが変わりました。復旧内容をもう一度確認してください");
            String tree=meta.getString("tree","");if(tree.isEmpty())throw new IOException("復旧前の保全用に端末フォルダーを選択してください");
            Map<String,Object> before=new HashMap<>(raw.getAll());
            // A valid empty current state is still protected before a user-confirmed restore.
            String safety=BackupFormat.create(before,"safety",System.currentTimeMillis(),version());
            File saved=append(safety);
            new BackupFiles(context,Uri.parse(tree)).append(saved.getName(),safety);
            write(new File(directory,saved.getName()+".sent"),"ok");
            Map<String,Object> merged=new HashMap<>(before);
            for(String key:new ArrayList<>(merged.keySet()))if(BackupFormat.type(key)!=null)merged.remove(key);
            merged.putAll(incoming);
            // Coupon data is a device display cache. No server/API writes; current server projections win.
            for(String k:Arrays.asList("coupon","couponAt","couponMonth","couponRevenueMonthlyV1","couponRevenueDailyV2"))if(before.containsKey(k))merged.put(k,before.get(k));
            if(!BackupPreferences.replace(raw,merged)||!merged.equals(raw.getAll())){
                BackupPreferences.replace(raw,before);throw new IOException("復旧内容を保存できませんでした。復旧前ファイルは保持しています");
            }
            meta.edit().putInt("lastNonemptyRecords",BackupFormat.records(BackupFormat.select(merged))).commit();
            try{capture("safety",true);}catch(Exception e){failure(e);}
        }
        RevenueWidget.renderAll(context);CalendarWidget.renderAll(context);retry();
    }
}
