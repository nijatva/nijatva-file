package com.nijatva.file;

import android.app.AlertDialog;
import android.content.Context;
import android.view.ContextThemeWrapper;

/**
 * Every dialog in the app is built through here, so they always get the dark
 * rounded theme (R.style.AppDialogTheme) regardless of the activity theme.
 */
public final class Dlg {

    private Dlg() {}

    public static AlertDialog.Builder b(Context c) {
        try {
            return new AlertDialog.Builder(new ContextThemeWrapper(c, R.style.AppDialogTheme));
        } catch (Throwable t) {
            // never let a theme problem take the whole screen down
            return new AlertDialog.Builder(c);
        }
    }
}
