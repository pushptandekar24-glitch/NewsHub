package com.apihub.dto.auth;

import com.apihub.entity.User;
import java.time.Instant;
import java.util.List;
import java.util.Comparator;

/**
 * WHY a DTO instead of returning the User entity: the entity carries the
 * password hash and lazy JPA proxies. Serialising it would leak the hash and
 * risk LazyInitializationException. DTOs make the API contract explicit.
 */
public record UserResponse(
        Long id,
        String name,
        String email,
        String role,
        List<String> interests,
        Instant createdAt
) {
    public static UserResponse from(User user, boolean includeInterests) {
        List<String> interests = includeInterests
                ? user.getInterests().stream()
                      .sorted(Comparator.comparing(c -> c.getDisplayOrder() == null ? 0 : c.getDisplayOrder()))
                      .map(c -> c.getSlug())
                      .toList()
                : List.of();
        return new UserResponse(user.getId(), user.getName(), user.getEmail(),
                user.getRole().name(), interests, user.getCreatedAt());
    }
}
