package tv.atlas.launcher;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Toast;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public final class HomeActivity extends Activity {
    private ClockWidget clock;
    private HeadlinesWidget headlines;
    private HomeAppsWidget homeApps;
    private AppsDrawerWidget drawer;
    private NetworkStatusWidget networkStatus;
    private AtlasBackdrop backdrop;
    private AtlasPreferences atlasPreferences;
    private boolean reorganizingApps;
    private String movingPackage;
    private List<String> draftPinOrder;
    private List<AppCatalog.Entry> apps;
    private List<TvInputCatalog.Entry> tvInputs;
    private FrameLayout screen;
    private final AtomicBoolean refreshing = new AtomicBoolean(false);

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildHome();
    }

    private void buildHome() {
        atlasPreferences = new AtlasPreferences(this);
        float density = getResources().getDisplayMetrics().density;
        float widthDp = getResources().getDisplayMetrics().widthPixels / density;
        float heightDp = getResources().getDisplayMetrics().heightPixels / density;
        screen = new FrameLayout(this);
        screen.setBackgroundColor(0xff081421);
        backdrop = new AtlasBackdrop(this);
        screen.addView(backdrop, new FrameLayout.LayoutParams(-1, -1));

        int side = AtlasStyle.dp(screen, widthDp * .042f);
        int top = AtlasStyle.dp(screen, heightDp * .05f);

        clock = new ClockWidget(this, heightDp);
        FrameLayout.LayoutParams clockParams = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.LEFT);
        clockParams.leftMargin = side;
        clockParams.topMargin = top;
        screen.addView(clock, clockParams);

        LinearLayout topActions = new LinearLayout(this);
        topActions.setOrientation(LinearLayout.HORIZONTAL);
        topActions.setGravity(Gravity.CENTER_VERTICAL);
        networkStatus = new NetworkStatusWidget(this);
        AtlasStyle.makeFocusable(networkStatus);
        topActions.addView(networkStatus, new LinearLayout.LayoutParams(-2, -2));

        LinearLayout settingsButton = new LinearLayout(this);
        settingsButton.setOrientation(LinearLayout.HORIZONTAL);
        settingsButton.setGravity(Gravity.CENTER);
        settingsButton.setPadding(AtlasStyle.dp(settingsButton, 16), AtlasStyle.dp(settingsButton, 10),
                AtlasStyle.dp(settingsButton, 18), AtlasStyle.dp(settingsButton, 10));
        AtlasStyle.makeFocusable(settingsButton);
        android.widget.ImageView gear = AtlasStyle.glyph(settingsButton, R.drawable.ic_atlas_settings);
        settingsButton.addView(gear, new LinearLayout.LayoutParams(AtlasStyle.dp(settingsButton, 30), -2));
        TextView settingsLabel = AtlasStyle.text(settingsButton, "Configurações", 14, AtlasStyle.WHITE, true);
        settingsLabel.setSingleLine(true);
        LinearLayout.LayoutParams settingsLabelParams = new LinearLayout.LayoutParams(-2, -2);
        settingsLabelParams.leftMargin = AtlasStyle.dp(settingsButton, 8);
        settingsButton.addView(settingsLabel, settingsLabelParams);
        settingsButton.setContentDescription("Abrir configurações da TV");
        settingsButton.setOnClickListener(view -> openSettings());
        LinearLayout.LayoutParams settingsButtonParams = new LinearLayout.LayoutParams(-2, -2);
        settingsButtonParams.leftMargin = AtlasStyle.dp(topActions, 12);
        topActions.addView(settingsButton, settingsButtonParams);

        FrameLayout.LayoutParams topActionsParams = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.RIGHT);
        topActionsParams.rightMargin = side;
        topActionsParams.topMargin = top;
        screen.addView(topActions, topActionsParams);

        headlines = new HeadlinesWidget(this, this::refreshHeadlines);
        int newsWidth = AtlasStyle.dp(screen, widthDp * .59f);
        FrameLayout.LayoutParams newsParams = new FrameLayout.LayoutParams(newsWidth, -2, Gravity.TOP | Gravity.RIGHT);
        newsParams.rightMargin = side;
        newsParams.topMargin = AtlasStyle.dp(screen, heightDp * .18f);
        screen.addView(headlines, newsParams);

        apps = AppCatalog.installed(this);
        tvInputs = TvInputCatalog.installed(this);
        addHomeApps(widthDp, heightDp);

        setContentView(screen);
        applyAtlasPreferences();
        applyImmersiveMode();
        homeApps.post(homeApps::focusFirst);
    }

    private void loadHeadlines(boolean force) {
        if (force && !refreshing.compareAndSet(false, true)) return;
        headlines.setUpdated(force ? "Atualizando fontes…" : "Atualização ao abrir");
        HeadlineStore.load(this, force, items -> {
            headlines.setHeadlines(items);
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
            headlines.setUpdated("Atualizado às " + time);
            refreshing.set(false);
        });
    }

    private void refreshHeadlines() { loadHeadlines(true); }

    private void openDrawer() {
        if (drawer == null) {
            drawer = new AppsDrawerWidget(this, apps, tvInputs, this::closeDrawer, this::launchApp,
                    this::showAppActions, this::launchInput, this::showAtlasSettings);
            screen.addView(drawer, new FrameLayout.LayoutParams(-1, -1));
        }
        drawer.setVisibility(View.VISIBLE);
        drawer.bringToFront();
        drawer.post(drawer::focusFirst);
    }

    private void addHomeApps(float widthDp, float heightDp) {
        List<String> pinOrder = draftPinOrder == null ? AppPins.get(this, apps) : new ArrayList<>(draftPinOrder);
        homeApps = new HomeAppsWidget(this, apps, pinOrder, reorganizingApps,
                this::openDrawer, this::launchApp, this::showHomeAppActions);
        int width = AtlasStyle.dp(screen, widthDp * .31f);
        FrameLayout.LayoutParams appsParams = new FrameLayout.LayoutParams(width,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT);
        appsParams.leftMargin = AtlasStyle.dp(screen, widthDp * .042f);
        appsParams.topMargin = AtlasStyle.dp(screen, heightDp * .22f);
        int position = drawer == null ? screen.getChildCount() : screen.indexOfChild(drawer);
        screen.addView(homeApps, Math.max(0, position), appsParams);
    }

    private void refreshHomeApps() {
        refreshHomeApps(null);
    }

    private void refreshHomeApps(String focusPackage) {
        if (screen == null) return;
        float widthDp = getResources().getDisplayMetrics().widthPixels /
                getResources().getDisplayMetrics().density;
        float heightDp = getResources().getDisplayMetrics().heightPixels /
                getResources().getDisplayMetrics().density;
        if (homeApps != null) screen.removeView(homeApps);
        addHomeApps(widthDp, heightDp);
        if (drawer == null) homeApps.post(() -> {
            if (focusPackage == null || !homeApps.focusPackage(focusPackage)) homeApps.focusFirst();
        });
    }

    private void openSettings() {
        try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
        catch (Exception ignored) {
            Toast.makeText(this, "Configurações indisponíveis", Toast.LENGTH_SHORT).show();
        }
    }

    private void openLauncherSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
        } catch (Exception unavailable) {
            Toast.makeText(this, "Seletor de launcher indisponível nas configurações da TV",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void showAtlasSettings() {
        Dialog dialog = new Dialog(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        int padding = AtlasStyle.dp(panel, 24);
        panel.setPadding(padding, padding, padding, padding);
        panel.setBackground(AtlasStyle.panel(0xf0101f2d, 0x65dce8f0, 20, panel));
        TextView title = AtlasStyle.text(panel, "Configurações do Atlas", 24, AtlasStyle.WHITE, true);
        panel.addView(title, new LinearLayout.LayoutParams(-1, AtlasStyle.dp(panel, 48)));
        ScrollView scroll = new ScrollView(this);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, AtlasStyle.dp(panel, 420)));
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setClipChildren(false);
        scroll.addView(rows);

        addSettingRow(rows, "Papel de parede", () -> AtlasPreferences.WALLPAPERS[atlasPreferences.wallpaper()], () -> {
            atlasPreferences.setWallpaper(atlasPreferences.wallpaper() + 1);
            backdrop.setPalette(atlasPreferences.wallpaper());
        });
        addSettingRow(rows, "Relógio", () -> enabledLabel(atlasPreferences.showClock()), () -> {
            atlasPreferences.setVisible("show_clock", !atlasPreferences.showClock());
            applyAtlasPreferences();
        });
        addSettingRow(rows, "Indicador de rede", () -> enabledLabel(atlasPreferences.showNetwork()), () -> {
            atlasPreferences.setVisible("show_network", !atlasPreferences.showNetwork());
            applyAtlasPreferences();
        });
        addSettingRow(rows, "Widget de notícias", () -> enabledLabel(atlasPreferences.showNews()), () -> {
            atlasPreferences.setVisible("show_news", !atlasPreferences.showNews());
            applyAtlasPreferences();
        });
        addSettingRow(rows, "Launcher padrão", () -> "Trocar", () -> {
            dialog.dismiss();
            openLauncherSettings();
        });
        addSectionLabel(rows, "FONTES DE NOTÍCIAS");
        for (int i = 0; i < AtlasPreferences.SOURCE_IDS.length; i++) {
            final String id = AtlasPreferences.SOURCE_IDS[i];
            final String name = AtlasPreferences.SOURCE_NAMES[i];
            addSettingRow(rows, name, () -> enabledLabel(atlasPreferences.sourceEnabled(id)), () -> {
                atlasPreferences.setSourceEnabled(id, !atlasPreferences.sourceEnabled(id));
                headlines.setSources(atlasPreferences);
                loadHeadlines(true);
            });
        }
        dialog.setContentView(panel);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = AtlasStyle.dp(panel, 620);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.CENTER;
            params.dimAmount = .68f;
            window.setAttributes(params);
        }
        dialog.show();
        if (window != null) window.setLayout(AtlasStyle.dp(panel, 620), WindowManager.LayoutParams.WRAP_CONTENT);
        rows.getChildAt(1).requestFocus();
    }

    private void addSettingRow(LinearLayout parent, String label,
                               java.util.function.Supplier<String> value, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AtlasStyle.dp(row, 14), 0, AtlasStyle.dp(row, 14), 0);
        AtlasStyle.makeFocusable(row);
        TextView name = AtlasStyle.text(row, label, 17, AtlasStyle.WHITE, true);
        TextView current = AtlasStyle.text(row, value.get(), 15, AtlasStyle.CYAN, true);
        row.addView(name, new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(current, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, AtlasStyle.dp(row, 50));
        params.topMargin = AtlasStyle.dp(row, 4);
        parent.addView(row, params);
        row.setOnClickListener(view -> {
            action.run();
            current.setText(value.get());
        });
    }

    private void addSectionLabel(LinearLayout parent, String label) {
        TextView heading = AtlasStyle.text(parent, label, 12, AtlasStyle.MUTED, true);
        heading.setLetterSpacing(.14f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, AtlasStyle.dp(parent, 36));
        params.topMargin = AtlasStyle.dp(parent, 6);
        parent.addView(heading, params);
    }

    private String enabledLabel(boolean enabled) { return enabled ? "Ativado" : "Desativado"; }

    private void applyAtlasPreferences() {
        if (clock != null) clock.setVisibility(atlasPreferences.showClock() ? View.VISIBLE : View.GONE);
        if (networkStatus != null) networkStatus.setVisibility(atlasPreferences.showNetwork() ? View.VISIBLE : View.GONE);
        if (headlines != null) {
            headlines.setVisibility(atlasPreferences.showNews() ? View.VISIBLE : View.GONE);
            headlines.setSources(atlasPreferences);
        }
        if (backdrop != null) backdrop.setPalette(atlasPreferences.wallpaper());
    }

    private void showAppActions(AppCatalog.Entry app) {
        boolean pinned = AppPins.isPinned(this, app.packageName, apps);
        Dialog dialog = new Dialog(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        int padding = AtlasStyle.dp(panel, 24);
        panel.setPadding(padding, padding, padding, padding);
        panel.setBackground(AtlasStyle.panel(0xf0101f2d, 0x65dce8f0, 20, panel));
        TextView title = AtlasStyle.text(panel, app.label, 24, AtlasStyle.WHITE, true);
        title.setMaxLines(1);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        panel.addView(title, new LinearLayout.LayoutParams(-1, AtlasStyle.dp(panel, 48)));

        addAppAction(panel, dialog, "Abrir", () -> launchApp(app));
        addAppAction(panel, dialog, pinned ? "Remover da tela inicial" : "Fixar na tela inicial", () -> {
            if (pinned) {
                AppPins.unpin(this, app.packageName, apps);
                refreshHomeApps();
                Toast.makeText(this, app.label + " removido da tela inicial", Toast.LENGTH_SHORT).show();
            } else if (AppPins.pin(this, app.packageName, apps)) {
                refreshHomeApps();
                Toast.makeText(this, app.label + " fixado na tela inicial", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Remova um app fixado antes de adicionar outro", Toast.LENGTH_LONG).show();
            }
        });
        addAppAction(panel, dialog, "Desinstalar", () -> requestUninstall(app));
        addAppAction(panel, dialog, "Cancelar", dialog::dismiss);

        dialog.setContentView(panel);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = AtlasStyle.dp(panel, 500);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.CENTER;
            params.dimAmount = .68f;
            window.setAttributes(params);
        }
        dialog.show();
        if (window != null) window.setLayout(AtlasStyle.dp(panel, 500), WindowManager.LayoutParams.WRAP_CONTENT);
        panel.getChildAt(1).requestFocus();
    }

    private void showHomeAppActions(AppCatalog.Entry app) {
        Dialog dialog = new Dialog(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        int padding = AtlasStyle.dp(panel, 22);
        panel.setPadding(padding, padding, padding, padding);
        panel.setBackground(AtlasStyle.panel(0xf0101f2d, 0x65dce8f0, 20, panel));
        TextView title = AtlasStyle.text(panel, "Organizar " + app.label, 22, AtlasStyle.WHITE, true);
        title.setMaxLines(1);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        panel.addView(title, new LinearLayout.LayoutParams(-1, AtlasStyle.dp(panel, 46)));
        addAppAction(panel, dialog, "Reorganizar", () -> startReorganization(app));
        addAppAction(panel, dialog, "Abrir", () -> launchApp(app));
        addAppAction(panel, dialog, "Remover da tela inicial", () -> {
            AppPins.unpin(this, app.packageName, apps);
            refreshHomeApps();
        });
        addAppAction(panel, dialog, "Cancelar", dialog::dismiss);
        dialog.setContentView(panel);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = AtlasStyle.dp(panel, 500);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.CENTER;
            params.dimAmount = .68f;
            window.setAttributes(params);
        }
        dialog.show();
        if (window != null) window.setLayout(AtlasStyle.dp(panel, 500), WindowManager.LayoutParams.WRAP_CONTENT);
        panel.getChildAt(1).requestFocus();
    }

    private void startReorganization(AppCatalog.Entry app) {
        draftPinOrder = new ArrayList<>(AppPins.get(this, apps));
        if (!draftPinOrder.contains(app.packageName)) {
            draftPinOrder = null;
            return;
        }
        movingPackage = app.packageName;
        reorganizingApps = true;
        refreshHomeApps(movingPackage);
    }

    private void finishReorganization(boolean commit) {
        String focus = movingPackage;
        if (commit && draftPinOrder != null) AppPins.setOrder(this, draftPinOrder);
        reorganizingApps = false;
        movingPackage = null;
        draftPinOrder = null;
        refreshHomeApps(focus);
        Toast.makeText(this, commit ? "Ordem dos apps salva" : "Reorganização cancelada",
                Toast.LENGTH_SHORT).show();
    }

    private void addAppAction(LinearLayout panel, Dialog dialog, String label, Runnable action) {
        TextView row = AtlasStyle.text(panel, label, 18, AtlasStyle.WHITE, false);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AtlasStyle.dp(panel, 14), 0, AtlasStyle.dp(panel, 14), 0);
        AtlasStyle.makeFocusable(row);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, AtlasStyle.dp(panel, 58));
        params.topMargin = AtlasStyle.dp(panel, 5);
        panel.addView(row, params);
        row.setOnClickListener(view -> {
            dialog.dismiss();
            action.run();
        });
    }

    private void requestUninstall(AppCatalog.Entry app) {
        try {
            closeDrawer();
            Intent intent = new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + app.packageName));
            startActivity(intent);
        } catch (Exception error) {
            Toast.makeText(this, "O Android não permite desinstalar " + app.label, Toast.LENGTH_LONG).show();
        }
    }

    private void closeDrawer() {
        if (drawer != null) {
            screen.removeView(drawer);
            drawer = null;
        }
        if (homeApps != null) homeApps.focusFirst();
    }

    private void launchApp(AppCatalog.Entry app) {
        try {
            closeDrawer();
            AppCatalog.launch(this, app);
        } catch (Exception ignored) {
            headlines.setUpdated("Não foi possível abrir " + app.label);
            closeDrawer();
        }
    }

    private void launchInput(TvInputCatalog.Entry input) {
        try {
            closeDrawer();
            TvInputCatalog.launch(this, input);
        } catch (Exception ignored) {
            headlines.setUpdated("Não foi possível abrir " + input.label);
            closeDrawer();
        }
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (reorganizingApps && event.getAction() == KeyEvent.ACTION_DOWN) {
            AppPins.Direction direction = null;
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_LEFT: direction = AppPins.Direction.LEFT; break;
                case KeyEvent.KEYCODE_DPAD_RIGHT: direction = AppPins.Direction.RIGHT; break;
                case KeyEvent.KEYCODE_DPAD_UP: direction = AppPins.Direction.UP; break;
                case KeyEvent.KEYCODE_DPAD_DOWN: direction = AppPins.Direction.DOWN; break;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
                case KeyEvent.KEYCODE_BUTTON_A:
                    finishReorganization(true);
                    return true;
                case KeyEvent.KEYCODE_BACK:
                    finishReorganization(false);
                    return true;
            }
            if (direction != null) {
                if (draftPinOrder != null && AppPins.moveInOrder(draftPinOrder, movingPackage, direction)) {
                    refreshHomeApps(movingPackage);
                }
                return true;
            }
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_BACK &&
                drawer != null && drawer.getVisibility() == View.VISIBLE) {
            closeDrawer();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override protected void onResume() {
        super.onResume();
        if (clock != null) clock.setRunning(true);
        if (networkStatus != null) networkStatus.setRunning(true);
        if (screen != null) {
            apps = AppCatalog.installed(this);
            tvInputs = TvInputCatalog.installed(this);
            refreshHomeApps();
        }
        if (headlines != null) loadHeadlines(false);
    }

    @Override protected void onPause() {
        if (clock != null) clock.setRunning(false);
        if (networkStatus != null) networkStatus.setRunning(false);
        super.onPause();
    }

    @Override public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_RUNNING_LOW) AtlasAppIcon.clearCache();
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyImmersiveMode();
    }

    private void applyImmersiveMode() {
        WindowInsetsController controller = getWindow().getInsetsController();
        if (controller == null) return;
        controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
        controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }
}
