package com.nijatva.file;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;
import android.provider.Settings;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Core helpers: categories, sizes, storage stats, recycle bin, permissions, zip. */
public final class Fm {

    public static final int CAT_IMAGE = 0, CAT_AUDIO = 1, CAT_VIDEO = 2, CAT_DOC = 3, CAT_APK = 4, CAT_OTHER = 5;

    private static final String[] IMG = {"jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif", "svg"};
    private static final String[] AUD = {"mp3", "wav", "aac", "m4a", "ogg", "oga", "flac", "opus", "amr", "mid"};
    private static final String[] VID = {"mp4", "mkv", "avi", "mov", "3gp", "webm", "flv", "m4v", "ts", "mpg", "mpeg"};
    private static final String[] DOC = {"pdf", "doc", "docx", "txt", "rtf", "xls", "xlsx", "ppt", "pptx",
            "csv", "md", "odt", "ods", "odp", "epub", "json", "xml", "log"};

    private Fm() {}

    /** Real installed version, so the About screen never goes stale. */
    public static String versionName(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "";
        }
    }

    public static String ext(String name) {
        int i = name.lastIndexOf('.');
        return i >= 0 ? name.substring(i + 1).toLowerCase(Locale.US) : "";
    }

    public static int catOf(String name) {
        String e = ext(name);
        if (in(e, IMG)) return CAT_IMAGE;
        if (in(e, AUD)) return CAT_AUDIO;
        if (in(e, VID)) return CAT_VIDEO;
        if (in(e, DOC)) return CAT_DOC;
        if (e.equals("apk")) return CAT_APK;
        return CAT_OTHER;
    }

    private static boolean in(String s, String[] arr) {
        for (String a : arr) if (a.equals(s)) return true;
        return false;
    }

    public static String catName(int c) {
        switch (c) {
            case CAT_IMAGE: return "Images";
            case CAT_AUDIO: return "Audio";
            case CAT_VIDEO: return "Videos";
            case CAT_DOC: return "Documents";
            case CAT_APK: return "Apps (APK)";
            default: return "Files";
        }
    }

    public static String fmtSize(long b) {
        if (b < 0) b = 0;
        if (b < 1024) return b + " B";
        double kb = b / 1024.0;
        if (kb < 1024) return String.format(Locale.US, "%.1f kB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format(Locale.US, "%.2f MB", mb);
        double gb = mb / 1024.0;
        if (gb < 1024) return String.format(Locale.US, "%.2f GB", gb);
        return String.format(Locale.US, "%.2f TB", gb / 1024.0);
    }

    public static String datePattern(String label) {
        if ("08/10/2026".equals(label)) return "dd/MM/yyyy";
        if ("Oct 8, 2026".equals(label)) return "MMM d, yyyy";
        return "d MMM yyyy";
    }

    /** Size formatted in the unit chosen in Settings (Auto by default). */
    public static String fmtSize(Context c, long b) {
        String unit = Store.fileSizeUnit(c);
        if (unit == null || unit.isEmpty() || unit.equals("Auto")) return fmtSize(b);
        double v = Math.max(0, b);
        switch (unit) {
            case "B": return String.format(Locale.US, "%.0f B", v);
            case "kB": return String.format(Locale.US, "%.1f kB", v / 1024.0);
            case "MB": return String.format(Locale.US, "%.2f MB", v / 1048576.0);
            case "GB": return String.format(Locale.US, "%.2f GB", v / 1073741824.0);
            default: return fmtSize(b);
        }
    }

    public static String mimeOf(String name) {
        String e = ext(name);
        String m = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(e);
        if (m != null) return m;
        switch (catOf(name)) {
            case CAT_IMAGE: return "image/*";
            case CAT_AUDIO: return "audio/*";
            case CAT_VIDEO: return "video/*";
            case CAT_DOC: return "text/plain";
            case CAT_APK: return "application/vnd.android.package-archive";
            default: return "*/*";
        }
    }

    public static List<File> children(File dir) {
        File[] arr = dir.listFiles();
        if (arr == null) return new ArrayList<>();
        List<File> out = new ArrayList<>(Arrays.asList(arr));
        Collections.sort(out, new Comparator<File>() {
            public int compare(File a, File b) {
                if (a.isDirectory() != b.isDirectory()) return a.isDirectory() ? -1 : 1;
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });
        return out;
    }

    public static long dirSize(File f) {
        if (f == null || !f.exists()) return 0;
        if (f.isFile()) return f.length();
        long total = 0;
        File[] arr = f.listFiles();
        if (arr != null) for (File c : arr) total += dirSize(c);
        return total;
    }

    public static long[] storage() {
        try {
            File root = Environment.getExternalStorageDirectory();
            StatFs s = new StatFs(root.getAbsolutePath());
            long total = s.getBlockCountLong() * s.getBlockSizeLong();
            long free = s.getAvailableBlocksLong() * s.getBlockSizeLong();
            return new long[]{total, free};
        } catch (Exception e) {
            return new long[]{0, 0};
        }
    }

    public static List<File> byCategory(Context ctx, int cat) {
        List<File> out = new ArrayList<>();
        String[] cols = {MediaStore.MediaColumns.DATA};
        Uri uri;
        String sel = null;
        String[] selArgs = null;
        if (cat == CAT_OTHER) return scanFallback(CAT_OTHER);
        switch (cat) {
            case CAT_IMAGE: uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI; break;
            case CAT_AUDIO: uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI; break;
            case CAT_VIDEO: uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI; break;
            case CAT_APK:
                uri = MediaStore.Files.getContentUri("external");
                sel = MediaStore.MediaColumns.MIME_TYPE + "=?";
                selArgs = new String[]{"application/vnd.android.package-archive"};
                break;
            default:
                uri = MediaStore.Files.getContentUri("external");
                sel = MediaStore.MediaColumns.MIME_TYPE + " LIKE ? OR " + MediaStore.MediaColumns.MIME_TYPE + " LIKE ? OR "
                        + MediaStore.MediaColumns.MIME_TYPE + " LIKE ? OR " + MediaStore.MediaColumns.MIME_TYPE + " LIKE ?";
                selArgs = new String[]{"application/pdf", "text/%", "application/msword%", "application/vnd.ms-%"};
                break;
        }
        Cursor c = null;
        try {
            c = ctx.getContentResolver().query(uri, cols, sel, selArgs, null);
            if (c != null) {
                int di = c.getColumnIndex(MediaStore.MediaColumns.DATA);
                while (c.moveToNext()) {
                    String p = c.getString(di);
                    if (p != null) out.add(new File(p));
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        if (out.isEmpty()) out = scanFallback(cat);
        return out;
    }

    /** If MediaStore gave nothing (e.g. permission missing), scan the usual media folders. */
    private static List<File> scanFallback(int cat) {
        List<File> out = new ArrayList<>();
        File ext = Environment.getExternalStorageDirectory();
        File[] roots = {new File(ext, "DCIM"), new File(ext, "Pictures"), new File(ext, "Movies"),
                new File(ext, "Music"), new File(ext, "Download"), new File(ext, "Downloads"),
                new File(ext, "Documents"), new File(ext, "Audio"), new File(ext, "WhatsApp/Media"),
                new File(ext, "Telegram")};
        for (File r : roots) scanDir(r, cat, out, 0);
        return out;
    }

    private static void scanDir(File dir, int cat, List<File> out, int depth) {
        if (dir == null || !dir.exists() || depth > 4 || out.size() > 2000) return;
        File[] arr = dir.listFiles();
        if (arr == null) return;
        for (File f : arr) {
            if (f.isDirectory()) scanDir(f, cat, out, depth + 1);
            else if (catOf(f.getName()) == cat) out.add(f);
        }
    }

    /** True when the app can browse every folder (All files access). */
    public static boolean hasFullAccess(Context ctx) {
        if (Build.VERSION.SDK_INT >= 30) return Environment.isExternalStorageManager();
        return ctx.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasStorageAccess(Context ctx) {
        if (Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager()) return true;
        if (Build.VERSION.SDK_INT >= 33) {
            return ctx.checkSelfPermission("android.permission.READ_MEDIA_IMAGES") == PackageManager.PERMISSION_GRANTED
                    || ctx.checkSelfPermission("android.permission.READ_MEDIA_VIDEO") == PackageManager.PERMISSION_GRANTED
                    || ctx.checkSelfPermission("android.permission.READ_MEDIA_AUDIO") == PackageManager.PERMISSION_GRANTED;
        }
        return ctx.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestStorageAccess(Activity a, int reqCode) {
        if (Build.VERSION.SDK_INT >= 33) {
            a.requestPermissions(new String[]{
                    "android.permission.READ_MEDIA_IMAGES",
                    "android.permission.READ_MEDIA_VIDEO",
                    "android.permission.READ_MEDIA_AUDIO"}, reqCode);
        } else if (Build.VERSION.SDK_INT >= 30) {
            a.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, reqCode);
        } else {
            a.requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, reqCode);
        }
    }

    public static void requestAllFiles(Activity a) {
        try {
            Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + a.getPackageName()));
            a.startActivity(i);
        } catch (Exception e) {
            try { a.startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)); }
            catch (Exception ignored) { }
        }
    }

    public static void openAppSettings(Activity a) {
        try {
            a.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + a.getPackageName())));
        } catch (Exception ignored) {
        }
    }

    public static Uri mediaUri(Context c, String path) {
        try {
            Uri q = MediaStore.Files.getContentUri("external");
            Cursor cur = c.getContentResolver().query(q,
                    new String[]{MediaStore.MediaColumns._ID},
                    MediaStore.MediaColumns.DATA + "=?", new String[]{path}, null);
            if (cur != null) {
                if (cur.moveToFirst()) {
                    long id = cur.getLong(0);
                    cur.close();
                    return ContentUris.withAppendedId(q, id);
                }
                cur.close();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static InputStream openMedia(Context c, String path) throws IOException {
        File f = new File(path);
        if (f.exists() && f.canRead()) return new FileInputStream(f);
        Uri u = mediaUri(c, path);
        if (u != null) {
            InputStream in = c.getContentResolver().openInputStream(u);
            if (in != null) return in;
        }
        return new FileInputStream(f);
    }

    // ---------------- recycle bin ----------------

    private static Boolean rootWritable = null;

    private static boolean canWrite(File dir) {
        try {
            if (!dir.exists()) dir.mkdirs();
            File probe = new File(dir, ".w");
            boolean ok = probe.createNewFile() || probe.exists();
            if (probe.exists()) probe.delete();
            return ok;
        } catch (Exception e) {
            return false;
        }
    }

    public static File recycleDir(Context c) {
        File ext = new File(Environment.getExternalStorageDirectory(), "FileManager/.RecycleBin");
        if (rootWritable == null) rootWritable = canWrite(ext);
        if (rootWritable) return ext;
        File alt = new File(c.getExternalFilesDir(null), "RecycleBin");
        if (!alt.exists()) alt.mkdirs();
        return alt;
    }

    /** SD card / USB OTG roots (secondary volumes). */
    public static List<File> externalRoots(Context c) {
        List<File> out = new ArrayList<>();
        try {
            File[] dirs = c.getExternalFilesDirs(null);
            if (dirs == null) return out;
            String primary = Environment.getExternalStorageDirectory().getAbsolutePath();
            for (File d : dirs) {
                if (d == null) continue;
                String p = d.getAbsolutePath();
                int idx = p.indexOf("/Android/");
                if (idx <= 0) continue;
                File root = new File(p.substring(0, idx));
                if (!root.getAbsolutePath().equals(primary) && root.exists()) out.add(root);
            }
        } catch (Exception ignored) { }
        return out;
    }

    /** Delete recycle-bin entries older than `days`. */
    public static int purgeOldTrash(Context c, int days) {
        int n = 0;
        long cut = System.currentTimeMillis() - (long) days * 24 * 3600 * 1000;
        for (String[] e : recycleEntries(c)) {
            try {
                long t = Long.parseLong(e[3]);
                if (t < cut) { purge(c, e[1], e[2]); n++; }
            } catch (Exception ignored) { }
        }
        return n;
    }

    private static File indexFile(Context c) {
        return new File(recycleDir(c), "index.txt");
    }

    public static File recycle(Context c, File src) throws IOException {
        File dir = recycleDir(c);
        String stored = System.currentTimeMillis() + "_" + src.getName();
        File dest = new File(dir, stored);
        moveFile(src, dest);
        append(indexFile(c), src.getAbsolutePath() + "\t" + System.currentTimeMillis() + "\t" + stored);
        return dest;
    }

    public static List<String[]> recycleEntries(Context c) {
        List<String[]> out = new ArrayList<>();
        File idx = indexFile(c);
        if (!idx.exists()) return out;
        try {
            for (String line : readLines(idx)) {
                String[] p = line.split("\t");
                if (p.length >= 3) {
                    File stored = new File(recycleDir(c), p[2]);
                    out.add(new String[]{new File(p[0]).getName(), p[0], p[2], p[1], String.valueOf(stored.exists())});
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    public static void restore(Context c, String originalPath, String storedName) throws IOException {
        File stored = new File(recycleDir(c), storedName);
        File target = new File(originalPath);
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        if (stored.exists()) moveFile(stored, target);
        removeEntry(c, originalPath, storedName);
    }

    public static void purge(Context c, String originalPath, String storedName) {
        File stored = new File(recycleDir(c), storedName);
        if (stored.exists()) deleteRecursive(stored);
        removeEntry(c, originalPath, storedName);
    }

    public static void emptyBin(Context c) {
        File dir = recycleDir(c);
        File[] arr = dir.listFiles();
        if (arr != null) for (File f : arr) if (!f.getName().equals("index.txt")) deleteRecursive(f);
        indexFile(c).delete();
    }

    private static void removeEntry(Context c, String originalPath, String storedName) {
        File idx = indexFile(c);
        if (!idx.exists()) return;
        try {
            List<String> keep = new ArrayList<>();
            for (String line : readLines(idx)) {
                String[] p = line.split("\t");
                if (p.length >= 3 && p[0].equals(originalPath) && p[2].equals(storedName)) continue;
                keep.add(line);
            }
            writeLines(idx, keep);
        } catch (Exception ignored) {
        }
    }

    private static List<String> readLines(File f) throws IOException {
        List<String> out = new ArrayList<>();
        java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(new FileInputStream(f)));
        try {
            String l;
            while ((l = r.readLine()) != null) if (l.trim().length() > 0) out.add(l);
        } finally {
            r.close();
        }
        return out;
    }

    private static void writeLines(File f, List<String> lines) throws IOException {
        FileOutputStream fos = new FileOutputStream(f, false);
        try {
            for (String l : lines) fos.write((l + "\n").getBytes("UTF-8"));
        } finally {
            fos.close();
        }
    }

    private static void append(File f, String line) throws IOException {
        FileOutputStream fos = new FileOutputStream(f, true);
        try {
            fos.write((line + "\n").getBytes("UTF-8"));
        } finally {
            fos.close();
        }
    }

    // ---------------- file ops ----------------

    public static void moveFile(File src, File dest) throws IOException {
        if (src.renameTo(dest)) return;
        copyFile(src, dest);
        deleteRecursive(src);
    }

    public static void copyFile(File src, File dest) throws IOException {
        if (src.isDirectory()) {
            String s = src.getCanonicalPath();
            String d = dest.getCanonicalPath();
            if (d.equals(s) || d.startsWith(s + File.separator)) {
                throw new IOException("Cannot copy a folder into itself");
            }
            if (!dest.exists()) dest.mkdirs();
            File[] arr = src.listFiles();
            if (arr != null) for (File c : arr) copyFile(c, new File(dest, c.getName()));
            return;
        }
        File parent = dest.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dest);
        try {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        } finally {
            in.close();
            out.close();
        }
    }

    public static boolean deleteRecursive(File f) {
        if (f == null) return false;
        if (f.isDirectory()) {
            File[] arr = f.listFiles();
            if (arr != null) for (File c : arr) deleteRecursive(c);
        }
        return f.delete();
    }

    public static String uniqueName(File dir, String name) {
        File f = new File(dir, name);
        if (!f.exists()) return name;
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        int i = 1;
        while (true) {
            String cand = base + " (" + i + ")" + ext;
            if (!new File(dir, cand).exists()) return cand;
            i++;
        }
    }

    // ---------------- zip ----------------

    public static void zip(List<File> sources, File outZip) throws IOException {
        ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outZip)));
        final String skip = outZip.getCanonicalPath();
        try {
            for (File src : sources) addToZip(zos, src, src.getName(), skip);
        } finally {
            zos.close();
        }
    }

    private static void addToZip(ZipOutputStream zos, File f, String entryName, String skipPath) throws IOException {
        if (f.isFile() && f.getCanonicalPath().equals(skipPath)) return; // never zip the output into itself
        if (f.isDirectory()) {
            File[] arr = f.listFiles();
            if (arr == null || arr.length == 0) {
                zos.putNextEntry(new ZipEntry(entryName + "/"));
                zos.closeEntry();
            } else {
                for (File c : arr) addToZip(zos, c, entryName + "/" + c.getName(), skipPath);
            }
        } else {
            zos.putNextEntry(new ZipEntry(entryName));
            FileInputStream in = new FileInputStream(f);
            try {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) zos.write(buf, 0, n);
            } finally {
                in.close();
            }
            zos.closeEntry();
        }
    }

    public static int unzip(File zipFile, File destDir) throws IOException {
        int count = 0;
        String destPath = destDir.getCanonicalPath();
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
        try {
            ZipEntry e;
            byte[] buf = new byte[8192];
            while ((e = zis.getNextEntry()) != null) {
                File out = new File(destDir, e.getName());
                if (!out.getCanonicalPath().startsWith(destPath)) { zis.closeEntry(); continue; }
                if (e.isDirectory()) {
                    out.mkdirs();
                } else {
                    File parent = out.getParentFile();
                    if (parent != null && !parent.exists()) parent.mkdirs();
                    FileOutputStream fos = new FileOutputStream(out);
                    try {
                        int n;
                        while ((n = zis.read(buf)) > 0) fos.write(buf, 0, n);
                    } finally {
                        fos.close();
                    }
                    count++;
                }
                zis.closeEntry();
            }
        } finally {
            zis.close();
        }
        return count;
    }

    /** Sort by mode: 0 date, 1 name, 2 size. */
    public static void sort(List<File> files, final int mode, final boolean asc) {
        Collections.sort(files, new Comparator<File>() {
            public int compare(File a, File b) {
                if (a.isDirectory() != b.isDirectory()) return a.isDirectory() ? -1 : 1;
                int r;
                switch (mode) {
                    case 1: r = a.getName().compareToIgnoreCase(b.getName()); break;
                    case 2: r = Long.compare(a.length(), b.length()); break;
                    case 3: r = ext(a.getName()).compareToIgnoreCase(ext(b.getName())); break;
                    default: r = Long.compare(a.lastModified(), b.lastModified()); break;
                }
                return asc ? r : -r;
            }
        });
    }

    /** Files above minBytes, biggest first. */
    private static File[] scanRoots() {
        File ext = Environment.getExternalStorageDirectory();
        return new File[]{new File(ext, "DCIM"), new File(ext, "Pictures"), new File(ext, "Download"),
                new File(ext, "Downloads"), new File(ext, "WhatsApp"), new File(ext, "Movies"),
                new File(ext, "Music"), new File(ext, "Documents")};
    }

    private static void collectFiles(File dir, List<File> out, int depth) {
        if (dir == null || !dir.exists() || depth > 6 || out.size() > 5000) return;
        File[] arr = dir.listFiles();
        if (arr == null) return;
        for (File f : arr) {
            if (f.isDirectory()) collectFiles(f, out, depth + 1);
            else out.add(f);
        }
    }

    private static String md5(File f) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            InputStream in = new FileInputStream(f);
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) > 0) md.update(b, 0, n);
            in.close();
            StringBuilder sb = new StringBuilder();
            for (byte x : md.digest()) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /** Groups of identical files (same size + MD5). */
    public static List<List<File>> findDuplicates(Context c) {
        List<File> all = new ArrayList<>();
        for (File r : scanRoots()) collectFiles(r, all, 0);

        Map<Long, List<File>> bySize = new HashMap<>();
        for (File f : all) {
            long len = f.length();
            if (len <= 0) continue;
            List<File> l = bySize.get(len);
            if (l == null) { l = new ArrayList<>(); bySize.put(len, l); }
            l.add(f);
        }

        List<List<File>> out = new ArrayList<>();
        for (List<File> sameSize : bySize.values()) {
            if (sameSize.size() < 2) continue;
            Map<String, List<File>> byHash = new HashMap<>();
            for (File f : sameSize) {
                String h = md5(f);
                if (h == null) continue;
                List<File> l = byHash.get(h);
                if (l == null) { l = new ArrayList<>(); byHash.put(h, l); }
                l.add(f);
            }
            for (List<File> g : byHash.values()) if (g.size() > 1) out.add(g);
        }
        return out;
    }

    /** Folders that contain nothing (recursively). */
    public static List<File> findEmptyFolders(Context c) {
        List<File> out = new ArrayList<>();
        for (File r : scanRoots()) emptyRec(r, out, 0);
        return out;
    }

    private static boolean emptyRec(File dir, List<File> out, int depth) {
        if (dir == null || !dir.exists() || depth > 6) return false;
        File[] arr = dir.listFiles();
        if (arr == null) return false;
        boolean empty = true;
        for (File f : arr) {
            if (f.isDirectory()) {
                if (!emptyRec(f, out, depth + 1)) empty = false;
            } else {
                empty = false;
            }
        }
        if (empty && depth > 0) out.add(dir);
        return empty;
    }

    public static List<File> largeFiles(Context c, long minBytes) {
        List<File> out = new ArrayList<>();
        File ext = Environment.getExternalStorageDirectory();
        File[] roots = {new File(ext, "Download"), new File(ext, "Downloads"), new File(ext, "DCIM"),
                new File(ext, "Movies"), new File(ext, "Pictures"), new File(ext, "Documents"),
                new File(ext, "WhatsApp"), new File(ext, "Telegram")};
        for (File r : roots) collectLarge(r, minBytes, out, 0);
        Collections.sort(out, new Comparator<File>() {
            public int compare(File a, File b) { return Long.compare(b.length(), a.length()); }
        });
        return out;
    }

    private static void collectLarge(File dir, long min, List<File> out, int depth) {
        if (dir == null || !dir.exists() || depth > 5 || out.size() > 200) return;
        File[] arr = dir.listFiles();
        if (arr == null) return;
        for (File f : arr) {
            if (f.isDirectory()) collectLarge(f, min, out, depth + 1);
            else if (f.length() >= min) out.add(f);
        }
    }

    public static List<File> search(File root, String query, int maxDepth) {
        List<File> out = new ArrayList<>();
        searchRec(root, query.toLowerCase(Locale.US), out, 0, maxDepth, 300);
        return out;
    }

    private static void searchRec(File dir, String q, List<File> out, int depth, int maxDepth, int limit) {
        if (depth > maxDepth || out.size() >= limit) return;
        File[] arr = dir.listFiles();
        if (arr == null) return;
        for (File f : arr) {
            if (out.size() >= limit) return;
            if (f.getName().toLowerCase(Locale.US).contains(q)) out.add(f);
            if (f.isDirectory()) searchRec(f, q, out, depth + 1, maxDepth, limit);
        }
    }
}
