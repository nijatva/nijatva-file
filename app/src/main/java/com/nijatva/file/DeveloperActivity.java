package com.nijatva.file;

import com.nijatva.signature.SignatureDeveloperActivity;
import com.nijatva.signature.SignatureTheme;

/**
 * The developer card, from the shared signature kit: the same name, handle, figures
 * and links the other Nijatva apps show. It reads the app's own theme, so it opens in
 * whichever surface the file manager is in.
 */
public class DeveloperActivity extends SignatureDeveloperActivity {

    @Override
    protected int storedTheme() {
        return Store.themeMode(this);
    }

    @Override
    protected int storedAccent() {
        int mode = Store.accentMode(this);
        return SignatureTheme.ACCENT_VALUES[Math.max(0, Math.min(2, mode))];
    }
}
