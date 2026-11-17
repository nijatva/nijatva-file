package com.nijatva.file;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.graphics.drawable.Icon;
import android.content.pm.ShortcutManager;
import android.content.pm.ShortcutInfo;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** "All files" browser: list / grid / waterfall, sort, search, share, details and file ops. */
public class BrowserActivity extends Activity {

    private static final int REQ_ADD_FILES = 1001;
    private static List<File> clipboard = new ArrayList<>();
    private static boolean clipCut = false;

    private File currentDir;
    private final List<Item> items = new ArrayList<>();
    private final Set<Item> selected = new HashSet<>();
    private boolean selectionMode = false;

    private int category = -1;
    private boolean appsMode = false, newMode = false, searchMode = false, largeMode = false;
    private boolean searching = false;
    private int viewMode = 0;

    private FrameLayout holder;
    private ListView list;
    private GridView grid;
    private Adapter adapter;
    private TextView title, subtitle, sectionLabel;
    private LinearLayout actionBar;
    private LinearLayout searchRow;
    private EditText searchInput;
    private CheckBox searchAllBox;

    private final HashMap<String, Bitmap> thumbs = new HashMap<>();

    static class Item {
        String name, sub;
        boolean isDir;
        File file;
        Runnable open;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Theme.apply(this);
        viewMode = Store.viewMode(this);
        readExtras();
        buildUi();
        load();
    }

    private void readExtras() {
        Intent i = getIntent();
        if (i.hasExtra("path")) currentDir = new File(i.getStringExtra("path"));
        if (i.hasExtra("category")) category = i.getIntExtra("category", -1);
        appsMode = i.getBooleanExtra("apps", false);
        newMode = i.getBooleanExtra("newfiles", false);
        searchMode = i.getBooleanExtra("search", false);
        largeMode = i.getBooleanExtra("large", false);
        if (currentDir == null) currentDir = Environment.getExternalStorageDirectory();
    }

    private void buildUi() {
        LinearLayout root = Ui.col(this);
        root.setBackgroundColor(MainActivity.BG);

        LinearLayout header = Ui.row(this);
        header.setPadding(Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 8), Ui.dp(this, 10));
        TextView back = Ui.text(this, "\u2190", 24, MainActivity.TEXT, false);
        back.setPadding(0, 0, Ui.dp(this, 14), 0);
        back.setOnClickListener(v -> onBack());

        LinearLayout titles = Ui.col(this);
        titles.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        title = Ui.text(this, "All files", 22, MainActivity.TEXT, true);
        subtitle = Ui.text(this, "", 13, MainActivity.SUB, false);
        titles.addView(title);
        titles.addView(subtitle);

