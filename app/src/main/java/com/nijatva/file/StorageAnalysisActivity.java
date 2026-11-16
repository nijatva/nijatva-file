package com.nijatva.file;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Storage analysis: total/used/free + per-category breakdown. */
public class StorageAnalysisActivity extends Activity {

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
        header.addView(Ui.text(this, "Storage Analysis", 18, MainActivity.TEXT, true));
        root.addView(header);

        ScrollView sv = new ScrollView(this);
        LinearLayout body = Ui.col(this);
        body.setPadding(Ui.dp(this, 14), Ui.dp(this, 6), Ui.dp(this, 14), Ui.dp(this, 24));
        sv.addView(body);

        long[] s = Fm.storage();
        long total = s[0], free = s[1], used = total - free;
        int pct = total > 0 ? (int) (used * 100 / total) : 0;

        LinearLayout card = Ui.col(this);
        card.setBackground(Ui.round(MainActivity.CARD, 14, this, 0, 0));
        card.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16));
        card.addView(Ui.text(this, "Internal storage", 16, MainActivity.TEXT, true));
        TextView line = Ui.text(this, Fm.fmtSize(used) + " used  \u2022  " + Fm.fmtSize(free) + " free  \u2022  " + Fm.fmtSize(total) + " total", 13, MainActivity.SUB, false);
        line.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 10));
        card.addView(line);
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(pct);
        card.addView(bar);
        TextView pctv = Ui.text(this, pct + "% used", 13, MainActivity.BLUE, true);
        pctv.setGravity(Gravity.RIGHT);
        pctv.setPadding(0, Ui.dp(this, 6), 0, 0);
        card.addView(pctv);
        body.addView(card);
        body.addView(Ui.spacer(this, 14));

        body.addView(Ui.text(this, "By category", 15, MainActivity.TEXT, true));
        body.addView(Ui.spacer(this, 8));

        final LinearLayout catBox = Ui.col(this);
        body.addView(catBox);

        new Thread(() -> {
            final List<String[]> rows = new ArrayList<>();
            long grand = 0;
            for (int c = 0; c <= Fm.CAT_DOC; c++) {
                List<File> fs = Fm.byCategory(this, c);
                long sz = 0;
                for (File f : fs) sz += f.length();
                grand += sz;
                rows.add(new String[]{Fm.catName(c), fs.size() + " files", Fm.fmtSize(sz)});
            }
            final long grandFinal = grand;
            runOnUiThread(() -> {
                for (String[] r : rows) {
                    LinearLayout row = Ui.row(this);
                    row.setBackground(Ui.round(MainActivity.CARD, 10, this, 0, 0));
                    row.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    lp.setMargins(0, 0, 0, Ui.dp(this, 8));
                    row.setLayoutParams(lp);

                    LinearLayout c1 = Ui.col(this);
                    c1.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                    c1.addView(Ui.text(this, r[0], 14, MainActivity.TEXT, true));
                    c1.addView(Ui.text(this, r[1], 12, MainActivity.SUB, false));
                    row.addView(c1);
                    row.addView(Ui.text(this, r[2], 14, MainActivity.BLUE, true));
                    catBox.addView(row);
                }
                LinearLayout tot = Ui.row(this);
                tot.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
                TextView tl = Ui.text(this, "Media total", 14, MainActivity.TEXT, true);
                tl.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                tot.addView(tl);
                tot.addView(Ui.text(this, Fm.fmtSize(grandFinal), 14, MainActivity.BLUE, true));
                catBox.addView(tot);
            });
        }).start();

        root.addView(sv);
        setContentView(root);
    }
}
