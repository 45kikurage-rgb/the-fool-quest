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

    boolean begin() {
        long now = System.currentTimeMillis();
        return prefs.edit().clear()
            .putLong("started", now)
            .putString("month", RevenueMath.month(now))
            .commit();
    }

    void expectReceipt() {
        prefs.edit().putLong("opened", System.currentTimeMillis()).commit();
    }

    /** Session transitions and writes run on the main thread. */
    boolean consume(long started, long opened, String month) {
        long now = System.currentTimeMillis();
        return started == started() && opened == opened() && month.equals(month())
            && month.equals(RevenueMath.month(now)) && PayFetchPolicy.receiptActive(started, opened, now)
            && prefs.edit().clear().commit();
    }

    void clear() { prefs.edit().clear().commit(); }
}
