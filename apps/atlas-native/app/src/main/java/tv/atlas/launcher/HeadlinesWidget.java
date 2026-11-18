package tv.atlas.launcher;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Two-column editorial widget with one readable lead story per topic. */
final class HeadlinesWidget extends LinearLayout {
    private final LinearLayout cyberStories;
    private final LinearLayout worldStories;
    private final TextView updated;

    HeadlinesWidget(Context context, Runnable refresh) {
        super(context);
        setOrientation(VERTICAL);
        int pad = AtlasStyle.dp(this, 18);
        setPadding(pad, pad, pad, AtlasStyle.dp(this, 12));
        setBackground(AtlasStyle.panel(0xe90a1725, 0x4adbe7f0, 18, this));

        LinearLayout heading = new LinearLayout(context);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.setOrientation(HORIZONTAL);
        LinearLayout titleBlock = new LinearLayout(context);
        titleBlock.setOrientation(VERTICAL);
        TextView title = AtlasStyle.text(this, "Radar", 27, AtlasStyle.WHITE, true);
        TextView subtitle = AtlasStyle.text(this, "Segurança digital · Cenário global", 15, AtlasStyle.MUTED, false);
        LayoutParams subtitleParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = AtlasStyle.dp(this, 4);
        titleBlock.addView(title);
        titleBlock.addView(subtitle, subtitleParams);
        android.widget.ImageView button = AtlasStyle.glyph(this, R.drawable.ic_atlas_refresh);
        button.setPadding(AtlasStyle.dp(this, 14), AtlasStyle.dp(this, 14),
                AtlasStyle.dp(this, 14), AtlasStyle.dp(this, 14));
        AtlasStyle.makeFocusable(button);
        button.setContentDescription("Atualizar manchetes");
        button.setOnClickListener(view -> refresh.run());
        int buttonSize = AtlasStyle.dp(this, 52);
        LayoutParams titleParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        heading.addView(titleBlock, titleParams);
        heading.addView(button, new LayoutParams(buttonSize, buttonSize));
        addView(heading, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        LinearLayout columns = new LinearLayout(context);
        columns.setOrientation(HORIZONTAL);
        LayoutParams columnsParams = new LayoutParams(LayoutParams.MATCH_PARENT, AtlasStyle.dp(this, 240));
        columnsParams.topMargin = AtlasStyle.dp(this, 14);
        addView(columns, columnsParams);
        cyberStories = createColumn(columns, "SEGURANÇA DIGITAL", AtlasStyle.CYAN);
        worldStories = createColumn(columns, "CENÁRIO GLOBAL", AtlasStyle.LAVENDER);

        LinearLayout footer = new LinearLayout(context);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        updated = AtlasStyle.text(this, "Atualização ao abrir", 13, AtlasStyle.MUTED, false);
        TextView sources = AtlasStyle.text(this, "", 13, AtlasStyle.MUTED, false);
        footer.addView(updated, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        footer.addView(sources, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        LayoutParams footerParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        footerParams.topMargin = AtlasStyle.dp(this, 12);
        addView(footer, footerParams);

        setHeadlines(new ArrayList<>());
    }

    private LinearLayout createColumn(LinearLayout parent, String title, int accent) {
        LinearLayout column = new LinearLayout(getContext());
        column.setOrientation(VERTICAL);
        column.setPadding(AtlasStyle.dp(this, 16), AtlasStyle.dp(this, 14),
                AtlasStyle.dp(this, 16), AtlasStyle.dp(this, 8));
        column.setBackground(AtlasStyle.panel(0xef07131f, 0x39dce8f0, 13, this));
        TextView label = AtlasStyle.text(this, "●  " + title, 15, accent, true);
        label.setLetterSpacing(.08f);
        column.addView(label, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        LinearLayout stories = new LinearLayout(getContext());
        stories.setOrientation(VERTICAL);
        LayoutParams storiesParams = new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f);
        storiesParams.topMargin = AtlasStyle.dp(this, 8);
        column.addView(stories, storiesParams);
        LayoutParams columnParams = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
        if (parent.getChildCount() > 0) columnParams.leftMargin = AtlasStyle.dp(this, 12);
        parent.addView(column, columnParams);
        return stories;
    }

    void setHeadlines(List<Headline> headlines) {
        ArrayList<Headline> cyber = new ArrayList<>(), world = new ArrayList<>();
        for (Headline item : headlines) {
            ArrayList<Headline> target = "Cyber".equals(item.category) ? cyber : world;
            if (target.isEmpty()) target.add(item);
        }
        populate(cyberStories, cyber);
        populate(worldStories, world);
    }

    void setUpdated(String value) { updated.setText(value); }

    void setSources(AtlasPreferences preferences) {
        StringBuilder labels = new StringBuilder();
        for (int i = 0; i < AtlasPreferences.SOURCE_IDS.length; i++) {
            if (!preferences.sourceEnabled(AtlasPreferences.SOURCE_IDS[i])) continue;
            if (labels.length() > 0) labels.append(" · ");
            labels.append(AtlasPreferences.SOURCE_NAMES[i]);
        }
        LinearLayout footer = (LinearLayout) getChildAt(getChildCount() - 1);
        View sourceView = footer.getChildAt(1);
        if (sourceView instanceof TextView) ((TextView) sourceView).setText(labels.length() == 0 ? "Fontes desativadas" : labels.toString());
    }

    private void populate(LinearLayout target, List<Headline> items) {
        target.removeAllViews();
        if (items.isEmpty()) {
            TextView empty = AtlasStyle.text(this, "Manchetes indisponíveis", 15, AtlasStyle.MUTED, false);
            target.addView(empty, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            Headline item = items.get(i);
            LinearLayout story = new LinearLayout(getContext());
            story.setOrientation(VERTICAL);
            story.setPadding(0, AtlasStyle.dp(this, 5), 0, AtlasStyle.dp(this, 7));
            TextView title = AtlasStyle.text(this, item.title, 18, AtlasStyle.WHITE, true);
            title.setMaxLines(5);
            title.setEllipsize(android.text.TextUtils.TruncateAt.END);
            title.setLineSpacing(AtlasStyle.dp(this, 2), 1f);
            story.addView(title, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            TextView source = AtlasStyle.text(this, item.source.toUpperCase(java.util.Locale.ROOT), 13, AtlasStyle.MUTED, true);
            source.setLetterSpacing(.08f);
            LayoutParams sourceParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            sourceParams.topMargin = AtlasStyle.dp(this, 5);
            story.addView(source, sourceParams);
            if (i > 0) {
                View divider = new View(getContext());
                divider.setBackgroundColor(Color.argb(30, 220, 232, 240));
                LayoutParams lineParams = new LayoutParams(LayoutParams.MATCH_PARENT, AtlasStyle.dp(this, 1));
                lineParams.topMargin = AtlasStyle.dp(this, 3);
                target.addView(divider, lineParams);
            }
            target.addView(story, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        }
    }
}
