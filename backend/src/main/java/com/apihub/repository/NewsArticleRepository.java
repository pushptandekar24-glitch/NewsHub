package com.apihub.repository;

import com.apihub.entity.NewsArticle;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    Optional<NewsArticle> findByExternalId(String externalId);

    /**
     * Trending = real click data, recency-bounded. No fabricated numbers.
     * Articles with zero clicks fall back to "most recent" ordering.
     */
    @Query("""
           select a from NewsArticle a
           where a.publishedAt >= :since
           order by a.clickCount desc, a.publishedAt desc
           """)
    List<NewsArticle> findTrendingSince(@Param("since") Instant since, Pageable pageable);

    /** Candidate pool for trend scoring: everything recent, unranked. */
    @Query("select a from NewsArticle a where a.publishedAt >= :since order by a.publishedAt desc")
    List<NewsArticle> findRecentSince(@Param("since") Instant since, Pageable pageable);

    @Modifying
    @Query("update NewsArticle a set a.clickCount = a.clickCount + 1 where a.id = :id")
    void incrementClickCount(@Param("id") Long id);

    @Query("""
           select a from NewsArticle a
           where a.category.id = :categoryId and a.id <> :excludeId
           order by a.publishedAt desc
           """)
    List<NewsArticle> findRelatedByCategory(@Param("categoryId") Long categoryId,
                                            @Param("excludeId") Long excludeId,
                                            Pageable pageable);
}
