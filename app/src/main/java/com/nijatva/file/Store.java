package com.nijatva.file;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/** Lightweight preference store: starred files, safe-folder PIN, view/settings toggles. */
public final class Store {

    private Store() {}

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences("fm_prefs", Context.MODE_PRIVATE);
    }

    // ---- starred ----
    public static Set<String> starred(Context c) {
        return new HashSet<>(p(c).getStringSet("starred", new HashSet<String>()));
    }

    public static boolean isStarred(Context c, String path) {
        return starred(c).contains(path);
    }

    public static void star(Context c, String path, boolean on) {
        Set<String> s = starred(c);
        if (on) s.add(path);
        else s.remove(path);
        p(c).edit().putStringSet("starred", s).apply();
    }

    // ---- safe folder ----
    public static String pin(Context c) { return p(c).getString("pin", null); }
    public static void setPin(Context c, String pin) { p(c).edit().putString("pin", pin).apply(); }

    // ---- browse / view ----
    public static boolean showHidden(Context c) { return p(c).getBoolean("showHidden", false); }
    public static void setShowHidden(Context c, boolean b) { p(c).edit().putBoolean("showHidden", b).apply(); }

    public static int viewMode(Context c) { return p(c).getInt("viewMode", 0); }
    public static void setViewMode(Context c, int m) { p(c).edit().putInt("viewMode", m).apply(); }

    public static int sortBy(Context c) { return p(c).getInt("sortBy", 0); }
    public static void setSortBy(Context c, int s) { p(c).edit().putInt("sortBy", s).apply(); }

    public static boolean applyToAll(Context c) { return p(c).getBoolean("applyToAll", true); }
    public static void setApplyToAll(Context c, boolean b) { p(c).edit().putBoolean("applyToAll", b).apply(); }

    // ---- search ----
    public static boolean smartSearch(Context c) { return p(c).getBoolean("smartSearch", true); }
    public static void setSmartSearch(Context c, boolean b) { p(c).edit().putBoolean("smartSearch", b).apply(); }

    public static boolean pauseHistory(Context c) { return p(c).getBoolean("pauseHistory", false); }
    public static void setPauseHistory(Context c, boolean b) { p(c).edit().putBoolean("pauseHistory", b).apply(); }

    // ---- recycle bin settings ----
    public static boolean recycleDefault(Context c) { return p(c).getBoolean("recycleDefault", true); }
    public static void setRecycleDefault(Context c, boolean b) { p(c).edit().putBoolean("recycleDefault", b).apply(); }

    public static boolean recycleConfirm(Context c) { return p(c).getBoolean("recycleConfirm", true); }
    public static void setRecycleConfirm(Context c, boolean b) { p(c).edit().putBoolean("recycleConfirm", b).apply(); }

    // ---- notifications / advanced ----
    public static boolean storageNotify(Context c) { return p(c).getBoolean("storageNotify", true); }
    public static void setStorageNotify(Context c, boolean b) { p(c).edit().putBoolean("storageNotify", b).apply(); }

    public static boolean alwaysNavIcon(Context c) { return p(c).getBoolean("alwaysNavIcon", true); }
    public static void setAlwaysNavIcon(Context c, boolean b) { p(c).edit().putBoolean("alwaysNavIcon", b).apply(); }

    public static boolean detectUsb(Context c) { return p(c).getBoolean("detectUsb", true); }
    public static void setDetectUsb(Context c, boolean b) { p(c).edit().putBoolean("detectUsb", b).apply(); }

    public static boolean showSystemStorage(Context c) { return p(c).getBoolean("showSystemStorage", false); }
    public static void setShowSystemStorage(Context c, boolean b) { p(c).edit().putBoolean("showSystemStorage", b).apply(); }

    public static String fileSizeUnit(Context c) { return p(c).getString("fileSizeUnit", "Auto"); }
    public static void setFileSizeUnit(Context c, String u) { p(c).edit().putString("fileSizeUnit", u).apply(); }

    public static String dateFormat(Context c) { return p(c).getString("dateFormat", "8 Oct 2026"); }
    public static void setDateFormat(Context c, String f) { p(c).edit().putString("dateFormat", f).apply(); }

    public static String musicEngine(Context c) { return p(c).getString("musicEngine", "Built-in player"); }
    public static void setMusicEngine(Context c, String e) { p(c).edit().putString("musicEngine", e).apply(); }

    // ---- advanced menus ----
    public static boolean menuBookmark(Context c) { return p(c).getBoolean("mBookmark", true); }
    public static boolean menuHomeScreen(Context c) { return p(c).getBoolean("mHomeScreen", false); }
    public static boolean menuHideUnhide(Context c) { return p(c).getBoolean("mHideUnhide", false); }
    public static boolean menuOpenAs(Context c) { return p(c).getBoolean("mOpenAs", false); }
    public static void setMenu(Context c, String key, boolean b) { p(c).edit().putBoolean(key, b).apply(); }

    public static boolean recentsCaptures(Context c) { return p(c).getBoolean("recentsCaptures", true); }
    public static void setRecentsCaptures(Context c, boolean b) { p(c).edit().putBoolean("recentsCaptures", b).apply(); }

    // ---- appearance ----
    public static int themeMode(Context c) { return p(c).getInt("themeMode", 1); }
    public static void setThemeMode(Context c, int m) { p(c).edit().putInt("themeMode", m).apply(); }
    public static int accentMode(Context c) { return p(c).getInt("accentMode", 1); }
    public static void setAccentMode(Context c, int m) { p(c).edit().putInt("accentMode", m).apply(); }

    public static boolean sortAsc(Context c) { return p(c).getBoolean("sortAsc", false); }
    public static void setSortAsc(Context c, boolean b) { p(c).edit().putBoolean("sortAsc", b).apply(); }


    public static int trashDays(Context c) { return p(c).getInt("trashDays", 30); }
    public static void setTrashDays(Context c, int d) { p(c).edit().putInt("trashDays", d).apply(); }

    public static boolean searchAll(Context c) { return p(c).getBoolean("searchAll", true); }
    public static void setSearchAll(Context c, boolean b) { p(c).edit().putBoolean("searchAll", b).apply(); }

    public static String language(Context c) { return p(c).getString("language", "English (India)"); }
    public static void setLanguage(Context c, String lang) { p(c).edit().putString("language", lang).apply(); }

    // ---- default apps per extension (MIME types) ----
    public static String mimeApp(Context c, String ext) {
        return p(c).getString("mime_" + ext.toLowerCase(java.util.Locale.US), null);
    }

    public static void setMimeApp(Context c, String ext, String component) {
        p(c).edit().putString("mime_" + ext.toLowerCase(java.util.Locale.US), component).apply();
    }

    public static java.util.Map<String, String> allMimeApps(Context c) {
        java.util.Map<String, String> out = new java.util.LinkedHashMap<>();
        for (java.util.Map.Entry<String, ?> e : p(c).getAll().entrySet()) {
            if (e.getKey().startsWith("mime_") && e.getValue() instanceof String) {
                out.put(e.getKey().substring(5), (String) e.getValue());
            }
        }
        return out;
    }

    public static void clearMimeApps(Context c) {
        android.content.SharedPreferences.Editor ed = p(c).edit();
        for (String k : p(c).getAll().keySet()) if (k.startsWith("mime_")) ed.remove(k);
        ed.apply();
    }
}
