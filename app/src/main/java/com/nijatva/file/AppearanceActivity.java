package com.nijatva.file;

import com.nijatva.signature.SignatureAppearanceActivity;
import com.nijatva.signature.SignatureTheme;

/**
 * Appearance, drawn by the suite's signature screen so every Nijatva app offers the
 * same four themes and three accents in the same layout.
 *
 * <p>The choice is not kept in a second place: it goes into this app's own settings,
 * the ones {@link Theme} and {@link Store} already read, so the file manager's chrome
 * follows it as soon as the screen closes.
 */
public class AppearanceActivity extends SignatureAppearanceActivity {

    @Override
    protected int storedTheme() {
        return Store.themeMode(this);
    }

    @Override
    protected int storedAccent() {
        return SignatureTheme.ACCENT_VALUES[accentIndex(Store.accentMode(this))];
    }

    @Override
    protected void store(int theme, int accent) {
        Store.setThemeMode(this, theme);
        Store.setAccentMode(this, accentIndex(accent));
    }

    /** Store keeps accents as an index; the signature screen hands back a colour. */
    private int accentIndex(int accent) {
        for (int i = 0; i < SignatureTheme.ACCENT_VALUES.length; i++) {
            if (SignatureTheme.ACCENT_VALUES[i] == accent) {
                return i;
            }
        }
        return 0;
    }
}
