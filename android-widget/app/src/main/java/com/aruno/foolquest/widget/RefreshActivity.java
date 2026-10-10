package com.aruno.foolquest.widget;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;

/** User-initiated fetch, using the same foreground path as the settings screen. */
public final class RefreshActivity extends Activity {
    static PendingIntent tapIntent(Context c) {
        Intent intent=new Intent(c,RefreshActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(c,1,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setFinishOnTouchOutside(false);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        TextView status=new TextView(this);status.setText("更新中");status.setTextSize(14);status.setTextColor(Color.WHITE);status.setGravity(Gravity.CENTER);
        float density=getResources().getDisplayMetrics().density;
        status.setPadding(Math.round(24*density),Math.round(12*density),Math.round(24*density),Math.round(12*density));
        GradientDrawable background=new GradientDrawable();background.setColor(0xee171717);background.setCornerRadius(12*density);status.setBackground(background);
        setContentView(status);
        RevenueJob.cancelQueuedManual(this);
        RevenueUpdate.start(this,()->runOnUiThread(()->{
            if(!isFinishing()&&!isDestroyed())finishAndRemoveTask();
        }));
    }
    // singleTask keeps repeated taps attached to this request; no duplicate fetch.
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);}
}
