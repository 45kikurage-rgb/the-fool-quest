#!/usr/bin/env python3
"""Exercise the real RevenueWidget entry point with a controlled main-looper queue.

The Android substitutes only provide scheduling and record published snapshots;
they do not replace the production renderAll ordering being tested.
"""

from pathlib import Path
import shutil
import subprocess
import tempfile


ROOT = Path(__file__).resolve().parents[1]
PACKAGE = "com/aruno/foolquest/widget/next/"
STUBS = {
    "android/app/PendingIntent.java": "package android.app; public class PendingIntent {}",
    "android/content/ComponentName.java": """package android.content;
        public class ComponentName {public ComponentName(Context c,Class<?> k){}}""",
    "android/content/Context.java": """package android.content;
        public class Context {public Context getApplicationContext(){return this;}
        public android.content.res.Resources getResources(){return new android.content.res.Resources();}}""",
    "android/content/Intent.java": """package android.content;
        public class Intent {public static final String ACTION_BOOT_COMPLETED="boot",
        ACTION_MY_PACKAGE_REPLACED="replace"; public String getAction(){return "";}}""",
    "android/content/res/Resources.java": """package android.content.res;
        public class Resources {public android.util.DisplayMetrics getDisplayMetrics(){
        return new android.util.DisplayMetrics();} public Configuration getConfiguration(){return new Configuration();}}""",
    "android/content/res/Configuration.java": """package android.content.res;
        public class Configuration {public static final int ORIENTATION_LANDSCAPE=2; public int orientation=1;}""",
    "android/util/DisplayMetrics.java": """package android.util;
        public class DisplayMetrics {public int widthPixels=360; public float density=1;}""",
    "android/util/Log.java": """package android.util;
        public class Log {public static int w(String tag,String text){throw new AssertionError(text);}}""",
    "android/util/SizeF.java": """package android.util;
        public class SizeF {public float getWidth(){return 360;} public float getHeight(){return 196;}}""",
    "android/os/Build.java": """package android.os;
        public class Build {public static class VERSION {public static int SDK_INT=30;}}""",
    "android/os/Bundle.java": """package android.os;
        public class Bundle {public int getInt(String k,int d){return d;}
        public <T> java.util.ArrayList<T> getParcelableArrayList(String k){return null;}}""",
    "android/os/Looper.java": """package android.os;
        public class Looper {
        private static final Thread MAIN_THREAD=Thread.currentThread();
        private static final Looper MAIN=new Looper();
        public static Looper getMainLooper(){return MAIN;}
        public static Looper myLooper(){return Thread.currentThread()==MAIN_THREAD?MAIN:null;}}
        """,
    "android/os/Handler.java": """package android.os;
        public class Handler {
        private static final java.util.Queue<Runnable> QUEUE=new java.util.concurrent.ConcurrentLinkedQueue<>();
        public Handler(Looper looper){}
        public boolean post(Runnable r){QUEUE.add(r);return true;}
        public static void drain(){Runnable r;while((r=QUEUE.poll())!=null)r.run();}}
        """,
    "android/widget/RemoteViews.java": """package android.widget;
        public class RemoteViews {public int version; public RemoteViews(int v){version=v;}
        public RemoteViews(java.util.Map<android.util.SizeF,RemoteViews> m){}}""",
    "android/appwidget/AppWidgetProvider.java": """package android.appwidget;
        public class AppWidgetProvider {
        public void onReceive(android.content.Context c,android.content.Intent i){}
        public void onUpdate(android.content.Context c,AppWidgetManager m,int[] i){}
        public void onAppWidgetOptionsChanged(android.content.Context c,AppWidgetManager m,int id,android.os.Bundle b){}
        public void onEnabled(android.content.Context c){} public void onDisabled(android.content.Context c){}}
        """,
    "android/appwidget/AppWidgetManager.java": """package android.appwidget;
        public class AppWidgetManager {
        public static final String OPTION_APPWIDGET_MAX_WIDTH="mw",OPTION_APPWIDGET_MIN_WIDTH="w",
        OPTION_APPWIDGET_MAX_HEIGHT="mh",OPTION_APPWIDGET_MIN_HEIGHT="h",OPTION_APPWIDGET_SIZES="s";
        public static final AppWidgetManager INSTANCE=new AppWidgetManager();
        public final java.util.List<Integer> published=new java.util.ArrayList<>();
        public static AppWidgetManager getInstance(android.content.Context c){return INSTANCE;}
        public int[] getAppWidgetIds(android.content.ComponentName n){return new int[]{1};}
        public android.os.Bundle getAppWidgetOptions(int id){return new android.os.Bundle();}
        public void updateAppWidget(int id,android.widget.RemoteViews v){
        if(android.os.Looper.myLooper()!=android.os.Looper.getMainLooper())
        throw new AssertionError("Widget published off the main looper");published.add(v.version);}}
        """,
    PACKAGE + "Support.java": """package com.aruno.foolquest.widget.next;
        class RevenueStore {
        static volatile int version=1; static int reads; static class Data {}
        static Data read(android.content.Context c){reads++;return new Data();}
        static Object prefs(android.content.Context c){return null;}}
        class DisplaySettings {int version; static DisplaySettings load(Object p){
        DisplaySettings s=new DisplaySettings();s.version=RevenueStore.version;return s;}}
        class DailyTargetReceiver {static void schedule(android.content.Context c){}
        static void cancel(android.content.Context c){}}
        class CalendarWidget {static int[] ids(android.content.Context c){return new int[0];}}
        class RevenueJob {static void schedule(android.content.Context c){}
        static void cancel(android.content.Context c){} static void requestRefresh(android.content.Context c){}}
        class NativeWidgetViews {static class Result {android.widget.RemoteViews views;}
        static Result create(android.content.Context c,RevenueStore.Data d,DisplaySettings s,int w,int h){
        Result r=new Result();r.views=new android.widget.RemoteViews(s.version);return r;}}
        """,
    PACKAGE + "RenderOrderRegression.java": """package com.aruno.foolquest.widget.next;
        public class RenderOrderRegression {
        static void ok(boolean value,String why){if(!value)throw new AssertionError(why);}
        public static void main(String[] args)throws Exception {
        android.os.Looper.getMainLooper();
        android.content.Context c=new android.content.Context();
        java.util.List<Integer> shown=android.appwidget.AppWidgetManager.INSTANCE.published;
        java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();
        Thread old=new Thread(()->{try{RevenueWidget.renderAll(c);}catch(Throwable e){failure.set(e);}});
        old.start();old.join();if(failure.get()!=null)throw new AssertionError(failure.get());
        ok(RevenueStore.reads==0,"Background request captured an obsolete snapshot before main dispatch");
        ok(shown.isEmpty(),"Background request published before main dispatch");
        RevenueStore.version=2;
        RevenueWidget.renderAll(c);
        ok(shown.equals(java.util.Arrays.asList(2)),"Main-thread render should remain immediate");
        android.os.Handler.drain();
        ok(shown.equals(java.util.Arrays.asList(2,2)),"Queued old request replaced latest settings with an old snapshot");
        ok(RevenueStore.reads==2,"Each actual render must read the current snapshot");
        System.out.println("PASS gauge render serialization: background deferred, main immediate, queued request retains latest settings");
        }}
        """,
}


def main():
    with tempfile.TemporaryDirectory(prefix="aruno-widget-render-order-") as directory:
        workspace = Path(directory)
        for name, text in STUBS.items():
            target = workspace / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(text, encoding="utf-8")
        shutil.copyfile(
            ROOT / "app/src/main/java" / PACKAGE / "RevenueWidget.java",
            workspace / PACKAGE / "RevenueWidget.java",
        )
        compiler = ["javac"] if shutil.which("javac") else ["java", "com.sun.tools.javac.Main"]
        subprocess.run(
            compiler + ["-source", "8", "-target", "8", "-Xlint:-options", "-d", str(workspace / "classes")]
            + [str(path) for path in workspace.rglob("*.java")],
            check=True,
        )
        subprocess.run(
            ["java", "-cp", str(workspace / "classes"),
             "com.aruno.foolquest.widget.next.RenderOrderRegression"],
            check=True,
        )


if __name__ == "__main__":
    main()
