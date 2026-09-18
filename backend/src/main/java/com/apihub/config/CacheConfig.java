package com.apihub.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Per-cache TTLs, because these things go stale at very different rates.
 *
 * WHY caching matters here: each feed request now fans out to TWO providers,
 * and both free tiers allow ~100 requests/day. Without caching, a user clicking
 * through five categories would spend 10 calls. With a 3-minute TTL, a burst of
 * navigation costs almost nothing.
 *
 * WHY the TTLs differ:
 *   newsFeed   3 min — browsing is bursty; 3 minutes is well inside "current"
 *   newsSearch 2 min — searches are more varied, so entries are less reusable
 *   categories 30 min — changes only when an admin edits them (also evicted
 *                       explicitly by CategoryService)
 *
 * Nothing user-specific is cached. Saved-article state is applied after the
 * cache, in NewsService, so two users never see each other's bookmarks.
 */
@Configuration
public class CacheConfig {

    public static final String NEWS_FEED = "newsFeed";
    public static final String NEWS_SEARCH = "newsSearch";
    public static final String CATEGORIES = "categories";

    @Bean
    public CacheManager cacheManager() {
        Map<String, Duration> ttls = new ConcurrentHashMap<>();
        ttls.put(NEWS_FEED, Duration.ofMinutes(3));
        ttls.put(NEWS_SEARCH, Duration.ofMinutes(2));
        ttls.put(CATEGORIES, Duration.ofMinutes(30));

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(ttls.entrySet().stream()
                .map(entry -> (Cache) new CaffeineCache(entry.getKey(),
                        Caffeine.newBuilder()
                                .expireAfterWrite(entry.getValue())
                                .maximumSize(500)
                                .build()))
                .toList());
        return manager;
    }
}
