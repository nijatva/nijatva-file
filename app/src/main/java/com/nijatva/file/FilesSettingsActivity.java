package com.nijatva.file;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Settings: default app, hidden files, recents, plus advanced options. */
public class FilesSettingsActivity extends Activity {

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
        header.addView(Ui.text(this, "Settings", 22, MainActivity.TEXT, true));
        root.addView(header);

        ScrollView sv = new ScrollView(this);
        LinearLayout body = Ui.col(this);
        body.setPadding(Ui.dp(this, 14), Ui.dp(this, 4), Ui.dp(this, 14), Ui.dp(this, 28));
        sv.addView(body);

        // ---- card 1 ----
        LinearLayout c1 = card();
        c1.addView(navRow("Appearance", "Theme and accent colour", () -> startActivity(new android.content.Intent(this, AppearanceActivity.class))));
        c1.addView(divider());
        c1.addView(navRow("Developer", "About the developer", () -> startActivity(new android.content.Intent(this, DeveloperActivity.class))));
        c1.addView(divider());
        c1.addView(navRow("App Manager", "Back up APKs, uninstall apps", () -> startActivity(new android.content.Intent(this, AppManagerActivity.class))));
        c1.addView(divider());
        c1.addView(navRow("Smart Clean", "Duplicates, empty folders, large files", () -> startActivity(new android.content.Intent(this, ToolsActivity.class))));
        c1.addView(divider());
        c1.addView(navRow("Device info", "Storage, RAM, battery, CPU", () -> startActivity(new android.content.Intent(this, DeviceInfoActivity.class))));
        c1.addView(divider());
        c1.addView(navRow("File access (All files)", "Allow browsing every folder", () -> Fm.requestAllFiles(this)));
        c1.addView(divider());
        c1.addView(navRow("Set default app for opening files", null, () -> startActivity(new android.content.Intent(this, MimeTypesActivity.class))));
        c1.addView(divider());
        c1.addView(toggleRow("Display hidden files", "Show hidden files starting with a \".\" (dot).", Store.showHidden(this), v -> Store.setShowHidden(this, v)));
        body.addView(c1);
        body.addView(gap(16));

