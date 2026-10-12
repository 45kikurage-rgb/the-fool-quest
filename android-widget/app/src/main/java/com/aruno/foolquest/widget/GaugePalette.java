package com.aruno.foolquest.widget;

/** Shared gauge defaults, alternate-lap colors, and native drawing dimensions. */
final class GaugePalette {
    private GaugePalette() {}

    static final int FIRST_GREEN = 0xff33ff66;
    static final int SECOND_LIME = 0xffccff00;
    static final int PACE_BEHIND = 0xffffd43b;
    static final int PACE_LOW = 0xffff4545;
    static final int PACE_UNKNOWN = 0xff888888;
    static final int MARKER_WHITE = 0xffffffff;

    static final int LAP_LABEL_WIDTH_DP = 84; // Fits ×9 at the maximum 48dp track height.
    static final int LAP_LABEL_GAP_DP = 2;
    static final int LAP_LABEL_PROBE_SIZE = 100;
    static final int MARKER_WIDTH_DP = 2;
    static final int MARKER_OVERHANG_DP = 2;
    static final float DIVIDER_WIDTH_DP = MARKER_WIDTH_DP / 2f;
    static final int DIVIDER_STEP_PERCENT = 20;
    static final int LABEL_WHITE = 0xffffffff;
    static final int LABEL_OUTLINE_BLACK = 0xff000000;
    static final int DIVIDER_LABEL_SP = 12;
    static final int DIVIDER_LABEL_GAP_DP = 2;
    static final int LABEL_OUTLINE_DP = 1;
    static final int MAX_GAUGE_HEIGHT_DP = 48;

    static int currentColor(long lap,int first,int second) {
        return (lap & 1L) == 0L ? second : first;
    }
    static int previousColor(long lap,int first,int second) {
        return (lap & 1L) == 0L ? first : second;
    }
    static boolean hasMarker(long lap,float progress) {
        return lap >= 2 && progress > 0f && progress < 1f;
    }
}
