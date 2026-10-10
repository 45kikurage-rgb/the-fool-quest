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

/** User-initiated direct fetch; a transparent private task returns behind home. */
public final class RefreshActivity extends Activity {
    static PendingIntent tapIntent(Context c) {
        Intent intent=new Intent(c,RefreshActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_NO_ANIMATION);
        return PendingIntent.getActivity(c,1,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setFinishOnTouchOutside(false);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        // Return behind the home task, without drawing a float or taking input.
        // Alpha=0 also permits cross-UID touch
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
    @Override protected void onResume(){super.onResume();moveTaskToBack(true);overridePendingTransition(0,0);}
    // singleTask keeps repeated taps on the same in-flight request. onResume
    // returns it behind home again; completion removes only this private task.
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);}
}
