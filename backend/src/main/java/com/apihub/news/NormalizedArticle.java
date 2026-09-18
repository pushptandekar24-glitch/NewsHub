package com.apihub.news;

import java.time.Instant;

/**
 * One article, in OUR shape, regardless of which provider produced it.
 *
 * This record is the whole point of the provider abstraction: NewsAPI returns
 * `urlToImage` and a nested `source.name`; GNews returns `image` and a
 * differently-shaped source object. Everything downstream sees only this.
 *
 * `provider` is carried through so the UI can show where a story came from and
 * so the aggregator can report which provider failed.
 */
public record NormalizedArticle(
        String title,
        String description,
        String source,
        String author,
        String imageUrl,
        String url,
        Instant publishedAt,
        String category,
        String country,
        String language,
        String provider
) {
    public boolean isUsable() {
        return title != null && !title.isBlank()
                && url != null && !url.isBlank()
                && publishedAt != null
                // NewsAPI emits placeholder rows for removed articles
                && !"[Removed]".equalsIgnoreCase(title);
    }

    /** Lowercased title + description, used for relevance matching. */
    public String searchableText() {
        return ((title == null ? "" : title) + " " + (description == null ? "" : description))
                .toLowerCase();
    }

    public NormalizedArticle withContext(String category, String country, String language) {
        return new NormalizedArticle(title, description, source, author, imageUrl, url,
                publishedAt, category, country, language, provider);
    }
}
