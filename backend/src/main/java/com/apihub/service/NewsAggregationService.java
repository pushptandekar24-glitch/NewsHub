package com.apihub.service;

import com.apihub.news.AggregatedFeed;
import com.apihub.news.ArticleDeduplicator;
import com.apihub.news.ArticleRelevanceFilter;
import com.apihub.news.NewsProvider;
import com.apihub.news.NewsQuery;
import com.apihub.news.NormalizedArticle;
import com.apihub.news.ProviderResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Queries every configured provider, then merges the results.
 *
 * Pipeline:
 *   1. call all providers in PARALLEL (they are independent network calls;
 *      running them sequentially would double the latency for no reason)
 *   2. collect failures as data rather than exceptions
 *   3. drop articles that do not belong to the requested category
 *   4. de-duplicate across providers
 *   5. sort by publishedAt DESC — newest genuinely first
 *
 * FALLBACK BEHAVIOUR: if one provider fails we still return the other's
 * results, with the failure recorded in `errors` so the UI can mention it.
 * Only when every provider fails does the caller surface an error page.
 */
@Service
public class NewsAggregationService {

    private static final Logger log = LoggerFactory.getLogger(NewsAggregationService.class);

    private final List<NewsProvider> providers;
    private final ArticleRelevanceFilter relevanceFilter;
    private final ArticleDeduplicator deduplicator;

    public NewsAggregationService(List<NewsProvider> providers,
                                  ArticleRelevanceFilter relevanceFilter,
                                  ArticleDeduplicator deduplicator) {
        this.providers = providers;
        this.relevanceFilter = relevanceFilter;
        this.deduplicator = deduplicator;
    }

    public AggregatedFeed headlines(NewsQuery query) {
        return aggregate(query, false);
    }

    public AggregatedFeed search(NewsQuery query) {
        return aggregate(query, true);
    }

    // -----------------------------------------------------------------------

    private AggregatedFeed aggregate(NewsQuery query, boolean searchMode) {
        List<NewsProvider> active = providers.stream().filter(NewsProvider::isConfigured).toList();

        if (active.isEmpty()) {
            return new AggregatedFeed(List.of(), 0,
                    List.of("No news provider is configured. Set NEWS_API_KEY or GNEWS_API_KEY in backend/.env."),
                    true);
        }

        // Parallel fan-out. Each future already swallows its own failure and
        // returns ProviderResult.failure(...), so join() cannot blow up here.
        List<CompletableFuture<ProviderResult>> futures = active.stream()
                .map(provider -> CompletableFuture.supplyAsync(() ->
                        searchMode ? provider.search(query) : provider.fetchTopHeadlines(query)))
                .toList();

        List<ProviderResult> results = futures.stream().map(CompletableFuture::join).toList();

        List<String> errors = new ArrayList<>();
        List<NormalizedArticle> merged = new ArrayList<>();
        long totalAvailable = 0;

        for (ProviderResult result : results) {
            if (result.failed()) {
                errors.add(result.error());
                continue;
            }
            merged.addAll(result.articles());
            totalAvailable += result.totalResults();
        }

        boolean everyProviderFailed = errors.size() == results.size();
        if (everyProviderFailed) {
            log.warn("All providers failed for query: {}", query.effectiveQuery());
            return new AggregatedFeed(List.of(), 0, errors, true);
        }

        // 3. relevance — this is where MLB/NFL leaves the Cricket page
        List<NormalizedArticle> relevant = relevanceFilter.filter(merged, query.category());

        // 4. sort BEFORE dedupe so the newest copy of a duplicated story survives
        List<NormalizedArticle> sorted = new ArrayList<>(relevant);
        sorted.sort(Comparator.comparing(NormalizedArticle::publishedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        // 5. dedupe keeps the first occurrence, which is now the freshest
        List<NormalizedArticle> deduped = deduplicator.dedupe(sorted);

        // The upstream total is meaningless after filtering; cap it at something
        // honest so pagination does not advertise pages that cannot be filled.
        long honestTotal = Math.min(totalAvailable, (long) deduped.size() * 4);

        return new AggregatedFeed(deduped, Math.max(honestTotal, deduped.size()), errors, false);
    }
}
