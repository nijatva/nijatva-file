package com.nijatva.file;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.security.MessageDigest;

/** Safe folder: a PIN-gated directory under FileManager/.Safe */
public final class Vault {

    private Vault() {}

    public static File dir(Context c) {
        File d = new File(Environment.getExternalStorageDirectory(), "FileManager/.Safe");
        try {
            if (!d.exists()) d.mkdirs();
            File probe = new File(d, ".w");
            boolean ok = probe.createNewFile() || probe.exists();
            if (probe.exists()) probe.delete();
            if (ok) return d;
        } catch (Exception ignored) { }
        File alt = new File(c.getExternalFilesDir(null), "Safe");
        if (!alt.exists()) alt.mkdirs();
        return alt;
    }

    /** PINs are stored hashed (SHA-256), never in plain text. */
    static String hash(String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(pin.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte x : d) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) {
            return pin;
        }
    }

    private static boolean matches(String stored, String entered) {
        if (stored == null) return false;
        if (stored.equals(hash(entered))) return true;
        // migrate older plain-text PINs
        return stored.length() != 64 && stored.equals(entered);
    }

    private static EditText pinField(Activity a, String hint) {
        EditText in = new EditText(a);
        in.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        in.setHint(hint);
        return in;
    }

    public static void open(final Activity a) {
        final String existing = Store.pin(a);

        if (existing == null) {
            final EditText in = pinField(a, "4-digit PIN");
            Dlg.b(a)
                    .setTitle("Set a PIN for Safe folder")
                    .setView(in)
                    .setPositiveButton("Save", (d, w) -> {
                        String v = in.getText().toString().trim();
                        if (v.length() < 4) { toast(a, "PIN must be 4+ digits"); return; }
                        Store.setPin(a, hash(v));
                        toast(a, "PIN saved");
                        launch(a);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        final EditText in = pinField(a, "Enter PIN");
        Dlg.b(a)
                .setTitle("Enter PIN")
                .setView(in)
                .setPositiveButton("Unlock", (d, w) -> {
                    if (matches(existing, in.getText().toString().trim())) launch(a);
                    else toast(a, "Wrong PIN");
                })
                .setNeutralButton("Change PIN", (d, w) -> changePin(a, existing))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void changePin(final Activity a, final String existing) {
        final EditText current = pinField(a, "Current PIN");
        Dlg.b(a)
                .setTitle("Current PIN")
                .setView(current)
                .setPositiveButton("Next", (d, w) -> {
                    if (!matches(existing, current.getText().toString().trim())) { toast(a, "Wrong PIN"); return; }
                    final EditText fresh = pinField(a, "New 4-digit PIN");
                    Dlg.b(a)
                            .setTitle("New PIN")
                            .setView(fresh)
                            .setPositiveButton("Save", (d2, w2) -> {
                                String v = fresh.getText().toString().trim();
                                if (v.length() < 4) { toast(a, "PIN must be 4+ digits"); return; }
                                Store.setPin(a, hash(v));
                                toast(a, "PIN updated");
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void launch(Activity a) {
        Intent i = new Intent(a, BrowserActivity.class);
        i.putExtra("path", dir(a).getAbsolutePath());
        a.startActivity(i);
    }

    private static void toast(Activity a, String s) {
        Toast.makeText(a, s, Toast.LENGTH_SHORT).show();
    }
}
