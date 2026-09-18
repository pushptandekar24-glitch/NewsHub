package com.apihub.common;

import java.time.Instant;

/**
 * Uniform success envelope for every endpoint.
 *
 * WHY: a consistent shape means the React layer can write ONE response handler
 * instead of special-casing each endpoint. Named ApiResult (not ApiResponse) to
 * avoid colliding with Swagger's @ApiResponse annotation.
 */
public record ApiResult<T>(boolean success, String message, T data, Instant timestamp) {

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(true, "OK", data, Instant.now());
    }

    public static <T> ApiResult<T> ok(T data, String message) {
        return new ApiResult<>(true, message, data, Instant.now());
    }
}
