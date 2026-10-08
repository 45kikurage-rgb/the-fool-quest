package com.aruno.foolquest.widget;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;

public final class RevenueJob extends JobService {
    static final int PERIODIC=32001;
    static void schedule(Context c){
        JobScheduler js=c.getSystemService(JobScheduler.class);
        if(js.getPendingJob(PERIODIC)!=null)return;
        js.schedule(new JobInfo.Builder(PERIODIC,new ComponentName(c,RevenueJob.class))
            .setPeriodic(30*60*1000L,5*60*1000L).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true).build());
    }
    static void cancel(Context c){c.getSystemService(JobScheduler.class).cancel(PERIODIC);}
    @Override public boolean onStartJob(JobParameters p){
        RevenueUpdate.start(this,()->jobFinished(p,false));return true;
    }
    @Override public boolean onStopJob(JobParameters p){return true;}
}
