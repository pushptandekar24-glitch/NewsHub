package com.apihub.service;

import com.apihub.dto.CategoryResponse;
import com.apihub.entity.Category;
import com.apihub.exception.ResourceNotFoundException;
import com.apihub.repository.CategoryRepository;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /** Cached: this list is read on every page load and changes only when an admin edits it. */
    @Cacheable("categories")
    @Transactional(readOnly = true)
    public List<CategoryResponse> getActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAsc()
                .stream().map(CategoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getBySlug(String slug) {
        return categoryRepository.findBySlugIgnoreCase(slug)
                .map(CategoryResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("No category with slug '" + slug + "'"));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> search(String term) {
        return categoryRepository.findByNameContainingIgnoreCaseAndActiveTrue(term)
                .stream().map(CategoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<Category> findBySlugs(List<String> slugs) {
        if (slugs == null || slugs.isEmpty()) return List.of();
        return slugs.stream()
                .map(categoryRepository::findBySlugIgnoreCase)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .toList();
    }

    @CacheEvict(cacheNames = "categories", allEntries = true)
    @Transactional
    public CategoryResponse setActive(Long id, boolean active) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        category.setActive(active);
        return CategoryResponse.from(categoryRepository.save(category));
    }
}
