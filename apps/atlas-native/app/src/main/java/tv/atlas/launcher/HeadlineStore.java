package tv.atlas.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.Xml;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import java.io.FilterInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Keeps the feed/cache policy behind one small load(context, force, callback) interface. */
final class HeadlineStore {
    private static final long CACHE_MILLIS = 30 * 60 * 1000L;
    private static final int MAX_BYTES = 256 * 1024;
    private static final int MAX_ARTICLE_BYTES = 768 * 1024;
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

    /** Loads readable article text in the background; no browser or WebView is opened. */
    static void loadArticle(Headline headline, Consumer<String> callback) {
        IO.execute(() -> {
            String text = headline.url.isEmpty() ? "" : fetchArticleText(headline.url);
            if (text.isEmpty()) text = headline.summary;
            final String result = text;
            MAIN.post(() -> callback.accept(result));
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
            for (Headline story : readStories(feed)) {
                if (story.title.length() < 8 ||
                        (feed.geopolitical && !isGeopolitical(story.title))) continue;
                result.add(story);
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

    private static List<Headline> readStories(Feed feed) {
        HttpURLConnection connection = null;
        List<Headline> stories = new ArrayList<>(8);
        try {
            connection = (HttpURLConnection) new URL(feed.url).openConnection();
            connection.setConnectTimeout(4000);
            connection.setReadTimeout(4000);
            connection.setRequestProperty("User-Agent", "AtlasTV/0.2 AndroidTV");
            connection.setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml");
            connection.connect();
            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) return stories;

            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(new BoundedInputStream(connection.getInputStream(), MAX_BYTES), null);
            int event = parser.getEventType();
            boolean insideItem = false;
            String title = "", summary = "", url = "";
            while (event != XmlPullParser.END_DOCUMENT && stories.size() < 8) {
                if (event == XmlPullParser.START_TAG) {
                    String tag = parser.getName();
                    if ("item".equals(tag) || "entry".equals(tag)) {
                        insideItem = true;
                        title = "";
                        summary = "";
                        url = "";
                    } else if (insideItem && "title".equals(tag)) {
                        title = cleanFeedText(readTagText(parser));
                    } else if (insideItem && ("description".equals(tag) || "summary".equals(tag) ||
                            "content".equals(tag) || "encoded".equals(tag))) {
                        String value = cleanFeedText(readTagText(parser));
                        if (value.length() > summary.length()) summary = value;
                    } else if (insideItem && "link".equals(tag)) {
                        String href = parser.getAttributeValue(null, "href");
                        String relation = parser.getAttributeValue(null, "rel");
                        if (href != null && !href.isEmpty()) {
                            if (url.isEmpty() || "alternate".equalsIgnoreCase(relation)) url = href.trim();
                        } else {
                            String text = readTagText(parser).trim();
                            if (!text.isEmpty()) url = text;
                        }
                    }
                } else if (event == XmlPullParser.END_TAG &&
                        ("item".equals(parser.getName()) || "entry".equals(parser.getName()))) {
                    if (!title.isEmpty()) {
                        stories.add(new Headline(title, feed.source, feed.category, summary,
                                safeArticleUrl(url)));
                    }
                    insideItem = false;
                }
                event = parser.next();
            }
        } catch (Exception ignored) {
            // A failed feed leaves the last good cache available to the UI.
        } finally {
            if (connection != null) connection.disconnect();
        }
        return stories;
    }

    private static String readTagText(XmlPullParser parser) throws Exception {
        StringBuilder value = new StringBuilder();
        int depth = 1;
        while (depth > 0) {
            int event = parser.next();
            if (event == XmlPullParser.START_TAG) depth++;
            else if (event == XmlPullParser.END_TAG) depth--;
            else if (event == XmlPullParser.TEXT || event == XmlPullParser.CDSECT ||
                    event == XmlPullParser.ENTITY_REF) {
                if (parser.getText() != null) value.append(parser.getText());
            }
        }
        return value.toString();
    }

    private static String cleanFeedText(String value) {
        if (value == null || value.trim().isEmpty()) return "";
        String text = Html.fromHtml(value, Html.FROM_HTML_MODE_COMPACT).toString();
        return normalizeArticleText(text, 1800);
    }

    private static String safeArticleUrl(String value) {
        try {
            URL url = new URL(value);
            String scheme = url.getProtocol();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) ? value : "";
        } catch (Exception ignored) { return ""; }
    }

    private static String fetchArticleText(String address) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(address).openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(4500);
            connection.setReadTimeout(5500);
            connection.setRequestProperty("User-Agent", "AtlasTV/0.2 AndroidTV");
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml");
            connection.connect();
            String contentType = connection.getContentType();
            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300 ||
                    (contentType != null && !contentType.toLowerCase(java.util.Locale.ROOT).contains("html"))) {
                return "";
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (InputStream input = new BoundedInputStream(connection.getInputStream(), MAX_ARTICLE_BYTES)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) >= 0) {
                    if (count > 0) bytes.write(buffer, 0, count);
                }
            }
            String html = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
            return extractArticleText(html);
        } catch (Exception ignored) {
            return "";
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String extractArticleText(String html) {
        String cleaned = html.replaceAll("(?is)<!--.*?-->", " ");
        for (String tag : new String[] {"script", "style", "nav", "header", "footer", "aside", "form", "svg", "noscript", "iframe"}) {
            cleaned = cleaned.replaceAll("(?is)<" + tag + "\\b[^>]*>.*?</" + tag + "\\s*>", " ");
        }
        String semantic = firstElement(cleaned, "article");
        if (semantic.isEmpty()) semantic = firstElement(cleaned, "main");
        if (semantic.isEmpty()) semantic = firstElement(cleaned, "body");
        if (!semantic.isEmpty()) cleaned = semantic;
        cleaned = cleaned.replaceAll("(?i)<br\\s*/?>|</(p|div|li|h[1-6]|section|blockquote|article)>", "\n");
        cleaned = cleaned.replaceAll("(?s)<[^>]+>", " ");
        String text = Html.fromHtml(cleaned, Html.FROM_HTML_MODE_COMPACT).toString();
        return normalizeArticleText(text, 12000);
    }

    private static String firstElement(String html, String tag) {
        Matcher matcher = Pattern.compile("(?is)<" + tag + "\\b[^>]*>(.*?)</" + tag + "\\s*>")
                .matcher(html);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String normalizeArticleText(String value, int maxLength) {
        String text = value.replace('\u00a0', ' ').replaceAll("[\\t\\x0B\\f\\r ]+", " ")
                .replaceAll(" *\\n *", "\n").replaceAll("\\n{3,}", "\n\n").trim();
        return text.length() <= maxLength ? text : text.substring(0, maxLength).trim() + "…";
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
                items.add(new Headline(row.getString("title"), row.getString("source"),
                        row.getString("category"), row.optString("summary", ""), row.optString("url", "")));
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
                row.put("summary", item.summary);
                row.put("url", item.url);
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
