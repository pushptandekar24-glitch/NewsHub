package com.apihub.controller;

import com.apihub.common.ApiResult;
import com.apihub.dto.CategoryResponse;
import com.apihub.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categories", description = "News categories (database-driven, admin-editable)")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "All active categories, in display order")
    public ResponseEntity<ApiResult<List<CategoryResponse>>> list() {
        return ResponseEntity.ok(ApiResult.ok(categoryService.getActiveCategories()));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "One category by slug")
    public ResponseEntity<ApiResult<CategoryResponse>> get(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResult.ok(categoryService.getBySlug(slug)));
    }
}
