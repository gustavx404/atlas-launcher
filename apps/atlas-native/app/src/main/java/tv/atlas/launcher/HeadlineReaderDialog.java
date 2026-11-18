package tv.atlas.launcher;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Reads a feed summary or article text in Atlas without switching to a browser. */
final class HeadlineReaderDialog {
    private HeadlineReaderDialog() {}

    static void show(Context context, Headline headline) {
        Dialog dialog = new Dialog(context);
        LinearLayout panel = new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(AtlasStyle.dp(panel, 30), AtlasStyle.dp(panel, 28),
                AtlasStyle.dp(panel, 30), AtlasStyle.dp(panel, 24));
        panel.setBackground(AtlasStyle.glassPanel(panel, 22));

        TextView category = AtlasStyle.text(panel,
                headline.category.toUpperCase(java.util.Locale.ROOT), 13,
                "Cyber".equals(headline.category) ? AtlasStyle.CYAN : AtlasStyle.LAVENDER, true);
        category.setLetterSpacing(.12f);
        panel.addView(category, new LinearLayout.LayoutParams(-1, -2));

        TextView title = AtlasStyle.text(panel, headline.title, 27, AtlasStyle.WHITE, true);
        title.setLineSpacing(AtlasStyle.dp(panel, 2), 1f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = AtlasStyle.dp(panel, 12);
        panel.addView(title, titleParams);

        TextView source = AtlasStyle.text(panel, headline.source.toUpperCase(java.util.Locale.ROOT),
                13, AtlasStyle.MUTED, true);
        source.setLetterSpacing(.08f);
        LinearLayout.LayoutParams sourceParams = new LinearLayout.LayoutParams(-1, -2);
        sourceParams.topMargin = AtlasStyle.dp(panel, 10);
        panel.addView(source, sourceParams);

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(android.view.View.OVER_SCROLL_NEVER);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, 0, 1f);
        scrollParams.topMargin = AtlasStyle.dp(panel, 18);
        scrollParams.bottomMargin = AtlasStyle.dp(panel, 12);
        panel.addView(scroll, scrollParams);

        TextView body = AtlasStyle.text(panel, initialText(headline), 20, 0xffe7eef5, false);
        body.setLineSpacing(AtlasStyle.dp(panel, 5), 1.06f);
        body.setTextIsSelectable(false);
        scroll.addView(body, new ScrollView.LayoutParams(-1, -2));

        LinearLayout actions = new LinearLayout(context);
        actions.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        TextView firstAction = null;
        if (!headline.url.isEmpty()) {
            TextView load = action(panel, "Carregar texto da matéria", false);
            firstAction = load;
            actions.addView(load, actionParams(panel));
            load.setOnClickListener(view -> {
                load.setText("Carregando matéria…");
                load.setEnabled(false);
                HeadlineStore.loadArticle(headline, text -> {
                    if (!dialog.isShowing()) return;
                    body.setText(text == null || text.trim().isEmpty()
                            ? fallbackText(headline) : text);
                    scroll.scrollTo(0, 0);
                    load.setText("Texto carregado");
                    load.setEnabled(true);
                });
            });
        }
        TextView close = action(panel, "Fechar", true);
        LinearLayout.LayoutParams closeParams = actionParams(panel);
        closeParams.leftMargin = AtlasStyle.dp(panel, 10);
        actions.addView(close, closeParams);
        close.setOnClickListener(view -> dialog.dismiss());
        panel.addView(actions, new LinearLayout.LayoutParams(-1, AtlasStyle.dp(panel, 58)));

        dialog.setContentView(panel);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = AtlasStyle.dp(panel, 780);
            params.height = AtlasStyle.dp(panel, 470);
            params.gravity = Gravity.CENTER;
            params.dimAmount = .76f;
            window.setAttributes(params);
        }
        dialog.show();
        if (window != null) window.setLayout(AtlasStyle.dp(panel, 780), AtlasStyle.dp(panel, 470));
        (firstAction == null ? close : firstAction).requestFocus();
    }

    private static String initialText(Headline headline) {
        return headline.summary.isEmpty()
                ? "A fonte não incluiu um resumo no feed. Você pode carregar o texto da matéria aqui."
                : headline.summary;
    }

    private static String fallbackText(Headline headline) {
        return headline.summary.isEmpty()
                ? "Não foi possível extrair o texto completo desta página. Confira a conexão e tente novamente."
                : headline.summary + "\n\nNão foi possível carregar o texto completo agora.";
    }

    private static TextView action(LinearLayout owner, String label, boolean primary) {
        TextView button = AtlasStyle.text(owner, label, 15,
                primary ? AtlasStyle.WHITE : AtlasStyle.CYAN, true);
        button.setGravity(Gravity.CENTER);
        button.setPadding(AtlasStyle.dp(owner, 18), 0, AtlasStyle.dp(owner, 18), 0);
        AtlasStyle.makeFocusable(button);
        return button;
    }

    private static LinearLayout.LayoutParams actionParams(LinearLayout owner) {
        return new LinearLayout.LayoutParams(-2, AtlasStyle.dp(owner, 52));
    }
}
