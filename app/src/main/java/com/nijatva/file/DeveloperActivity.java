package com.nijatva.file;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** About the developer. */
public class DeveloperActivity extends Activity {

    private static final String GITHUB = "https://github.com/vasudev-work";
    private static final String EMAIL = "vasudev.work@rediffmail.com";

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
        header.addView(Ui.text(this, "Developer", 22, MainActivity.TEXT, true));
        root.addView(header);

        ScrollView sv = new ScrollView(this);
        LinearLayout body = Ui.col(this);
        body.setPadding(Ui.dp(this, 14), 0, Ui.dp(this, 14), Ui.dp(this, 28));
        sv.addView(body);

        TextView label = Ui.text(this, "Developer", 13, MainActivity.BLUE, true);
        label.setPadding(Ui.dp(this, 4), 0, 0, Ui.dp(this, 8));
        body.addView(label);

        TextView about = Ui.text(this, "About the developer", 15, MainActivity.TEXT, false);
        about.setPadding(Ui.dp(this, 4), 0, 0, Ui.dp(this, 10));
        body.addView(about);

        LinearLayout card = Ui.col(this);
        card.setBackground(Ui.round(MainActivity.CARD, 20, this, 0, 0));
        card.setPadding(Ui.dp(this, 18), Ui.dp(this, 22), Ui.dp(this, 18), Ui.dp(this, 22));
        card.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView avatar = Ui.text(this, "VD", 26, Color.WHITE, true);
        avatar.setGravity(Gravity.CENTER);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(0xFF2F80ED);
        avatar.setBackground(circle);
        card.addView(avatar, new LinearLayout.LayoutParams(Ui.dp(this, 84), Ui.dp(this, 84)));

        TextView name = Ui.text(this, "Vasudev Dangaya", 22, MainActivity.TEXT, true);
        name.setGravity(Gravity.CENTER);
        name.setPadding(0, Ui.dp(this, 14), 0, 0);
        card.addView(name);

        TextView handle = Ui.text(this, "@vasudev-work", 15, 0xFF22C55E, true);
        handle.setGravity(Gravity.CENTER);
        card.addView(handle);

        TextView bio = Ui.text(this, "I build Android apps with clean UI and real, working features.\nKotlin \u00B7 Android SDK \u00B7 no unnecessary dependencies.", 14, MainActivity.SUB, false);
        bio.setGravity(Gravity.CENTER);
        bio.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 16));
        card.addView(bio);

        LinearLayout stats = Ui.row(this);
        stats.setBackground(Ui.round(Ui.chipBg(this), 14, this, 0, 0));
        stats.setPadding(Ui.dp(this, 10), Ui.dp(this, 16), Ui.dp(this, 10), Ui.dp(this, 16));
        stats.addView(stat("8", "Extensions"));
        stats.addView(stat("34", "Screens"));
        stats.addView(stat("180k", "Filter rules"));
        card.addView(stats);

        TextView maint = Ui.text(this, "Maintains", 14, MainActivity.TEXT, true);
        maint.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 8));
        LinearLayout mrow = Ui.row(this);
        mrow.addView(chip("N"));
        mrow.addView(chip("A"));
        mrow.addView(chip("Q"));
        mrow.addView(chip("+22"));
        card.addView(maint);
        card.addView(mrow);

        card.addView(infoRow("Location", "India"));
        card.addView(infoRow("Website", "github.com/vasudev-work"));
        card.addView(infoRow("Email", EMAIL));

        TextView btn = Ui.text(this, "View GitHub Profile", 16, 0xFF111111, true);
        btn.setGravity(Gravity.CENTER);
        btn.setBackground(Ui.round(0xFFFFFFFF, 28, this, 0, 0));
        btn.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 16));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        blp.setMargins(0, Ui.dp(this, 18), 0, 0);
        btn.setLayoutParams(blp);
        btn.setOnClickListener(v -> {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB))); }
            catch (Exception ignored) { }
        });
        card.addView(btn);

        body.addView(card);

        TextView credit = Ui.text(this, "Designed & developed by Vasudev Dangaya", 13, MainActivity.SUB, false);
        credit.setGravity(Gravity.CENTER);
        credit.setPadding(0, Ui.dp(this, 22), 0, Ui.dp(this, 14));
        body.addView(credit);

        TextView india = Ui.text(this, "MAKE IN INDIA", 15, Color.WHITE, true);
        india.setGravity(Gravity.CENTER);
        GradientDrawable grad = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFFF59E0B, 0xFFF97316, 0xFF16A34A});
        grad.setCornerRadius(Ui.dp(this, 26));
        india.setBackground(grad);
        india.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 14));
        body.addView(india);

        root.addView(sv);
        setContentView(root);
    }

    private LinearLayout stat(String value, String label) {
        LinearLayout c = Ui.col(this);
        c.setGravity(Gravity.CENTER);
        c.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView v = Ui.text(this, value, 20, MainActivity.TEXT, true);
        v.setGravity(Gravity.CENTER);
        TextView l = Ui.text(this, label, 12, MainActivity.SUB, false);
        l.setGravity(Gravity.CENTER);
        c.addView(v);
        c.addView(l);
        return c;
    }

    private TextView chip(String s) {
        TextView t = Ui.text(this, s, 15, Color.WHITE, true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(Ui.round(0xFF2F80ED, 10, this, 0, 0));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 42));
        lp.setMargins(0, 0, Ui.dp(this, 8), 0);
        t.setLayoutParams(lp);
        return t;
    }

    private LinearLayout infoRow(String label, String value) {
        LinearLayout r = Ui.row(this);
        r.setPadding(0, Ui.dp(this, 14), 0, 0);
        TextView l = Ui.text(this, label, 14, MainActivity.SUB, false);
        l.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        r.addView(l);
        r.addView(Ui.text(this, value, 14, MainActivity.TEXT, true));
        return r;
    }
}