        // ---- card 2 ----
        LinearLayout c2 = card();
        LinearLayout ver = Ui.col(this);
        ver.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));
        ver.addView(Ui.text(this, "Version", 15, MainActivity.TEXT, true));
        ver.addView(Ui.text(this, Fm.versionName(this), 13, MainActivity.SUB, false));
        c2.addView(ver);
        c2.addView(divider());
        c2.addView(navRow("About Nijatva File", null, () -> Dlg.b(this)
                .setTitle("About Nijatva File")
                .setMessage("Nijatva File \u2014 version " + Fm.versionName(this) + "\n\nA file manager for Android with list / grid views, a built-in image, video and music player, a text editor, ZIP support, favourites, Recycle Bin and storage analysis.\n\nDesigned & developed by Vasudev Dangaya\nVasudev.work@rediffmail.com")
                .setPositiveButton("OK", null).show()));
        c2.addView(divider());
        c2.addView(navRow("Privacy", null, () -> Dlg.b(this)
                .setTitle("Privacy")
                .setMessage("This app works entirely on your device. It does not upload your files anywhere, has no analytics and needs no account.\n\nFile access is used only to list and open your files.")
                .setPositiveButton("OK", null).show()));
        body.addView(c2);
        body.addView(gap(16));

        // ---- advanced ----
        body.addView(section("Advanced settings"));
        LinearLayout c3 = card();
        c3.addView(toggleRow("Use the Recycle Bin by default", "Deleted files go to the Recycle Bin.", Store.recycleDefault(this), v -> Store.setRecycleDefault(this, v)));
        c3.addView(divider());
        c3.addView(toggleRow("Show recycle confirmation", "Ask before moving files to the Recycle Bin.", Store.recycleConfirm(this), v -> Store.setRecycleConfirm(this, v)));
        c3.addView(divider());
        c3.addView(valueRow("File size unit", Store.fileSizeUnit(this), () -> pick("File size unit", new String[]{"Auto", "B", "kB", "MB", "GB"}, Store.fileSizeUnit(this), u -> Store.setFileSizeUnit(this, u))));
        c3.addView(divider());
        c3.addView(valueRow("Date format", Store.dateFormat(this), () -> pick("Date format", new String[]{"8 Oct 2026", "08/10/2026", "Oct 8, 2026"}, Store.dateFormat(this), f -> Store.setDateFormat(this, f))));
        body.addView(c3);
        body.addView(gap(16));

        body.addView(section("Show advanced menus"));
        LinearLayout c4 = card();
        c4.addView(toggleRow("Add Bookmark", null, Store.menuBookmark(this), v -> Store.setMenu(this, "mBookmark", v)));
        c4.addView(divider());
        c4.addView(toggleRow("Add to Home screen", null, Store.menuHomeScreen(this), v -> Store.setMenu(this, "mHomeScreen", v)));
        c4.addView(divider());
        c4.addView(toggleRow("Hide/Unhide", null, Store.menuHideUnhide(this), v -> Store.setMenu(this, "mHideUnhide", v)));
        c4.addView(divider());
        c4.addView(toggleRow("Open as", null, Store.menuOpenAs(this), v -> Store.setMenu(this, "mOpenAs", v)));
        body.addView(c4);
        body.addView(gap(16));

        body.addView(section("Other"));
        LinearLayout c5 = card();
        c5.addView(navRow("Temporary files", null, () -> startActivity(new android.content.Intent(this, CleanActivity.class))));
        body.addView(c5);

        root.addView(sv);
        setContentView(root);
    }

    interface BoolCb { void on(boolean v); }
    interface StrCb { void on(String v); }

    private LinearLayout card() {
        LinearLayout c = Ui.col(this);
        c.setBackground(Ui.round(MainActivity.CARD, 16, this, 0, 0));
        c.setPadding(0, Ui.dp(this, 2), 0, Ui.dp(this, 2));
        return c;
    }

    private android.view.View divider() {
        android.view.View v = new android.view.View(this);
        v.setBackgroundColor(Ui.dividerColor(this));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 1));
        lp.setMargins(Ui.dp(this, 16), 0, Ui.dp(this, 16), 0);
        v.setLayoutParams(lp);
        return v;
    }

    private TextView section(String title) {
        TextView t = Ui.text(this, title, 13, MainActivity.BLUE, true);
        t.setPadding(Ui.dp(this, 4), 0, 0, Ui.dp(this, 8));
        return t;
    }

    private android.view.View gap(int h) { return Ui.spacer(this, h); }

    private LinearLayout navRow(String title, String sub, final Runnable onClick) {
        LinearLayout r = Ui.row(this);
        r.setPadding(Ui.dp(this, 16), Ui.dp(this, 15), Ui.dp(this, 16), Ui.dp(this, 15));
        LinearLayout c = Ui.col(this);
        c.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        c.addView(Ui.text(this, title, 15, MainActivity.TEXT, true));
        if (sub != null) c.addView(Ui.text(this, sub, 12, MainActivity.SUB, false));
        r.addView(c);
        r.addView(Ui.text(this, "\u203A", 20, MainActivity.SUB, false));
        r.setOnClickListener(v -> onClick.run());
        return r;
    }

    private LinearLayout toggleRow(String title, String sub, boolean checked, final BoolCb cb) {
        LinearLayout r = Ui.row(this);
        r.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14));
        LinearLayout c = Ui.col(this);
        c.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        c.addView(Ui.text(this, title, 15, MainActivity.TEXT, true));
        if (sub != null) {
            TextView s = Ui.text(this, sub, 12, MainActivity.SUB, false);
            s.setPadding(0, Ui.dp(this, 3), 0, 0);
            c.addView(s);
        }
        r.addView(c);
        final CheckBox cbx = new CheckBox(this);
        cbx.setChecked(checked);
        cbx.setOnCheckedChangeListener((bv, v) -> cb.on(v));
        r.addView(cbx);
        r.setOnClickListener(v -> cbx.setChecked(!cbx.isChecked()));
        return r;
    }

    private LinearLayout valueRow(String title, String value, final Runnable onClick) {
        LinearLayout r = Ui.row(this);
        r.setPadding(Ui.dp(this, 16), Ui.dp(this, 15), Ui.dp(this, 16), Ui.dp(this, 15));
        LinearLayout c = Ui.col(this);
        c.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        c.addView(Ui.text(this, title, 15, MainActivity.TEXT, true));
        r.addView(c);
        TextView val = Ui.text(this, value == null ? "" : value, 13, MainActivity.SUB, false);
        r.addView(val);
        if (onClick != null) r.setOnClickListener(v -> onClick.run());
        return r;
    }

    private void pick(String title, String[] options, String current, final StrCb cb) {
        int checked = 0;
        for (int i = 0; i < options.length; i++) if (options[i].equals(current)) checked = i;
        Dlg.b(this)
                .setTitle(title)
                .setSingleChoiceItems(options, checked, (d, w) -> {
                    cb.on(options[w]);
                    d.dismiss();
                    recreate();
                })
                .show();
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
