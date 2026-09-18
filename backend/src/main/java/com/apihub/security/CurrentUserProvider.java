package com.apihub.security;

import com.apihub.entity.User;
import com.apihub.exception.ResourceNotFoundException;
import com.apihub.repository.UserRepository;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Single place to answer "who is calling?". Services depend on this instead of
 * reaching into SecurityContextHolder directly, which keeps them testable.
 */
@Component
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public CurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<Long> currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser principal) {
            return Optional.of(principal.getId());
        }
        return Optional.empty();
    }

    public boolean isAuthenticated() {
        return currentUserId().isPresent();
    }

    /** For endpoints already behind .authenticated() — absence is a bug, not a 401. */
    public User requireCurrentUser() {
        Long id = currentUserId().orElseThrow(
                () -> new ResourceNotFoundException("No authenticated user in the security context"));
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
