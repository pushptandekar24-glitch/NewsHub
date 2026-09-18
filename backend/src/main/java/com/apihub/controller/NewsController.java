package com.apihub.controller;

import com.apihub.common.ApiResult;
import com.apihub.common.PageResponse;
import com.apihub.dto.news.ArticleDetailResponse;
import com.apihub.dto.news.ArticleResponse;
import com.apihub.exception.BadRequestException;
import com.apihub.news.Countries;
import com.apihub.news.NewsQuery;
import com.apihub.security.CurrentUserProvider;
import com.apihub.service.NewsService;
import com.apihub.service.SavedArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public news API.
 *
 * Every list endpoint accepts the same filter vocabulary:
 *   page, pageSize (alias: limit), category, country, language, q, from, to, sort
 */
@RestController
@RequestMapping("/api/news")
@Tag(name = "News", description = "Global news feed, search, trending and article details")
public class NewsController {

    private final NewsService newsService;
    private final SavedArticleService savedArticleService;
    private final CurrentUserProvider currentUserProvider;

    public NewsController(NewsService newsService, SavedArticleService savedArticleService,
                          CurrentUserProvider currentUserProvider) {
        this.newsService = newsService;
        this.savedArticleService = savedArticleService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    @Operation(summary = "Browse the feed, optionally filtered by category and country")
    public ResponseEntity<ApiResult<PageResponse<ArticleResponse>>> feed(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String country,
            @RequestParam(defaultValue = "en") String language,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        validateCountry(country);
        int size = resolvePageSize(pageSize, limit);

        return ResponseEntity.ok(ApiResult.ok(newsService.getFeed(
                category, country, language, page, size, parseDate(from, "from"), parseDate(to, "to"))));
    }

    @GetMapping("/search")
    @Operation(summary = "Search across both providers, newest or most relevant first")
    public ResponseEntity<ApiResult<PageResponse<ArticleResponse>>> search(
            @RequestParam String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String country,
            @RequestParam(defaultValue = "en") String language,
            @RequestParam(defaultValue = "publishedAt") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        if (q == null || q.isBlank()) {
            throw new BadRequestException("Search query 'q' is required");
        }
        validateCountry(country);
        int size = resolvePageSize(pageSize, limit);

        return ResponseEntity.ok(ApiResult.ok(newsService.search(
                q, category, country, language, sort, page, size,
                parseDate(from, "from"), parseDate(to, "to"))));
    }

    @GetMapping("/category/{slug}")
    @Operation(summary = "Feed for one category, using that category's targeted query")
    public ResponseEntity<ApiResult<PageResponse<ArticleResponse>>> byCategory(
            @PathVariable String slug,
            @RequestParam(required = false) String country,
            @RequestParam(defaultValue = "en") String language,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Integer limit) {

        validateCountry(country);
        return ResponseEntity.ok(ApiResult.ok(newsService.getFeed(
                slug, country, language, page, resolvePageSize(pageSize, limit), null, null)));
    }

    @GetMapping("/country/{code}")
    @Operation(summary = "Feed for one country/region")
    public ResponseEntity<ApiResult<PageResponse<ArticleResponse>>> byCountry(
            @PathVariable String code,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "en") String language,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Integer limit) {

        validateCountry(code);
        return ResponseEntity.ok(ApiResult.ok(newsService.getFeed(
                category, code, language, page, resolvePageSize(pageSize, limit), null, null)));
    }

    @GetMapping("/trending")
    @Operation(summary = "Trending stories, scored by recency + source coverage + real clicks")
    public ResponseEntity<ApiResult<PageResponse<ArticleResponse>>> trending(
            @RequestParam(defaultValue = "12") int limit) {
        return ResponseEntity.ok(ApiResult.ok(newsService.getTrending(Math.min(Math.max(limit, 1), 50))));
    }

    @GetMapping("/personalized")
    @Operation(summary = "Feed built from the signed-in user's selected interests")
    public ResponseEntity<ApiResult<PageResponse<ArticleResponse>>> personalized(
            @RequestParam(required = false) String country,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Integer limit) {

        var user = currentUserProvider.requireCurrentUser();
        return ResponseEntity.ok(ApiResult.ok(newsService.getPersonalized(
                user, country, page, resolvePageSize(pageSize, limit))));
    }

    @GetMapping("/saved/list")
    @Operation(summary = "The current user's saved articles")
    public ResponseEntity<ApiResult<PageResponse<ArticleResponse>>> saved(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(ApiResult.ok(
                savedArticleService.listSaved(page, resolvePageSize(pageSize, limit))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Article detail: summary, key points, related stories, source link")
    public ResponseEntity<ApiResult<ArticleDetailResponse>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(newsService.getArticleDetail(id)));
    }

    @PostMapping("/{id}/save")
    @Operation(summary = "Toggle bookmark for an article")
    public ResponseEntity<ApiResult<Map<String, Boolean>>> toggleSave(@PathVariable Long id) {
        boolean nowSaved = savedArticleService.toggleSave(id);
        return ResponseEntity.ok(ApiResult.ok(Map.of("saved", nowSaved),
                nowSaved ? "Article saved" : "Article removed from saved"));
    }

    // ----------------------------------------------------------- helpers

    /** `pageSize` is the documented name; `limit` is kept as an alias. */
    private int resolvePageSize(Integer pageSize, Integer limit) {
        int value = pageSize != null ? pageSize
                  : limit != null ? limit
                  : NewsQuery.DEFAULT_PAGE_SIZE;
        return Math.min(Math.max(value, 1), NewsQuery.MAX_PAGE_SIZE);
    }

    /** Accepts either a full ISO instant or a plain yyyy-MM-dd date. */
    private Instant parseDate(String value, String field) {
        if (value == null || value.isBlank()) return null;
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant();
            } catch (DateTimeParseException ex) {
                throw new BadRequestException(
                        "Invalid '" + field + "' date. Use yyyy-MM-dd or an ISO-8601 instant.");
            }
        }
    }

    private void validateCountry(String code) {
        if (code != null && !code.isBlank() && !Countries.isSupported(code)) {
            throw new BadRequestException("Unsupported country code '" + code
                    + "'. See GET /api/countries for the supported list.");
        }
    }
}
