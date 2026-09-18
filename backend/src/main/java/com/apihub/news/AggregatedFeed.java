package com.apihub.news;

import java.util.List;

/**
 * The merged output of every provider for one query.
 *
 * @param articles   deduplicated, relevance-filtered, newest-first
 * @param totalAvailable best estimate of how many results exist upstream,
 *                       used to drive pagination
 * @param errors     one message per provider that failed; empty on full success
 * @param allFailed  true when no provider returned anything usable
 */
public record AggregatedFeed(
        List<NormalizedArticle> articles,
        long totalAvailable,
        List<String> errors,
        boolean allFailed
) { }
