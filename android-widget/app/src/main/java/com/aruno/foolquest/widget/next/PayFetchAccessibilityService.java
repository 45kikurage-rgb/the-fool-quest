package com.aruno.foolquest.widget.next;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

/**
 * Accessibility service used only by the point-history CSV fetch. It does
 * nothing until PayFetchActivity starts a fetch session from the in-app
 * "データ更新" action.
 */
public final class PayFetchAccessibilityService extends AccessibilityService {
    static volatile PayFetchAccessibilityService instance;
    private ChromePayAssistant assistant;

    @Override public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        assistant = new ChromePayAssistant(this);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (assistant != null) assistant.onAccessibilityEvent(event);
    }

    @Override public void onInterrupt() { stopPayFetch(); }

    @Override public void onDestroy() {
        if (instance == this) instance = null;
        if (assistant != null) assistant.stop();
        super.onDestroy();
    }

    void startPayFetch() {
        if (assistant != null) assistant.start();
    }

    void stopPayFetch() {
        if (assistant != null) assistant.stop();
    }
}
