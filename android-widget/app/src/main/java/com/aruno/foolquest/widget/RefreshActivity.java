package com.aruno.foolquest.widget;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.View;

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
        // Keep the same user-triggered Activity lifetime, without drawing a float
        // or consuming the launcher's input. Alpha=0 also permits cross-UID touch
        // pass-through on Android 12+, unlike merely transparent view content.
        View content=new View(this);content.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        setContentView(content);
        WindowManager.LayoutParams window=getWindow().getAttributes();
        window.alpha=0f;window.width=1;window.height=1;window.gravity=Gravity.TOP|Gravity.START;window.windowAnimations=0;
        window.flags|=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        getWindow().setAttributes(window);
        overridePendingTransition(0,0);
        RevenueJob.cancelQueuedManual(this);
        RevenueUpdate.start(this,()->runOnUiThread(()->{
            if(!isFinishing()&&!isDestroyed()){finishAndRemoveTask();overridePendingTransition(0,0);}
        }));
    }
    // singleTask keeps repeated taps attached to this request; no duplicate fetch.
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);}
}
