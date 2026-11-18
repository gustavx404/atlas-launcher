package tv.atlas.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;

/** Returns one launchable activity per installed package, preferring Android TV activities. */
final class AppCatalog {
    private static final Set<String> ESSENTIAL_SYSTEM_APPS = new HashSet<>(Arrays.asList(
            "com.android.vending",
            "com.google.android.youtube.tv",
            "com.netflix.ninja",
            "com.tcl.tv"));

    private AppCatalog() {}

    static List<Entry> installed(Context context) {
        PackageManager manager = context.getPackageManager();
        LinkedHashMap<String, ResolveInfo> unique = new LinkedHashMap<>();
        for (String category : new String[] {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
            // MATCH_DEFAULT_ONLY hides TV launcher activities that omit CATEGORY_DEFAULT.
            List<ResolveInfo> matches = manager.queryIntentActivities(query, 0);
            for (ResolveInfo info : matches) {
                String packageName = info.activityInfo.packageName;
                if (!packageName.equals(context.getPackageName()) && isVisibleApp(info)) {
                    unique.putIfAbsent(packageName, info);
                }
            }
        }
        List<Entry> result = new ArrayList<>(unique.size());
        for (ResolveInfo info : unique.values()) {
            result.add(new Entry(info.loadLabel(manager).toString(), info.activityInfo.packageName, info.activityInfo.name));
        }
        result.sort((left, right) -> left.label.toLowerCase(Locale.ROOT).compareTo(right.label.toLowerCase(Locale.ROOT)));
        return result;
    }

    private static boolean isVisibleApp(ResolveInfo info) {
        int flags = info.activityInfo.applicationInfo.flags;
        boolean systemApp = (flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0;
        return !systemApp || ESSENTIAL_SYSTEM_APPS.contains(info.activityInfo.packageName);
    }

    static void launch(Context context, Entry app) {
        Intent intent = new Intent(Intent.ACTION_MAIN)
                .setComponent(new ComponentName(app.packageName, app.activityName))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    static Drawable icon(Context context, Entry app) {
        try {
            // Application icons are usually the artwork users recognize; activity icons
            // often fall back to Android's generic launcher glyph on TV firmware.
            return context.getPackageManager().getApplicationIcon(app.packageName);
        } catch (PackageManager.NameNotFoundException missing) {
            try { return context.getPackageManager().getActivityIcon(new ComponentName(app.packageName, app.activityName)); }
            catch (Exception ignored) { return null; }
        }
    }

    static final class Entry {
        final String label, packageName, activityName;
        Entry(String label, String packageName, String activityName) {
            this.label = label; this.packageName = packageName; this.activityName = activityName;
        }
    }
}
