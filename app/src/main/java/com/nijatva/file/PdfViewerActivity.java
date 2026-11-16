package com.nijatva.file;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;

/** Lightweight PDF viewer using the platform PdfRenderer (no extra library). */
public class PdfViewerActivity extends Activity {

    private LinearLayout pages;
    private TextView status;
    private int totalPages = 0;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Theme.apply(this);
        final String path = getIntent().getStringExtra("path");
        if (path == null) { finish(); return; }

        LinearLayout root = Ui.col(this);
        root.setBackgroundColor(MainActivity.BG);

        LinearLayout header = Ui.row(this);
        header.setBackgroundColor(MainActivity.CARD);
        header.setPadding(Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14));
        TextView back = Ui.text(this, "\u2190", 22, MainActivity.TEXT, false);
        back.setPadding(0, 0, Ui.dp(this, 14), 0);
        back.setOnClickListener(v -> finish());
        TextView title = Ui.text(this, new File(path).getName(), 16, MainActivity.TEXT, true);
        title.setSingleLine(true);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(back);
        header.addView(title);
        root.addView(header);

        status = Ui.text(this, "Opening\u2026", 13, MainActivity.SUB, false);
        status.setPadding(Ui.dp(this, 14), Ui.dp(this, 8), Ui.dp(this, 14), Ui.dp(this, 8));
        root.addView(status);

        ScrollView sv = new ScrollView(this);
        pages = Ui.col(this);
        pages.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 8), Ui.dp(this, 20));
        sv.addView(pages);
        root.addView(sv);

        setContentView(root);
        render(path);
    }

    private void render(final String path) {
        new Thread(() -> {
            ParcelFileDescriptor pfd = null;
            PdfRenderer renderer = null;
            int made = 0;
            String error = null;
            try {
                pfd = ParcelFileDescriptor.open(new File(path), ParcelFileDescriptor.MODE_READ_ONLY);
                renderer = new PdfRenderer(pfd);
                totalPages = renderer.getPageCount();
                int count = Math.min(totalPages, 40);
                int targetW = getResources().getDisplayMetrics().widthPixels - Ui.dp(this, 20);
                for (int i = 0; i < count; i++) {
                    PdfRenderer.Page page = renderer.openPage(i);
                    float scale = targetW / (float) page.getWidth();
                    int h = Math.max(1, (int) (page.getHeight() * scale));
                    Bitmap bm = Bitmap.createBitmap(targetW, h, Bitmap.Config.ARGB_8888);
                    bm.eraseColor(Color.WHITE);
                    page.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    page.close();
                    final Bitmap out = bm;
                    final int index = i + 1;
                    runOnUiThread(() -> {
                        ImageView iv = new ImageView(this);
                        iv.setImageBitmap(out);
                        iv.setAdjustViewBounds(true);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        lp.setMargins(0, 0, 0, Ui.dp(this, 10));
                        iv.setLayoutParams(lp);
                        pages.addView(iv);
                        status.setText("Page " + index + " of " + Math.min(totalPages, 40));
                    });
                    made++;
                }
            } catch (Exception e) {
                error = "Cannot open this PDF" + (e.getMessage() != null ? " (" + e.getMessage() + ")" : "");
            } finally {
                try { if (renderer != null) renderer.close(); } catch (Exception ignored) { }
                try { if (pfd != null) pfd.close(); } catch (Exception ignored) { }
            }
            final String err = error;
            final int total = made;
            runOnUiThread(() -> {
                if (err != null) {
                    status.setText(err);
                    status.setGravity(Gravity.CENTER);
                } else {
                    status.setText(total + " page(s)");
                }
            });
        }).start();
    }

}
