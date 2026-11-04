package com.aruno.foolquest.widget.next;

/** App-only entry to the point-history fetch. CSV opens use PayFetchActivity. */
public final class PayFetchEntryActivity extends PayFetchActivity {
    @Override boolean isInternalEntry() { return true; }
}
