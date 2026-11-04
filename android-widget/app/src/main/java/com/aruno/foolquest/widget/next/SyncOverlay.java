package com.aruno.foolquest.widget.next;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;

/**
 * Small status banner shown while Chrome is being operated. Phase 1 keeps
 * this banner form; a full-screen cover is a separate later phase and must
 * not use FLAG_NOT_TOUCHABLE (taps would reach the covered Chrome).
 */
final class SyncOverlay {
    private final AccessibilityService service;
    private final WindowManager windowManager;
    private TextView view;

    SyncOverlay(AccessibilityService service) {
        this.service = service;
        this.windowManager = (WindowManager) service.getSystemService(AccessibilityService.WINDOW_SERVICE);
    }

    void show(String message) {
        TextView current = view;
        if (current != null) {
            current.setText(message);
            return;
        }
        TextView text = new TextView(service);
        text.setText(message);
        text.setTextSize(15);
        text.setTextColor(Color.WHITE);
        text.setGravity(Gravity.CENTER);
        text.setPadding(dp(18), dp(12), dp(18), dp(12));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(18, 18, 18));
        background.setStroke(dp(2), Color.WHITE);
        background.setCornerRadius(dp(8));
        text.setBackground(background);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.y = dp(42);
        windowManager.addView(text, params);
        view = text;
    }

    void hide() {
        if (view != null) {
            try { windowManager.removeView(view); } catch (RuntimeException ignored) {}
            view = null;
        }
    }

    private int dp(int value) {
        return Math.round(value * service.getResources().getDisplayMetrics().density);
    }
}
