package com.nijatva.file;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.io.InputStream;
import java.util.Locale;

/** One built-in player for images, videos and music. */
public class MediaViewerActivity extends Activity {

    private String[] paths = new String[0];
    private int index = 0;

    private FrameLayout stage;
    private LinearLayout header, controls;
    private TextView title, counter, position, playPause;
    private SeekBar seek;
    private ImageView image;
    private VideoView video;
    private MediaPlayer player;
    private GestureDetector gestures;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        public void run() {
            if (player != null) {
                try {
                    int pos = player.getCurrentPosition();
                    int dur = player.getDuration();
                    if (dur > 0) {
                        seek.setProgress(pos * 100 / dur);
                        position.setText(fmt(pos) + "  /  " + fmt(dur));
                    }
                } catch (Exception ignored) {
                }
                handler.postDelayed(this, 500);
            }
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Theme.apply(this);
        Intent i = getIntent();
        if (i.hasExtra("paths")) paths = i.getStringArrayExtra("paths");
        index = i.getIntExtra("index", 0);
        if (paths == null || paths.length == 0) { finish(); return; }
        if (index < 0 || index >= paths.length) index = 0;
        buildUi();
        show();
    }

    private void buildUi() {
        LinearLayout root = Ui.col(this);
        root.setBackgroundColor(0xFF0D0D0D);

        header = Ui.row(this);
        header.setBackgroundColor(0xFF1A1A1A);
        header.setPadding(Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16));
        TextView back = Ui.text(this, "\u2190", 22, Color.WHITE, false);
        back.setPadding(0, 0, Ui.dp(this, 14), 0);
        back.setOnClickListener(v -> finish());
        LinearLayout titles = Ui.col(this);
        titles.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        title = Ui.text(this, "", 15, Color.WHITE, true);
        title.setSingleLine(true);
        counter = Ui.text(this, "", 11, 0xFF9E9E9E, false);
        titles.addView(title);
        titles.addView(counter);
        header.addView(back);
        header.addView(titles);
        root.addView(header);

        stage = new FrameLayout(this);
        stage.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(stage);

