package tv.atlas.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/** A static shader backdrop avoids loading and decoding a full-screen wallpaper bitmap. */
final class AtlasBackdrop extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int palette;
    private Shader base;
    private Shader glow;

    AtlasBackdrop(Context context) {
        super(context);
        setWillNotDraw(false);
        palette = new AtlasPreferences(context).wallpaper();
    }

    void setPalette(int value) {
        palette = Math.floorMod(value, AtlasPreferences.WALLPAPERS.length);
        rebuildShaders(getWidth(), getHeight());
        invalidate();
    }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        rebuildShaders(width, height);
    }

    private void rebuildShaders(int width, int height) {
        if (width <= 0 || height <= 0) return;
        int[] colors;
        int[] glowColors;
        if (palette == 1) {
            colors = new int[] {0xff071a21, 0xff103446, 0xff182c4a};
            glowColors = new int[] {0x8b29a99b, 0x45425f88, 0x00253b56};
        } else if (palette == 2) {
            colors = new int[] {0xff130f22, 0xff282344, 0xff252c50};
            glowColors = new int[] {0x8b8e69c5, 0x454c4d85, 0x00253b56};
        } else {
            colors = new int[] {0xff07111d, 0xff11243b, 0xff172641};
            glowColors = new int[] {0x8b7477a8, 0x45424d75, 0x00253b56};
        }
        base = new LinearGradient(0, 0, width, height, colors, null, Shader.TileMode.CLAMP);
        glow = new RadialGradient(width * .95f, height * .82f, Math.max(width, height) * .62f,
                glowColors, null, Shader.TileMode.CLAMP);
    }

    @Override protected void onDraw(Canvas canvas) {
        paint.setShader(base);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setShader(glow);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setShader(null);
    }
}
