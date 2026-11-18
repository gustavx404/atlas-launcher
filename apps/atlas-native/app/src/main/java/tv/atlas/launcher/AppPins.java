package tv.atlas.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Persists the apps shown in the home-screen shortcut grid. */
final class AppPins {
    static final int MAX_PINS = 4;
    enum Direction { LEFT, RIGHT, UP, DOWN }
    private static final String PREFS = "home_pins";
    private static final String KEY = "packages";
    private static final String ORDERED_KEY = "packages_ordered";
    private static final List<String> DEFAULTS = Arrays.asList(
            "com.google.android.youtube.tv", "com.netflix.ninja", "com.tcl.tv");

    private AppPins() {}

    static List<String> get(Context context, List<AppCatalog.Entry> apps) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String ordered = preferences.getString(ORDERED_KEY, null);
        Set<String> legacy = ordered == null ? preferences.getStringSet(KEY, null) : null;
        List<String> source;
        if (ordered != null) source = ordered.isEmpty() ? new ArrayList<>() : Arrays.asList(ordered.split(","));
        else if (legacy != null) source = new ArrayList<>(legacy);
        else source = DEFAULTS;
        Set<String> installed = new HashSet<>();
        for (AppCatalog.Entry app : apps) installed.add(app.packageName);
        List<String> result = new ArrayList<>();
        for (String packageName : source) {
            if (installed.contains(packageName) && !result.contains(packageName) && result.size() < MAX_PINS) {
                result.add(packageName);
            }
        }
        if ((ordered != null && result.size() != source.size()) || legacy != null) save(context, result);
        return result;
    }

    static boolean isPinned(Context context, String packageName, List<AppCatalog.Entry> apps) {
        return get(context, apps).contains(packageName);
    }

    /** Returns true when added, false if already pinned or all slots are occupied. */
    static boolean pin(Context context, String packageName, List<AppCatalog.Entry> apps) {
        List<String> pins = get(context, apps);
        if (pins.contains(packageName) || pins.size() >= MAX_PINS) return false;
        pins.add(packageName);
        save(context, pins);
        return true;
    }

    static boolean unpin(Context context, String packageName, List<AppCatalog.Entry> apps) {
        List<String> pins = get(context, apps);
        if (!pins.remove(packageName)) return false;
        save(context, pins);
        return true;
    }

    static boolean moveInOrder(List<String> pins, String packageName, Direction direction) {
        int index = pins.indexOf(packageName);
        if (index < 0) return false;
        int target;
        switch (direction) {
            case LEFT: target = index % 2 == 1 ? index - 1 : -1; break;
            case RIGHT: target = index % 2 == 0 && index + 1 < pins.size() ? index + 1 : -1; break;
            case UP: target = index >= 2 ? index - 2 : -1; break;
            case DOWN: target = index + 2 < pins.size() ? index + 2 : -1; break;
            default: target = -1;
        }
        if (target < 0 || target >= pins.size()) return false;
        String displaced = pins.get(target);
        pins.set(target, packageName);
        pins.set(index, displaced);
        return true;
    }

    static void setOrder(Context context, List<String> pins) { save(context, pins); }

    private static void save(Context context, List<String> pins) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(ORDERED_KEY, String.join(",", pins)).remove(KEY).apply();
    }
}
