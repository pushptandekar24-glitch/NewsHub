package com.apihub.common;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Slim pagination envelope. We do NOT return Spring's Page directly because its
 * JSON shape is unstable across versions and leaks internals to the frontend.
 *
 * `warnings` carries non-fatal problems — typically "one news provider is down
 * but the other answered" — so the UI can show a banner without the request
 * having to fail.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last,
        List<String> warnings
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast(), List.of());
    }

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        return of(content, page, size, totalElements, List.of());
    }

    public static <T> PageResponse<T> of(List<T> content, int page, int size,
                                         long totalElements, List<String> warnings) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages,
                page >= totalPages - 1, warnings == null ? List.of() : warnings);
    }
}
