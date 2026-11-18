package tv.atlas.launcher;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Lists and opens the TV's passthrough sources, including HDMI inputs. */
final class TvInputCatalog {
    private TvInputCatalog() {}

    static List<Entry> installed(Context context) {
        Object service = context.getSystemService(Context.TV_INPUT_SERVICE);
        if (!(service instanceof TvInputManager)) return new ArrayList<>();
        List<Entry> result = new ArrayList<>();
        for (TvInputInfo info : ((TvInputManager) service).getTvInputList()) {
            if (!info.isPassthroughInput()) continue;
            CharSequence label = info.loadLabel(context);
            result.add(new Entry(label == null ? "Entrada de TV" : label.toString(), info.getId(), info));
        }
        result.sort(Comparator.comparing(entry -> entry.label.toLowerCase(java.util.Locale.ROOT)));
        return result;
    }

    static void launch(Context context, Entry input) {
        Uri channel = TvContract.buildChannelUriForPassthroughInput(input.id);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(channel, TvContract.Channels.CONTENT_ITEM_TYPE);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    static Drawable icon(Context context, Entry input) {
        return context.getDrawable(R.drawable.ic_tv_input);
    }

    static final class Entry {
        final String label;
        final String id;
        final TvInputInfo info;

        Entry(String label, String id, TvInputInfo info) {
            this.label = label;
            this.id = id;
            this.info = info;
        }
    }
}
