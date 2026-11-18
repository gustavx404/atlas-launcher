package tv.atlas.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Xml;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** Keeps the feed/cache policy behind one small load(context, force, callback) interface. */
final class HeadlineStore {
    private static final long CACHE_MILLIS = 30 * 60 * 1000L;
    private static final int MAX_BYTES = 256 * 1024;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "atlas-feeds");
        thread.setDaemon(true);
        return thread;
    });

    private static final Feed[] FEEDS = {
        new Feed("certbr", "CERT.br", "Cyber", "https://cert.br/rss/certbr-rss.xml", false),
        new Feed("cisa", "CISA", "Cyber", "https://www.cisa.gov/cybersecurity-advisories/all.xml", false),
        new Feed("bbcmundo", "BBC Mundo", "Geopolítica", "https://feeds.bbci.co.uk/portuguese/rss.xml", true),
        new Feed("dwbrasil", "DW Brasil", "Geopolítica", "https://rss.dw.com/rdf/rss-br-br", true)
    };

    private HeadlineStore() {}

    static void load(Context context, boolean force, Consumer<List<Headline>> callback) {
        Context app = context.getApplicationContext();
        AtlasPreferences settings = new AtlasPreferences(app);
        List<Headline> cached = filterEnabled(readCache(app), settings);
        if (!force || cached.isEmpty()) MAIN.post(() -> callback.accept(cached));
        if (!force && !cached.isEmpty() &&
                System.currentTimeMillis() - preferences(app).getLong("updated", 0) < CACHE_MILLIS) return;
        IO.execute(() -> {
            List<Headline> fresh = fetch(settings);
            if (!fresh.isEmpty() || !anySourceEnabled(settings)) writeCache(app, fresh);
            MAIN.post(() -> callback.accept(fresh.isEmpty() && anySourceEnabled(settings) ? cached : fresh));
        });
    }

    private static boolean anySourceEnabled(AtlasPreferences settings) {
        for (String id : AtlasPreferences.SOURCE_IDS) if (settings.sourceEnabled(id)) return true;
        return false;
    }

    private static List<Headline> filterEnabled(List<Headline> items, AtlasPreferences settings) {
        List<Headline> visible = new ArrayList<>(4);
        for (Headline item : items) {
            for (Feed feed : FEEDS) {
                if (feed.source.equals(item.source) && settings.sourceEnabled(feed.id)) {
                    visible.add(item);
                    break;
                }
            }
        }
        return visible;
    }

    private static List<Headline> fetch(AtlasPreferences preferences) {
        List<Headline> result = new ArrayList<>(4);
        for (Feed feed : FEEDS) {
            if (!preferences.sourceEnabled(feed.id)) continue;
            int categoryCount = countCategory(result, feed.category);
            if (categoryCount >= 2) continue;
            for (String title : readTitles(feed.url)) {
                if (title.length() < 8 || (feed.geopolitical && !isGeopolitical(title))) continue;
                result.add(new Headline(title, feed.source, feed.category));
                if (countCategory(result, feed.category) >= 2) break;
            }
        }
        return result;
    }

    private static int countCategory(List<Headline> items, String category) {
        int count = 0;
        for (Headline item : items) if (item.category.equals(category)) count++;
        return count;
    }

    private static List<String> readTitles(String address) {
        HttpURLConnection connection = null;
        List<String> titles = new ArrayList<>(8);
        try {
            connection = (HttpURLConnection) new URL(address).openConnection();
            connection.setConnectTimeout(4000);
            connection.setReadTimeout(4000);
            connection.setRequestProperty("User-Agent", "AtlasTV/0.2 AndroidTV");
            connection.setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml");
            connection.connect();
            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) return titles;

            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(new BoundedInputStream(connection.getInputStream(), MAX_BYTES), null);
            int event = parser.getEventType();
            boolean insideItem = false;
            while (event != XmlPullParser.END_DOCUMENT && titles.size() < 8) {
                if (event == XmlPullParser.START_TAG) {
                    String tag = parser.getName();
                    if ("item".equals(tag) || "entry".equals(tag)) insideItem = true;
                    else if (insideItem && "title".equals(tag)) {
                        String title = parser.nextText().trim();
                        if (!title.isEmpty()) titles.add(title);
                    }
                } else if (event == XmlPullParser.END_TAG &&
                        ("item".equals(parser.getName()) || "entry".equals(parser.getName()))) {
                    insideItem = false;
                }
                event = parser.next();
            }
        } catch (Exception ignored) {
            // A failed feed leaves the last good cache available to the UI.
        } finally {
            if (connection != null) connection.disconnect();
        }
        return titles;
    }

    private static boolean isGeopolitical(String title) {
        String normalized = title.toLowerCase(java.util.Locale.ROOT);
        return Arrays.asList("guerra", "conflito", "ataque", "invas", "míssil", "militar",
                "israel", "gaza", "hamas", "ucrânia", "rússia", "china", "irã", "taiwan",
                "otan", "sanção", "diplomacia", "governo", "presidente", "parlamento",
                "eleição", "cúpula", "tratado", "fronteira", "refugiad", "onu")
                .stream().anyMatch(normalized::contains);
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences("atlas-headlines", Context.MODE_PRIVATE);
    }

    private static List<Headline> readCache(Context context) {
        List<Headline> items = new ArrayList<>(4);
        try {
            JSONArray rows = new JSONArray(preferences(context).getString("items", "[]"));
            for (int i = 0; i < rows.length() && i < 4; i++) {
                JSONObject row = rows.getJSONObject(i);
                items.add(new Headline(row.getString("title"), row.getString("source"), row.getString("category")));
            }
        } catch (Exception ignored) { }
        return items;
    }

    private static void writeCache(Context context, List<Headline> items) {
        JSONArray rows = new JSONArray();
        for (Headline item : items) {
            JSONObject row = new JSONObject();
            try {
                row.put("title", item.title);
                row.put("source", item.source);
                row.put("category", item.category);
                rows.put(row);
            } catch (Exception ignored) { }
        }
        preferences(context).edit().putString("items", rows.toString())
                .putLong("updated", System.currentTimeMillis()).apply();
    }

    private static final class Feed {
        final String id, source, category, url;
        final boolean geopolitical;
        Feed(String id, String source, String category, String url, boolean geopolitical) {
            this.id = id; this.source = source; this.category = category; this.url = url; this.geopolitical = geopolitical;
        }
    }

    private static final class BoundedInputStream extends FilterInputStream {
        private int remaining;
        BoundedInputStream(InputStream input, int limit) { super(input); remaining = limit; }
        @Override public int read() throws IOException {
            if (remaining <= 0) return -1;
            int value = super.read();
            if (value >= 0) remaining--;
            return value;
        }
        @Override public int read(byte[] bytes, int offset, int length) throws IOException {
            if (remaining <= 0) return -1;
            int read = super.read(bytes, offset, Math.min(length, remaining));
            if (read > 0) remaining -= read;
            return read;
        }
    }
}
