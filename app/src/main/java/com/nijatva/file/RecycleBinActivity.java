package com.nijatva.file;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Recycle bin: restore or permanently delete recycled files. */
public class RecycleBinActivity extends Activity {

    private LinearLayout box;
    private TextView emptyLine;

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
        TextView t = Ui.text(this, "Recycle Bin", 18, MainActivity.TEXT, true);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView empty = Ui.text(this, "EMPTY", 13, MainActivity.BLUE, true);
        empty.setOnClickListener(v -> confirmEmpty());
        header.addView(back);
        header.addView(t);
        header.addView(empty);
        root.addView(header);

        emptyLine = Ui.text(this, "", 13, MainActivity.SUB, false);
        emptyLine.setPadding(Ui.dp(this, 14), 0, Ui.dp(this, 14), Ui.dp(this, 8));
        root.addView(emptyLine);

        ScrollView sv = new ScrollView(this);
        box = Ui.col(this);
        box.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), Ui.dp(this, 24));
        sv.addView(box);
        root.addView(sv);

        setContentView(root);
        refresh();
    }

    private void refresh() {
        box.removeAllViews();
        List<String[]> entries = Fm.recycleEntries(this);
        emptyLine.setText(entries.isEmpty() ? "Recycle Bin is empty" : entries.size() + " item(s)");
        SimpleDateFormat fmt = new SimpleDateFormat("d MMM yyyy, HH:mm", Locale.US);
        for (final String[] e : entries) {
            LinearLayout card = Ui.col(this);
            card.setBackground(Ui.round(MainActivity.CARD, 10, this, 0, 0));
            card.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, Ui.dp(this, 8));
            card.setLayoutParams(lp);

            card.addView(Ui.text(this, e[0], 14, MainActivity.TEXT, true));
            TextView path = Ui.text(this, e[1], 11, MainActivity.SUB, false);
            path.setPadding(0, Ui.dp(this, 2), 0, Ui.dp(this, 2));
            card.addView(path);
            String when = e[3] != null ? fmt.format(new Date(Long.parseLong(e[3]))) : "";
            card.addView(Ui.text(this, "Deleted " + when, 11, MainActivity.SUB, false));

            LinearLayout btns = Ui.row(this);
            btns.setPadding(0, Ui.dp(this, 8), 0, 0);
            TextView restore = Ui.text(this, "RESTORE", 13, MainActivity.BLUE, true);
            restore.setPadding(0, Ui.dp(this, 6), Ui.dp(this, 24), Ui.dp(this, 6));
            restore.setOnClickListener(v -> new Thread(() -> {
                String msg;
                try {
                    File target = new File(e[1]);
                    if (target.exists()) msg = "A file already exists at that path";
                    else { Fm.restore(this, e[1], e[2]); msg = "Restored"; }
                } catch (Exception ex) {
                    msg = "Restore failed";
                }
                final String m = msg;
                runOnUiThread(() -> { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); refresh(); });
            }).start());
            TextView del = Ui.text(this, "DELETE", 13, 0xFFD93025, true);
            del.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
            del.setOnClickListener(v -> new Thread(() -> {
                Fm.purge(this, e[1], e[2]);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Deleted permanently", Toast.LENGTH_SHORT).show();
                    refresh();
                });
            }).start());
            btns.addView(restore);
            btns.addView(del);
            card.addView(btns);

            box.addView(card);
        }
    }

    private void confirmEmpty() {
        Dlg.b(this)
                .setTitle("Empty Recycle Bin?")
                .setMessage("All items will be permanently deleted.")
                .setPositiveButton("Empty", (d, w) -> { Fm.emptyBin(this); refresh(); })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
