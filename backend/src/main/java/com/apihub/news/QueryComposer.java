package com.apihub.news;

/**
 * Pure helper for combining a topic/category query with a country-relevance
 * boost into one boolean query string.
 *
 * WHY this exists as its own class rather than inline string concatenation:
 * NewsAPI's /everything endpoint has no `country` parameter at all (confirmed
 * against the current NewsAPI docs — /top-headlines' country parameter is now
 * documented as supporting only "us"). The only way to bias /everything toward
 * a country is to fold country terms into the `q` string itself. Keeping that
 * composition in one small, pure, unit-testable place means the same logic
 * cannot drift between call sites and is trivial to verify without any HTTP
 * mocking.
 */
public final class QueryComposer {

    private QueryComposer() { }

    /**
     * Combines a base query (category or free-text search, may be null) with a
     * country-boost query (may be null) using boolean AND.
     *
     * - both present -> "(base) AND (boost)" — narrows results to that country
     * - only one present -> that one, unchanged
     * - neither present -> null (caller treats this as "nothing to search for")
     */
    public static String withCountryBoost(String base, String countryBoost) {
        boolean hasBase = base != null && !base.isBlank();
        boolean hasBoost = countryBoost != null && !countryBoost.isBlank();

        if (hasBase && hasBoost) return "(" + base.trim() + ") AND (" + countryBoost.trim() + ")";
        if (hasBase) return base.trim();
        if (hasBoost) return countryBoost.trim();
        return null;
    }
}
