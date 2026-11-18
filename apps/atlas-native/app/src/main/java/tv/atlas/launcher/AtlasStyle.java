package tv.atlas.launcher;

import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.BitmapShader;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

final class AtlasStyle {
    static final int WHITE = Color.rgb(244, 247, 250);
    static final int MUTED = Color.rgb(169, 186, 201);
    static final int CYAN = Color.rgb(121, 215, 208);
    static final int LAVENDER = Color.rgb(178, 189, 246);

    private AtlasStyle() {}

    static int dp(View view, float value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }

    static GradientDrawable panel(int fill, int stroke, float radius, View view) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(view, radius));
        shape.setStroke(dp(view, 1), stroke);
        return shape;
    }

    static Bitmap iconBitmap(Drawable drawable, int sizePx) {
        if (drawable == null) return null;
        android.graphics.Rect originalBounds = new android.graphics.Rect(drawable.getBounds());
        drawable = drawable.mutate();
        Bitmap source = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(source);
        int intrinsicWidth = drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : sizePx;
        int intrinsicHeight = drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : sizePx;
        float artworkSize = sizePx * .84f;
        float scale = Math.min(artworkSize / intrinsicWidth, artworkSize / intrinsicHeight);
        int width = Math.round(intrinsicWidth * scale);
        int height = Math.round(intrinsicHeight * scale);
        int left = (sizePx - width) / 2;
        int top = (sizePx - height) / 2;
        drawable.setBounds(left, top, left + width, top + height);
        drawable.draw(canvas);
        Bitmap rounded = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas roundedCanvas = new Canvas(rounded);
        Paint mask = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        mask.setShader(new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        RectF artwork = new RectF(left, top, left + width, top + height);
        float radius = Math.min(width, height) * .24f;
        roundedCanvas.drawRoundRect(artwork, radius, radius, mask);
        mask.setShader(null);
        source.recycle();
        drawable.setBounds(originalBounds);
        return rounded;
    }

    static TextView text(View owner, String value, float sizeSp, int color, boolean bold) {
        TextView text = new TextView(owner.getContext());
        text.setText(value);
        text.setTextColor(color);
        text.setTextSize(sizeSp);
        text.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        text.setIncludeFontPadding(false);
        return text;
    }

    static void makeFocusable(View view) {
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.setBackground(new android.graphics.drawable.StateListDrawable());
        GradientDrawable idle = panel(Color.argb(225, 17, 34, 48), Color.argb(55, 220, 232, 240), 14, view);
        GradientDrawable focused = panel(Color.argb(238, 220, 232, 240), Color.WHITE, 14, view);
        android.graphics.drawable.StateListDrawable states = new android.graphics.drawable.StateListDrawable();
        states.addState(new int[] {android.R.attr.state_focused}, focused);
        states.addState(new int[] {android.R.attr.state_pressed}, focused);
        states.addState(new int[] {}, idle);
        view.setBackground(states);
        view.setOnFocusChangeListener((focusedView, hasFocus) -> {
            if (focusedView instanceof android.view.ViewGroup) {
                ((android.view.ViewGroup) focusedView).setAlpha(hasFocus ? 1f : .96f);
            }
            focusedView.animate().cancel();
            focusedView.setScaleX(1f);
            focusedView.setScaleY(1f);
            focusedView.setElevation(hasFocus ? dp(focusedView, 5) : 0);
            setTextColor(focusedView, hasFocus ? 0xff102131 : WHITE);
            if (focusedView instanceof android.widget.ImageView) {
                ((android.widget.ImageView) focusedView).setColorFilter(hasFocus ? 0xff102131 : WHITE);
            }
        });
    }

    private static void setTextColor(View view, int color) {
        if (view instanceof TextView) ((TextView) view).setTextColor(color);
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) setTextColor(group.getChildAt(i), color);
        }
    }

    static TextView centered(View owner, String value, float sizeSp, int color, boolean bold) {
        TextView text = text(owner, value, sizeSp, color, bold);
        text.setGravity(Gravity.CENTER);
        return text;
    }

    static android.widget.ImageView glyph(View owner, int resource) {
        android.widget.ImageView image = new android.widget.ImageView(owner.getContext());
        image.setImageResource(resource);
        image.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        image.setColorFilter(WHITE);
        image.setFocusable(false);
        return image;
    }
}
