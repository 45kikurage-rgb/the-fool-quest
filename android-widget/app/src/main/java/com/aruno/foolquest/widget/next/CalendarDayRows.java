package com.aruno.foolquest.widget.next;

import android.widget.RemoteViews;

/**
 * Calendar rows have independent layout templates and stable view IDs.
 * Change only ORDER to rearrange daily rows; data and amount calculation are separate.
 * Day number is always above these rows.
 */
final class CalendarDayRows {
    static final int TOTAL=0, COUPON=1, TIKTOK=2, LOSS=3;

    // Daily display order: total, earned Coupon, earned TikTok Lite, red combined loss.
    private static final int[] ORDER={TOTAL,COUPON,TIKTOK,LOSS};

    private CalendarDayRows(){}

    static int[] order(){return ORDER.clone();}

    static void append(RemoteViews cell,String packageName){
        for(int row:ORDER)
            cell.addView(R.id.calendar_amount_rows,new RemoteViews(packageName,layout(row)));
    }

    private static int layout(int row){
        switch(row){
            case TOTAL:return R.layout.calendar_row_total;
            case COUPON:return R.layout.calendar_row_coupon;
            case TIKTOK:return R.layout.calendar_row_tiktok;
            case LOSS:return R.layout.calendar_row_loss;
            default:throw new IllegalArgumentException("Unknown calendar row");
        }
    }
}
