package tv.atlas.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.util.LruCache;
import android.view.View;

/** Shared app icon rendering and restrained Atlas framing for home and library tiles. */
final class AtlasAppIcon {
    private static final int CACHE_BYTES = 2 * 1024 * 1024;
    private static final LruCache<String, Bitmap> BITMAPS = new LruCache<String, Bitmap>(CACHE_BYTES) {
        @Override protected int sizeOf(String key, Bitmap value) { return value.getByteCount(); }
    };

    private AtlasAppIcon() {}

    static Bitmap bitmap(Context context, AppCatalog.Entry app, int sizePx) {
        String key = app.packageName + ':' + sizePx;
        synchronized (BITMAPS) {
            Bitmap cached = BITMAPS.get(key);
            if (cached != null && !cached.isRecycled()) return cached;
        }
        Bitmap rendered = AtlasStyle.iconBitmap(AppCatalog.icon(context, app), sizePx);
        if (rendered == null) return null;
        synchronized (BITMAPS) { BITMAPS.put(key, rendered); }
        return rendered;
    }

    static GradientDrawable plate(View owner, String packageName, float radius) {
        int accent = accent(packageName);
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[] {withAlpha(accent, 0x30), 0xff122131});
        drawable.setCornerRadius(AtlasStyle.dp(owner, radius));
        drawable.setStroke(AtlasStyle.dp(owner, 1), withAlpha(accent, 0x58));
        return drawable;
    }

    static void clearCache() {
        synchronized (BITMAPS) { BITMAPS.evictAll(); }
    }

    private static int accent(String packageName) {
        if (packageName == null) return AtlasStyle.CYAN;
        if (packageName.contains("youtube")) return 0xffff4b55;
        if (packageName.contains("netflix")) return 0xffe50914;
        if (packageName.contains("vending")) return 0xff70c98a;
        if (packageName.contains("download")) return 0xffffa45f;
        if (packageName.contains("browser") || packageName.contains("chrome")) return 0xff78baff;
        if (packageName.contains("tv") || packageName.contains("input")) return AtlasStyle.CYAN;
        return AtlasStyle.LAVENDER;
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00ffffff) | (alpha << 24);
    }
}
