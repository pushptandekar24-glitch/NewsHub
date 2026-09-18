package com.apihub.news;

/**
 * Strategy interface for external news sources.
 *
 * WHY an interface: NewsAPI.org's free tier is development-only and capped at
 * 100 requests/day, so this project needs to be able to switch or combine
 * providers. With this seam, both are queried by NewsAggregationService and
 * either can fail without taking the feed down.
 *
 * Implementations MUST NOT throw for upstream failures — they return
 * ProviderResult.failure(...) so the aggregator can fall back.
 */
public interface NewsProvider {

    /** Config value identifying this implementation, e.g. "newsapi". */
    String name();

    /** Human-readable name used in error messages, e.g. "NewsAPI". */
    String displayName();

    /** Headlines / browse feed. Uses category + country where supported. */
    ProviderResult fetchTopHeadlines(NewsQuery query);

    /** Free-text or boolean search across the provider's index, newest first. */
    ProviderResult search(NewsQuery query);

    /** True when the provider's native category list covers this value. */
    boolean supportsNativeCategory(String providerCategory);

    /** True when the provider is configured with an API key. */
    boolean isConfigured();
}
