package com.nijatva.file;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Set;

/** Starred (favourite) files. */
public class StarredActivity extends Activity {

    private LinearLayout box;
    private TextView count;

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
        header.addView(Ui.text(this, "Starred", 20, MainActivity.TEXT, true));
        root.addView(header);

        count = Ui.text(this, "", 12, MainActivity.SUB, false);
        count.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 8));
        root.addView(count);

        ScrollView sv = new ScrollView(this);
        box = Ui.col(this);
        box.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), Ui.dp(this, 24));
        sv.addView(box);
        root.addView(sv);

        setContentView(root);
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        box.removeAllViews();
        Set<String> paths = Store.starred(this);
        int shown = 0;
        for (final String p : paths) {
            final File f = new File(p);
            if (!f.exists()) continue;
            shown++;
            LinearLayout card = Ui.col(this);
            card.setBackground(Ui.round(MainActivity.CARD, 12, this, 0, 0));
            card.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, Ui.dp(this, 8));
            card.setLayoutParams(lp);

            LinearLayout r = Ui.row(this);
            TextView star = Ui.text(this, "\u2605", 18, 0xFFF9AB00, true);
            star.setPadding(0, 0, Ui.dp(this, 14), 0);
            LinearLayout c = Ui.col(this);
            c.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            c.addView(Ui.text(this, f.getName(), 14, MainActivity.TEXT, true));
            TextView sub = Ui.text(this, (f.isDirectory() ? "Folder" : Fm.fmtSize(f.length())) + "  \u2022  " + f.getParent(), 11, MainActivity.SUB, false);
            c.addView(sub);
            r.addView(star);
            r.addView(c);
            card.addView(r);

            card.setOnClickListener(v -> {
                Intent i = new Intent(this, BrowserActivity.class);
                i.putExtra("path", (f.isDirectory() ? f : f.getParentFile()).getAbsolutePath());
                startActivity(i);
            });
            card.setOnLongClickListener(v -> {
                Dlg.b(this)
                        .setTitle(f.getName())
                        .setItems(new String[]{"Remove from Starred"}, (d, w) -> {
                            Store.star(this, p, false);
                            Toast.makeText(this, "Removed", Toast.LENGTH_SHORT).show();
                            refresh();
                        })
                        .show();
                return true;
            });
            box.addView(card);
        }
        count.setText(shown == 0 ? "No starred items yet.\nLong-press a file and choose Star." : shown + " starred item(s)");
    }
}
