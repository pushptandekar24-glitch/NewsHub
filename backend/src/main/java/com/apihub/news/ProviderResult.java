package com.apihub.news;

import java.util.List;

/**
 * @param articles      normalized articles from one provider
 * @param totalResults  provider-reported total, used for pagination hints
 * @param provider      which provider produced this
 * @param error         null on success; a human-readable message on failure.
 *                      Failures are DATA, not exceptions, because the
 *                      aggregator needs to continue with the other provider.
 */
public record ProviderResult(
        List<NormalizedArticle> articles,
        long totalResults,
        String provider,
        String error
) {
    public static ProviderResult success(List<NormalizedArticle> articles, long total, String provider) {
        return new ProviderResult(articles, total, provider, null);
    }

    public static ProviderResult failure(String provider, String error) {
        return new ProviderResult(List.of(), 0, provider, error);
    }

    public boolean failed() {
        return error != null;
    }
}
