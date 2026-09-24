package com.apihub.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.apihub.news.AggregatedFeed;
import com.apihub.news.ArticleDeduplicator;
import com.apihub.news.ArticleRelevanceFilter;
import com.apihub.news.NewsProvider;
import com.apihub.news.NewsQuery;
import com.apihub.news.NormalizedArticle;
import com.apihub.news.ProviderResult;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests provider fallback, partial failure and no-result behaviour at the
 * aggregation layer — where that logic actually lives — using lightweight fake
 * NewsProvider implementations instead of mocking HTTP. This exercises exactly
 * the scenarios the country-filtering fix depends on: one provider succeeding
 * while another is unavailable, both failing, and both succeeding with
 * overlapping results that must be merged and de-duplicated.
 */
class NewsAggregationServiceTest {

    private final ArticleRelevanceFilter relevanceFilter = new ArticleRelevanceFilter();
    private final ArticleDeduplicator deduplicator = new ArticleDeduplicator();

    private NewsQuery query() {
        return new NewsQuery(null, null, "nl", "en", 0, 20, "publishedAt", null, null);
    }

    private NormalizedArticle article(String title, String source, String provider, Instant publishedAt) {
        return new NormalizedArticle(title, "A description", source, null, null,
                "https://example.com/" + title.hashCode() + "-" + provider,
                publishedAt, null, "nl", "en", provider);
    }

    @Test
    @DisplayName("one provider failing does not fail the request when the other succeeds")
    void fallsBackToTheOtherProviderOnPartialFailure() {
        FakeNewsProvider working = FakeNewsProvider.succeedingWith("newsapi",
                List.of(article("Dutch economy grows", "NOS", "newsapi", Instant.now())));
        FakeNewsProvider broken = FakeNewsProvider.failingWith("gnews", "GNews request limit reached");

        NewsAggregationService service =
                new NewsAggregationService(List.of(working, broken), relevanceFilter, deduplicator);

        AggregatedFeed feed = service.headlines(query());

        assertThat(feed.allFailed()).isFalse();
        assertThat(feed.isPartial()).isTrue();
        assertThat(feed.articles()).hasSize(1);
        assertThat(feed.errors()).containsExactly("GNews request limit reached");
        assertThat(feed.status()).isEqualTo("PARTIAL");
        assertThat(feed.friendlyMessage())
                .isEqualTo("Some news sources are temporarily unavailable. Showing available results from other providers.");
    }

    @Test
    @DisplayName("both providers failing surfaces allFailed so the caller can return a clean error")
    void bothProvidersFailing() {
        FakeNewsProvider a = FakeNewsProvider.failingWith("newsapi", "NewsAPI daily request limit reached");
        FakeNewsProvider b = FakeNewsProvider.failingWith("gnews", "GNews request limit reached");

        NewsAggregationService service =
                new NewsAggregationService(List.of(a, b), relevanceFilter, deduplicator);

        AggregatedFeed feed = service.headlines(query());

        assertThat(feed.allFailed()).isTrue();
        assertThat(feed.articles()).isEmpty();
        assertThat(feed.status()).isEqualTo("ALL_FAILED");
        assertThat(feed.friendlyMessage()).isEqualTo("Unable to load news right now. Please try again shortly.");
    }

    @Test
    @DisplayName("both providers succeeding but nothing relevant is a clean NO_RESULTS, not an error")
    void noResultsIsNotAnError() {
        FakeNewsProvider a = FakeNewsProvider.succeedingWith("newsapi", List.of());
        FakeNewsProvider b = FakeNewsProvider.succeedingWith("gnews", List.of());

        NewsAggregationService service =
                new NewsAggregationService(List.of(a, b), relevanceFilter, deduplicator);

        AggregatedFeed feed = service.headlines(query());

        assertThat(feed.allFailed()).isFalse();
        assertThat(feed.isPartial()).isFalse();
        assertThat(feed.articles()).isEmpty();
        assertThat(feed.status()).isEqualTo("NO_RESULTS");
        assertThat(feed.friendlyMessage()).isNull();
    }

    @Test
    @DisplayName("an unconfigured provider is skipped silently, not counted as a failure")
    void unconfiguredProviderIsSkipped() {
        FakeNewsProvider configured = FakeNewsProvider.succeedingWith("newsapi",
                List.of(article("Story one", "NOS", "newsapi", Instant.now())));
        FakeNewsProvider unconfigured = FakeNewsProvider.notConfigured("gnews");

        NewsAggregationService service =
                new NewsAggregationService(List.of(configured, unconfigured), relevanceFilter, deduplicator);

        AggregatedFeed feed = service.headlines(query());

        assertThat(feed.status()).isEqualTo("OK");
        assertThat(feed.errors()).isEmpty();
        assertThat(feed.articles()).hasSize(1);
    }

    @Test
    @DisplayName("overlapping results from both providers are merged, sorted newest-first, and de-duplicated")
    void mergesAndDeduplicatesAcrossProviders() {
        Instant now = Instant.now();
        NormalizedArticle older = article("Amsterdam housing market cools", "NOS", "newsapi",
                now.minus(2, ChronoUnit.HOURS));
        NormalizedArticle newer = article("Dutch elections: what to know", "NOS", "newsapi", now);
        NormalizedArticle duplicate = new NormalizedArticle(
                "Dutch elections: what to know", "A description", "NOS", null, null,
                newer.url() + "?utm_source=x",
                now.minus(10, ChronoUnit.MINUTES), null, "nl", "en", "gnews");

        FakeNewsProvider a = FakeNewsProvider.succeedingWith("newsapi", List.of(older, newer));
        FakeNewsProvider b = FakeNewsProvider.succeedingWith("gnews", List.of(duplicate));

        NewsAggregationService service =
                new NewsAggregationService(List.of(a, b), relevanceFilter, deduplicator);

        AggregatedFeed feed = service.headlines(query());

        assertThat(feed.status()).isEqualTo("OK");
        // "Dutch elections" appears from both providers but must collapse to one.
        assertThat(feed.articles()).hasSize(2);
        assertThat(feed.articles().get(0).publishedAt()).isAfterOrEqualTo(feed.articles().get(1).publishedAt());
    }

    /** Minimal in-test double for NewsProvider — no HTTP, no mocking framework needed. */
    private static final class FakeNewsProvider implements NewsProvider {
        private final String name;
        private final boolean configured;
        private final ProviderResult result;

        private FakeNewsProvider(String name, boolean configured, ProviderResult result) {
            this.name = name;
            this.configured = configured;
            this.result = result;
        }

        static FakeNewsProvider succeedingWith(String name, List<NormalizedArticle> articles) {
            return new FakeNewsProvider(name, true, ProviderResult.success(articles, articles.size(), name));
        }

        static FakeNewsProvider failingWith(String name, String error) {
            return new FakeNewsProvider(name, true, ProviderResult.failure(name, error));
        }

        static FakeNewsProvider notConfigured(String name) {
            return new FakeNewsProvider(name, false, ProviderResult.failure(name, name + " not configured"));
        }

        @Override public String name() { return name; }
        @Override public String displayName() { return name; }
        @Override public ProviderResult fetchTopHeadlines(NewsQuery query) { return result; }
        @Override public ProviderResult search(NewsQuery query) { return result; }
        @Override public boolean supportsNativeCategory(String providerCategory) { return false; }
        @Override public boolean isConfigured() { return configured; }
    }
}