        TextView add = Ui.text(this, "+", 26, MainActivity.TEXT, false);
        add.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 10), 0);
        add.setOnClickListener(this::plusMenu);
        TextView searchBtn = Ui.text(this, "\uD83D\uDD0D", 20, MainActivity.TEXT, false);
        searchBtn.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 8), 0);
        searchBtn.setOnClickListener(v -> toggleSearch());
        TextView menuBtn = Ui.text(this, "\u22EE", 22, MainActivity.TEXT, false);
        menuBtn.setPadding(Ui.dp(this, 8), 0, 0, 0);
        menuBtn.setOnClickListener(this::showMenu);

        header.addView(back);
        header.addView(titles);
        header.addView(add);
        header.addView(searchBtn);
        header.addView(menuBtn);
        root.addView(header);

        if (Build.VERSION.SDK_INT >= 30 && !Fm.hasFullAccess(this)) {
            LinearLayout b = Ui.row(this);
            b.setBackgroundColor(0xFF3A2A00);
            b.setPadding(Ui.dp(this, 16), Ui.dp(this, 10), Ui.dp(this, 16), Ui.dp(this, 10));
            TextView t = Ui.text(this, "All files access needed to browse everything", 12, MainActivity.TEXT, false);
            t.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            TextView g = Ui.text(this, "ALLOW", 12, MainActivity.BLUE, true);
            g.setPadding(Ui.dp(this, 10), 0, 0, 0);
            g.setOnClickListener(v -> Fm.requestAllFiles(this));
            b.addView(t);
            b.addView(g);
            root.addView(b);
        }

        sectionLabel = Ui.text(this, "All files", 13, MainActivity.BLUE, true);
        sectionLabel.setPadding(Ui.dp(this, 16), Ui.dp(this, 2), Ui.dp(this, 16), Ui.dp(this, 8));
        sectionLabel.setOnClickListener(v -> onBack());
        root.addView(sectionLabel);

        searchRow = Ui.col(this);
        searchRow.setBackgroundColor(MainActivity.CARD);
        searchRow.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));
        searchRow.setVisibility(View.GONE);
        LinearLayout inputLine = Ui.row(this);
        searchInput = new EditText(this);
        searchInput.setHint("Search by name or extension\u2026");
        searchInput.setTextColor(MainActivity.TEXT);
        searchInput.setHintTextColor(MainActivity.SUB);
        searchInput.setInputType(InputType.TYPE_CLASS_TEXT);
        searchInput.setSingleLine(true);
        searchInput.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView go = Ui.text(this, "GO", 14, MainActivity.BLUE, true);
        go.setPadding(Ui.dp(this, 12), Ui.dp(this, 6), Ui.dp(this, 6), Ui.dp(this, 6));
        go.setOnClickListener(v -> doSearch());
        inputLine.addView(searchInput);
        inputLine.addView(go);
        searchRow.addView(inputLine);
        searchAllBox = new CheckBox(this);
        searchAllBox.setText("Search all storage");
        searchAllBox.setChecked(Store.searchAll(this));
        searchRow.addView(searchAllBox);
        root.addView(searchRow);

        adapter = new Adapter();

        list = new ListView(this);
        list.setDivider(null);
        list.setDividerHeight(0);
        list.setBackgroundColor(MainActivity.BG);
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, pos, id) -> { if (pos >= 0 && pos < items.size()) onItemClick(items.get(pos)); });
        list.setOnItemLongClickListener((p, v, pos, id) -> { if (pos >= 0 && pos < items.size()) onItemLong(items.get(pos)); return true; });

        grid = new GridView(this);
        grid.setNumColumns(3);
        grid.setBackgroundColor(MainActivity.BG);
        grid.setAdapter(adapter);
        grid.setOnItemClickListener((p, v, pos, id) -> { if (pos >= 0 && pos < items.size()) onItemClick(items.get(pos)); });
        grid.setOnItemLongClickListener((p, v, pos, id) -> { if (pos >= 0 && pos < items.size()) onItemLong(items.get(pos)); return true; });

        holder = new FrameLayout(this);
        holder.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(holder);
        renderView();

        actionBar = Ui.row(this);
        actionBar.setBackgroundColor(MainActivity.CARD);
        actionBar.setPadding(Ui.dp(this, 2), Ui.dp(this, 8), Ui.dp(this, 2), Ui.dp(this, 8));
        actionBar.setVisibility(View.GONE);
        actionBar.addView(action("Copy", v -> doCopy(false)));
        actionBar.addView(action("Move", v -> doCopy(true)));
        actionBar.addView(action("Share", v -> doShare()));
        actionBar.addView(action("ZIP", v -> doCompress()));
        actionBar.addView(action("Delete", v -> doDelete()));
        actionBar.addView(action("Close", v -> exitSelection()));
        root.addView(actionBar);

        setContentView(root);
    }

    private void renderView() {
        if (holder == null) return;
        holder.removeAllViews();
        if (items.isEmpty()) {
            LinearLayout empty = Ui.col(this);
            empty.setGravity(Gravity.CENTER);
            TextView e = Ui.text(this, "\uD83D\uDCC1", 40, MainActivity.SUB, false);
            e.setGravity(Gravity.CENTER);
            TextView m = Ui.text(this, Fm.hasStorageAccess(this) ? "Nothing here" : "No files \u2014 grant file access", 15, MainActivity.SUB, false);
            m.setGravity(Gravity.CENTER);
            m.setPadding(0, Ui.dp(this, 10), 0, 0);
            empty.addView(e);
            empty.addView(m);
            holder.addView(empty);
            return;
        }
        if (viewMode == 0) holder.addView(list);
        else {
            grid.setNumColumns(viewMode == 2 ? 2 : 3);
            holder.addView(grid);
        }
        adapter.notifyDataSetChanged();
    }

    private TextView action(String label, View.OnClickListener l) {
        TextView t = Ui.text(this, label, 11, MainActivity.BLUE, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(Ui.dp(this, 2), Ui.dp(this, 10), Ui.dp(this, 2), Ui.dp(this, 10));
        t.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        t.setOnClickListener(l);
        return t;
    }

    private void toggleSearch() {
        searching = !searching;
        searchRow.setVisibility(searching ? View.VISIBLE : View.GONE);
        if (searching) searchInput.requestFocus();
        else load();
    }

    private void doSearch() {
        final String q = searchInput.getText().toString().trim();
        if (q.isEmpty()) { load(); return; }
        final boolean all = searchAllBox != null && searchAllBox.isChecked();
        Store.setSearchAll(this, all);
        final File root = all ? Environment.getExternalStorageDirectory() : currentDir;
        subtitle.setText("Searching\u2026");
        new Thread(() -> {
            final List<File> hits = Fm.search(root, q, all ? 10 : 8);
            runOnUiThread(() -> {
                items.clear();
                for (File f : hits) items.add(item(f));
                title.setText("Search: " + q);
                subtitle.setText(hits.size() + " results");
                sectionLabel.setText((all ? "All storage" : currentDir.getName()) + " \u00B7 search results");
                renderView();
            });
        }).start();
    }

    private void loadCategoryAsync() {
        subtitle.setText("Loading\u2026");
        final int cat = category;
        new Thread(() -> {
            final List<File> fs = Fm.byCategory(this, cat);
            Fm.sort(fs, Store.sortBy(this), Store.sortAsc(this));
            runOnUiThread(() -> {
                items.clear();
                for (File f : fs) items.add(item(f));
                title.setText(Fm.catName(cat));
                sectionLabel.setText(Fm.catName(cat));
                subtitle.setText(items.isEmpty() ? "No files found \u2014 grant file access" : items.size() + " items in total");
                renderView();
            });
        }).start();
    }

    private void loadRecentAsync() {
        subtitle.setText("Loading\u2026");
        new Thread(() -> {
            long cut = System.currentTimeMillis() - 7L * 24 * 3600 * 1000;
            List<File> all = new ArrayList<>();
            all.addAll(Fm.byCategory(this, Fm.CAT_IMAGE));
            all.addAll(Fm.byCategory(this, Fm.CAT_VIDEO));
            all.addAll(Fm.byCategory(this, Fm.CAT_DOC));
            final List<File> recent = new ArrayList<>();
            for (File f : all) if (f.lastModified() >= cut) recent.add(f);
            runOnUiThread(() -> {
                items.clear();
                for (File f : recent) items.add(item(f));
                title.setText("Recent");
                subtitle.setText(items.size() + " items in total");
                sectionLabel.setText("Recent");
                renderView();
            });
        }).start();
    }

    private void load() {
        items.clear();
        selected.clear();
        selectionMode = false;
        actionBar.setVisibility(View.GONE);

        if (appsMode) { loadApps(); return; }
        if (largeMode) {
            List<File> big = Fm.largeFiles(this, 50L * 1024 * 1024);
            for (File f : big) items.add(item(f));
            title.setText("Large files");
            subtitle.setText(items.size() + " files over 50 MB");
            sectionLabel.setText("Large files");
            renderView();
            return;
        }
        if (newMode) { loadRecentAsync(); return; }
        if (category >= 0) { loadCategoryAsync(); return; }

        if (!currentDir.exists()) currentDir = Environment.getExternalStorageDirectory();
        boolean showHidden = Store.showHidden(this);
        List<File> kids = new ArrayList<>();
        for (File f : Fm.children(currentDir)) {
            if (!showHidden && f.getName().startsWith(".")) continue;
            kids.add(f);
        }
        Fm.sort(kids, Store.sortBy(this), Store.sortAsc(this));
        for (File f : kids) items.add(item(f));
        boolean isRoot = currentDir.getAbsolutePath().equals(Environment.getExternalStorageDirectory().getAbsolutePath());
        title.setText(isRoot ? "All files" : currentDir.getName());
        sectionLabel.setText(breadcrumb());
        subtitle.setText(items.size() + " items in total");
        renderView();
    }

    private String breadcrumb() {
        String root = Environment.getExternalStorageDirectory().getAbsolutePath();
        String p = currentDir.getAbsolutePath();
        if (p.equals(root)) return "All files";
        String rel = p.startsWith(root) ? p.substring(root.length()) : p;
        String[] parts = rel.split("/");
        StringBuilder sb = new StringBuilder("All files");
        for (String s : parts) if (!s.isEmpty()) sb.append(" / ").append(s);
        return sb.toString();
    }

    private void loadApps() {
        items.clear();
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(main, 0);
        for (ResolveInfo ri : apps) {
            Item it = new Item();
            it.name = ri.loadLabel(pm).toString();
            it.sub = ri.activityInfo.packageName;
            it.isDir = false;
            final Intent launch = pm.getLaunchIntentForPackage(ri.activityInfo.packageName);
            it.open = () -> { if (launch != null) startActivity(launch); };
            items.add(it);
        }
        title.setText("Apps");
        subtitle.setText(items.size() + " items in total");
        renderView();
    }

    private Item item(File f) {
        Item it = new Item();
        it.name = f.getName();
        it.isDir = f.isDirectory();
        it.file = f;
        String date = new SimpleDateFormat(Fm.datePattern(Store.dateFormat(this)), Locale.US).format(new Date(f.lastModified()));
        it.sub = (it.isDir ? folderCount(f) + " items" : Fm.fmtSize(this, f.length())) + "  |  " + date;
        it.open = () -> {
            if (f.isDirectory()) { currentDir = f; category = -1; load(); }
            else openFile(f);
        };
        return it;
    }

    private String folderCount(File dir) {
        File[] arr = dir.listFiles();
        return String.valueOf(arr == null ? 0 : arr.length);
    }

    private void onItemClick(Item it) {
        if (selectionMode) { toggleSelect(it); return; }
        if (it.open != null) it.open.run();
    }

    private void onItemLong(Item it) {
        if (it.file == null) return;
        try { holder.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); } catch (Exception ignored) { }
        if (!selectionMode) { selectionMode = true; actionBar.setVisibility(View.VISIBLE); }
        toggleSelect(it);
    }

    private void toggleSelect(Item it) {
        if (selected.contains(it)) selected.remove(it);
        else selected.add(it);
        if (selected.isEmpty()) exitSelection();
        else { subtitle.setText(selected.size() + " selected"); adapter.notifyDataSetChanged(); }
    }

    private void exitSelection() {
        selectionMode = false;
        selected.clear();
        actionBar.setVisibility(View.GONE);
        load();
    }

    private List<File> selectedFiles() {
        List<File> out = new ArrayList<>();
        for (Item it : selected) if (it.file != null) out.add(it.file);
        return out;
    }

    private void plusMenu(View anchor) {
        PopupMenu pm = new PopupMenu(this, anchor);
        pm.getMenu().add("New folder");
        pm.getMenu().add("Add files");
        pm.setOnMenuItemClickListener(mi -> {
            if (mi.getTitle().toString().equals("New folder")) newFolder();
            else addFiles();
            return true;
        });
        pm.show();
    }

    private void showMenu(View anchor) {
        PopupMenu pm = new PopupMenu(this, anchor);
        pm.getMenu().add("Grid view");
        pm.getMenu().add("Waterfall");
        pm.getMenu().add("List view");
        pm.getMenu().add("Edit");
        pm.getMenu().add("Sort\u2026");
        pm.getMenu().add("Search");
        pm.getMenu().add("Select all");
        pm.getMenu().add("Add to Home screen");
        pm.getMenu().add("Batch rename");
        pm.getMenu().add("Convert image");
        pm.getMenu().add("Details");
        pm.getMenu().add("Share");
        pm.getMenu().add("Compress to ZIP");
        pm.getMenu().add("Extract ZIP");
        pm.getMenu().add("Open with");
        pm.getMenu().add("Save as");
        pm.getMenu().add("Paste here" + (clipboard.isEmpty() ? "" : " (" + clipboard.size() + ")"));
        pm.getMenu().add("App Manager");
        pm.getMenu().add("Storage analysis");
        pm.getMenu().add("Default apps");
        pm.getMenu().add("Recycle Bin");
        pm.getMenu().add("Settings");
        pm.setOnMenuItemClickListener(mi -> {
            String t = mi.getTitle().toString();
            if (t.equals("Grid view")) { viewMode = 1; Store.setViewMode(this, 1); renderView(); }
            else if (t.equals("Waterfall")) { viewMode = 2; Store.setViewMode(this, 2); renderView(); }
            else if (t.equals("List view")) { viewMode = 0; Store.setViewMode(this, 0); renderView(); }
            else if (t.equals("Edit")) selectAll();
            else if (t.equals("Sort\u2026")) sortDialog();
            else if (t.equals("Search")) toggleSearch();
            else if (t.equals("Select all")) selectAll();
            else if (t.equals("Add to Home screen")) pinShortcut();
            else if (t.equals("Batch rename")) batchRename();
            else if (t.equals("Convert image")) convertImage();
            else if (t.equals("Details")) showDetails();
            else if (t.equals("Share")) doShare();
            else if (t.equals("Compress to ZIP")) doCompress();
            else if (t.equals("Extract ZIP")) doExtract();
            else if (t.equals("Open with")) openWith();
            else if (t.equals("Save as")) saveAs();
            else if (t.startsWith("Paste here")) doPaste();
            else if (t.equals("App Manager")) startActivity(new Intent(this, AppManagerActivity.class));
            else if (t.equals("Storage analysis")) startActivity(new Intent(this, StorageAnalysisActivity.class));
            else if (t.equals("Default apps")) startActivity(new Intent(this, MimeTypesActivity.class));
            else if (t.equals("Recycle Bin")) startActivity(new Intent(this, RecycleBinActivity.class));
            else if (t.equals("Settings")) startActivity(new Intent(this, FilesSettingsActivity.class));
            return true;
        });
        pm.show();
    }

    private void sortDialog() {
        LinearLayout box = Ui.col(this);
        box.setPadding(Ui.dp(this, 20), Ui.dp(this, 10), Ui.dp(this, 20), Ui.dp(this, 6));
        final RadioGroup rg = new RadioGroup(this);
        rg.setOrientation(RadioGroup.VERTICAL);
        String[] opts = {"Date modified", "Size", "Filename", "Type"};
        for (int i = 0; i < opts.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(opts[i]);
            rb.setId(3000 + i);
            rg.addView(rb);
        }
        rg.check(3000 + Math.max(0, Math.min(3, Store.sortBy(this))));
        box.addView(rg);
        final CheckBox asc = new CheckBox(this);
        asc.setText("Ascending (A \u2192 Z)");
        asc.setChecked(Store.sortAsc(this));
        asc.setPadding(0, Ui.dp(this, 8), 0, 0);
        box.addView(asc);

        Dlg.b(this)
                .setTitle("Sort")
                .setView(box)
                .setPositiveButton("Apply", (d, w) -> {
                    int id = rg.getCheckedRadioButtonId() - 3000;
                    if (id < 0 || id > 3) id = 0;
                    Store.setSortBy(this, id);
                    Store.setSortAsc(this, asc.isChecked());
                    load();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Share selected files. */
    private void doShare() {
        List<File> files = selectedFiles();
        if (files.isEmpty()) { toast("Select files to share"); return; }
        try {
            if (files.size() == 1) {
                File f = files.get(0);
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType(Fm.mimeOf(f.getName()));
                i.putExtra(Intent.EXTRA_STREAM, FmProvider.uriFor(f));
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(i, "Share"));
            } else {
                ArrayList<Uri> uris = new ArrayList<>();
                for (File f : files) uris.add(FmProvider.uriFor(f));
                Intent i = new Intent(Intent.ACTION_SEND_MULTIPLE);
                i.setType("*/*");
                i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(i, "Share"));
            }
        } catch (Exception e) {
            toast("Cannot share");
        }
    }

    /** Pin the current folder to the launcher home screen. */
    private void pinShortcut() {
        if (Build.VERSION.SDK_INT < 26) { toast("Needs Android 8 or newer"); return; }
        try {
            ShortcutManager sm = (ShortcutManager) getSystemService(SHORTCUT_SERVICE);
            if (sm == null || !sm.isRequestPinShortcutSupported()) { toast("Launcher does not support pinning"); return; }
            Intent i = new Intent(this, ShortcutActivity.class);
            i.setAction(Intent.ACTION_VIEW);
            i.putExtra("path", currentDir.getAbsolutePath());
            String label = currentDir.getName().isEmpty() ? "Storage" : currentDir.getName();
            ShortcutInfo si = new ShortcutInfo.Builder(this, "pin" + System.currentTimeMillis())
                    .setShortLabel(label)
                    .setLongLabel(currentDir.getAbsolutePath())
                    .setIcon(Icon.createWithResource(this, R.drawable.ic_folder_blue))
                    .setIntent(i)
                    .build();
            sm.requestPinShortcut(si, null);
            toast("Pin requested \u2014 confirm on your home screen");
        } catch (Exception e) {
            toast("Could not pin");
        }
    }

    /** Batch rename: prefix + running number, keeps extensions. */
    private void batchRename() {
        final List<File> files = selectedFiles();
        if (files.isEmpty()) { toast("Select files to rename"); return; }
        LinearLayout box = Ui.col(this);
        box.setPadding(Ui.dp(this, 20), Ui.dp(this, 8), Ui.dp(this, 20), Ui.dp(this, 4));
        final EditText prefix = new EditText(this);
        prefix.setHint("Prefix");
        prefix.setText("File");
        final EditText start = new EditText(this);
        start.setHint("Start number");
        start.setText("1");
        start.setInputType(InputType.TYPE_CLASS_NUMBER);
        box.addView(prefix);
        box.addView(start);

        Dlg.b(this)
                .setTitle("Batch rename (" + files.size() + ")")
                .setView(box)
                .setPositiveButton("Rename", (d, w) -> {
                    String pre = prefix.getText().toString().trim();
                    if (pre.isEmpty()) pre = "File";
                    int n;
                    try { n = Integer.parseInt(start.getText().toString().trim()); } catch (Exception e) { n = 1; }
                    final String p = pre;
                    final int startNum = n;
                    final List<File> fs = new ArrayList<>(files);
                    new Thread(() -> {
                        Collections.sort(fs, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                        int ok = 0;
                        for (File f : fs) {
                            try {
                                String ext = Fm.ext(f.getName());
                                String name = p + "_" + String.format(Locale.US, "%03d", startNum + ok) + (ext.isEmpty() ? "" : "." + ext);
                                File dest = new File(f.getParentFile(), Fm.uniqueName(f.getParentFile(), name));
                                if (f.renameTo(dest)) ok++;
                            } catch (Exception ignored) { }
                        }
                        final int done = ok;
                        runOnUiThread(() -> { toast(done + " renamed"); exitSelection(); });
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Convert selected images to PNG / JPEG / WEBP. */
    private void convertImage() {
        final List<File> files = selectedFiles();
        if (files.isEmpty()) { toast("Select images to convert"); return; }
        final String[] fmts = {"PNG", "JPEG", "WEBP"};
        Dlg.b(this)
                .setTitle("Convert to")
                .setItems(fmts, (d, w) -> {
                    final String fmt = fmts[w];
                    new Thread(() -> {
                        int ok = 0;
                        for (File f : files) {
                            if (Fm.catOf(f.getName()) != Fm.CAT_IMAGE) continue;
                            try {
                                BitmapFactory.Options bo = new BitmapFactory.Options();
                                bo.inJustDecodeBounds = true;
                                BitmapFactory.decodeFile(f.getAbsolutePath(), bo);
                                int sc = 1;
                                while (bo.outWidth / sc > 2400 || bo.outHeight / sc > 2400) sc *= 2;
                                BitmapFactory.Options bo2 = new BitmapFactory.Options();
                                bo2.inSampleSize = sc;
                                Bitmap bm = BitmapFactory.decodeFile(f.getAbsolutePath(), bo2);
                                if (bm == null) continue;
                                String base = f.getName();
                                int dot = base.lastIndexOf('.');
                                if (dot > 0) base = base.substring(0, dot);
                                String ext = fmt.equals("JPEG") ? "jpg" : fmt.toLowerCase(Locale.US);
                                File out = new File(currentDir, Fm.uniqueName(currentDir, base + "." + ext));
                                FileOutputStream fos = new FileOutputStream(out);
                                Bitmap.CompressFormat cf = fmt.equals("PNG") ? Bitmap.CompressFormat.PNG
                                        : (fmt.equals("WEBP") ? Bitmap.CompressFormat.WEBP : Bitmap.CompressFormat.JPEG);
                                bm.compress(cf, 92, fos);
                                fos.close();
                                ok++;
                            } catch (Exception ignored) { }
                        }
                        final int done = ok;
                        runOnUiThread(() -> { toast(done + " image(s) converted"); exitSelection(); });
                    }).start();
                })
                .show();
    }

    /** File details dialog. */
    private void showDetails() {
        List<File> files = selectedFiles();
        if (files.isEmpty()) { toast("Select a file"); return; }
        final File f = files.get(0);
        new Thread(() -> {
            long size = f.isDirectory() ? Fm.dirSize(f) : f.length();
            StringBuilder sb = new StringBuilder();
            sb.append("Name: ").append(f.getName()).append("\n");
            sb.append("Path: ").append(f.getAbsolutePath()).append("\n");
            sb.append("Type: ").append(f.isDirectory() ? "Folder" : Fm.mimeOf(f.getName())).append("\n");
            sb.append("Size: ").append(Fm.fmtSize(this, size)).append("\n");
            sb.append("Modified: ").append(DateFormat.format("d MMM yyyy, h:mm a", f.lastModified())).append("\n");
            sb.append("Readable: ").append(f.canRead() ? "yes" : "no").append("   Writable: ").append(f.canWrite() ? "yes" : "no");
            final String msg = sb.toString();
            runOnUiThread(() -> Dlg.b(this).setTitle("Details").setMessage(msg).setPositiveButton("OK", null).show());
        }).start();
    }

    private void newFolder() {
        final EditText in = new EditText(this);
        in.setHint("Folder name");
        Dlg.b(this)
                .setTitle("New folder")
                .setView(in)
                .setPositiveButton("Create", (d, w) -> {
                    String nn = in.getText().toString().trim();
                    if (nn.isEmpty()) return;
                    File dest = new File(currentDir, nn);
                    toast(dest.mkdirs() ? "Created" : "Could not create");
                    load();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addFiles() {
        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(i, REQ_ADD_FILES);
        } catch (Exception e) {
            toast("No file picker available");
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_ADD_FILES || res != RESULT_OK || data == null) return;
        final List<Uri> uris = new ArrayList<>();
        if (data.getClipData() != null) {
            for (int i = 0; i < data.getClipData().getItemCount(); i++) uris.add(data.getClipData().getItemAt(i).getUri());
        } else if (data.getData() != null) {
            uris.add(data.getData());
        }
        if (uris.isEmpty()) return;
        new Thread(() -> {
            int ok = 0;
            for (Uri u : uris) {
                try {
                    String name = queryName(u);
                    File dest = new File(currentDir, Fm.uniqueName(currentDir, name));
                    InputStream in = getContentResolver().openInputStream(u);
                    OutputStream out = new FileOutputStream(dest);
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                    in.close();
                    out.close();
                    ok++;
                } catch (Exception ignored) { }
            }
            final int done = ok;
            runOnUiThread(() -> { toast(done + " file(s) added"); load(); });
        }).start();
    }

    private String queryName(Uri u) {
        Cursor c = null;
        try {
            c = getContentResolver().query(u, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (i >= 0) {
                    String n = c.getString(i);
                    if (n != null && !n.isEmpty()) return n;
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        return "file_" + System.currentTimeMillis();
    }

    private void doCopy(boolean cut) {
        clipboard = selectedFiles();
        clipCut = cut;
        Toast.makeText(this, (cut ? "Move" : "Copy") + " ready: " + clipboard.size() + " item(s). Open a folder and Paste.", Toast.LENGTH_LONG).show();
        exitSelection();
    }

    private void doCompress() {
        final List<File> files = selectedFiles();
        if (files.isEmpty()) return;
        String base = files.size() == 1 ? stripExt(files.get(0).getName()) : "archive";
        final File out = new File(currentDir, Fm.uniqueName(currentDir, base + ".zip"));
        Dlg.b(this)
                .setTitle("Compress to ZIP?")
                .setMessage(files.size() + " item(s) \u2192 " + out.getName())
                .setPositiveButton("Compress", (d, w) -> {
                    exitSelection();
                    new Thread(() -> {
                        String msg;
                        try { Fm.zip(files, out); msg = "Created " + out.getName(); }
                        catch (Exception e) { msg = "Compress failed"; }
                        final String m = msg;
                        runOnUiThread(() -> { toast(m); load(); });
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void doExtract() {
        final List<File> files = selectedFiles();
        if (files.size() != 1) { toast("Select one ZIP file to extract"); return; }
        final File z = files.get(0);
        if (!z.getName().toLowerCase(Locale.US).endsWith(".zip")) { toast("Only ZIP extraction is supported"); return; }
        final File dest = new File(currentDir, Fm.uniqueName(currentDir, stripExt(z.getName())));
        Dlg.b(this)
                .setTitle("Extract ZIP?")
                .setMessage(z.getName() + " \u2192 " + dest.getName() + "/")
                .setPositiveButton("Extract", (d, w) -> {
                    exitSelection();
                    new Thread(() -> {
                        String msg;
                        try { int n = Fm.unzip(z, dest); msg = "Extracted " + n + " file(s)"; }
                        catch (Exception e) { msg = "Extract failed"; }
                        final String m = msg;
                        runOnUiThread(() -> { toast(m); load(); });
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String stripExt(String name) {
        int d = name.lastIndexOf('.');
        return d > 0 ? name.substring(0, d) : name;
    }

    private void doPaste() {
        if (clipboard.isEmpty()) { toast("Clipboard is empty"); return; }
        final List<File> srcs = new ArrayList<>(clipboard);
        final boolean cut = clipCut;
        final File dir = currentDir;
        toast("Pasting\u2026");
        new Thread(() -> {
            int ok = 0;
            for (File src : srcs) {
                try {
                    String name = Fm.uniqueName(dir, src.getName());
                    File dest = new File(dir, name);
                    if (cut) Fm.moveFile(src, dest);
                    else Fm.copyFile(src, dest);
                    ok++;
                } catch (Exception ignored) { }
            }
            if (cut) clipboard.clear();
            final int done = ok;
            runOnUiThread(() -> { toast(done + " item(s) pasted"); load(); });
        }).start();
    }

    private void doDelete() {
        final List<File> files = selectedFiles();
        if (files.isEmpty()) return;
        if (Store.recycleConfirm(this)) {
            Dlg.b(this)
                    .setTitle("Move to Recycle Bin?")
                    .setMessage(files.size() + " item(s) will be moved to the Recycle Bin.")
                    .setPositiveButton("Move", (d, w) -> recycleAll(files))
                    .setNegativeButton("Cancel", null)
                    .show();
        } else recycleAll(files);
    }

    private void recycleAll(final List<File> files) {
        new Thread(() -> {
            int ok = 0;
            for (File f : files) {
                try { Fm.recycle(this, f); ok++; }
                catch (Exception ignored) { }
            }
            final int done = ok;
            runOnUiThread(() -> {
                toast(done + " moved to Recycle Bin");
                exitSelection();
            });
        }).start();
    }

    private void selectAll() {
        selected.clear();
        for (Item it : items) if (it.file != null) selected.add(it);
        if (selected.isEmpty()) { toast("Nothing to select"); return; }
        selectionMode = true;
        actionBar.setVisibility(View.VISIBLE);
        subtitle.setText(selected.size() + " selected");
        adapter.notifyDataSetChanged();
    }

    private boolean isText(String name) {
        String e = Fm.ext(name);
        switch (e) {
            case "txt": case "md": case "json": case "xml": case "log": case "csv": case "ini":
            case "yml": case "yaml": case "html": case "htm": case "js": case "css": case "java":
            case "kt": case "py": case "sh": case "c": case "cpp": case "h": case "gradle":
            case "properties": case "srt": case "vtt":
                return true;
            default:
                return false;
        }
    }

    private void openFile(File f) {
        int cat = Fm.catOf(f.getName());
        if (cat == Fm.CAT_IMAGE || cat == Fm.CAT_VIDEO || cat == Fm.CAT_AUDIO) { openMedia(f); return; }
        if (Fm.ext(f.getName()).equals("pdf")) {
            Intent p = new Intent(this, PdfViewerActivity.class);
            p.putExtra("path", f.getAbsolutePath());
            startActivity(p);
            return;
        }
        if (isText(f.getName())) { openText(f); return; }
        openExternal(f, false);
    }

    private void openText(File f) {
        Intent i = new Intent(this, TextEditorActivity.class);
        i.putExtra("path", f.getAbsolutePath());
        startActivity(i);
    }

    private void openExternal(File f, boolean forceChooser) {
        Uri uri = FmProvider.uriFor(f);
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(uri, Fm.mimeOf(f.getName()));
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        String comp = Store.mimeApp(this, Fm.ext(f.getName()));
        if (comp != null && !forceChooser) {
            try {
                String[] p = comp.split("/");
                i.setClassName(p[0], p[1]);
            } catch (Exception ignored) { }
        }
        try {
            if (forceChooser) startActivity(Intent.createChooser(i, "Open with"));
            else startActivity(i);
        } catch (ActivityNotFoundException e) {
            try { startActivity(Intent.createChooser(i, "Open with")); }
            catch (Exception e2) { toast("No app can open this file"); }
        } catch (Exception e) {
            toast("Could not open file");
        }
    }

    private void openWith() {
        List<File> files = selectedFiles();
        if (files.size() != 1) { toast("Select one file to open"); return; }
        openExternal(files.get(0), true);
    }

    private void saveAs() {
        List<File> files = selectedFiles();
        if (files.size() != 1) { toast("Select one file to copy"); return; }
        final File src = files.get(0);
        final EditText in = new EditText(this);
        in.setText(src.getName());
        in.setSelection(src.getName().length());
        Dlg.b(this)
                .setTitle("Save a copy as")
                .setView(in)
                .setPositiveButton("Save", (d, w) -> {
                    String nn = in.getText().toString().trim();
                    if (nn.isEmpty()) return;
                    final String name = nn;
                    new Thread(() -> {
                        String msg;
                        try {
                            File dest = new File(currentDir, Fm.uniqueName(currentDir, name));
                            Fm.copyFile(src, dest);
                            msg = "Saved " + dest.getName();
                        } catch (Exception e) { msg = "Could not save"; }
                        final String m = msg;
                        runOnUiThread(() -> { toast(m); exitSelection(); });
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openMedia(File f) {
        ArrayList<String> media = new ArrayList<>();
        int idx = 0;
        for (Item it : items) {
            if (it.file == null || it.file.isDirectory()) continue;
            int c = Fm.catOf(it.file.getName());
            if (c == Fm.CAT_IMAGE || c == Fm.CAT_VIDEO || c == Fm.CAT_AUDIO) {
                if (it.file.getAbsolutePath().equals(f.getAbsolutePath())) idx = media.size();
                media.add(it.file.getAbsolutePath());
            }
        }
        if (media.isEmpty()) media.add(f.getAbsolutePath());
        Intent i = new Intent(this, MediaViewerActivity.class);
        i.putExtra("paths", media.toArray(new String[0]));
        i.putExtra("index", idx);
        startActivity(i);
    }

    private void onBack() {
        if (selectionMode) { exitSelection(); return; }
        if (searching) { toggleSearch(); return; }
        if (!appsMode && category < 0 && !newMode && !searchMode && !largeMode) {
            File parent = currentDir.getParentFile();
            if (parent != null && currentDir.getAbsolutePath().length() > 1) {
                currentDir = parent;
                load();
                return;
            }
        }
        finish();
    }

    @Override
    public void onBackPressed() { onBack(); }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    private void loadThumb(final Item it, final ImageView target) {
        final String path = it.file.getAbsolutePath();
        target.setTag(path);
        Bitmap cached = thumbs.get(path);
        if (cached != null) { target.setImageBitmap(cached); return; }
        target.setImageResource(R.drawable.ic_cat_images);
        new Thread(() -> {
            Bitmap bm = null;
            try {
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(path, o);
                int scale = 1;
                while (o.outWidth / scale > 160 || o.outHeight / scale > 160) scale *= 2;
                BitmapFactory.Options o2 = new BitmapFactory.Options();
                o2.inSampleSize = scale;
                bm = BitmapFactory.decodeFile(path, o2);
            } catch (Exception ignored) { }
            final Bitmap b = bm;
            if (b != null) {
                runOnUiThread(() -> {
                    thumbs.put(path, b);
                    if (path.equals(target.getTag())) target.setImageBitmap(b);
                });
            }
        }).start();
    }

    class Adapter extends BaseAdapter {
        private int iconFor(Item it) {
            if (it.isDir) return R.drawable.ic_folder_blue;
            switch (Fm.catOf(it.name)) {
                case Fm.CAT_IMAGE: return R.drawable.ic_cat_images;
                case Fm.CAT_AUDIO: return R.drawable.ic_cat_audio;
                case Fm.CAT_VIDEO: return R.drawable.ic_cat_videos;
                case Fm.CAT_DOC: return R.drawable.ic_cat_documents;
                case Fm.CAT_APK: return R.drawable.ic_cat_apk;
                default: return R.drawable.ic_cat_file;
            }
        }

        private int colorFor(Item it) {
            if (it.isDir) return 0;
            switch (Fm.catOf(it.name)) {
                case Fm.CAT_IMAGE: return 0xFFFF5252;
                case Fm.CAT_AUDIO: return 0xFF00BCD4;
                case Fm.CAT_VIDEO: return 0xFF7C4DFF;
                case Fm.CAT_DOC: return 0xFF2196F3;
                case Fm.CAT_APK: return 0xFF4CAF50;
                default: return 0xFF78909C;
            }
        }

        public int getCount() { return items.size(); }
        public Object getItem(int p) { return items.get(p); }
        public long getItemId(int p) { return p; }

        public View getView(int pos, View cv, ViewGroup parent) {
            Item it = items.get(pos);
            boolean sel = selected.contains(it);
            boolean isImage = it.file != null && Fm.catOf(it.name) == Fm.CAT_IMAGE;

            if (viewMode != 0) {
                LinearLayout card = Ui.col(BrowserActivity.this);
                card.setGravity(Gravity.CENTER_HORIZONTAL);
                card.setPadding(Ui.dp(BrowserActivity.this, 8), Ui.dp(BrowserActivity.this, 14), Ui.dp(BrowserActivity.this, 8), Ui.dp(BrowserActivity.this, 14));
                card.setBackground(Ui.round(sel ? Ui.selectedBg(BrowserActivity.this) : MainActivity.CARD, 16, BrowserActivity.this, 0, 0));

                ImageView glyph = new ImageView(BrowserActivity.this);
                glyph.setScaleType(ImageView.ScaleType.CENTER_CROP);
                int size = viewMode == 2 ? 88 : 68;
                if (isImage) {
                    glyph.setBackground(Ui.round(Ui.chipBg(BrowserActivity.this), 12, BrowserActivity.this, 0, 0));
                    loadThumb(it, glyph);
                } else {
                    glyph.setImageResource(iconFor(it));
                    glyph.setScaleType(ImageView.ScaleType.FIT_CENTER);
                }
                card.addView(glyph, new LinearLayout.LayoutParams(Ui.dp(BrowserActivity.this, size), Ui.dp(BrowserActivity.this, size)));

                TextView name = Ui.text(BrowserActivity.this, it.name, 13, MainActivity.TEXT, true);
                name.setGravity(Gravity.CENTER);
                name.setMaxLines(2);
                name.setPadding(0, Ui.dp(BrowserActivity.this, 10), 0, 0);
                card.addView(name);
                if (it.isDir) {
                    TextView sub = Ui.text(BrowserActivity.this, it.sub.split("\\|")[0].trim(), 11, MainActivity.SUB, false);
                    sub.setGravity(Gravity.CENTER);
                    card.addView(sub);
                }
                return card;
            }

            LinearLayout row = Ui.row(BrowserActivity.this);
            row.setPadding(Ui.dp(BrowserActivity.this, 16), Ui.dp(BrowserActivity.this, 10), Ui.dp(BrowserActivity.this, 12), Ui.dp(BrowserActivity.this, 10));
            row.setBackgroundColor(sel ? Ui.selectedBg(BrowserActivity.this) : MainActivity.BG);

            ImageView glyph = new ImageView(BrowserActivity.this);
            glyph.setScaleType(ImageView.ScaleType.FIT_CENTER);
            if (isImage) {
                glyph.setScaleType(ImageView.ScaleType.CENTER_CROP);
                glyph.setBackground(Ui.round(Ui.chipBg(BrowserActivity.this), 10, BrowserActivity.this, 0, 0));
                loadThumb(it, glyph);
            } else {
                glyph.setImageResource(iconFor(it));
            }
            LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(Ui.dp(BrowserActivity.this, 46), Ui.dp(BrowserActivity.this, 46));
            glp.setMargins(0, 0, Ui.dp(BrowserActivity.this, 16), 0);
            row.addView(glyph, glp);

            LinearLayout col = Ui.col(BrowserActivity.this);
            col.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            TextView name = Ui.text(BrowserActivity.this, it.name, 16, MainActivity.TEXT, true);
            TextView sub = Ui.text(BrowserActivity.this, it.sub, 13, MainActivity.SUB, false);
            sub.setPadding(0, Ui.dp(BrowserActivity.this, 3), 0, 0);
            col.addView(name);
            col.addView(sub);
            row.addView(col);

            if (it.file != null && Store.isStarred(BrowserActivity.this, it.file.getAbsolutePath())) {
                TextView s = Ui.text(BrowserActivity.this, "\u2605", 15, 0xFFF9AB00, false);
                s.setPadding(Ui.dp(BrowserActivity.this, 8), 0, 0, 0);
                row.addView(s);
            }
            row.addView(Ui.text(BrowserActivity.this, "\u203A", 22, MainActivity.SUB, false));
            return row;
        }
    }
}
