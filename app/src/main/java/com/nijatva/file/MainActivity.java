package com.nijatva.file;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;

/** "Nijatva File" home: storage card, category tiles, sources, quick access, bottom nav. */
public class MainActivity extends Activity {

    static int BG = 0xFF000000, CARD = 0xFF1E1E1E, TEXT = 0xFFFFFFFF, SUB = 0xFF9E9E9E, BLUE = 0xFF2F80ED;

    private TextView storageLine, photosSub, videosSub, audioSub, docsSub, apksSub, archivesSub;
    private SegmentedBar segBar;
    private TextView legend;
    private String lastSig;
    private boolean hadAccess;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        lastSig = Theme.apply(this);
        hadAccess = Fm.hasStorageAccess(this);
        buildUi();
        if (!Fm.hasStorageAccess(this)) {
            Toast.makeText(this, "Grant file access to browse storage", Toast.LENGTH_LONG).show();
            Fm.requestStorageAccess(this, 42);
        }
        if (Build.VERSION.SDK_INT >= 30 && !Fm.hasFullAccess(this)) {
            Toast.makeText(this, "Tap ALLOW ALL FILES so APKs, ZIPs and all folders show", Toast.LENGTH_LONG).show();
        }
        refresh();
        autoCleanTrash();
    }

    private void autoCleanTrash() {
        new Thread(() -> {
            try { Fm.purgeOldTrash(this, Store.trashDays(this)); } catch (Exception ignored) { }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String sig = Theme.apply(this);
        if (lastSig != null && !lastSig.equals(sig)) { lastSig = sig; recreate(); return; }
        lastSig = sig;
        boolean acc = Fm.hasStorageAccess(this);
        if (acc != hadAccess) { hadAccess = acc; recreate(); return; }
        refresh();
    }

    private void buildUi() {
        LinearLayout root = Ui.col(this);
        root.setBackgroundColor(BG);

        ScrollView sv = new ScrollView(this);
        LinearLayout body = Ui.col(this);
        body.setPadding(Ui.dp(this, 16), Ui.dp(this, 10), Ui.dp(this, 16), Ui.dp(this, 16));
        sv.addView(body);
        sv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(sv);

        if (!Fm.hasFullAccess(this) || !Fm.hasStorageAccess(this)) body.addView(accessBanner());

        // top icons
        LinearLayout top = Ui.row(this);
        top.setGravity(Gravity.END);
        top.addView(iconBtn("\uD83D\uDD0D", v -> openSearch()));
        top.addView(iconBtn("\u22EE", this::homeMenu));
        body.addView(top);

        // title
        TextView title = Ui.text(this, "Nijatva File", 30, TEXT, true);
        title.setPadding(0, Ui.dp(this, 2), 0, Ui.dp(this, 14));
        body.addView(title);

        // storage card
        LinearLayout card = Ui.col(this);
        card.setBackground(Ui.round(CARD, 16, this, 0, 0));
        card.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 18));
        card.addView(Ui.text(this, "All files", 13, TEXT, true));
        LinearLayout line = Ui.row(this);
        line.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
        storageLine = Ui.text(this, "\u2014", 26, TEXT, true);
        line.addView(storageLine);
        card.addView(line);
        segBar = new SegmentedBar(this);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 10));
        slp.setMargins(0, Ui.dp(this, 4), 0, Ui.dp(this, 12));
        segBar.setLayoutParams(slp);
        segBar.setTrackColor(Ui.dividerColor(this));
        card.addView(segBar);
        card.addView(legendRow());
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.setMargins(0, 0, 0, Ui.dp(this, 16));
        card.setLayoutParams(clp);
        body.addView(card);
        body.addView(chipRow());

        // categories 3 x 2
        body.addView(catRow(
                tile("Photos", R.drawable.ic_cat_images, 0xFFFF5252, () -> openCategory(Fm.CAT_IMAGE), t -> photosSub = t),
                tile("Videos", R.drawable.ic_cat_videos, 0xFF7C4DFF, () -> openCategory(Fm.CAT_VIDEO), t -> videosSub = t),
                tile("Audio", R.drawable.ic_cat_audio, 0xFF00BCD4, () -> openCategory(Fm.CAT_AUDIO), t -> audioSub = t)));
        body.addView(gap(10));
        body.addView(catRow(
                tile("Documents", R.drawable.ic_cat_documents, 0xFF2196F3, () -> openCategory(Fm.CAT_DOC), t -> docsSub = t),
                tile("APKs", R.drawable.ic_cat_apk, 0xFF4CAF50, () -> openCategory(Fm.CAT_APK), t -> apksSub = t),
                tile("Archives", R.drawable.ic_cat_archive, 0xFFFFC107, () -> openCategory(Fm.CAT_OTHER), t -> archivesSub = t)));
        body.addView(gap(20));

        // Sources
        body.addView(sectionHeader("Sources", () -> toast("Sources")));
        LinearLayout sourcesCard = sectionCard(
                rowItem(R.drawable.ic_cat_netaccess, 0xFF2F80ED, "Bluetooth", null, () -> toast("Bluetooth")),
                rowItem(R.drawable.ic_cat_downloads, 0xFF22C55E, "Downloads", null,
                        () -> openDir(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))));
        for (final File sd : Fm.externalRoots(this)) {
            sourcesCard.addView(rowItem(R.drawable.ic_folder_blue, 0, sd.getName(), null, () -> openDir(sd)));
        }
        sourcesCard.addView(rowItem(0, 0, "Add source", null, () -> addSource()));
        body.addView(sourcesCard);
        body.addView(gap(18));

        // Quick access
        body.addView(sectionHeader("Quick access", () -> toast("Quick access")));
        LinearLayout quickCard = sectionCard();
        int pinned = 0;
        for (String sp : Store.starred(this)) {
            File pf = new File(sp);
            if (pf.isDirectory() && pf.exists()) {
                quickCard.addView(rowItem(R.drawable.ic_folder_blue, 0, pf.getName(), pf.getAbsolutePath(), () -> openDir(pf)));
                pinned++;
            }
        }
        if (pinned == 0) quickCard.addView(rowItem(0, 0, "Add folders", "Star a folder to pin it here", () -> toast("Open a folder, long-press \u2192 Star")));
        else quickCard.addView(rowItem(0, 0, "Add folders", null, () -> toast("Star a folder to pin it here")));
        body.addView(quickCard);
        body.addView(gap(18));

        // Tag
        body.addView(sectionHeader("Tag", () -> toast("Tags")));
        body.addView(gap(20));

        // bottom nav
        root.addView(bottomNav());
        setContentView(root);
    }

    private LinearLayout bottomNav() {
        LinearLayout nav = Ui.row(this);
        try { nav.setElevation(Ui.dp(this, 8)); } catch (Exception ignored) { }
        nav.setBackgroundColor(0xE6111111);
        nav.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 12));
        nav.addView(navItem("\u25F4", "Recent", false, () -> {
            Intent i = new Intent(this, BrowserActivity.class);
            i.putExtra("newfiles", true);
            startActivity(i);
        }));
        nav.addView(navItem("\uD83D\uDCC1", "Files", true, () -> openDir(Environment.getExternalStorageDirectory())));
        return nav;
    }

    private LinearLayout navItem(String glyph, String label, boolean active, final Runnable action) {
        LinearLayout c = Ui.col(this);
        c.setGravity(Gravity.CENTER);
        c.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView g = Ui.text(this, glyph, 20, active ? BLUE : SUB, false);
        g.setGravity(Gravity.CENTER);
        TextView t = Ui.text(this, label, 12, active ? BLUE : SUB, active);
        t.setGravity(Gravity.CENTER);
        c.addView(g);
        c.addView(t);
        c.setOnClickListener(v -> action.run());
        return c;
    }

    private TextView iconBtn(String glyph, View.OnClickListener l) {
        TextView t = Ui.text(this, glyph, 22, TEXT, false);
        t.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 4), Ui.dp(this, 8));
        t.setOnClickListener(l);
        return t;
    }

    private LinearLayout accessBanner() {
        boolean needAll = Build.VERSION.SDK_INT >= 30 && !Fm.hasFullAccess(this);
        LinearLayout card = Ui.col(this);
        card.setBackground(Ui.round(0xFF3A2A00, 14, this, 0, 0));
        card.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, Ui.dp(this, 14));
        card.setLayoutParams(lp);

        card.addView(Ui.text(this, needAll ? "All files access needed" : "File access needed", 15, TEXT, true));
        TextView s = Ui.text(this, needAll
                ? "To see APKs, ZIPs, documents and every folder, allow \u201CAll files access\u201D for Nijatva File."
                : "Allow access so images, videos and music can be listed and opened.", 12, SUB, false);
        s.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 12));
        card.addView(s);

        TextView btn = Ui.text(this, needAll ? "ALLOW ALL FILES" : "GRANT ACCESS", 14, Color.WHITE, true);
        btn.setGravity(Gravity.CENTER);
        btn.setBackground(Ui.round(BLUE, 10, this, 0, 0));
        btn.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 12));
        btn.setOnClickListener(v -> {
            if (needAll) Fm.requestAllFiles(this);
            else Fm.requestStorageAccess(this, 42);
        });
        card.addView(btn);

        TextView link = Ui.text(this, "Open app settings", 12, BLUE, false);
        link.setPadding(0, Ui.dp(this, 12), 0, 0);
        link.setOnClickListener(v -> Fm.openAppSettings(this));
        card.addView(link);
        return card;
    }

    private void homeMenu(View anchor) {
        android.widget.PopupMenu pm = new android.widget.PopupMenu(this, anchor);
        pm.getMenu().add("Edit");
        pm.getMenu().add("Smart Clean");
        pm.getMenu().add("Device info");
        pm.getMenu().add("App Manager");
        pm.getMenu().add("Recycle Bin");
        pm.getMenu().add("Settings");
        pm.setOnMenuItemClickListener(mi -> {
            String t = mi.getTitle().toString();
            if (t.equals("Settings")) openSettings();
            else if (t.equals("Smart Clean")) startActivity(new Intent(this, ToolsActivity.class));
            else if (t.equals("Device info")) startActivity(new Intent(this, DeviceInfoActivity.class));
            else if (t.equals("App Manager")) startActivity(new Intent(this, AppManagerActivity.class));
            else if (t.equals("Recycle Bin")) startActivity(new Intent(this, RecycleBinActivity.class));
            else toast("Edit");
            return true;
        });
        pm.show();
    }

    private View gap(int h) { return Ui.spacer(this, h); }

    private LinearLayout sectionHeader(String title, final Runnable action) {
        LinearLayout row = Ui.row(this);
        row.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 10));
        TextView t = Ui.text(this, title, 17, TEXT, true);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView chev = Ui.text(this, "\u02C4", 18, SUB, false);
        row.addView(t);
        row.addView(chev);
        row.setOnClickListener(v -> action.run());
        return row;
    }

    private LinearLayout sectionCard(LinearLayout... rows) {
        LinearLayout card = Ui.col(this);
        card.setBackground(Ui.round(CARD, 16, this, 0, 0));
        card.setPadding(Ui.dp(this, 14), Ui.dp(this, 6), Ui.dp(this, 14), Ui.dp(this, 6));
        for (LinearLayout r : rows) card.addView(r);
        return card;
    }

    private LinearLayout rowItem(int iconRes, int color, String label, String sub, final Runnable action) {
        LinearLayout r = Ui.row(this);
        r.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 12));
        if (iconRes != 0) {
            ImageView ic = new ImageView(this);
            ic.setImageResource(iconRes);
            ic.setScaleType(ImageView.ScaleType.FIT_CENTER);
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(Ui.dp(this, 38), Ui.dp(this, 38));
            ilp.setMargins(0, 0, Ui.dp(this, 14), 0);
            r.addView(ic, ilp);
        } else {
            TextView plus = Ui.text(this, "+", 20, SUB, false);
            plus.setGravity(Gravity.CENTER);
            plus.setBackground(Ui.round(Ui.chipBg(this), 17, this, 1, Ui.dividerColor(this)));
            LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(Ui.dp(this, 34), Ui.dp(this, 34));
            plp.setMargins(0, 0, Ui.dp(this, 14), 0);
            r.addView(plus, plp);
        }
        LinearLayout c = Ui.col(this);
        c.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        c.addView(Ui.text(this, label, 15, TEXT, true));
        if (sub != null) c.addView(Ui.text(this, sub, 12, SUB, false));
        r.addView(c);
        r.addView(Ui.text(this, "\u203A", 20, SUB, false));
        r.setOnClickListener(v -> action.run());
        return r;
    }

    interface Sub { void set(TextView t); }

    private LinearLayout tile(String label, int iconRes, int color, final Runnable onClick, final Sub subSetter) {
        LinearLayout card = Ui.col(this);
        card.setBackground(Ui.round(CARD, 16, this, 0, 0));
        card.setPadding(Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16));
        card.setGravity(Gravity.CENTER_HORIZONTAL);

        ImageView ic = new ImageView(this);
        ic.setImageResource(iconRes);
        ic.setScaleType(ImageView.ScaleType.FIT_CENTER);
        card.addView(ic, new LinearLayout.LayoutParams(Ui.dp(this, 50), Ui.dp(this, 50)));

        TextView t = Ui.text(this, label, 13, TEXT, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 2));
        TextView s = Ui.text(this, "", 11, SUB, false);
        s.setGravity(Gravity.CENTER);
        card.addView(t);
        card.addView(s);
        if (subSetter != null) subSetter.set(s);
        card.setOnClickListener(v -> onClick.run());
        pressScale(card);
        try { card.setElevation(Ui.dp(this, 2)); } catch (Exception ignored) { }
        return card;
    }

    private LinearLayout catRow(LinearLayout... cards) {
        LinearLayout row = Ui.row(this);
        row.setGravity(Gravity.TOP);
        for (LinearLayout c : cards) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(Ui.dp(this, 4), 0, Ui.dp(this, 4), 0);
            c.setLayoutParams(lp);
            row.addView(c);
        }
        return row;
    }

    private LinearLayout legendRow() {
        LinearLayout wrap = Ui.col(this);
        int[][] colors = {{0xFFFF5252}, {0xFF7C4DFF}, {0xFF00BCD4}, {0xFF2196F3}, {0xFF4CAF50}, {0xFF37474F}};
        String[] labels = {"Images", "Videos", "Audio", "Docs", "Apps", "Free"};
        LinearLayout r1 = Ui.row(this);
        LinearLayout r2 = Ui.row(this);
        for (int i = 0; i < labels.length; i++) {
            LinearLayout item = Ui.row(this);
            View dot = new View(this);
            dot.setBackground(Ui.round(colors[i][0], 4, this, 0, 0));
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(Ui.dp(this, 8), Ui.dp(this, 8));
            dlp.setMargins(0, 0, Ui.dp(this, 5), 0);
            item.addView(dot, dlp);
            item.addView(Ui.text(this, labels[i], 10, MainActivity.SUB, false));
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            item.setLayoutParams(ilp);
            if (i < 3) r1.addView(item); else r2.addView(item);
        }
        wrap.addView(r1);
        wrap.addView(r2);
        return wrap;
    }

    private void pressScale(View v) {
        v.setOnTouchListener((view, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) view.animate().scaleX(0.96f).scaleY(0.96f).setDuration(90).start();
            else if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL)
                view.animate().scaleX(1f).scaleY(1f).setDuration(90).start();
            return false;
        });
    }

    private long catSize(int cat) {
        long t = 0;
        for (File f : Fm.byCategory(this, cat)) t += f.length();
        return t;
    }

    private HorizontalScrollView chipRow() {
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = Ui.row(this);
        row.setPadding(0, 0, 0, Ui.dp(this, 14));
        row.addView(chip("Images", () -> openCategory(Fm.CAT_IMAGE)));
        row.addView(chip("Videos", () -> openCategory(Fm.CAT_VIDEO)));
        row.addView(chip("Audio", () -> openCategory(Fm.CAT_AUDIO)));
        row.addView(chip("Documents", () -> openCategory(Fm.CAT_DOC)));
        row.addView(chip("APKs", () -> openCategory(Fm.CAT_APK)));
        row.addView(chip("Large files", this::openLarge));
        hs.addView(row);
        return hs;
    }

    private TextView chip(String label, final Runnable action) {
        TextView t = Ui.text(this, label, 13, MainActivity.TEXT, true);
        t.setPadding(Ui.dp(this, 16), Ui.dp(this, 9), Ui.dp(this, 16), Ui.dp(this, 9));
        t.setBackground(Ui.round(MainActivity.CARD, 20, this, 0, 0));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, Ui.dp(this, 8), 0);
        t.setLayoutParams(lp);
        t.setOnClickListener(v -> action.run());
        return t;
    }

    private void openLarge() {
        Intent i = new Intent(this, BrowserActivity.class);
        i.putExtra("large", true);
        startActivity(i);
    }

    private void refresh() {
        long[] s = Fm.storage();
        long used = s[0] - s[1];
        int pct = s[0] > 0 ? (int) (used * 100 / s[0]) : 0;
        if (storageLine != null) {
            storageLine.setText(Fm.fmtSize(used) + "  |  " + Fm.fmtSize(s[0]));
        }

        new Thread(() -> {
            final String[] out = new String[6];
            try {
                out[0] = String.valueOf(Fm.byCategory(this, Fm.CAT_IMAGE).size());
                out[1] = String.valueOf(Fm.byCategory(this, Fm.CAT_VIDEO).size());
                out[2] = String.valueOf(Fm.byCategory(this, Fm.CAT_AUDIO).size());
                out[3] = String.valueOf(Fm.byCategory(this, Fm.CAT_DOC).size());
                out[4] = String.valueOf(Fm.byCategory(this, Fm.CAT_APK).size());
            } catch (Exception ignored) {
            }
            out[5] = String.valueOf(countArchives());
            runOnUiThread(() -> {
                if (photosSub != null) photosSub.setText(out[0]);
                if (videosSub != null) videosSub.setText(out[1]);
                if (audioSub != null) audioSub.setText(out[2]);
                if (docsSub != null) docsSub.setText(out[3]);
                if (apksSub != null) apksSub.setText(out[4]);
                if (archivesSub != null) archivesSub.setText(out[5]);
            });
        }).start();
    }

    private int countArchives() {
        int n = 0;
        File ext = Environment.getExternalStorageDirectory();
        File[] roots = {new File(ext, "Download"), new File(ext, "Downloads"), new File(ext, "Documents")};
        for (File r : roots) n += countArchives(r, 0);
        return n;
    }

    private int countArchives(File dir, int depth) {
        if (dir == null || !dir.exists() || depth > 3) return 0;
        int n = 0;
        File[] arr = dir.listFiles();
        if (arr == null) return 0;
        for (File f : arr) {
            if (f.isDirectory()) n += countArchives(f, depth + 1);
            else {
                String e = Fm.ext(f.getName());
                if (e.equals("zip") || e.equals("rar") || e.equals("7z") || e.equals("tar") || e.equals("gz")) n++;
            }
        }
        return n;
    }

    private void addSource() {
        final String[] opts = {"Bluetooth", "Downloads", "SD card", "Cloud"};
        Dlg.b(this).setTitle("Add source").setItems(opts, (d, w) -> toast(opts[w] + " added")).show();
    }

    private void openCategory(int cat) {
        Intent i = new Intent(this, BrowserActivity.class);
        i.putExtra("category", cat);
        startActivity(i);
    }

    private void openDir(File dir) {
        Intent i = new Intent(this, BrowserActivity.class);
        i.putExtra("path", dir.getAbsolutePath());
        startActivity(i);
    }

    private void openSearch() {
        Intent i = new Intent(this, BrowserActivity.class);
        i.putExtra("path", Environment.getExternalStorageDirectory().getAbsolutePath());
        i.putExtra("search", true);
        startActivity(i);
    }

    private void openSettings() {
        startActivity(new Intent(this, FilesSettingsActivity.class));
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
