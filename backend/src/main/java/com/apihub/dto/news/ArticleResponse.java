package com.apihub.dto.news;

import com.apihub.entity.NewsArticle;
import java.time.Instant;

/** The shape rendered by an article CARD in the grid. */
public record ArticleResponse(
        Long id,
        String title,
        String description,
        String source,
        String author,
        String imageUrl,
        String articleUrl,
        String categorySlug,
        String categoryName,
        String country,
        String language,
        String provider,
        Instant publishedAt,
        boolean saved,
        Integer trendRank,
        Integer sourceCoverage
) {
    public static ArticleResponse from(NewsArticle a, boolean saved) {
        return from(a, saved, null, null);
    }

    public static ArticleResponse from(NewsArticle a, boolean saved, Integer trendRank, Integer sourceCoverage) {
        return new ArticleResponse(
                a.getId(), a.getTitle(), a.getDescription(), a.getSource(), a.getAuthor(),
                a.getImageUrl(), a.getArticleUrl(),
                a.getCategory() == null ? null : a.getCategory().getSlug(),
                a.getCategory() == null ? null : a.getCategory().getName(),
                a.getCountry(), a.getLanguage(), a.getProvider(),
                a.getPublishedAt(), saved, trendRank, sourceCoverage);
    }
}
