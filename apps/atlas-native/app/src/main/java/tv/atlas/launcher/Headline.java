package tv.atlas.launcher;

final class Headline {
    final String title;
    final String source;
    final String category;
    final String summary;
    final String url;

    Headline(String title, String source, String category) {
        this(title, source, category, "", "");
    }

    Headline(String title, String source, String category, String summary, String url) {
        this.title = title;
        this.source = source;
        this.category = category;
        this.summary = summary == null ? "" : summary;
        this.url = url == null ? "" : url;
    }
}
