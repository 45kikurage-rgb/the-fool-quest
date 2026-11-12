"""Fault-injection test for backup continuation using explicit Android/SAF doubles.
Run with: python3 scripts/test-backup-failure.py path/to/json.jar
"""
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

result = Path(__file__).resolve().parents[1] / "app/src/main/java/com/aruno/foolquest/widget/next"
jsonjar = Path(sys.argv[1]).resolve()
assert jsonjar.is_file()
stubs = {
    "android/util/AtomicFile.java": """
package android.util;
import java.io.*;
import java.nio.file.*;
public class AtomicFile {
  public static String silentFailure;
  private final File base, pending;
  public AtomicFile(File base) { this.base=base; pending=new File(base+".new"); }
  public FileOutputStream startWrite() throws IOException { return new FileOutputStream(pending); }
  public void finishWrite(FileOutputStream out) {
    try { out.close(); } catch(IOException e) { throw new UncheckedIOException(e); }
    if (!base.getName().equals(silentFailure) && !pending.renameTo(base)) throw new AssertionError("fixture rename failed");
  }
  public void failWrite(FileOutputStream out) { try {out.close();}catch(IOException ignored){} pending.delete(); }
  public byte[] readFully() throws IOException { return Files.readAllBytes(base.toPath()); }
  public void delete() { base.delete();pending.delete(); }
}
""",
    "android/content/SharedPreferences.java": """
package android.content;
import java.util.*;
public interface SharedPreferences {
  interface OnSharedPreferenceChangeListener {}
  Map<String,?> getAll();
  String getString(String k,String d);
  Set<String> getStringSet(String k,Set<String> d);
  int getInt(String k,int d);
  long getLong(String k,long d);
  float getFloat(String k,float d);
  boolean getBoolean(String k,boolean d);
  boolean contains(String k);
  void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l);
  void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l);
  Editor edit();
  interface Editor {
    Editor putString(String k,String v);Editor putStringSet(String k,Set<String> v);
    Editor putInt(String k,int v);Editor putLong(String k,long v);Editor putFloat(String k,float v);
    Editor putBoolean(String k,boolean v);Editor remove(String k);Editor clear();boolean commit();void apply();
  }
}
""",
    "android/content/Context.java": """
package android.content;
import java.io.File;
import java.lang.reflect.Proxy;
import java.util.*;
import android.net.Uri;
public class Context {
  public static final int MODE_PRIVATE=0;
  private final File root;
  private final Map<String,SharedPreferences> prefs=new HashMap<>();
  public Context(File root) {this.root=root;}
  public File getFilesDir(){return root;}
  public File getNoBackupFilesDir(){return root;}
  public Context getApplicationContext(){return this;}
  public String getPackageName(){return "fixture";}
  public android.app.FakePackageManager getPackageManager(){return new android.app.FakePackageManager();}
  public ContentResolver getContentResolver(){return new ContentResolver();}
  public static final class ContentResolver {public void takePersistableUriPermission(Uri uri,int grant){}}
  public SharedPreferences getSharedPreferences(String name,int mode) {
    return prefs.computeIfAbsent(name,unused->{
      Map<String,Object> values=new HashMap<>();
      return (SharedPreferences)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{SharedPreferences.class},(p,m,a)->{
        String method=m.getName();
        if(method.equals("getAll"))return new HashMap<>(values);
        if(method.equals("contains"))return values.containsKey(a[0]);
        if(method.startsWith("get"))return values.getOrDefault(a[0],a[1]);
        if(!method.equals("edit"))throw new UnsupportedOperationException(method);
        Map<String,Object> working=new HashMap<>(values);
        return Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{SharedPreferences.Editor.class},(e,em,ea)->{
          String change=em.getName();
          if(change.startsWith("put"))working.put((String)ea[0],ea[1]);
          else if(change.equals("remove"))working.remove(ea[0]);
          else if(change.equals("clear"))working.clear();
          else if(change.equals("commit")||change.equals("apply")){
            values.clear();values.putAll(working);return change.equals("commit")?Boolean.TRUE:null;
          } else throw new UnsupportedOperationException(change);
          return e;
        });
      });
    });
  }
}
""",
    "android/app/FakePackageManager.java": """
package android.app;
public class FakePackageManager {
  public static class Info {public int versionCode=37;}
  public Info getPackageInfo(String name,int flags){return new Info();}
}
""",
    "android/content/Intent.java": """
package android.content;
public class Intent {public static final int FLAG_GRANT_READ_URI_PERMISSION=1,FLAG_GRANT_WRITE_URI_PERMISSION=2;}
""",
    "android/net/Uri.java": """
package android.net;
public record Uri(String value) {
  public static Uri parse(String s){return new Uri(s);}
  public String getAuthority(){return java.net.URI.create(value).getAuthority();}
  public String toString(){return value;}
}
""",
    "android/os/Handler.java": """
package android.os;
public class Handler {
  public Handler(Looper ignored){}
  public void post(Runnable task){task.run();}
}
""",
    "android/os/Looper.java": """
package android.os;
public class Looper {public static Looper getMainLooper(){return new Looper();}}
""",
    "android/provider/DocumentsContract.java": """
package android.provider;
public class DocumentsContract {
  public static String getTreeDocumentId(android.net.Uri u){return u.toString();}
}
""",
    "android/widget/Toast.java": """
package android.widget;
public class Toast {
  public static final int LENGTH_LONG=1;
  public static Toast makeText(android.content.Context c,String message,int duration){return new Toast();}
  public void show(){}
}
""",
    "com/aruno/foolquest/widget/next/FixtureDependencies.java": """
package com.aruno.foolquest.widget.next;
import android.content.Context;
import android.net.Uri;
import java.util.*;
import org.json.JSONObject;

final class BackupFiles {
  static final Map<String,String> copies=new LinkedHashMap<>();
  static boolean denied=false;
  BackupFiles(Context context,Uri tree){}
  Map<String,Uri> documents(){
    if(denied)throw new SecurityException("synthetic SAF loss");
    Map<String,Uri> docs=new HashMap<>();
    for(String name:copies.keySet())docs.put(name,Uri.parse(name));
    return docs;
  }
  static String read(Context context,Uri uri){return copies.get(uri.toString());}
  void append(String name,String json){copies.put(name,json);}
  void prune(){}
}
final class RevenueWidget {static void renderAll(Context c){throw new AssertionError("not exercised");}}
final class CalendarWidget {static void renderAll(Context c){throw new AssertionError("not exercised");}}
final class RevenueManager {
  static void validate(JSONObject data){throw new AssertionError("not exercised");}
  static void requireMonth(String month){throw new AssertionError("not exercised");}
  static long strictNumber(Object number,boolean negative){throw new AssertionError("not exercised");}
  static void checkDate(String day){throw new AssertionError("not exercised");}
  static long plus(long a,long b){throw new AssertionError("not exercised");}
}
""",
    "com/aruno/foolquest/widget/next/SafFixture.java": """
package com.aruno.foolquest.widget.next;
import android.content.Context;
import android.util.AtomicFile;
import java.io.*;
import java.nio.file.*;
import java.util.Map;
import org.json.JSONObject;
public class SafFixture {
  static void require(boolean value){if(!value)throw new AssertionError();}
  public static void main(String[] args)throws Exception {
    var coordinator=new BackupCoordinator(new Context(new File(args[0])));
    coordinator.meta.edit().putString("tree","content://com.android.externalstorage.documents/tree/fixture").commit();
    String valid=BackupFormat.create(Map.of("tiktok",123L,"calendar.forecastHidden.7",1),"change",1000,37);
    File corrupt=new File(coordinator.directory,"aruno-result-change-1000-a.json");
    File next=new File(coordinator.directory,"aruno-result-change-2000-b.json");
    JSONObject envelope=new JSONObject(valid).put("sha256","0".repeat(64));
    Files.writeString(corrupt.toPath(),envelope.toString());
    BackupCoordinator.write(next,valid);
    coordinator.flush();
    require(BackupFiles.copies.size()==1 && valid.equals(BackupFiles.copies.get(next.getName())));
    require(next.isFile() && corrupt.isFile() && new File(next+".sent").exists());
    require(!new File(corrupt+".sent").exists());
    require(coordinator.meta.getInt("checksumMismatchCount",0)==1);
    require(coordinator.status().contains("SHA-256不一致で端末に保持したファイル：1件"));
    var unsent=BackupCoordinator.class.getDeclaredMethod("unsent");
    unsent.setAccessible(true);
    require(Boolean.FALSE.equals(unsent.invoke(coordinator)));
    System.out.println("PASS: valid successor exported; corrupted original retained without sent marker, warning shown, no retry spin.");

    BackupFiles.copies.clear();
    Files.delete(new File(next+".sent").toPath());
    BackupFiles.denied=true;
    try {coordinator.flush();throw new AssertionError("expected SAF loss");}catch(SecurityException expected){}
    require(BackupFiles.copies.isEmpty() && !new File(next+".sent").exists());
    BackupFiles.denied=false;
    BackupFiles.copies.put(next.getName(),"synthetic mismatch");
    try {coordinator.flush();throw new AssertionError("expected destination mismatch");}
    catch(IOException e){require(e.getMessage().contains("破損"));}
    require("synthetic mismatch".equals(BackupFiles.copies.get(next.getName())) && !new File(next+".sent").exists());
    BackupFiles.copies.clear();
    coordinator.flush();
    require(valid.equals(BackupFiles.copies.get(next.getName())) && new File(next+".sent").exists());
    System.out.println("R-04 controls: denied SAF and destination mismatch do not mark success; valid journal exports unchanged.");

    File write=new File(coordinator.directory,"write-check.json");
    BackupCoordinator.write(write,"original");
    AtomicFile.silentFailure=write.getName();
    try {BackupCoordinator.write(write,"changed");throw new AssertionError("expected readback mismatch");}
    catch(IOException e){require(e.getMessage().contains("保存後照合"));}
    require("original".equals(BackupCoordinator.read(write)));
    System.out.println("R-04 control: Result already rejects silent AtomicFile replacement failure via readback.");
  }
}
""",
}

with tempfile.TemporaryDirectory(prefix="aruno-preaction-jvm-") as folder:
    temp = Path(folder)
    files = []
    for rel, source in stubs.items():
        target = temp / rel
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(source, encoding="utf-8")
        files.append(target)
    for directory, package, classes in (
        (result, "com/aruno/foolquest/widget/next", ["BackupCoordinator", "BackupPreferences", "BackupFormat", "RevenueMath"]),
    ):
        for name in classes:
            src = list(directory.glob(name+".java*"))
            assert len(src)==1, (directory, name, src)
            target = temp / package / (name+".java")
            shutil.copyfile(src[0],target)
            files.append(target)
    output = temp / "classes"
    subprocess.run(["java","--module","jdk.compiler/com.sun.tools.javac.Main","-encoding","UTF-8","-cp",jsonjar,"-d",output,*files],check=True)
    for fixture in ("com.aruno.foolquest.widget.next.SafFixture",):
        root = temp / fixture
        root.mkdir()
        subprocess.run(["java","-cp",str(output)+":"+str(jsonjar),fixture,root],check=True)
