package com.nijatva.file;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Appearance: theme + accent colour. */
public class AppearanceActivity extends Activity {

    private static final String[] THEMES = {"System", "Dark", "Light", "Black"};
    private static final String[] ACCENTS = {"Green", "Saffron", "Indigo", "Auto"};

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Theme.apply(this);
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = Ui.col(this);
        root.setBackgroundColor(MainActivity.BG);

        LinearLayout header = Ui.row(this);
        header.setPadding(Ui.dp(this, 12), Ui.dp(this, 16), Ui.dp(this, 12), Ui.dp(this, 16));
        TextView back = Ui.text(this, "\u2190", 24, MainActivity.TEXT, false);
        back.setPadding(0, 0, Ui.dp(this, 14), 0);
        back.setOnClickListener(v -> finish());
        header.addView(back);
        header.addView(Ui.text(this, "Appearance", 22, MainActivity.TEXT, true));
        root.addView(header);

        ScrollView sv = new ScrollView(this);
        LinearLayout body = Ui.col(this);
        body.setPadding(Ui.dp(this, 14), Ui.dp(this, 4), Ui.dp(this, 14), Ui.dp(this, 28));
        sv.addView(body);

        TextView label = Ui.text(this, "APPEARANCE", 13, MainActivity.SUB, true);
        label.setPadding(Ui.dp(this, 4), 0, 0, Ui.dp(this, 10));
        body.addView(label);

        LinearLayout card = Ui.col(this);
        card.setBackground(Ui.round(MainActivity.CARD, 18, this, 0, 0));
        card.setPadding(Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18));

        card.addView(Ui.text(this, "Theme", 20, MainActivity.TEXT, false));
        card.addView(chips(THEMES, Store.themeMode(this), v -> { Store.setThemeMode(this, v); recreate(); }));
        card.addView(divider());
        card.addView(Ui.text(this, "Accent colour", 20, MainActivity.TEXT, false));
        card.addView(chips(ACCENTS, Store.accentMode(this), v -> { Store.setAccentMode(this, v); recreate(); }));

        body.addView(card);
        root.addView(sv);
        setContentView(root);
    }

    interface Pick { void on(int index); }

    private android.view.View chips(String[] labels, int selected, final Pick cb) {
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = Ui.row(this);
        row.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 6));
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            boolean sel = i == selected;
            int accent = Theme.accentColor(this);
            TextView chip = Ui.text(this, labels[i], 16, sel ? 0xFF111111 : Ui.onChip(this), true);
            chip.setTypeface(Typeface.DEFAULT_BOLD);
            chip.setSingleLine(true);
            chip.setMaxLines(1);
            chip.setGravity(Gravity.CENTER);
            chip.setMinWidth(Ui.dp(this, 74));
            chip.setPadding(Ui.dp(this, 18), Ui.dp(this, 12), Ui.dp(this, 18), Ui.dp(this, 12));
            chip.setBackground(Ui.round(sel ? accent : Ui.chipBg(this), 26, this, 0, 0));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, Ui.dp(this, 8), 0);
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> cb.on(idx));
            row.addView(chip);
        }
        hs.addView(row);
        return hs;
    }

    private android.view.View divider() {
        android.view.View v = new android.view.View(this);
        v.setBackgroundColor(Ui.dividerColor(this));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 1));
        lp.setMargins(0, Ui.dp(this, 18), 0, Ui.dp(this, 18));
        v.setLayoutParams(lp);
        return v;
    }
}
