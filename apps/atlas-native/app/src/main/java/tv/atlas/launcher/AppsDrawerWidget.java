package tv.atlas.launcher;

import android.content.Context;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;
import java.util.ArrayList;

/** Full-screen, D-pad-first app grid, kept out of the view tree until requested. */
final class AppsDrawerWidget extends FrameLayout {
    private View firstApp;
    private View firstInput;
    private View closeButton;
    private View atlasSettingsButton;
    private final List<View> appTiles = new ArrayList<>();
    private final List<View> inputTiles = new ArrayList<>();

    AppsDrawerWidget(Context context, List<AppCatalog.Entry> apps, List<TvInputCatalog.Entry> inputs,
                     Runnable close, java.util.function.Consumer<AppCatalog.Entry> launchApp,
                     java.util.function.Consumer<AppCatalog.Entry> appActions,
                     java.util.function.Consumer<TvInputCatalog.Entry> launchInput,
                     Runnable openAtlasSettings) {
        super(context);
        setFocusable(true);
        setBackgroundColor(0xff081421);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setClipChildren(false);
        int side = AtlasStyle.dp(this, 64);
        content.setPadding(side, AtlasStyle.dp(this, 36), side, AtlasStyle.dp(this, 16));
        addView(content, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        LinearLayout heading = new LinearLayout(context);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = AtlasStyle.text(this, "Aplicativos", 30, AtlasStyle.WHITE, true);
        LinearLayout titleBlock = new LinearLayout(context);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        titleBlock.addView(title);
        TextView count = AtlasStyle.text(this, apps.size() + " apps · " + inputs.size() + " entradas", 13, AtlasStyle.MUTED, false);
        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        countParams.topMargin = AtlasStyle.dp(this, 6);
        titleBlock.addView(count, countParams);
        heading.addView(titleBlock, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout headingActions = new LinearLayout(context);
        headingActions.setGravity(Gravity.CENTER_VERTICAL);
        android.widget.ImageView atlasSettings = AtlasStyle.glyph(this, R.drawable.ic_atlas_settings);
        atlasSettings.setPadding(AtlasStyle.dp(this, 14), AtlasStyle.dp(this, 14),
                AtlasStyle.dp(this, 14), AtlasStyle.dp(this, 14));
        AtlasStyle.makeFocusable(atlasSettings);
        atlasSettings.setContentDescription("Configurações do Atlas");
        atlasSettings.setOnClickListener(view -> openAtlasSettings.run());
        atlasSettingsButton = atlasSettings;
        atlasSettings.setOnKeyListener((view, keyCode, event) -> handleHeaderKey(view, keyCode, event));
        int actionSize = AtlasStyle.dp(this, 54);
        headingActions.addView(atlasSettings, new LinearLayout.LayoutParams(actionSize, actionSize));
        android.widget.ImageView closeButton = AtlasStyle.glyph(this, R.drawable.ic_atlas_close);
        closeButton.setPadding(AtlasStyle.dp(this, 14), AtlasStyle.dp(this, 14),
                AtlasStyle.dp(this, 14), AtlasStyle.dp(this, 14));
        this.closeButton = closeButton;
        AtlasStyle.makeFocusable(closeButton);
        closeButton.setContentDescription("Fechar aplicativos");
        closeButton.setOnClickListener(view -> close.run());
        closeButton.setOnKeyListener((view, keyCode, event) -> handleHeaderKey(view, keyCode, event));
        int closeSize = AtlasStyle.dp(this, 54);
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(closeSize, closeSize);
        closeParams.leftMargin = AtlasStyle.dp(this, 8);
        headingActions.addView(closeButton, closeParams);
        heading.addView(headingActions);
        content.addView(heading, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        if (!inputs.isEmpty()) {
            addSectionLabel(content, "ENTRADAS DA TV");
            GridLayout inputGrid = newGrid(context);
            content.addView(inputGrid, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            for (TvInputCatalog.Entry input : inputs) {
                View tile = addTile(inputGrid, input.label, input.id,
                        AtlasStyle.iconBitmap(TvInputCatalog.icon(context, input), AtlasStyle.dp(this, 44)),
                        () -> launchInput.accept(input), null);
                inputTiles.add(tile);
                if (firstInput == null) firstInput = tile;
            }
            bindGridKeys(inputTiles, true);
            View divider = new View(context);
            divider.setBackgroundColor(0x35dce8f0);
            LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, AtlasStyle.dp(this, 1));
            dividerParams.topMargin = AtlasStyle.dp(this, 8);
            content.addView(divider, dividerParams);
        }

        ScrollView scroll = new ScrollView(context);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setClipChildren(false);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f);
        scrollParams.topMargin = AtlasStyle.dp(this, 6);
        content.addView(scroll, scrollParams);

        LinearLayout listing = new LinearLayout(context);
        listing.setOrientation(LinearLayout.VERTICAL);
        listing.setClipChildren(false);
        scroll.addView(listing, new ScrollView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        addSectionLabel(listing, "APLICATIVOS");
        GridLayout grid = newGrid(context);
        listing.addView(grid, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        for (AppCatalog.Entry app : apps) {
            View tile = addTile(grid, app.label, app.packageName,
                    AtlasAppIcon.bitmap(context, app, AtlasStyle.dp(this, 44)),
                    () -> launchApp.accept(app), () -> appActions.accept(app));
            appTiles.add(tile);
            if (firstApp == null) firstApp = tile;
        }
        bindGridKeys(appTiles, false);
        if (apps.isEmpty()) {
            TextView empty = AtlasStyle.text(this, "Nenhum app com atalho de inicialização foi encontrado.", 16, AtlasStyle.MUTED, false);
            listing.addView(empty);
        }

    }

    private GridLayout newGrid(Context context) {
        GridLayout grid = new GridLayout(context);
        grid.setColumnCount(4);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        grid.setClipChildren(false);
        return grid;
    }

    private void addSectionLabel(LinearLayout parent, String label) {
        TextView heading = AtlasStyle.text(this, label, 11, AtlasStyle.MUTED, true);
        heading.setLetterSpacing(.16f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.topMargin = AtlasStyle.dp(this, 6);
        params.bottomMargin = AtlasStyle.dp(this, 5);
        parent.addView(heading, params);
    }

    private View addTile(GridLayout grid, String label, String iconKey, android.graphics.Bitmap iconBitmap,
                         Runnable action, Runnable longAction) {
        LinearLayout tile = new LinearLayout(getContext());
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setOrientation(LinearLayout.HORIZONTAL);
        tile.setPadding(AtlasStyle.dp(this, 12), AtlasStyle.dp(this, 9), AtlasStyle.dp(this, 10), AtlasStyle.dp(this, 9));
        AtlasStyle.makeFocusable(tile);
        tile.setContentDescription("Abrir " + label);
        tile.setOnClickListener(view -> action.run());
        if (longAction != null) tile.setOnLongClickListener(view -> {
            longAction.run();
            return true;
        });

        int iconSize = AtlasStyle.dp(this, 44);
        tile.addView(AtlasAppIcon.create(this, iconKey, label, iconBitmap, 44),
                new LinearLayout.LayoutParams(iconSize, iconSize));

        TextView name = AtlasStyle.text(this, label, 15, AtlasStyle.WHITE, true);
        name.setMaxLines(2);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        nameParams.leftMargin = AtlasStyle.dp(this, 10);
        tile.addView(name, nameParams);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f));
        params.width = 0;
        params.height = AtlasStyle.dp(this, 72);
        params.setMargins(AtlasStyle.dp(this, 5), AtlasStyle.dp(this, 5), AtlasStyle.dp(this, 5), AtlasStyle.dp(this, 5));
        grid.addView(tile, params);
        return tile;
    }

    void focusFirst() {
        if (firstApp != null) firstApp.requestFocus();
        else if (firstInput != null) firstInput.requestFocus();
        else if (closeButton != null) closeButton.requestFocus();
    }

    @Override public View focusSearch(View focused, int direction) {
        View next = super.focusSearch(focused, direction);
        if (next == null || !contains(next)) {
            if (direction == View.FOCUS_UP && closeButton != null && focused != closeButton) return closeButton;
            return focused;
        }
        return next;
    }

    private boolean contains(View candidate) {
        android.view.ViewParent parent = candidate.getParent();
        while (parent instanceof View) {
            if (parent == this) return true;
            parent = parent.getParent();
        }
        return false;
    }

    private void bindGridKeys(List<View> tiles, boolean inputs) {
        for (View tile : tiles) {
            tile.setOnKeyListener((view, keyCode, event) -> {
                if (!isDirectionKey(keyCode)) return false;
                if (event.getAction() == KeyEvent.ACTION_DOWN) moveFocus(tile, tiles, inputs, keyCode);
                return true;
            });
        }
    }

    private void moveFocus(View current, List<View> rowTiles, boolean inputs, int keyCode) {
        int index = rowTiles.indexOf(current);
        if (index < 0 || rowTiles.isEmpty()) return;
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            int rowStart = index / 4 * 4;
            int rowEnd = Math.min(rowStart + 4, rowTiles.size()) - 1;
            int target = keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                    ? (index == rowStart ? rowEnd : index - 1)
                    : (index == rowEnd ? rowStart : index + 1);
            rowTiles.get(target).requestFocus();
            return;
        }

        if (inputs) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                closeButton.requestFocus();
            } else if (!appTiles.isEmpty()) {
                appTiles.get(Math.min(index, Math.min(3, appTiles.size() - 1))).requestFocus();
            }
            return;
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            if (index >= 4) {
                rowTiles.get(index - 4).requestFocus();
            } else if (!inputTiles.isEmpty()) {
                inputTiles.get(Math.min(index, inputTiles.size() - 1)).requestFocus();
            } else {
                closeButton.requestFocus();
            }
        } else if (index + 4 < rowTiles.size()) {
            rowTiles.get(index + 4).requestFocus();
        } else {
            int column = index % 4;
            rowTiles.get(Math.min(column, rowTiles.size() - 1)).requestFocus();
        }
    }

    private boolean handleHeaderKey(View focused, int keyCode, KeyEvent event) {
        if (!isDirectionKey(keyCode)) return false;
        if (event.getAction() != KeyEvent.ACTION_DOWN) return true;
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) return true;
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (!inputTiles.isEmpty()) inputTiles.get(0).requestFocus();
            else if (firstApp != null) firstApp.requestFocus();
            return true;
        }
        if (focused == closeButton && keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            atlasSettingsButton.requestFocus();
            return true;
        }
        if (focused == atlasSettingsButton && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            closeButton.requestFocus();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) return true;
        return true;
    }

    private boolean isDirectionKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN;
    }
}
