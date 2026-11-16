package com.nijatva.file;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;

/** Smart Clean: duplicate files, empty folders, large files. */
public class ToolsActivity extends Activity {

    private TextView dupSub, emptySub;
    private List<List<File>> dups;
    private List<File> empties;

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
        header.addView(Ui.text(this, "Smart Clean", 22, MainActivity.TEXT, true));
        root.addView(header);

        ScrollView sv = new ScrollView(this);
        LinearLayout body = Ui.col(this);
        body.setPadding(Ui.dp(this, 14), 0, Ui.dp(this, 14), Ui.dp(this, 28));
        sv.addView(body);

        TextView note = Ui.text(this, "Scans DCIM, Pictures, Download, WhatsApp, Movies, Music and Documents.", 12, MainActivity.SUB, false);
        note.setPadding(Ui.dp(this, 4), 0, 0, Ui.dp(this, 12));
        body.addView(note);

        LinearLayout card = Ui.col(this);
        card.setBackground(Ui.round(MainActivity.CARD, 16, this, 0, 0));
        card.setPadding(0, Ui.dp(this, 2), 0, Ui.dp(this, 2));

        dupSub = addRow(card, "Duplicate files", "Scanning\u2026", () -> cleanDuplicates());
        card.addView(divider());
        emptySub = addRow(card, "Empty folders", "Scanning\u2026", () -> cleanEmpty());
        card.addView(divider());
        addRow(card, "Large files", "Files over 50 MB", () -> {
            android.content.Intent i = new android.content.Intent(this, BrowserActivity.class);
            i.putExtra("large", true);
            startActivity(i);
        });
        card.addView(divider());
        addRow(card, "Storage analysis", "See what is using space", () -> startActivity(new android.content.Intent(this, StorageAnalysisActivity.class)));

        body.addView(card);
        root.addView(sv);
        setContentView(root);

        scan();
    }

    private void scan() {
        new Thread(() -> {
            final List<List<File>> d = Fm.findDuplicates(this);
            final List<File> e = Fm.findEmptyFolders(this);
            runOnUiThread(() -> {
                dups = d;
                empties = e;
                long bytes = 0;
                int extra = 0;
                for (List<File> g : d) {
                    for (int i = 1; i < g.size(); i++) { bytes += g.get(i).length(); extra++; }
                }
                if (dupSub != null)
                    dupSub.setText(extra == 0 ? "No duplicates found" : extra + " duplicate(s) \u00B7 " + Fm.fmtSize(bytes) + " recoverable");
                if (emptySub != null)
                    emptySub.setText(e.isEmpty() ? "No empty folders" : e.size() + " empty folder(s)");
            });
        }).start();
    }

    private void cleanDuplicates() {
        if (dups == null) { toast("Still scanning\u2026"); return; }
        final int extra = countExtras();
        if (extra == 0) { toast("No duplicates to clean"); return; }
        Dlg.b(this)
                .setTitle("Delete duplicates?")
                .setMessage(extra + " duplicate copy/copies will be moved to the Recycle Bin. The first copy of each file is kept.")
                .setPositiveButton("Move to Bin", (d, w) -> new Thread(() -> {
                    int n = 0;
                    for (List<File> g : dups) {
                        for (int i = 1; i < g.size(); i++) {
                            try { Fm.recycle(this, g.get(i)); n++; } catch (Exception ignored) { }
                        }
                    }
                    final int done = n;
                    runOnUiThread(() -> { toast(done + " moved to Recycle Bin"); scan(); });
                }).start())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private int countExtras() {
        int extra = 0;
        for (List<File> g : dups) extra += Math.max(0, g.size() - 1);
        return extra;
    }

    private void cleanEmpty() {
        if (empties == null) { toast("Still scanning\u2026"); return; }
        if (empties.isEmpty()) { toast("No empty folders"); return; }
        Dlg.b(this)
                .setTitle("Remove empty folders?")
                .setMessage(empties.size() + " empty folder(s) will be deleted.")
                .setPositiveButton("Delete", (d, w) -> new Thread(() -> {
                    int n = 0;
                    for (File f : empties) {
                        try { if (f.delete()) n++; } catch (Exception ignored) { }
                    }
                    final int done = n;
                    runOnUiThread(() -> { toast(done + " folder(s) removed"); scan(); });
                }).start())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private TextView addRow(LinearLayout card, String title, String sub, final Runnable action) {
        LinearLayout row = Ui.row(this);
        row.setPadding(Ui.dp(this, 16), Ui.dp(this, 15), Ui.dp(this, 16), Ui.dp(this, 15));
        LinearLayout c = Ui.col(this);
        c.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        c.addView(Ui.text(this, title, 15, MainActivity.TEXT, true));
        TextView s = Ui.text(this, sub, 12, MainActivity.SUB, false);
        s.setPadding(0, Ui.dp(this, 3), 0, 0);
        c.addView(s);
        row.addView(c);
        row.addView(Ui.text(this, "\u203A", 20, MainActivity.SUB, false));
        row.setOnClickListener(v -> action.run());
        card.addView(row);
        return s;
    }

    private android.view.View divider() {
        android.view.View v = new android.view.View(this);
        v.setBackgroundColor(Ui.dividerColor(this));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 1));
        lp.setMargins(Ui.dp(this, 16), 0, Ui.dp(this, 16), 0);
        v.setLayoutParams(lp);
        return v;
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
