package com.nijatva.file;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Entry point for "Open with → Nijatva File".
 * Handles file:// and content:// URIs, then routes to the right viewer.
 */
public class OpenWithActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Intent intent = getIntent();
        Uri uri = intent != null ? intent.getData() : null;
        if (uri == null) {
            toast("Nothing to open");
            finish();
            return;
        }
        try {
            String path = resolvePath(uri);
            if (path == null) {
                toast("Cannot read this file");
                finish();
                return;
            }
            dispatch(new File(path));
        } catch (Exception e) {
            toast("Cannot open this file");
            finish();
        }
    }

    private void dispatch(File f) {
        if (f.isDirectory()) {
            startActivity(new Intent(this, BrowserActivity.class).putExtra("path", f.getAbsolutePath()));
            finish();
            return;
        }
        int cat = Fm.catOf(f.getName());
        String ext = Fm.ext(f.getName());

        if (cat == Fm.CAT_IMAGE || cat == Fm.CAT_VIDEO || cat == Fm.CAT_AUDIO) {
            Intent i = new Intent(this, MediaViewerActivity.class);
            i.putExtra("paths", new String[]{f.getAbsolutePath()});
            i.putExtra("index", 0);
            startActivity(i);
        } else if (ext.equals("pdf")) {
            startActivity(new Intent(this, PdfViewerActivity.class).putExtra("path", f.getAbsolutePath()));
        } else if (isText(ext)) {
            startActivity(new Intent(this, TextEditorActivity.class).putExtra("path", f.getAbsolutePath()));
        } else {
            toast("This file type is not supported");
        }
        finish();
    }

    private boolean isText(String e) {
        switch (e) {
            case "txt": case "md": case "json": case "xml": case "log": case "csv": case "ini":
            case "yml": case "yaml": case "html": case "htm": case "js": case "css": case "java":
            case "kt": case "py": case "sh": case "c": case "cpp": case "h": case "gradle":
            case "properties": case "srt": case "vtt":
                return true;
            default:
                return false;
        }
    }

    /** file:// → direct path; content:// → copy into cache so our viewers can read it. */
    private String resolvePath(Uri uri) {
        if ("file".equals(uri.getScheme())) {
            String p = uri.getPath();
            return p != null ? p : null;
        }
        String name = "incoming_" + System.currentTimeMillis();
        Cursor c = null;
        try {
            c = getContentResolver().query(uri, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (i >= 0) {
                    String n = c.getString(i);
                    if (n != null && !n.isEmpty()) name = n;
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        File dir = new File(getCacheDir(), "incoming");
        if (!dir.exists()) dir.mkdirs();
        File out = new File(dir, name);
        InputStream in = null;
        OutputStream os = null;
        try {
            in = getContentResolver().openInputStream(uri);
            if (in == null) return null;
            os = new FileOutputStream(out);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
            return out.getAbsolutePath();
        } catch (Exception e) {
            return null;
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) { }
            try { if (os != null) os.close(); } catch (Exception ignored) { }
        }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
