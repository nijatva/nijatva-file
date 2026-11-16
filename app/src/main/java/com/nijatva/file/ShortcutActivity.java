package com.nijatva.file;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** Small exported entry point used by launcher shortcuts (opens a folder). */
public class ShortcutActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        String path = getIntent() != null ? getIntent().getStringExtra("path") : null;
        Intent i = new Intent(this, BrowserActivity.class);
        if (path != null) i.putExtra("path", path);
        startActivity(i);
        finish();
    }
}
