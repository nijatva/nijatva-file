package com.nijatva.file;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/** MIUI-style multi-coloured segmented storage bar. */
public class SegmentedBar extends View {

    private int[] colors = new int[0];
    private float[] values = new float[0];
    private int track = 0xFF2A2A2A;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path clip = new Path();

    public SegmentedBar(Context c) {
        super(c);
    }

    public SegmentedBar(Context c, AttributeSet a) {
        super(c, a);
    }

    public void setSegments(int[] colors, float[] values) {
        this.colors = colors;
        this.values = values;
        invalidate();
    }

    public void setTrackColor(int color) {
        this.track = color;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;
        float r = h / 2f;
        rect.set(0, 0, w, h);
        clip.reset();
        clip.addRoundRect(rect, r, r, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clip);

        paint.setColor(track);
        canvas.drawRect(0, 0, w, h, paint);

        float total = 0;
        for (float v : values) total += Math.max(0, v);
        if (total > 0) {
            float x = 0;
            for (int i = 0; i < values.length && i < colors.length; i++) {
                float seg = (Math.max(0, values[i]) / total) * w;
                if (seg <= 0) continue;
                paint.setColor(colors[i]);
                canvas.drawRect(x, 0, x + seg + 0.5f, h, paint);
                x += seg;
            }
        }
        canvas.restore();
    }
}
