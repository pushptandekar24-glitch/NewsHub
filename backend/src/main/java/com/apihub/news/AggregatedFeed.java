package com.apihub.news;

import java.util.List;

/**
 * The merged output of every provider for one query.
 *
 * @param articles   deduplicated, relevance-filtered, newest-first
 * @param totalAvailable best estimate of how many results exist upstream,
 *                       used to drive pagination
 * @param errors     one RAW message per provider that failed; empty on full
 *                   success. Kept verbatim (e.g. "GNews request limit
 *                   reached") for logs and for developer-facing surfaces like
 *                   the API Explorer — normal-user UI should prefer
 *                   {@link #friendlyMessage()} instead of joining these.
 * @param allFailed  true when no provider returned anything usable
 */
public record AggregatedFeed(
        List<NormalizedArticle> articles,
        long totalAvailable,
        List<String> errors,
        boolean allFailed
) {
    /** Some providers failed, but at least one succeeded. */
    public boolean isPartial() {
        return !errors.isEmpty() && !allFailed;
    }

    /**
     * One of four states, matching how the frontend should treat the response:
     *   OK                  — every configured provider succeeded, results exist
     *   PARTIAL              — some providers failed, but there are still results
     *   PARTIAL_NO_RESULTS   — some providers failed AND nothing relevant came back
     *   NO_RESULTS            — every provider succeeded, but nothing matched
     *   ALL_FAILED            — no provider returned anything usable (caller throws)
     */
    public String status() {
        if (allFailed) return "ALL_FAILED";
        if (isPartial()) return articles.isEmpty() ? "PARTIAL_NO_RESULTS" : "PARTIAL";
        return articles.isEmpty() ? "NO_RESULTS" : "OK";
    }

    /**
     * A single, user-safe sentence for the states where something is worth
     * telling a normal reader about. Returns null for OK and NO_RESULTS — those
     * are communicated by the page itself (a full grid, or its empty state)
     * rather than a banner.
     *
     * Deliberately generic rather than naming which provider failed: a normal
     * reader does not need to know "GNews" or "NewsAPI" by name, and providers
     * are an implementation detail of this application, not something the
     * person using it chose. The raw provider-specific detail is still in
     * {@link #errors()} and in the backend logs for debugging.
     */
    public String friendlyMessage() {
        return switch (status()) {
            case "PARTIAL" ->
                "Some news sources are temporarily unavailable. Showing available results from other providers.";
            case "PARTIAL_NO_RESULTS" ->
                "Some news sources are temporarily unavailable, and no matching stories were found from the rest right now.";
            case "ALL_FAILED" ->
                "Unable to load news right now. Please try again shortly.";
            default -> null;
        };
    }
}
