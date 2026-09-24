package com.apihub.service;

import com.apihub.common.PageResponse;
import com.apihub.dto.news.ArticleDetailResponse;
import com.apihub.dto.news.ArticleResponse;
import com.apihub.entity.Category;
import com.apihub.entity.NewsArticle;
import com.apihub.entity.User;
import com.apihub.exception.ExternalApiException;
import com.apihub.exception.ResourceNotFoundException;
import com.apihub.news.AggregatedFeed;
import com.apihub.news.CategoryQuery;
import com.apihub.news.CategoryQueryRegistry;
import com.apihub.news.NewsQuery;
import com.apihub.news.NormalizedArticle;
import com.apihub.repository.CategoryRepository;
import com.apihub.repository.NewsArticleRepository;
import com.apihub.repository.SavedArticleRepository;
import com.apihub.security.CurrentUserProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration layer for news.
 *
 *   Controller
 *      -> NewsService            (this class: category resolution, persistence, DTOs)
 *      -> NewsFetcher            (cache)
 *      -> NewsAggregationService (parallel fan-out, merge, filter, dedupe, sort)
 *      -> NewsProvider x2        (NewsAPI, GNews)
 *
 * Fetched metadata is persisted so that article detail pages, bookmarks and
 * click-based trending can exist — external articles arrive with no stable id.
 */
