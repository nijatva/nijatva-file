package com.nijatva.file;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small programmatic-UI helpers shared by all screens. Honours the Vasu interface scale. */
public final class Ui {

    public static final int HEADER = 0xFF37474F;
    public static final int ACCENT = 0xFF009688;
    public static final int BG = 0xFFF1F3F5;
    public static final int CARD = 0xFFFFFFFF;
    public static final int TEXT = 0xFF212121;
    public static final int SUBTEXT = 0xFF757575;

    private static float scale = 1f;

    private Ui() {}

    public static void setScale(float s) {
        scale = (s <= 0f) ? 1f : s;
    }

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v * scale, c.getResources().getDisplayMetrics()));
    }

    public static GradientDrawable round(int fill, int radiusDp, Context c, int strokeW, int strokeColor) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.RECTANGLE);
        g.setColor(fill);
        g.setCornerRadius(dp(c, radiusDp));
        if (strokeW > 0) g.setStroke(dp(c, strokeW), strokeColor);
        return g;
    }

    /** Theme-aware surfaces so nothing goes dark-on-dark in Light mode. */
    public static int chipBg(Context c) { return Theme.isDark(c) ? 0xFF232323 : 0xFFE6E8EB; }
    public static int selectedBg(Context c) { return Theme.isDark(c) ? 0xFF2A3A4A : 0xFFD8E6F7; }
    public static int dividerColor(Context c) { return Theme.isDark(c) ? 0xFF2A2A2A : 0xFFE0E0E0; }
    public static int onChip(Context c) { return Theme.isDark(c) ? 0xFFFFFFFF : 0xFF111111; }

    public static LinearLayout card(Context c) {
        LinearLayout l = col(c);
        l.setBackground(round(CARD, 12, c, 0, 0));
        return l;
    }

    public static LinearLayout col(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static TextView text(Context c, String s, int sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp * scale);
        t.setTextColor(color);
        if (bold) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        return t;
    }

    public static View spacer(Context c, int h) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, h)));
        return v;
    }
}
