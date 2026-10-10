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

    static final int LAP_LABEL_WIDTH_DP = 34;
    static final int LAP_FONT_DP = 9;
    static final int MARKER_WIDTH_DP = 2;
    static final int MARKER_OVERHANG_DP = 2;

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
