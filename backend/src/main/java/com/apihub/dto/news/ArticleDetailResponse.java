package com.apihub.dto.news;

import java.util.List;

/**
 * The article DETAIL page payload.
 *
 * COPYRIGHT NOTE: `description` is the provider-supplied snippet only. We never
 * store or serve the publisher's full body text. `summary` and `keyPoints` are
 * clearly flagged via `summaryGenerated` so the UI can label them
 * "AI-generated summary" and keep attribution on the original source.
 */
public record ArticleDetailResponse(
        ArticleResponse article,
        String summary,
        List<String> keyPoints,
        String whyItMatters,
        boolean summaryGenerated,
        List<ArticleResponse> related,
        long clickCount
) { }
