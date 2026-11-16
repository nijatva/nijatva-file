package com.nijatva.file;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

/** Read and edit text files (Fossify-style "Read text"). */
public class TextEditorActivity extends Activity {

    private static final long MAX_BYTES = 2 * 1024 * 1024;

    private File file;
    private EditText editor;
    private TextView status;
    private boolean readOnly = false;
    private TextToSpeech tts;
    private boolean speaking = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Theme.apply(this);
        String path = getIntent().getStringExtra("path");
        if (path == null) { finish(); return; }
        file = new File(path);
        buildUi();
        load();
        initTts();
    }

    private void initTts() {
        try {
            tts = new TextToSpeech(this, status -> { });
        } catch (Exception ignored) { }
    }

    private void toggleSpeak() {
        if (tts == null) { toast("Text-to-speech unavailable"); return; }
        if (speaking) {
            try { tts.stop(); } catch (Exception ignored) { }
            speaking = false;
            toast("Stopped");
            return;
        }
        String text = editor.getText().toString();
        if (text.trim().isEmpty()) { toast("Nothing to read"); return; }
        if (text.length() > 4000) text = text.substring(0, 4000);
        try {
            tts.setLanguage(Locale.getDefault());
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nijatva");
            speaking = true;
            toast("Reading aloud\u2026");
        } catch (Exception e) { toast("Could not start TTS"); }
    }

    @Override
    protected void onDestroy() {
        try { if (tts != null) { tts.stop(); tts.shutdown(); } } catch (Exception ignored) { }
        super.onDestroy();
    }


    private void buildUi() {
        LinearLayout root = Ui.col(this);
        root.setBackgroundColor(MainActivity.BG);

        LinearLayout header = Ui.row(this);
        header.setBackgroundColor(MainActivity.CARD);
        header.setPadding(Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14));
        TextView back = Ui.text(this, "\u2190", 22, MainActivity.TEXT, false);
        back.setPadding(0, 0, Ui.dp(this, 14), 0);
        back.setOnClickListener(v -> finish());
        TextView title = Ui.text(this, file.getName(), 16, MainActivity.TEXT, true);
        title.setSingleLine(true);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView speak = Ui.text(this, "\uD83D\uDD0A", 18, MainActivity.TEXT, false);
        speak.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 10), 0);
        speak.setOnClickListener(v -> toggleSpeak());
        TextView save = Ui.text(this, "SAVE", 14, MainActivity.BLUE, true);
        save.setPadding(Ui.dp(this, 12), Ui.dp(this, 6), 0, Ui.dp(this, 6));
        save.setOnClickListener(v -> save());
        header.addView(back);
        header.addView(title);
        header.addView(speak);
        header.addView(save);
        root.addView(header);

        editor = new EditText(this);
        editor.setBackgroundColor(MainActivity.BG);
        editor.setTextColor(MainActivity.TEXT);
        editor.setGravity(Gravity.TOP | Gravity.START);
        editor.setTypeface(Typeface.MONOSPACE);
        editor.setTextSize(14);
        editor.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        editor.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        editor.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { updateStatus(s); }
            public void afterTextChanged(Editable s) {}
        });
        root.addView(editor);

        status = Ui.text(this, "", 11, MainActivity.SUB, false);
        status.setPadding(Ui.dp(this, 14), Ui.dp(this, 6), Ui.dp(this, 14), Ui.dp(this, 8));
        root.addView(status);

        setContentView(root);
    }

    private void load() {
        new Thread(() -> {
            String text = null;
            String err = null;
            try {
                if (file.length() > MAX_BYTES) {
                    readOnly = true;
                    err = "File is large \u2014 opened read-only";
                }
                InputStream in = Fm.openMedia(this, file.getAbsolutePath());
                BufferedReader r = new BufferedReader(new InputStreamReader(in));
                StringBuilder sb = new StringBuilder();
                String line;
                int lines = 0;
                while ((line = r.readLine()) != null && lines < 20000) {
                    sb.append(line).append('\n');
                    lines++;
                }
                r.close();
                text = sb.toString();
            } catch (Exception e) {
                err = "Cannot read this file";
            }
            final String t = text, e = err;
            runOnUiThread(() -> {
                if (t != null) {
                    editor.setText(t);
                    editor.setSelection(0);
                    updateStatus(t);
                }
                if (e != null) toast(e);
            });
        }).start();
    }

    private void updateStatus(CharSequence s) {
        if (status == null) return;
        int lines = 1;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == '\n') lines++;
        status.setText(s.length() + " chars  \u2022  " + lines + " lines" + (readOnly ? "  \u2022  read-only" : ""));
    }

    private void save() {
        if (readOnly) { toast("Read-only"); return; }
        final String text = editor.getText().toString();
        new Thread(() -> {
            String msg;
            try {
                FileOutputStream fos = new FileOutputStream(file, false);
                fos.write(text.getBytes("UTF-8"));
                fos.close();
                msg = "Saved";
            } catch (Exception e) {
                msg = "Could not save (no write access)";
            }
            final String m = msg;
            new Handler(Looper.getMainLooper()).post(() -> toast(m));
        }).start();
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
