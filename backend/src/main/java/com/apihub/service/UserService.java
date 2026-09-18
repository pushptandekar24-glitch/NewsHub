package com.apihub.service;

import com.apihub.dto.auth.UserResponse;
import com.apihub.entity.User;
import com.apihub.exception.ResourceNotFoundException;
import com.apihub.repository.UserRepository;
import com.apihub.security.CurrentUserProvider;
import java.util.HashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CategoryService categoryService;
    private final CurrentUserProvider currentUserProvider;

    public UserService(UserRepository userRepository, CategoryService categoryService,
                       CurrentUserProvider currentUserProvider) {
        this.userRepository = userRepository;
        this.categoryService = categoryService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        Long id = currentUserProvider.requireCurrentUser().getId();
        User user = userRepository.findByIdWithInterests(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return UserResponse.from(user, true);
    }

    @Transactional
    public UserResponse updateInterests(List<String> slugs) {
        User user = currentUserProvider.requireCurrentUser();
        user.setInterests(new HashSet<>(categoryService.findBySlugs(slugs)));
        return UserResponse.from(userRepository.save(user), true);
    }
}