@Service
public class NewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsService.class);

    private static final int RELATED_LIMIT = 4;
    private static final int TRENDING_WINDOW_HOURS = 48;
    private static final int TRENDING_CANDIDATE_POOL = 300;
    /** Upstream pages beyond this are rarely useful and burn API quota. */
    private static final int MAX_PAGES = 10;

    private final NewsFetcher fetcher;
    private final CategoryQueryRegistry categoryQueryRegistry;
    private final TrendingService trendingService;
    private final NewsArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;
    private final SavedArticleRepository savedArticleRepository;
    private final ArticleSummaryService summaryService;
    private final CurrentUserProvider currentUserProvider;

    /** How far back a feed may reach. Configurable via NEWS_FRESHNESS_DAYS. */
    private final int freshnessDays;

    public NewsService(NewsFetcher fetcher,
                       CategoryQueryRegistry categoryQueryRegistry,
                       TrendingService trendingService,
                       NewsArticleRepository articleRepository,
                       CategoryRepository categoryRepository,
                       SavedArticleRepository savedArticleRepository,
                       ArticleSummaryService summaryService,
                       CurrentUserProvider currentUserProvider,
                       @Value("${app.news.freshness-days:7}") int freshnessDays) {
        this.fetcher = fetcher;
        this.categoryQueryRegistry = categoryQueryRegistry;
        this.trendingService = trendingService;
        this.articleRepository = articleRepository;
        this.categoryRepository = categoryRepository;
        this.savedArticleRepository = savedArticleRepository;
        this.summaryService = summaryService;
        this.currentUserProvider = currentUserProvider;
        this.freshnessDays = freshnessDays;
    }

    // ---------------------------------------------------------------- feeds

    /** GET /api/news — browse feed with optional category + country filters. */
    @Transactional
    public PageResponse<ArticleResponse> getFeed(String categorySlug, String country, String language,
                                                 int page, int pageSize, Instant from, Instant to) {

        Category category = resolveCategory(categorySlug);
        NewsQuery query = buildQuery(null, categorySlug, country, language,
                page, pageSize, "publishedAt", from, to);

        AggregatedFeed feed = fetcher.headlines(query);
        failIfEverythingFailed(feed);

        List<NewsArticle> stored = persistAll(feed.articles(), category, query.country(), query.language());
        return toPage(stored, page, pageSize, feed);
    }

    /** GET /api/news/search?q=... */
    @Transactional
    public PageResponse<ArticleResponse> search(String keyword, String categorySlug, String country,
                                                String language, String sortBy, int page, int pageSize,
                                                Instant from, Instant to) {

        Category category = resolveCategory(categorySlug);
        NewsQuery query = buildQuery(keyword, categorySlug, country, language,
                page, pageSize, sortBy, from, to);

        AggregatedFeed feed = fetcher.search(query);
        failIfEverythingFailed(feed);

        List<NewsArticle> stored = persistAll(feed.articles(), category, query.country(), query.language());
        return toPage(stored, page, pageSize, feed);
    }

    /**
     * GET /api/news/trending
     *
     * Ranked by TrendingService (recency + source coverage + real clicks) over
     * articles we have already collected in the last 48 hours. If the local pool
     * is thin — a fresh database — we prime it from the live feed first.
     */
    @Transactional
    public PageResponse<ArticleResponse> getTrending(int limit) {
        Instant since = Instant.now().minus(TRENDING_WINDOW_HOURS, ChronoUnit.HOURS);

        List<NewsArticle> candidates = articleRepository.findRecentSince(
                since, PageRequest.of(0, TRENDING_CANDIDATE_POOL));

        if (candidates.size() < limit * 2) {
            // Prime the pool, then re-read. Warming the cache this way means the
            // first visitor after a restart still gets a real ranking. If every
            // provider happens to be down right now, fall back to whatever local
            // candidates already exist rather than failing the whole page —
            // trending has its own data source (past clicks) that a transient
            // provider outage should not take down.
            try {
                getFeed(null, null, "en", 0, 40, null, null);
                candidates = articleRepository.findRecentSince(since, PageRequest.of(0, TRENDING_CANDIDATE_POOL));
            } catch (ExternalApiException ex) {
                log.warn("Could not prime trending pool (all providers failed): {}", ex.getMessage());
            }
        }

        List<TrendingService.ScoredArticle> ranked = trendingService.rank(candidates, limit);
        Set<Long> savedIds = savedArticleIdsForCurrentUser();

        List<ArticleResponse> content = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            TrendingService.ScoredArticle scored = ranked.get(i);
            content.add(ArticleResponse.from(scored.article(),
                    savedIds.contains(scored.article().getId()),
                    i + 1,
                    scored.coverageCount()));
        }

        return PageResponse.of(content, 0, limit, content.size());
    }

    /** GET /api/news/personalized — built from the user's saved interests. */
    @Transactional
    public PageResponse<ArticleResponse> getPersonalized(User user, String country, int page, int pageSize) {
        Set<Category> interests = user.getInterests();

        if (interests.isEmpty()) {
            return getFeed(null, country, "en", page, pageSize, null, null);
        }

        // Combine up to five interests into one OR query. One upstream request
        // instead of five matters a lot on a 100/day quota.
        String combined = interests.stream()
                .limit(5)
                .map(c -> categoryQueryRegistry.find(c.getSlug())
                        .map(CategoryQuery::query)
                        .orElse(c.getName()))
                .map(q -> "(" + q + ")")
                .reduce((a, b) -> a + " OR " + b)
                .orElse(null);

        NewsQuery query = new NewsQuery(combined, null, country, "en",
                page, pageSize, "publishedAt", null, null);

        AggregatedFeed feed = fetcher.search(query);
        failIfEverythingFailed(feed);

        List<NewsArticle> stored = persistAll(feed.articles(), null, query.country(), query.language());
        return toPage(stored, page, pageSize, feed);
    }

    // --------------------------------------------------------------- detail

    /** GET /api/news/{id} — detail page, and the only place clickCount increases. */
    @Transactional
    public ArticleDetailResponse getArticleDetail(Long id) {
        NewsArticle article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article", id));

        articleRepository.incrementClickCount(id);

        List<NewsArticle> related = article.getCategory() == null
                ? List.of()
                : articleRepository.findRelatedByCategory(
                        article.getCategory().getId(), article.getId(), PageRequest.of(0, RELATED_LIMIT));

        Set<Long> savedIds = savedArticleIdsForCurrentUser();

        return new ArticleDetailResponse(
                ArticleResponse.from(article, savedIds.contains(article.getId())),
                summaryService.buildSummary(article),
                summaryService.buildKeyPoints(article),
                summaryService.buildWhyItMatters(article),
                true,
                related.stream().map(a -> ArticleResponse.from(a, savedIds.contains(a.getId()))).toList(),
                article.getClickCount() + 1);
    }

    // ------------------------------------------------------------- internals

    private Category resolveCategory(String slug) {
        if (slug == null || slug.isBlank()) return null;
        return categoryRepository.findBySlugIgnoreCase(slug)
                .orElseThrow(() -> new ResourceNotFoundException("No category with slug '" + slug + "'"));
    }

    /**
     * Turns a category slug into a full query strategy.
     *
     * Order of preference:
     *   1. CategoryQueryRegistry — the code-owned strategy with relevance rules
     *   2. the DB `providerQuery` column — lets an admin override without a redeploy
     *   3. the category name as a plain keyword
     */
    private NewsQuery buildQuery(String keyword, String categorySlug, String country, String language,
                                 int page, int pageSize, String sortBy, Instant from, Instant to) {

        CategoryQuery categoryQuery = null;

        if (categorySlug != null && !categorySlug.isBlank()) {
            categoryQuery = categoryQueryRegistry.find(categorySlug).orElseGet(() -> {
                Category db = categoryRepository.findBySlugIgnoreCase(categorySlug).orElse(null);
                if (db == null) return null;
                String fallbackQuery = db.getProviderQuery() != null ? db.getProviderQuery() : db.getName();
                return new CategoryQuery(db.getSlug(), db.getProviderCategory(),
                        fallbackQuery, List.of(), List.of(), false);
            });
        }

        Instant effectiveFrom = from != null
                ? from
                : Instant.now().minus(freshnessDays, ChronoUnit.DAYS);

        return new NewsQuery(keyword, categoryQuery, country, language,
                Math.min(page, MAX_PAGES - 1), pageSize, sortBy, effectiveFrom, to);
    }

    private void failIfEverythingFailed(AggregatedFeed feed) {
        if (feed.allFailed()) {
            // Raw, provider-specific detail goes to the log for debugging.
            // The exception carries only the friendly sentence a normal user sees.
            log.warn("All news providers failed: {}", String.join(" | ", feed.errors()));
            throw new ExternalApiException(feed.friendlyMessage(), 502);
        }
    }

    /**
     * Upsert by deterministic external id so refreshing a feed does not create
     * duplicate rows and previously-saved articles keep their identity.
     */
    private List<NewsArticle> persistAll(List<NormalizedArticle> incoming, Category category,
                                         String country, String language) {
        List<NewsArticle> result = new ArrayList<>(incoming.size());

        for (NormalizedArticle n : incoming) {
            String externalId = sha256(n.url());

            NewsArticle entity = articleRepository.findByExternalId(externalId)
                    .orElseGet(NewsArticle::new);

            entity.setExternalId(externalId);
            entity.setTitle(truncate(n.title(), 500));
            entity.setDescription(truncate(n.description(), 1000));
            entity.setSource(truncate(n.source(), 120));
            entity.setAuthor(truncate(n.author(), 200));
            entity.setImageUrl(truncate(n.imageUrl(), 1000));
            entity.setArticleUrl(truncate(n.url(), 1000));
            entity.setPublishedAt(n.publishedAt() != null ? n.publishedAt() : Instant.now());
            entity.setLanguage(language);
            entity.setProvider(n.provider());

            // Do not blank out an existing classification with a broader query's nulls.
            if (category != null) entity.setCategory(category);
            if (country != null) entity.setCountry(country);

            result.add(articleRepository.save(entity));
        }
        return result;
    }

    /**
     * Slices the aggregated result down to one page.
     *
     * The providers were already asked for page N, so each page is a distinct
     * upstream window. We over-fetch (NewsQuery.fetchSize) because relevance
     * filtering and de-duplication discard results; the surplus is dropped here
     * rather than carried across pages, which would risk showing duplicates.
     */
    private PageResponse<ArticleResponse> toPage(List<NewsArticle> articles, int page, int pageSize,
                                                 AggregatedFeed feed) {
        Set<Long> savedIds = savedArticleIdsForCurrentUser();

        List<ArticleResponse> content = articles.stream()
                .limit(pageSize)
                .map(a -> ArticleResponse.from(a, savedIds.contains(a.getId())))
                .toList();

        long cappedTotal = Math.min(feed.totalAvailable(), (long) pageSize * MAX_PAGES);

        // Status/message come from the feed, not re-derived from content here,
        // because "no results after filtering" and "no results because a
        // provider failed" are different states even when both lists are empty.
        String status = content.isEmpty() && !feed.articles().isEmpty()
                ? "NO_RESULTS"   // everything on this page got cut by pageSize/paging, not a real empty result
                : feed.status();

        return PageResponse.of(content, page, pageSize,
                Math.max(cappedTotal, (long) page * pageSize + content.size()),
                status, feed.friendlyMessage(), feed.errors());
    }

    /** Lets every card render its bookmark state without an N+1 query. */
    private Set<Long> savedArticleIdsForCurrentUser() {
        Optional<Long> userId = currentUserProvider.currentUserId();
        if (userId.isEmpty()) return Set.of();

        Set<Long> ids = new HashSet<>();
        savedArticleRepository.findByUserId(userId.get())
                .forEach(s -> ids.add(s.getArticle().getId()));
        return ids;
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    /** Deterministic, collision-resistant id for a URL. */
    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable on this JVM", ex);
        }
    }
}
