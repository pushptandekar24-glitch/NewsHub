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
 * NewsAPI.org adapter.
 *
 * ENDPOINT CHOICE — this is also the fix for country filtering silently doing
 * nothing on this provider:
 *
 *   - /everything  is used whenever we have a text query OR a country OTHER
 *     THAN "us". It is the only endpoint that supports sortBy=publishedAt and
 *     a `from` date, AND — this is the important part — NewsAPI's current
 *     documentation for /top-headlines lists its `country` parameter as
 *     supporting only "us"; every other code is no longer reliable there.
 *     /everything itself has NO country parameter at all, so when a country is
 *     requested we fold it into the `q` string as a boolean AND via
 *     CountryQueryRegistry + QueryComposer (see those classes for why).
 *
 *   - /top-headlines is used ONLY for the plain worldwide/US browse case: no
 *     search text and no country (or country == "us"). That is the one
 *     situation where NewsAPI's own country param is still documented to work,
 *     and top-headlines' results are already recency-ordered.
 *
 * Previously, /everything never looked at q.country() at all, so selecting any
 * country while a category or search term was active (i.e. almost always) had
 * no effect whatsoever on this provider — GNews was silently doing all the
 * country-relevance work alone.
 *
 * FREE TIER LIMITS (verified Sept 2026): 100 requests/day, ~24h article delay,
 * development/localhost use only.
 */
@Component
public class NewsApiOrgProvider implements NewsProvider {

    private static final Logger log = LoggerFactory.getLogger(NewsApiOrgProvider.class);

    private static final Set<String> NATIVE_CATEGORIES = Set.of(
            "business", "entertainment", "general", "health", "science", "sports", "technology");

    /** The only country code NewsAPI's /top-headlines still documents support for. */
    private static final String TOP_HEADLINES_SUPPORTED_COUNTRY = "us";

    private final RestClient restClient;
    private final String apiKey;
    private final CountryQueryRegistry countryQueryRegistry;

    public NewsApiOrgProvider(RestClient.Builder builder,
                              @Value("${app.news.newsapi.base-url}") String baseUrl,
                              @Value("${app.news.newsapi.api-key:}") String apiKey,
                              CountryQueryRegistry countryQueryRegistry) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.countryQueryRegistry = countryQueryRegistry;
    }

    @Override public String name() { return "newsapi"; }
    @Override public String displayName() { return "NewsAPI"; }
    @Override public boolean isConfigured() { return apiKey != null && !apiKey.isBlank(); }

    @Override
    public boolean supportsNativeCategory(String providerCategory) {
        return providerCategory != null && NATIVE_CATEGORIES.contains(providerCategory.toLowerCase());
    }

    @Override
    public ProviderResult fetchTopHeadlines(NewsQuery q) {
        boolean hasQuery = q.effectiveQuery() != null && !q.effectiveQuery().isBlank();
        boolean countryNeedsEverything = q.country() != null
                && !TOP_HEADLINES_SUPPORTED_COUNTRY.equals(q.country());

        // Any of these push us to /everything, where country is applied as a
        // query boost instead of a native parameter.
        if (hasQuery || countryNeedsEverything) {
            return search(q);
        }

        boolean hasCategory = supportsNativeCategory(q.providerCategory());

        return call("/top-headlines", uri -> {
            if (hasCategory) uri.queryParam("category", q.providerCategory());
            // /top-headlines needs at least one anchor parameter.
            uri.queryParam("country", q.country() != null ? q.country() : "us");
        }, q);
    }

    @Override
    public ProviderResult search(NewsQuery q) {
        String countryBoost = q.country() == null
                ? null
                : countryQueryRegistry.queryBoost(q.country()).orElse(null);

        String query = QueryComposer.withCountryBoost(q.effectiveQuery(), countryBoost);
        if (query == null || query.isBlank()) {
            return ProviderResult.success(List.of(), 0, name());
        }

        return call("/everything", uri -> {
            uri.queryParam("q", query)
               .queryParam("language", q.language())
               // Newest first. This is the single most important parameter here.
               .queryParam("sortBy", "relevancy".equals(q.sortBy()) ? "relevancy" : "publishedAt")
               .queryParam("from", q.from().toString());
            if (q.to() != null) uri.queryParam("to", q.to().toString());
        }, q);
    }

    // -----------------------------------------------------------------------

    private ProviderResult call(String path, java.util.function.Consumer<UriComponentsBuilder> extra, NewsQuery q) {
        if (!isConfigured()) {
            return ProviderResult.failure(name(), "NEWS_API_KEY is not configured");
        }

        UriComponentsBuilder uri = UriComponentsBuilder.fromPath(path)
                .queryParam("page", q.providerPage())
                .queryParam("pageSize", q.fetchSize());
        extra.accept(uri);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = restClient.get()
                    .uri(uri.build().toUriString())
                    // Header auth rather than ?apiKey= so the secret stays out of access logs.
                    .header("X-Api-Key", apiKey)
                    .retrieve()
                    .body(Map.class);

            return mapResponse(body, q);

        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            int status = ex.getStatusCode().value();
            log.warn("NewsAPI returned {} for {}", status, path);
            return ProviderResult.failure(name(), describeUpstreamError(status));
        } catch (Exception ex) {
            log.warn("NewsAPI call failed: {}", ex.getMessage());
            return ProviderResult.failure(name(), "NewsAPI temporarily unavailable");
        }
    }

    private String describeUpstreamError(int status) {
        return switch (status) {
            case 401 -> "NewsAPI rejected the API key";
            case 426 -> "NewsAPI requires an upgraded plan for this request";
            case 429 -> "NewsAPI daily request limit reached";
            default  -> "NewsAPI temporarily unavailable (HTTP " + status + ")";
        };
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
                    str(m.get("author")),
                    str(m.get("urlToImage")),
                    str(m.get("url")),
                    parseInstant(str(m.get("publishedAt"))),
                    q.category() == null ? null : q.category().slug(),
                    q.country(),
                    q.language(),
                    name());

            if (article.isUsable()) out.add(article);
        }

        long total = body.get("totalResults") instanceof Number n ? n.longValue() : out.size();
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
