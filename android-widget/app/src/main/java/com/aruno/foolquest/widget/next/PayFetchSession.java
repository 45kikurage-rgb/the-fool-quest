package com.aruno.foolquest.widget.next;

import android.content.Context;
import android.content.SharedPreferences;

/** Short-lived fetch session state for one point-history CSV download. */
final class PayFetchSession {
    private final SharedPreferences prefs;

    PayFetchSession(Context context) {
        prefs = context.getSharedPreferences("pay_fetch", Context.MODE_PRIVATE);
    }

    long started() { return prefs.getLong("started", 0); }
    long opened() { return prefs.getLong("opened", 0); }
    String month() { return prefs.getString("month", ""); }
    boolean active() { return PayFetchPolicy.active(started(), System.currentTimeMillis()); }

    void begin() {
        long now = System.currentTimeMillis();
        prefs.edit().clear()
            .putLong("started", now)
            .putString("month", RevenueMath.month(now))
            .commit();
    }

    void expectReceipt() {
        prefs.edit().putLong("opened", System.currentTimeMillis()).commit();
    }

    void clear() { prefs.edit().clear().commit(); }
}
