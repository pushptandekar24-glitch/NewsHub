package com.apihub.repository;

import com.apihub.entity.SavedArticle;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedArticleRepository extends JpaRepository<SavedArticle, Long> {

    Page<SavedArticle> findByUserIdOrderBySavedAtDesc(Long userId, Pageable pageable);

    Optional<SavedArticle> findByUserIdAndArticleId(Long userId, Long articleId);

    boolean existsByUserIdAndArticleId(Long userId, Long articleId);

    List<SavedArticle> findByUserId(Long userId);

    void deleteByUserIdAndArticleId(Long userId, Long articleId);
}
