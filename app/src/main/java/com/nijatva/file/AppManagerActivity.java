package com.nijatva.file;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** App Manager: list installed apps, back up APKs, open app info, uninstall. */
public class AppManagerActivity extends Activity {

    private LinearLayout box;
    private TextView countLine;

    static class AppEntry {
        String label, pkg, apkPath;
        long size;
        android.graphics.drawable.Drawable icon;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Theme.apply(this);

        LinearLayout root = Ui.col(this);
        root.setBackgroundColor(MainActivity.BG);

        LinearLayout header = Ui.row(this);
        header.setPadding(Ui.dp(this, 12), Ui.dp(this, 16), Ui.dp(this, 12), Ui.dp(this, 16));
        TextView back = Ui.text(this, "\u2190", 24, MainActivity.TEXT, false);
        back.setPadding(0, 0, Ui.dp(this, 14), 0);
        back.setOnClickListener(v -> finish());
        header.addView(back);
        header.addView(Ui.text(this, "App Manager", 22, MainActivity.TEXT, true));
        root.addView(header);

        countLine = Ui.text(this, "", 13, MainActivity.SUB, false);
        countLine.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 8));
        root.addView(countLine);

        ScrollView sv = new ScrollView(this);
        box = Ui.col(this);
        box.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), Ui.dp(this, 24));
        sv.addView(box);
        root.addView(sv);

        setContentView(root);
        load();
    }

    private void load() {
        new Thread(() -> {
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> all = pm.getInstalledApplications(0);
            final List<AppEntry> apps = new ArrayList<>();
            for (ApplicationInfo ai : all) {
                AppEntry e = new AppEntry();
                e.label = pm.getApplicationLabel(ai).toString();
                e.pkg = ai.packageName;
                e.apkPath = ai.sourceDir;
                e.size = ai.sourceDir != null ? new File(ai.sourceDir).length() : 0;
                try { e.icon = pm.getApplicationIcon(ai); } catch (Exception ignored) { }
                apps.add(e);
            }
            Collections.sort(apps, new Comparator<AppEntry>() {
                public int compare(AppEntry a, AppEntry b) { return a.label.compareToIgnoreCase(b.label); }
            });
            runOnUiThread(() -> render(apps));
        }).start();
    }

    private void render(List<AppEntry> apps) {
        box.removeAllViews();
        countLine.setText(apps.size() + " installed apps");
        for (final AppEntry e : apps) {
            LinearLayout row = Ui.row(this);
            row.setBackground(Ui.round(MainActivity.CARD, 14, this, 0, 0));
            row.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, Ui.dp(this, 8));
            row.setLayoutParams(lp);

            ImageView icon = new ImageView(this);
            if (e.icon != null) icon.setImageDrawable(e.icon);
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 42));
            ilp.setMargins(0, 0, Ui.dp(this, 14), 0);
            row.addView(icon, ilp);

            LinearLayout col = Ui.col(this);
            col.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            col.addView(Ui.text(this, e.label, 15, MainActivity.TEXT, true));
            col.addView(Ui.text(this, e.pkg + "  \u00B7  " + Fm.fmtSize(e.size), 12, MainActivity.SUB, false));
            row.addView(col);

            row.addView(Ui.text(this, "\u203A", 20, MainActivity.SUB, false));
            row.setOnClickListener(v -> actions(e));
            box.addView(row);
        }
    }

    private void actions(final AppEntry e) {
        final String[] opts = {"Back up APK", "App info", "Uninstall"};
        Dlg.b(this)
                .setTitle(e.label)
                .setItems(opts, (d, w) -> {
                    if (w == 0) backup(e);
                    else if (w == 1) {
                        try {
                            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + e.pkg)));
                        } catch (Exception ex) { toast("Cannot open app info"); }
                    } else {
                        try {
                            startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + e.pkg)));
                        } catch (Exception ex) { toast("Cannot uninstall"); }
                    }
                })
                .show();
    }

    private void backup(final AppEntry e) {
        new Thread(() -> {
            String msg;
            try {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "NijatvaBackup");
                if (!dir.exists() && !dir.mkdirs()) {
                    dir = new File(getExternalFilesDir(null), "Backup");
                    if (!dir.exists()) dir.mkdirs();
                }
                File out = new File(dir, e.label.replaceAll("[^A-Za-z0-9._-]", "_") + ".apk");
                InputStream in = new FileInputStream(e.apkPath);
                OutputStream os = new FileOutputStream(out);
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
                in.close();
                os.close();
                msg = "Saved to " + out.getParentFile().getName() + "/" + out.getName();
            } catch (Exception ex) {
                msg = "Backup failed";
            }
            final String m = msg;
            runOnUiThread(() -> toast(m));
        }).start();
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
