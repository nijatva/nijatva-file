package com.nijatva.file;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** "Default apps" per file type. */
public class MimeTypesActivity extends Activity {

    private static final String[][] SYSTEM = {
            {"Photos", "jpg", "P", "0xFF2F80ED"},
            {"Videos", "mp4", "V", "0xFF8B5CF6"},
            {"Music", "mp3", "M", "0xFF14B8A6"},
    };

    private static final String[][] DOCS = {
            {"TXT", "txt", "T", "0xFF6B7280"},
            {"PDF document", "pdf", "P", "0xFFDC2626"},
            {"Word document", "docx", "W", "0xFF2563EB"},
            {"Excel worksheet", "xlsx", "X", "0xFF16A34A"},
            {"PowerPoint presentation", "pptx", "P", "0xFFEA580C"},
    };

    private LinearLayout body;

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
        header.addView(Ui.text(this, "Default apps", 22, MainActivity.TEXT, true));
        root.addView(header);

        ScrollView sv = new ScrollView(this);
        body = Ui.col(this);
        body.setPadding(Ui.dp(this, 14), 0, Ui.dp(this, 14), Ui.dp(this, 28));
        sv.addView(body);
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
        body.removeAllViews();

        body.addView(section("System"));
        LinearLayout c1 = card();
        for (int i = 0; i < SYSTEM.length; i++) {
            if (i > 0) c1.addView(divider());
            c1.addView(row(SYSTEM[i][0], SYSTEM[i][1], SYSTEM[i][2], SYSTEM[i][3]));
        }
        body.addView(c1);
        body.addView(Ui.spacer(this, 18));

        body.addView(section("Documents"));
        LinearLayout c2 = card();
        for (int i = 0; i < DOCS.length; i++) {
            if (i > 0) c2.addView(divider());
            c2.addView(row(DOCS[i][0], DOCS[i][1], DOCS[i][2], DOCS[i][3]));
        }
        body.addView(c2);
    }

    private TextView section(String s) {
        TextView t = Ui.text(this, s, 13, MainActivity.SUB, true);
        t.setPadding(Ui.dp(this, 4), Ui.dp(this, 4), 0, Ui.dp(this, 8));
        return t;
    }

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

    private LinearLayout row(String title, final String ext, String glyph, String colorHex) {
        LinearLayout r = Ui.row(this);
        r.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));

        int color = (int) Long.parseLong(colorHex.replace("0x", ""), 16);
        TextView ic = Ui.text(this, glyph, 16, 0xFFFFFFFF, true);
        ic.setGravity(Gravity.CENTER);
        ic.setTypeface(ic.getTypeface(), Typeface.BOLD);
        ic.setBackground(Ui.round(color, 9, this, 0, 0));
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(Ui.dp(this, 38), Ui.dp(this, 38));
        ilp.setMargins(0, 0, Ui.dp(this, 16), 0);
        r.addView(ic, ilp);

        LinearLayout c = Ui.col(this);
        c.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        c.addView(Ui.text(this, title, 16, MainActivity.TEXT, true));
        String comp = Store.mimeApp(this, ext);
        String sub = comp == null ? "No default" : appLabel(comp);
        TextView s = Ui.text(this, sub, 13, MainActivity.SUB, false);
        s.setPadding(0, Ui.dp(this, 2), 0, 0);
        c.addView(s);
        r.addView(c);
        r.addView(Ui.text(this, "\u203A", 20, MainActivity.SUB, false));
        r.setOnClickListener(v -> pick(ext, title));
        return r;
    }

    private String appLabel(String component) {
        try {
            String[] p = component.split("/");
            return getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(p[0], 0)).toString();
        } catch (Exception e) {
            return "Default apps";
        }
    }

    private void pick(final String ext, String title) {
        String mime = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        if (mime == null) mime = "*/*";
        Intent probe = new Intent(Intent.ACTION_VIEW);
        probe.setDataAndType(Uri.parse("content://x/x." + ext), mime);
        probe.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> apps = pm.queryIntentActivities(probe, 0);

        final List<String> labels = new ArrayList<>();
        final List<String> comps = new ArrayList<>();
        labels.add("No default");
        comps.add("");
        for (ResolveInfo ri : apps) {
            labels.add(ri.loadLabel(pm).toString());
            comps.add(ri.activityInfo.packageName + "/" + ri.activityInfo.name);
        }

        Dlg.b(this)
                .setTitle(title)
                .setItems(labels.toArray(new String[0]), (d, w) -> {
                    String comp = comps.get(w);
                    Store.setMimeApp(this, ext, comp.isEmpty() ? null : comp);
                    refresh();
                })
                .show();
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
