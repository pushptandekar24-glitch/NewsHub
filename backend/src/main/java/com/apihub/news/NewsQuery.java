package com.apihub.news;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Provider-agnostic description of "what news do we want?".
 *
 * Controllers build this; each NewsProvider translates it into that provider's
 * own query string. Adding a provider therefore never changes the controller.
 *
 * It is also used as a cache key, which is why it is a record — value equality
 * comes for free.
 */
public record NewsQuery(
        String keyword,             // free-text search, may be null
        CategoryQuery category,     // resolved category strategy, may be null
        String country,             // ISO-3166 alpha-2 lowercase; null = worldwide
        String language,            // ISO-639-1, default "en"
        int page,                   // ZERO-based (providers are 1-based; we convert)
        int pageSize,
        String sortBy,              // "publishedAt" | "relevancy" | "popularity"
        Instant from,               // lower bound on publishedAt
        Instant to                  // upper bound, usually null
) {
    public static final int MAX_PAGE_SIZE = 100;
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** Number of days back we look when the caller did not specify `from`. */
    public static final int DEFAULT_FRESHNESS_DAYS = 7;

    public NewsQuery {
        language = (language == null || language.isBlank()) ? "en" : language.toLowerCase();
        country = (country == null || country.isBlank() || "world".equalsIgnoreCase(country))
                ? null : country.toLowerCase();
        page = Math.max(page, 0);
        pageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        sortBy = (sortBy == null || sortBy.isBlank()) ? "publishedAt" : sortBy;

        // Freshness is the default, not an option. Without a `from` bound the
        // providers happily return month-old filler, which is exactly why the
        // feed used to be full of "1d ago" and older stories.
        if (from == null) {
            from = Instant.now().minus(DEFAULT_FRESHNESS_DAYS, ChronoUnit.DAYS);
        }
    }

    public boolean isSearch() {
        return keyword != null && !keyword.isBlank();
    }

    /** Providers use 1-based page numbers. */
    public int providerPage() {
        return page + 1;
    }

    public String providerCategory() {
        return category == null ? null : category.providerCategory();
    }

    /** The effective text query: explicit keyword wins, else the category query. */
    public String effectiveQuery() {
        if (isSearch()) return keyword;
        return category == null ? null : category.query();
    }

    /**
     * How many articles to ask the provider for.
     *
     * We over-fetch deliberately: relevance filtering and cross-provider
     * de-duplication both throw results away, so asking for exactly pageSize
     * would leave a half-empty page. 3x with a floor of 30 is enough in
     * practice for strict categories like Cricket.
     */
    public int fetchSize() {
        return Math.min(Math.max(pageSize * 3, 30), MAX_PAGE_SIZE);
    }

    public NewsQuery withPageSize(int newPageSize) {
        return new NewsQuery(keyword, category, country, language, page, newPageSize, sortBy, from, to);
    }
}
