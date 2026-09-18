package com.apihub.service;

import com.apihub.common.PageResponse;
import com.apihub.dto.news.ArticleResponse;
import com.apihub.entity.NewsArticle;
import com.apihub.entity.SavedArticle;
import com.apihub.entity.User;
import com.apihub.exception.ResourceNotFoundException;
import com.apihub.repository.NewsArticleRepository;
import com.apihub.repository.SavedArticleRepository;
import com.apihub.security.CurrentUserProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SavedArticleService {

    private final SavedArticleRepository savedArticleRepository;
    private final NewsArticleRepository articleRepository;
    private final CurrentUserProvider currentUserProvider;

    public SavedArticleService(SavedArticleRepository savedArticleRepository,
                               NewsArticleRepository articleRepository,
                               CurrentUserProvider currentUserProvider) {
        this.savedArticleRepository = savedArticleRepository;
        this.articleRepository = articleRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public boolean toggleSave(Long articleId) {
        User user = currentUserProvider.requireCurrentUser();
        NewsArticle article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article", articleId));

        var existing = savedArticleRepository.findByUserIdAndArticleId(user.getId(), articleId);
        if (existing.isPresent()) {
            savedArticleRepository.delete(existing.get());
            return false;   // now unsaved
        }
        savedArticleRepository.save(new SavedArticle(user, article));
        return true;        // now saved
    }

    @Transactional(readOnly = true)
    public PageResponse<ArticleResponse> listSaved(int page, int size) {
        User user = currentUserProvider.requireCurrentUser();

        var pageResult = savedArticleRepository
                .findByUserIdOrderBySavedAtDesc(user.getId(), PageRequest.of(page, size))
                .map(s -> ArticleResponse.from(s.getArticle(), true));

        return PageResponse.from(pageResult);
    }
}
