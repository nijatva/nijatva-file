package com.nijatva.file;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

/** "Temporary files": scan common temp/junk locations and delete them. */
public class CleanActivity extends Activity {

    private TextView junkLine;
    private long junkBytes = 0;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Theme.apply(this);

        LinearLayout root = Ui.col(this);
        root.setBackgroundColor(MainActivity.BG);

        LinearLayout header = Ui.row(this);
        header.setPadding(Ui.dp(this, 12), Ui.dp(this, 16), Ui.dp(this, 12), Ui.dp(this, 16));
        TextView back = Ui.text(this, "\u2190", 22, MainActivity.TEXT, false);
        back.setPadding(0, 0, Ui.dp(this, 14), 0);
        back.setOnClickListener(v -> finish());
        header.addView(back);
        header.addView(Ui.text(this, "Temporary files", 20, MainActivity.TEXT, true));
        root.addView(header);

        ScrollView sv = new ScrollView(this);
        LinearLayout body = Ui.col(this);
        body.setPadding(Ui.dp(this, 14), Ui.dp(this, 6), Ui.dp(this, 14), Ui.dp(this, 24));
        sv.addView(body);

        LinearLayout card = Ui.col(this);
        card.setBackground(Ui.round(MainActivity.CARD, 14, this, 0, 0));
        card.setPadding(Ui.dp(this, 18), Ui.dp(this, 22), Ui.dp(this, 18), Ui.dp(this, 22));
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView big = Ui.text(this, "\u2726", 40, 0xFF34A853, true);
        junkLine = Ui.text(this, "Scanning\u2026", 16, MainActivity.TEXT, true);
        junkLine.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 4));
        TextView sub = Ui.text(this, "Temporary and junk files", 12, MainActivity.SUB, false);
        card.addView(big);
        card.addView(junkLine);
        card.addView(sub);
        body.addView(card);
        body.addView(Ui.spacer(this, 16));

        TextView cleanBtn = Ui.text(this, "CLEAN NOW", 15, Color.WHITE, true);
        cleanBtn.setGravity(Gravity.CENTER);
        cleanBtn.setBackground(Ui.round(MainActivity.BLUE, 12, this, 0, 0));
        cleanBtn.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 14));
        cleanBtn.setOnClickListener(v -> clean());
        body.addView(cleanBtn);

        root.addView(sv);
        setContentView(root);
        scan();
    }

    private void scan() {
        new Thread(() -> {
            long total = 0;
            total += sizeOf(new File(Environment.getExternalStorageDirectory(), "Android/data"));
            total += sizeOf(new File(Environment.getExternalStorageDirectory(), "Download"));
            total += matchSize(new File(Environment.getExternalStorageDirectory(), "Download"), new String[]{".tmp", ".log", ".temp", ".bak"});
            total += sizeOf(getExternalCacheDir());
            total += sizeOf(getCacheDir());
            junkBytes = total;
            runOnUiThread(() -> junkLine.setText(Fm.fmtSize(junkBytes)));
        }).start();
    }

    private long sizeOf(File f) {
        if (f == null || !f.exists()) return 0;
        if (f.isFile()) return f.length();
        long t = 0;
        File[] arr = f.listFiles();
        if (arr != null) for (File c : arr) t += sizeOf(c);
        return t;
    }

    private long matchSize(File dir, String[] exts) {
        if (dir == null || !dir.exists()) return 0;
        long t = 0;
        File[] arr = dir.listFiles();
        if (arr == null) return 0;
        for (File c : arr) {
            String n = c.getName().toLowerCase();
            for (String e : exts) if (n.endsWith(e)) t += c.length();
        }
        return t;
    }

    private void clean() {
        Dlg.b(this)
                .setTitle("Clean junk files?")
                .setMessage("Delete temporary files (.tmp, .log, .temp, .bak) and app caches.")
                .setPositiveButton("Clean", (d, w) -> new Thread(() -> {
                    int n = 0;
                    File dl = new File(Environment.getExternalStorageDirectory(), "Download");
                    File[] arr = dl.listFiles();
                    if (arr != null) for (File c : arr) {
                        String nm = c.getName().toLowerCase();
                        if (nm.endsWith(".tmp") || nm.endsWith(".log") || nm.endsWith(".temp") || nm.endsWith(".bak")) {
                            if (c.delete()) n++;
                        }
                    }
                    clearDir(getExternalCacheDir());
                    clearDir(getCacheDir());
                    final int done = n;
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Cleaned " + done + " file(s)", Toast.LENGTH_SHORT).show();
                        scan();
                    });
                }).start())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearDir(File d) {
        if (d == null || !d.exists()) return;
        File[] arr = d.listFiles();
        if (arr != null) for (File c : arr) Fm.deleteRecursive(c);
    }
}
