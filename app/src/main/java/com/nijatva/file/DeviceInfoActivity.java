package com.nijatva.file;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

/** Device dashboard: storage, RAM, battery and CPU temperature. */
public class DeviceInfoActivity extends Activity {

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
        header.addView(Ui.text(this, "Device info", 22, MainActivity.TEXT, true));
        root.addView(header);

        ScrollView sv = new ScrollView(this);
        LinearLayout body = Ui.col(this);
        body.setPadding(Ui.dp(this, 14), 0, Ui.dp(this, 14), Ui.dp(this, 28));
        sv.addView(body);

        // Storage
        LinearLayout s1 = card();
        long[] st = Fm.storage();
        s1.addView(row("Internal storage", Fm.fmtSize(st[0] - st[1]) + " used"));
        s1.addView(divider());
        s1.addView(row("Total", Fm.fmtSize(st[0])));
        s1.addView(divider());
        s1.addView(row("Free", Fm.fmtSize(st[1])));
        body.addView(section("Storage"));
        body.addView(s1);
        body.addView(Ui.spacer(this, 16));

        // RAM
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        if (am != null) am.getMemoryInfo(mi);
        long totalRam = mi.totalMem;
        long availRam = mi.availMem;
        body.addView(section("Memory (RAM)"));
        LinearLayout s2 = card();
        s2.addView(row("Used", Fm.fmtSize(Math.max(0, totalRam - availRam))));
        s2.addView(divider());
        s2.addView(row("Available", Fm.fmtSize(availRam)));
        s2.addView(divider());
        s2.addView(row("Total", Fm.fmtSize(totalRam)));
        body.addView(s2);
        body.addView(Ui.spacer(this, 16));

        // Battery
        body.addView(section("Battery"));
        LinearLayout s3 = card();
        try {
            Intent bat = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (bat != null) {
                int level = bat.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = bat.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                int pct = scale > 0 ? level * 100 / scale : 0;
                int temp = bat.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
                int volt = bat.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
                int health = bat.getIntExtra(BatteryManager.EXTRA_HEALTH, 0);
                int status = bat.getIntExtra(BatteryManager.EXTRA_STATUS, 0);
                s3.addView(row("Level", pct + "%"));
                s3.addView(divider());
                s3.addView(row("Status", statusText(status)));
                s3.addView(divider());
                s3.addView(row("Temperature", String.format(java.util.Locale.US, "%.1f \u00B0C", temp / 10.0)));
                s3.addView(divider());
                s3.addView(row("Voltage", (volt / 1000.0) + " V"));
                s3.addView(divider());
                s3.addView(row("Health", healthText(health)));
            } else {
                s3.addView(row("Battery", "Unavailable"));
            }
        } catch (Exception e) {
            s3.addView(row("Battery", "Unavailable"));
        }
        body.addView(s3);
        body.addView(Ui.spacer(this, 16));

        // CPU + device
        body.addView(section("System"));
        LinearLayout s4 = card();
        s4.addView(row("CPU temperature", cpuTemp()));
        s4.addView(divider());
        s4.addView(row("Device", Build.MANUFACTURER + " " + Build.MODEL));
        s4.addView(divider());
        s4.addView(row("Android", Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")"));
        body.addView(s4);

        root.addView(sv);
        setContentView(root);
    }

    private String cpuTemp() {
        String[] zones = {"/sys/class/thermal/thermal_zone0/temp", "/sys/class/thermal/thermal_zone1/temp",
                "/sys/class/thermal/thermal_zone2/temp"};
        for (String z : zones) {
            try {
                BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(new File(z))));
                String line = r.readLine();
                r.close();
                if (line != null) {
                    double v = Double.parseDouble(line.trim());
                    if (v > 1000) v /= 1000.0;
                    return String.format(java.util.Locale.US, "%.1f \u00B0C", v);
                }
            } catch (Exception ignored) { }
        }
        return "Not available";
    }

    private String statusText(int s) {
        switch (s) {
            case BatteryManager.BATTERY_STATUS_CHARGING: return "Charging";
            case BatteryManager.BATTERY_STATUS_DISCHARGING: return "Discharging";
            case BatteryManager.BATTERY_STATUS_FULL: return "Full";
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING: return "Not charging";
            default: return "Unknown";
        }
    }

    private String healthText(int h) {
        switch (h) {
            case BatteryManager.BATTERY_HEALTH_GOOD: return "Good";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT: return "Overheat";
            case BatteryManager.BATTERY_HEALTH_DEAD: return "Dead";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: return "Over voltage";
            case BatteryManager.BATTERY_HEALTH_COLD: return "Cold";
            default: return "Unknown";
        }
    }

    private TextView section(String s) {
        TextView t = Ui.text(this, s, 13, MainActivity.BLUE, true);
        t.setPadding(Ui.dp(this, 4), 0, 0, Ui.dp(this, 8));
        return t;
    }

    private LinearLayout card() {
        LinearLayout c = Ui.col(this);
        c.setBackground(Ui.round(MainActivity.CARD, 16, this, 0, 0));
        c.setPadding(0, Ui.dp(this, 2), 0, Ui.dp(this, 2));
        return c;
    }

    private LinearLayout row(String label, String value) {
        LinearLayout r = Ui.row(this);
        r.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));
        TextView l = Ui.text(this, label, 14, MainActivity.SUB, false);
        l.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        r.addView(l);
        r.addView(Ui.text(this, value, 14, MainActivity.TEXT, true));
        return r;
    }

    private android.view.View divider() {
        android.view.View v = new android.view.View(this);
        v.setBackgroundColor(Ui.dividerColor(this));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 1));
        lp.setMargins(Ui.dp(this, 16), 0, Ui.dp(this, 16), 0);
        v.setLayoutParams(lp);
        return v;
    }
}
