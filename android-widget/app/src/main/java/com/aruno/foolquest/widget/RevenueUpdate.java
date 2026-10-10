package com.aruno.foolquest.widget;

import android.content.Context;
import android.net.ConnectivityManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.ArrayList;
import java.util.List;

final class RevenueUpdate {
    private static final AtomicBoolean BUSY=new AtomicBoolean(false);
    private static final ExecutorService EXECUTOR=Executors.newSingleThreadExecutor();
    private static final Object LOCK=new Object();
    private static final List<Runnable> WAITERS=new ArrayList<>();
    static boolean busy(){return BUSY.get();}
    static void start(Context c,Runnable done){
        Context app=c.getApplicationContext();
        synchronized(LOCK){
            if(done!=null)WAITERS.add(done);
            if(!BUSY.compareAndSet(false,true))return;
        }
        RevenueWidget.renderAll(app);
        EXECUTOR.execute(()->{
            HttpURLConnection con=null;
            try {
                String month=RevenueMath.month(System.currentTimeMillis());
                ConnectivityManager connectivity=app.getSystemService(ConnectivityManager.class);
                if(connectivity!=null&&connectivity.getActiveNetwork()==null)throw new RevenueHttp.Offline();
                con=(HttpURLConnection)new URL(RevenueStore.API+month).openConnection();
                String raw=RevenueHttp.read(con);long value;
                try{value=RevenueStore.validateCoupon(raw,month);}catch(Exception invalid){throw new RevenueHttp.InvalidResponse();}
                // A request crossing the JST month boundary must not become the new current month.
                RevenueStore.saveCoupon(app,month,value);
            }catch(Exception e){
                RevenueStore.saveFailure(app,RevenueFailure.code(e));
            }finally{
                if(con!=null)con.disconnect();
                List<Runnable> completed;
                synchronized(LOCK){BUSY.set(false);completed=new ArrayList<>(WAITERS);WAITERS.clear();}
                RevenueWidget.renderAll(app);
                for(Runnable callback:completed){try{callback.run();}catch(RuntimeException ignored){}}
            }
        });
    }
}
