package tv.atlas.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.function.Consumer;

/** A compact two-column app board that shares the home screen with the news panel. */
final class HomeAppsWidget extends LinearLayout {
    private View firstApp;

    HomeAppsWidget(Context context, List<AppCatalog.Entry> apps, List<String> pinOrder, boolean reorganizing,
                   Runnable openDrawer,
                   Consumer<AppCatalog.Entry> launchApp, Consumer<AppCatalog.Entry> appActions) {
        super(context);
        setOrientation(VERTICAL);
        setClipChildren(false);
        setClipToPadding(false);

        TextView heading = AtlasStyle.text(this, reorganizing ? "Organizando apps" : "Meus apps",
                18, AtlasStyle.WHITE, true);
        LayoutParams headingParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        headingParams.bottomMargin = AtlasStyle.dp(this, 3);
        addView(heading, headingParams);
        TextView organizeHint = AtlasStyle.text(this,
                reorganizing ? "Setas movem · OK salva · Voltar cancela" : "Segure um app para organizar",
                11, reorganizing ? AtlasStyle.CYAN : AtlasStyle.MUTED, false);
        LayoutParams hintParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        hintParams.bottomMargin = AtlasStyle.dp(this, 7);
        addView(organizeHint, hintParams);

        GridLayout grid = new GridLayout(context);
        grid.setColumnCount(2);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        grid.setClipChildren(false);
        addView(grid, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        for (String packageName : pinOrder) {
            for (AppCatalog.Entry app : apps) {
                if (!app.packageName.equals(packageName)) continue;
                addTile(grid, app.packageName, app.label,
                        AtlasAppIcon.bitmap(context, app, AtlasStyle.dp(this, 44)),
                        () -> launchApp.accept(app), () -> appActions.accept(app));
                break;
            }
        }
        addTile(grid, null, "Biblioteca", null, openDrawer, null);
    }

    private void addTile(GridLayout grid, String packageName, String label, Bitmap bitmap,
                         Runnable action, Runnable longAction) {
        LinearLayout tile = new LinearLayout(getContext());
        tile.setOrientation(VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(AtlasStyle.dp(this, 7), AtlasStyle.dp(this, 8),
                AtlasStyle.dp(this, 7), AtlasStyle.dp(this, 7));
        AtlasStyle.makeFocusable(tile);
        tile.setContentDescription(label.equals("Biblioteca") ? "Abrir biblioteca de apps" :
                "Abrir " + label + ". Segure para organizar na tela inicial.");
        tile.setTag(packageName);
        tile.setOnClickListener(view -> action.run());
        if (longAction != null) tile.setOnLongClickListener(view -> {
            longAction.run();
            return true;
        });
        if (firstApp == null) firstApp = tile;

        int iconSize = AtlasStyle.dp(this, 44);
        tile.addView(AtlasAppIcon.create(this, packageName, label, bitmap, 44),
                new LayoutParams(iconSize, iconSize));

        TextView name = AtlasStyle.text(this, label, 13, AtlasStyle.WHITE, true);
        name.setMaxLines(2);
        name.setGravity(Gravity.CENTER);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LayoutParams nameParams = new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f);
        nameParams.topMargin = AtlasStyle.dp(this, 5);
        tile.addView(name, nameParams);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f));
        params.width = 0;
        params.height = AtlasStyle.dp(this, 91);
        params.setMargins(AtlasStyle.dp(this, 4), AtlasStyle.dp(this, 4),
                AtlasStyle.dp(this, 4), AtlasStyle.dp(this, 4));
        grid.addView(tile, params);
    }

    void focusFirst() {
        if (firstApp != null) firstApp.requestFocus();
    }

    boolean focusPackage(String packageName) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (!(child instanceof GridLayout)) continue;
            GridLayout grid = (GridLayout) child;
            for (int j = 0; j < grid.getChildCount(); j++) {
                View tile = grid.getChildAt(j);
                if (packageName.equals(tile.getTag())) return tile.requestFocus();
            }
        }
        return false;
    }
}
