package com.apihub.news;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * GNews.io adapter — second source in the aggregation, and the deployable one.
 *
 * Differences absorbed here: GNews calls categories "topics", returns `image`
 * rather than `urlToImage`, uses `max` instead of `pageSize`, `lang` instead of
 * `language`, and `sortby` (lowercase b) instead of `sortBy`.
 *
 * Its /search endpoint accepts BOTH `country` and a boolean query, which
 * NewsAPI's /everything does not — that is why "Cricket + India" leans on
 * GNews for the country-scoped half of the result set.
 *
 * SIGNUP: https://gnews.io/register   ENV VAR: GNEWS_API_KEY
 */
@Component
public class GNewsProvider implements NewsProvider {

    private static final Logger log = LoggerFactory.getLogger(GNewsProvider.class);

    private static final Set<String> NATIVE_TOPICS = Set.of(
            "general", "world", "nation", "business", "technology",
            "entertainment", "sports", "science", "health");

    /** GNews rejects queries longer than this, so long boolean strings are trimmed. */
    private static final int MAX_QUERY_LENGTH = 190;

    private final RestClient restClient;
    private final String apiKey;

    public GNewsProvider(RestClient.Builder builder,
                         @Value("${app.news.gnews.base-url}") String baseUrl,
                         @Value("${app.news.gnews.api-key:}") String apiKey) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    @Override public String name() { return "gnews"; }
    @Override public String displayName() { return "GNews"; }
    @Override public boolean isConfigured() { return apiKey != null && !apiKey.isBlank(); }

    @Override
    public boolean supportsNativeCategory(String providerCategory) {
        return providerCategory != null && NATIVE_TOPICS.contains(providerCategory.toLowerCase());
    }

    @Override
    public ProviderResult fetchTopHeadlines(NewsQuery q) {
        if (q.effectiveQuery() != null && !q.effectiveQuery().isBlank()) {
            return search(q);
        }

        boolean hasTopic = supportsNativeCategory(q.providerCategory());

        return call("/top-headlines", uri -> {
            uri.queryParam("topic", hasTopic ? q.providerCategory() : "general");
            if (q.country() != null) uri.queryParam("country", q.country());
        }, q);
    }

    @Override
    public ProviderResult search(NewsQuery q) {
        String query = truncateQuery(q.effectiveQuery());
        if (query == null || query.isBlank()) {
            return ProviderResult.success(List.of(), 0, name());
        }

        return call("/search", uri -> {
            uri.queryParam("q", query)
               // Match against headline and summary only; full-content matching
               // pulls in articles that merely mention the term in passing.
               .queryParam("in", "title,description")
               .queryParam("sortby", "relevance".equals(q.sortBy()) ? "relevance" : "publishedAt")
               .queryParam("from", q.from().toString());
            if (q.country() != null) uri.queryParam("country", q.country());
            if (q.to() != null) uri.queryParam("to", q.to().toString());
        }, q);
    }

    /**
     * Keeps the leading OR-terms and drops the tail, so a long category query
     * degrades to its most important keywords instead of being rejected.
     */
    private String truncateQuery(String query) {
        if (query == null || query.length() <= MAX_QUERY_LENGTH) return query;
        String cut = query.substring(0, MAX_QUERY_LENGTH);
        int lastOr = cut.lastIndexOf(" OR ");
        return lastOr > 0 ? cut.substring(0, lastOr) : cut;
    }

    // -----------------------------------------------------------------------

    private ProviderResult call(String path, java.util.function.Consumer<UriComponentsBuilder> extra, NewsQuery q) {
        if (!isConfigured()) {
            return ProviderResult.failure(name(), "GNEWS_API_KEY is not configured");
        }

        UriComponentsBuilder uri = UriComponentsBuilder.fromPath(path)
                .queryParam("lang", q.language())
                .queryParam("max", Math.min(q.fetchSize(), 100))
                .queryParam("page", q.providerPage())
                .queryParam("apikey", apiKey);   // GNews only supports the query-param form
        extra.accept(uri);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = restClient.get()
                    .uri(uri.build().toUriString())
                    .retrieve()
                    .body(Map.class);

            return mapResponse(body, q);

        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            int status = ex.getStatusCode().value();
            log.warn("GNews returned {} for {}", status, path);
            return ProviderResult.failure(name(), status == 429
                    ? "GNews request limit reached"
                    : "GNews temporarily unavailable (HTTP " + status + ")");
        } catch (Exception ex) {
            log.warn("GNews call failed: {}", ex.getMessage());
            return ProviderResult.failure(name(), "GNews temporarily unavailable");
        }
    }

    @SuppressWarnings("unchecked")
    private ProviderResult mapResponse(Map<String, Object> body, NewsQuery q) {
        if (body == null) return ProviderResult.success(List.of(), 0, name());

        Object raw = body.get("articles");
        if (!(raw instanceof List<?> rawArticles)) {
            return ProviderResult.success(List.of(), 0, name());
        }

        List<NormalizedArticle> out = new ArrayList<>();
        for (Object item : rawArticles) {
            if (!(item instanceof Map<?, ?> m)) continue;

            Map<String, Object> sourceMap = m.get("source") instanceof Map
                    ? (Map<String, Object>) m.get("source") : Map.of();

            NormalizedArticle article = new NormalizedArticle(
                    str(m.get("title")),
                    str(m.get("description")),
                    str(sourceMap.get("name")),
                    null,                       // GNews does not expose an author field
                    str(m.get("image")),
                    str(m.get("url")),
                    parseInstant(str(m.get("publishedAt"))),
                    q.category() == null ? null : q.category().slug(),
                    q.country(),
                    q.language(),
                    name());

            if (article.isUsable()) out.add(article);
        }

        long total = body.get("totalArticles") instanceof Number n ? n.longValue() : out.size();
        return ProviderResult.success(out, total, name());
    }

    private static String str(Object o) { return o == null ? null : o.toString(); }

    private static Instant parseInstant(String value) {
        if (value == null) return null;
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
