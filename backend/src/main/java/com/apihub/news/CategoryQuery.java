package com.apihub.news;

import java.util.List;

/**
 * Everything the backend needs to know to turn a category slug into a good
 * provider query AND to judge whether a returned article actually belongs.
 *
 * WHY relevance terms exist: NewsAPI and GNews have ~7 native categories
 * between them. Mapping "Cricket" to the native "sports" category is what
 * caused MLB and NFL stories to appear on the Cricket page. A native category
 * is a coarse *hint*; `mustMatchAny` is the actual filter.
 *
 * @param slug            our category slug
 * @param providerCategory native provider category, or null
 * @param query           boolean query sent to the provider's search endpoint
 * @param mustMatchAny    article must contain at least one of these (lowercased)
 *                        in its title/description. Empty = no relevance filtering.
 * @param excludeAny      article is dropped if it contains any of these
 * @param strict          when true, articles failing mustMatchAny are removed
 *                        rather than merely down-ranked
 */
public record CategoryQuery(
        String slug,
        String providerCategory,
        String query,
        List<String> mustMatchAny,
        List<String> excludeAny,
        boolean strict
) {
    public boolean hasRelevanceRules() {
        return mustMatchAny != null && !mustMatchAny.isEmpty();
    }
}