        seek = new SeekBar(this);
        seek.setMax(100);
        seek.setPadding(Ui.dp(this, 14), 0, Ui.dp(this, 14), 0);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                if (fromUser && player != null) {
                    try { player.seekTo(p * player.getDuration() / 100); } catch (Exception ignored) {}
                }
            }
            public void onStartTrackingTouch(SeekBar sb) {}
            public void onStopTrackingTouch(SeekBar sb) {}
        });
        root.addView(seek);

        position = Ui.text(this, "", 12, 0xFF9E9E9E, false);
        position.setGravity(Gravity.CENTER);
        position.setPadding(0, Ui.dp(this, 4), 0, 0);
        root.addView(position);

        controls = Ui.row(this);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 22));
        controls.addView(ctl("\u23EE", v -> step(-1)));
        playPause = ctl("\u25B6", v -> togglePlay());
        controls.addView(playPause);
        controls.addView(ctl("\u23ED", v -> step(1)));
        root.addView(controls);

        gestures = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            public boolean onDown(MotionEvent e) { return true; }
            public boolean onSingleTapConfirmed(MotionEvent e) { toggleChrome(); return true; }
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                if (e1 == null || e2 == null) return false;
                float dx = e2.getX() - e1.getX();
                if (Math.abs(dx) > 120 && Math.abs(vx) > 120) {
                    step(dx < 0 ? 1 : -1);
                    return true;
                }
                return false;
            }
        });
        stage.setOnTouchListener((v, e) -> gestures.onTouchEvent(e));

        setContentView(root);
    }

    private TextView ctl(String glyph, View.OnClickListener l) {
        TextView t = Ui.text(this, glyph, 26, Color.WHITE, false);
        t.setGravity(Gravity.CENTER);
        t.setPadding(Ui.dp(this, 28), Ui.dp(this, 6), Ui.dp(this, 28), Ui.dp(this, 6));
        t.setOnClickListener(l);
        return t;
    }

    private void toggleChrome() {
        boolean vis = header.getVisibility() == View.VISIBLE;
        int v = vis ? View.GONE : View.VISIBLE;
        header.setVisibility(v);
        controls.setVisibility(v);
        boolean audio = paths.length > index && Fm.catOf(new File(paths[index]).getName()) == Fm.CAT_AUDIO;
        seek.setVisibility(audio ? v : View.GONE);
        position.setVisibility(audio ? v : View.GONE);
    }

    private void show() {
        try {
            releasePlayers();
            stage.removeAllViews();
            final String path = paths[index];
            File f = new File(path);
            title.setText(f.getName());
            counter.setText((index + 1) + " / " + paths.length);
            int cat = Fm.catOf(f.getName());

            if (cat == Fm.CAT_VIDEO) {
                hideAudioUi();
                playPause.setVisibility(View.GONE);
                video = new VideoView(this);
                video.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                MediaController mc = new MediaController(this);
                video.setMediaController(mc);
                mc.setAnchorView(video);
                stage.addView(video);
                Uri u = Fm.mediaUri(this, path);
                try {
                    if (u != null) video.setVideoURI(u); else video.setVideoPath(path);
                    video.setOnErrorListener((mp, w, e) -> { toast("Cannot play this video"); return true; });
                    video.setOnPreparedListener(mp -> video.start());
                    video.start();
                } catch (Exception e) {
                    toast("Cannot play this video");
                }
            } else if (cat == Fm.CAT_AUDIO) {
                showAudioUi();
                playPause.setVisibility(View.VISIBLE);
                ImageView art = new ImageView(this);
                art.setImageResource(R.drawable.ic_cat_audio);
                art.setScaleType(ImageView.ScaleType.FIT_CENTER);
                art.setBackground(Ui.round(0xFF1E1E1E, 28, this, 0, 0));
                int pad = Ui.dp(this, 56);
                art.setPadding(pad, pad, pad, pad);
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Ui.dp(this, 220), Ui.dp(this, 220));
                lp.gravity = Gravity.CENTER;
                stage.addView(art, lp);
                try {
                    player = new MediaPlayer();
                    Uri u = Fm.mediaUri(this, path);
                    if (u != null) player.setDataSource(this, u); else player.setDataSource(path);
                    player.prepare();
                    player.start();
                    playPause.setText("\u23F8");
                    handler.post(ticker);
                } catch (Exception e) {
                    toast("Cannot play this audio");
                }
            } else {
                hideAudioUi();
                playPause.setVisibility(View.GONE);
                image = new ImageView(this);
                image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                image.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                stage.addView(image);
                new Thread(() -> {
                    final Bitmap bm = decode(path);
                    runOnUiThread(() -> {
                        if (bm != null && image != null) image.setImageBitmap(bm);
                        else toast("Cannot open this image");
                    });
                }).start();
            }
        } catch (Exception e) {
            toast("Cannot open this file");
        }
    }

    private void showAudioUi() {
        seek.setVisibility(View.VISIBLE);
        position.setVisibility(View.VISIBLE);
    }

    private void hideAudioUi() {
        seek.setVisibility(View.GONE);
        position.setVisibility(View.GONE);
    }

    private Bitmap decode(String path) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            InputStream in1 = Fm.openMedia(this, path);
            BitmapFactory.decodeStream(in1, null, o);
            in1.close();
            int req = 1400, scale = 1;
            while (o.outWidth / scale > req || o.outHeight / scale > req) scale *= 2;
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            InputStream in2 = Fm.openMedia(this, path);
            Bitmap bm = BitmapFactory.decodeStream(in2, null, o2);
            in2.close();
            return bm;
        } catch (Exception e) {
            return null;
        }
    }

    private void togglePlay() {
        if (player == null) {
            if (video != null) {
                if (video.isPlaying()) video.pause(); else video.start();
            }
            return;
        }
        if (player.isPlaying()) {
            player.pause();
            playPause.setText("\u25B6");
        } else {
            player.start();
            playPause.setText("\u23F8");
            handler.post(ticker);
        }
    }

    private void step(int d) {
        if (paths.length == 0) return;
        index = (index + d + paths.length) % paths.length;
        show();
    }

    private void releasePlayers() {
        handler.removeCallbacks(ticker);
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) {}
            try { player.release(); } catch (Exception ignored) {}
            player = null;
        }
        if (video != null) {
            try { video.stopPlayback(); } catch (Exception ignored) {}
            video = null;
        }
        image = null;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null && player.isPlaying()) {
            player.pause();
            if (playPause != null) playPause.setText("\u25B6");
        }
        if (video != null && video.isPlaying()) video.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayers();
    }

    private String fmt(int ms) {
        int s = ms / 1000;
        return String.format(Locale.US, "%d:%02d", s / 60, s % 60);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
