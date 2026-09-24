package com.apihub.common;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Slim pagination envelope. We do NOT return Spring's Page directly because its
 * JSON shape is unstable across versions and leaks internals to the frontend.
 *
 * `status` and `message` distinguish the outcomes a news request can have, so
 * the frontend does not have to guess from an empty list alone whether that
 * means "nothing matched" or "a provider is down":
 *
 *   OK                  — normal, full result. message is null.
 *   PARTIAL              — some articles came back despite a provider issue.
 *   PARTIAL_NO_RESULTS   — a provider issue AND nothing relevant was found.
 *   NO_RESULTS            — every provider answered, nothing matched.
 *
 * `message` is the single user-facing sentence for the PARTIAL* and error
 * states (null otherwise) — see AggregatedFeed#friendlyMessage. `warnings`
 * keeps the raw, provider-specific detail (e.g. "GNews request limit
 * reached") for logs and developer-facing views like the API Explorer; normal
 * UI should prefer `message`.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last,
        String status,
        String message,
        List<String> warnings
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast(),
                page.getContent().isEmpty() ? "NO_RESULTS" : "OK", null, List.of());
    }

    /** Plain success — no provider metadata involved (e.g. saved articles, trending). */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        return of(content, page, size, totalElements, "OK", null, List.of());
    }

    /** Backward-compatible overload: derives status from warnings + content. */
    public static <T> PageResponse<T> of(List<T> content, int page, int size,
                                         long totalElements, List<String> warnings) {
        List<String> safeWarnings = warnings == null ? List.of() : warnings;
        String status = !safeWarnings.isEmpty()
                ? (content.isEmpty() ? "PARTIAL_NO_RESULTS" : "PARTIAL")
                : (content.isEmpty() ? "NO_RESULTS" : "OK");
        String message = safeWarnings.isEmpty() ? null
                : "Some news sources are temporarily unavailable. Showing available results from other providers.";
        return of(content, page, size, totalElements, status, message, safeWarnings);
    }

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements,
                                         String status, String message, List<String> warnings) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages,
                page >= totalPages - 1, status, message, warnings == null ? List.of() : warnings);
    }
}
