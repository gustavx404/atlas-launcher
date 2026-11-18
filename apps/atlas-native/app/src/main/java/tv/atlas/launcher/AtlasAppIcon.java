package tv.atlas.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

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

    static FrameLayout create(View owner, String packageName, String label, Bitmap bitmap, int sizeDp) {
        FrameLayout holder = new FrameLayout(owner.getContext());
        holder.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        int size = AtlasStyle.dp(owner, sizeDp);
        if (bitmap != null) {
            ImageView image = new ImageView(owner.getContext());
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setImageBitmap(bitmap);
            holder.addView(image, new FrameLayout.LayoutParams(-1, -1));
        } else if (packageName == null) {
            ImageView add = AtlasStyle.glyph(owner, R.drawable.ic_atlas_add);
            add.setColorFilter(AtlasStyle.CYAN);
            int glyphSize = AtlasStyle.dp(owner, Math.round(sizeDp * .52f));
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(glyphSize, glyphSize, Gravity.CENTER);
            holder.addView(add, params);
        } else {
            String initial = label == null || label.isEmpty() ? "?" :
                    label.substring(0, 1).toUpperCase(java.util.Locale.ROOT);
            TextView fallback = AtlasStyle.centered(owner, initial, 17, AtlasStyle.CYAN, true);
            holder.addView(fallback, new FrameLayout.LayoutParams(-1, -1));
        }
        return holder;
    }

    static void clearCache() {
        synchronized (BITMAPS) { BITMAPS.evictAll(); }
    }

}
