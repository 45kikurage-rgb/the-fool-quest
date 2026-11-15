"""Run exact update/daily-reader source with deterministic Android/network doubles.
No network requests or device credentials are used.
Usage: python3 scripts/test-update-failure.py path/to/json.jar
"""
from pathlib import Path
import os
import shutil
import subprocess
import sys
import tempfile

source = Path(__file__).resolve().parents[1] / 'app/src/main/java/com/aruno/foolquest/widget/next'
jsonjar = Path(sys.argv[1]).resolve()
assert jsonjar.is_file()
stubs = {
'android/content/SharedPreferences.java': '''package android.content;
public interface SharedPreferences {
String getString(String k,String d);long getLong(String k,long d);Editor edit();
interface Editor {Editor putString(String k,String v);Editor putLong(String k,long v);Editor remove(String k);boolean commit();void apply();}
}''',
'android/content/Context.java': '''package android.content;
import java.util.*;import java.lang.reflect.Proxy;
public class Context {
public static final int MODE_PRIVATE=0;
private final Map<String,SharedPreferences> stores=new HashMap<>();
public Context getApplicationContext(){return this;}
public <T>T getSystemService(Class<T> c){return c.cast(new android.net.ConnectivityManager());}
public SharedPreferences getSharedPreferences(String name,int mode){return stores.computeIfAbsent(name,n->{
Map<String,Object> values=new HashMap<>();
return (SharedPreferences)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{SharedPreferences.class},(p,m,a)->{
if(m.getName().startsWith("get"))return values.getOrDefault(a[0],a[1]);
Map<String,Object> working=new HashMap<>(values);
return Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{SharedPreferences.Editor.class},(e,em,ea)->{
String method=em.getName();if(method.startsWith("put"))working.put((String)ea[0],ea[1]);
else if(method.equals("remove"))working.remove(ea[0]);
else {values.clear();values.putAll(working);return method.equals("commit")?Boolean.TRUE:null;}
return e;});});});}
}''',
'android/net/ConnectivityManager.java': '''package android.net;public class ConnectivityManager {public Object getActiveNetwork(){return new Object();}}''',
'android/util/Base64.java': '''package android.util;public class Base64 {public static final int NO_WRAP=0;public static String encodeToString(byte[] b,int f){return java.util.Base64.getEncoder().encodeToString(b);}public static byte[] decode(String s,int f){return java.util.Base64.getDecoder().decode(s);}}''',
'android/security/keystore/KeyProperties.java': '''package android.security.keystore;public class KeyProperties {public static final String KEY_ALGORITHM_AES="AES",BLOCK_MODE_GCM="GCM",ENCRYPTION_PADDING_NONE="NoPadding";public static final int PURPOSE_ENCRYPT=1,PURPOSE_DECRYPT=2;}''',
'android/security/keystore/KeyGenParameterSpec.java': '''package android.security.keystore;public class KeyGenParameterSpec implements java.security.spec.AlgorithmParameterSpec {public static class Builder {public Builder(String n,int p){}public Builder setBlockModes(String...v){return this;}public Builder setEncryptionPaddings(String...v){return this;}public KeyGenParameterSpec build(){return new KeyGenParameterSpec();}}}''',
'com/aruno/foolquest/widget/next/Doubles.java': '''package com.aruno.foolquest.widget.next;
class RevenueWidget {static int calls,throwAt;static void renderAll(android.content.Context c){if(++calls==throwAt)throw new IllegalArgumentException("fixture redraw failure");}}
class CalendarWidget {static int calls;static boolean fail;static int[] ids(android.content.Context c){return new int[]{1};}static void renderAll(android.content.Context c){calls++;if(fail)throw new IllegalArgumentException("fixture calendar redraw failure");}}
class CalendarWidgetState {static String month(android.content.Context c,int id){return RevenueMath.month(System.currentTimeMillis());}}
class RevenueManager {static long strictNumber(Object n,boolean p){return ((Number)n).longValue();}static long plus(long a,long b){return a+b;}}
class RevenueStore {static int failures;static final String API="https://example.invalid/?month=";static android.content.SharedPreferences prefs(android.content.Context c){return c.getSharedPreferences("widget",0);}static long validateCoupon(String raw,String month)throws Exception {org.json.JSONObject j=new org.json.JSONObject(raw);if(!month.equals(j.getString("month")))throw new IllegalArgumentException();return j.getLong("coupon_revenue");}static void saveCoupon(android.content.Context c,String m,long v){}static void saveFailure(android.content.Context c,String code){failures++;}}
class RevenueHttp {static int calls,failDaily;static String daily;static class HttpFailure extends java.io.IOException{final int status;HttpFailure(int s){status=s;}}static class Offline extends java.io.IOException{}static class InvalidResponse extends java.io.IOException{}static String read(java.net.HttpURLConnection c)throws Exception {calls++;if(c.getURL().toString().contains("/daily?")){if(failDaily==401)throw new HttpFailure(401);if(failDaily==408)throw new java.net.SocketTimeoutException();return daily;}return "{\\"month\\":\\""+RevenueMath.month(System.currentTimeMillis())+"\\",\\"coupon_revenue\\":123}";}}
''',
'com/aruno/foolquest/widget/next/UpdateFailureTest.java': '''package com.aruno.foolquest.widget.next;
import java.util.*;import java.security.*;import java.security.cert.Certificate;import javax.crypto.spec.SecretKeySpec;
public class UpdateFailureTest {
public static class Keys extends KeyStoreSpi {
static boolean available=true;public Key engineGetKey(String s,char[] p){return available?new SecretKeySpec(new byte[16],"AES"):null;}
public Certificate[] engineGetCertificateChain(String a){return null;}public Certificate engineGetCertificate(String a){return null;}public Date engineGetCreationDate(String a){return null;}public void engineSetKeyEntry(String a,Key k,char[] p,Certificate[] c){}public void engineSetKeyEntry(String a,byte[] k,Certificate[] c){}public void engineSetCertificateEntry(String a,Certificate c){}public void engineDeleteEntry(String a){}public Enumeration<String> engineAliases(){return Collections.emptyEnumeration();}public boolean engineContainsAlias(String a){return available;}public int engineSize(){return 1;}public boolean engineIsKeyEntry(String a){return true;}public boolean engineIsCertificateEntry(String a){return false;}public String engineGetCertificateAlias(Certificate c){return null;}public void engineStore(java.io.OutputStream o,char[] p){}public void engineLoad(java.io.InputStream i,char[] p){}
}
static int checks;
static void ok(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
static void update(android.content.Context c)throws Exception {java.util.concurrent.CountDownLatch done=new java.util.concurrent.CountDownLatch(2);RevenueUpdate.start(c,done::countDown);RevenueUpdate.start(c,done::countDown);ok(done.await(3,java.util.concurrent.TimeUnit.SECONDS),"all callbacks complete");ok(!RevenueUpdate.busy(),"busy gate released");}
public static void main(String[] args)throws Exception {
Security.addProvider(new Provider("AndroidKeyStore",1.0,"fixture only"){{put("KeyStore.AndroidKeyStore",Keys.class.getName());}});
android.content.Context c=new android.content.Context();String month=RevenueMath.month(System.currentTimeMillis());
long now=System.currentTimeMillis();int last=Integer.parseInt(RevenueMath.date("dd",now))-1;
org.json.JSONObject days=new org.json.JSONObject();for(int i=1;i<=last;i++)days.put(String.format(java.util.Locale.US,"%s-%02d",month,i),0);
RevenueHttp.daily=new org.json.JSONObject().put("month",month).put("coupon_revenue",123).put("days",days).put("through",last==0?org.json.JSONObject.NULL:String.format(java.util.Locale.US,"%s-%02d",month,last)).toString();
ok(CouponDailyReader.status(c,month).contains("未設定"),"missing credential never reports success");
CouponDailyReader.setToken(c,"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");ok(CouponDailyReader.status(c,month).contains("未取得"),"new credential starts unverified");
RevenueWidget.throwAt=1;CalendarWidget.fail=true;RevenueHttp.failDaily=401;update(c);
ok(CouponDailyReader.status(c,month).contains("401"),"daily auth error exposed independently");ok(RevenueStore.failures==0,"daily failure does not mark monthly failure");
RevenueWidget.throwAt=RevenueWidget.calls+2;RevenueHttp.failDaily=408;update(c);ok(CouponDailyReader.status(c,month).contains("時間切れ"),"daily timeout exposed");
RevenueWidget.throwAt=0;CalendarWidget.fail=false;RevenueHttp.failDaily=0;update(c);ok(CouponDailyReader.status(c,month).contains("取得済み"),"success reported after actual validated save");
RevenueHttp.daily="{}";update(c);ok(CouponDailyReader.status(c,month).contains("API応答"),"invalid daily reply exposed");ok(CouponDailyReader.snapshot(RevenueStore.prefs(c),month)!=null,"last good daily projection retained");
CouponDailyReader.setToken(c,"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");ok(CouponDailyReader.status(c,month).contains("未取得"),"old credential status not reused");
Keys.available=false;update(c);ok(CouponDailyReader.status(c,month).contains("接続キー"),"unavailable key visible");
ok(c.getSharedPreferences("couponDailyFetchStatusV1",0).getString(month+".credential","").matches("[0-9a-f]{64}"),"status stores only ciphertext fingerprint");
CouponDailyReader.setToken(c,"");ok(CouponDailyReader.status(c,month).contains("未設定"),"disconnect visible");
System.out.println("PASS "+checks+" update failure/daily status checks (source + doubles; no network)");System.exit(0);
}
}'''
}
with tempfile.TemporaryDirectory(prefix='aruno-update-failure-') as directory:
    root = Path(directory)
    for name, content in stubs.items():
        target = root / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content, encoding='utf-8')
    for name in ['RevenueUpdate.java', 'CouponDailyReader.java', 'RevenueFailure.java', 'RevenueMath.java', 'CalendarDateMath.java']:
        shutil.copyfile(source / name, root / 'com/aruno/foolquest/widget/next' / name)
    javac = os.environ.get('WIDGET_JAVAC', 'javac')
    java = os.environ.get('WIDGET_JAVA', 'java')
    subprocess.run([javac, '-source', '8', '-target', '8', '-encoding', 'UTF-8', '-cp', str(jsonjar), '-d', str(root / 'classes'), *map(str,root.rglob('*.java'))], check=True)
    subprocess.run([java, '-cp', str(root / 'classes')+os.pathsep+str(jsonjar), 'com.aruno.foolquest.widget.next.UpdateFailureTest'], check=True, timeout=20)
