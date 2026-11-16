package com.nijatva.file;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

/** Applies theme + accent colour chosen in Appearance. */
public final class Theme {

    private Theme() {}

    public static final int THEME_SYSTEM = 0, THEME_DARK = 1, THEME_LIGHT = 2, THEME_BLACK = 3;
    public static final int ACCENT_GREEN = 0, ACCENT_SAFFRON = 1, ACCENT_INDIGO = 2;

    static final int[] ACCENTS = {0xFF22C55E, 0xFFF59E0B, 0xFF6366F1};

    public static int themeMode(Context c) { return Store.themeMode(c); }
    public static int accentMode(Context c) { return Store.accentMode(c); }
    public static int accentColor(Context c) {
        int m = accentMode(c);
        if (m == 3) { // Auto (system / Material You)
            if (Build.VERSION.SDK_INT >= 31) {
                try { return c.getColor(android.R.color.system_accent1_500); } catch (Exception ignored) { }
            }
            return ACCENTS[1];
        }
        return ACCENTS[Math.max(0, Math.min(2, m))];
    }

    public static boolean isDark(Context c) {
        int m = themeMode(c);
        if (m == THEME_DARK || m == THEME_BLACK) return true;
        if (m == THEME_LIGHT) return false;
        int night = c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return night == Configuration.UI_MODE_NIGHT_YES;
    }

    private static String signature(Context c) { return themeMode(c) + ":" + accentMode(c); }

    public static void applyWindow(android.app.Activity a) {
        try {
            int bar = isDark(a) ? 0xFF000000 : 0xFFF5F6F8;
            a.getWindow().setStatusBarColor(bar);
            a.getWindow().setNavigationBarColor(bar);
        } catch (Exception ignored) { }
    }

    public static String apply(Context c) {
        if (c instanceof android.app.Activity) applyWindow((android.app.Activity) c);
        int m = themeMode(c);
        if (m == THEME_BLACK) {
            MainActivity.BG = 0xFF000000;
            MainActivity.CARD = 0xFF141414;
            MainActivity.TEXT = 0xFFFFFFFF;
            MainActivity.SUB = 0xFF9E9E9E;
        } else if (isDark(c)) {
            MainActivity.BG = 0xFF121212;
            MainActivity.CARD = 0xFF1E1E1E;
            MainActivity.TEXT = 0xFFFFFFFF;
            MainActivity.SUB = 0xFF9E9E9E;
        } else {
            MainActivity.BG = 0xFFF5F6F8;
            MainActivity.CARD = 0xFFFFFFFF;
            MainActivity.TEXT = 0xFF111111;
            MainActivity.SUB = 0xFF5F6368;
        }
        MainActivity.BLUE = accentColor(c);
        return signature(c);
    }
}
