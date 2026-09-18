package com.apihub.service;

import com.apihub.news.AggregatedFeed;
import com.apihub.news.NewsQuery;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Caching wrapper around the aggregator.
 *
 * WHY a separate bean: Spring's @Cacheable works through a proxy, so a method
 * calling its own @Cacheable method inside the same class bypasses the cache
 * entirely. Putting the cached call on a different bean avoids that trap.
 *
 * WHY cache at all: the free news tiers allow ~100 requests/day EACH, and every
 * feed request now fans out to two providers. Without caching, clicking between
 * four categories would spend 8 of your 200 daily calls. TTLs are configured
 * per cache in CacheConfig.
 *
 * NOTE: nothing user-specific is cached here. The key is the NewsQuery record
 * only — no user id, no saved-article state. Per-user data is layered on after
 * the cache, in NewsService.
 */
@Component
public class NewsFetcher {

    private final NewsAggregationService aggregationService;

    public NewsFetcher(NewsAggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    @Cacheable(cacheNames = "newsFeed", key = "#query")
    public AggregatedFeed headlines(NewsQuery query) {
        return aggregationService.headlines(query);
    }

    @Cacheable(cacheNames = "newsSearch", key = "#query")
    public AggregatedFeed search(NewsQuery query) {
        return aggregationService.search(query);
    }
}
