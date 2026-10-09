package com.aruno.foolquest.widget;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;

public final class RevenueJob extends JobService {
    static final int PERIODIC=32001, MANUAL=32002;
    static void schedule(Context c){
        JobScheduler js=c.getSystemService(JobScheduler.class);
        if(js.getPendingJob(PERIODIC)!=null)return;
        js.schedule(new JobInfo.Builder(PERIODIC,new ComponentName(c,RevenueJob.class))
            .setPeriodic(30*60*1000L,5*60*1000L).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true).build());
    }
    static void requestRefresh(Context c) {
        if(RevenueUpdate.busy())return;
        JobScheduler js=c.getSystemService(JobScheduler.class);
        if(js.getPendingJob(MANUAL)!=null)return;
        // No network constraint: a manual offline tap should report offline promptly.
        // The receiver returns immediately; Android owns the longer network lifetime.
        int result=js.schedule(new JobInfo.Builder(MANUAL,new ComponentName(c,RevenueJob.class)).setOverrideDeadline(0).build());
        if(result!=JobScheduler.RESULT_SUCCESS){RevenueStore.saveFailure(c,"SCHEDULE");RevenueWidget.renderAll(c);}
    }
    static void cancel(Context c){JobScheduler js=c.getSystemService(JobScheduler.class);js.cancel(PERIODIC);js.cancel(MANUAL);}
    @Override public boolean onStartJob(JobParameters p){
        RevenueUpdate.start(this,()->jobFinished(p,false));return true;
    }
    @Override public boolean onStopJob(JobParameters p){return true;}
}
