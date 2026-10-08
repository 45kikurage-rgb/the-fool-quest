package com.aruno.foolquest.widget;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

final class RevenueUpdate {
    private static final AtomicBoolean BUSY=new AtomicBoolean(false);
    private static final ExecutorService EXECUTOR=Executors.newSingleThreadExecutor();
    static boolean busy(){return BUSY.get();}
    static void start(Context c,Runnable done){
        Context app=c.getApplicationContext();
        if(!BUSY.compareAndSet(false,true)){ if(done!=null)done.run(); return; }
        RevenueWidget.renderAll(app);
        EXECUTOR.execute(()->{
            HttpURLConnection con=null;
            try {
                String month=RevenueMath.month(System.currentTimeMillis());
                con=(HttpURLConnection)new URL(RevenueStore.API+month).openConnection();
                con.setConnectTimeout(2500);con.setReadTimeout(2500);con.setInstanceFollowRedirects(false);
                con.setRequestMethod("GET");con.setRequestProperty("Accept","application/json");con.setRequestProperty("Cache-Control","no-cache");
                if(con.getResponseCode()!=200)throw new Exception("HTTP "+con.getResponseCode());
                ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                try(InputStream in=con.getInputStream()){
                    byte[] buffer=new byte[2048];int n;long deadline=System.nanoTime()+2500000000L;
                    while((n=in.read(buffer))!=-1){
                        if(bytes.size()+n>32768||System.nanoTime()>deadline)throw new Exception("応答が大きすぎるか遅延しています");
                        bytes.write(buffer,0,n);
                    }
                }
                long value=RevenueStore.validateCoupon(bytes.toString("UTF-8"),month);
                // A request crossing the JST month boundary must not become the new current month.
                RevenueStore.saveCoupon(app,month,value);
            }catch(Exception e){
                RevenueStore.prefs(app).edit().putString("error","通信失敗。前回正常値を保持しています").apply();
            }finally{
                if(con!=null)con.disconnect();BUSY.set(false);RevenueWidget.renderAll(app);if(done!=null)done.run();
            }
        });
    }
}
