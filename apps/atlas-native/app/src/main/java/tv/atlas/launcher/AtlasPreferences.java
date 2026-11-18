package tv.atlas.launcher;

import android.content.Context;
import android.content.SharedPreferences;

/** Persistent user choices for the home screen, kept behind a small preference interface. */
final class AtlasPreferences {
    static final String[] SOURCE_IDS = {"certbr", "cisa", "bbcmundo", "dwbrasil"};
    static final String[] SOURCE_NAMES = {"CERT.br", "CISA", "BBC Mundo", "DW Brasil"};
    static final String[] WALLPAPERS = {"Noite azul", "Aurora", "Ametista"};
    private static final String FILE = "atlas-settings";
    private final SharedPreferences values;

    AtlasPreferences(Context context) {
        values = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    int wallpaper() { return values.getInt("wallpaper", 0); }
    void setWallpaper(int value) { values.edit().putInt("wallpaper", Math.floorMod(value, WALLPAPERS.length)).apply(); }
    boolean showClock() { return values.getBoolean("show_clock", true); }
    boolean showNetwork() { return values.getBoolean("show_network", true); }
    boolean showNews() { return values.getBoolean("show_news", true); }
    void setVisible(String key, boolean visible) { values.edit().putBoolean(key, visible).apply(); }
    boolean sourceEnabled(String id) { return values.getBoolean("source_" + id, true); }
    void setSourceEnabled(String id, boolean enabled) { values.edit().putBoolean("source_" + id, enabled).apply(); }
}
