package com.apihub.service;

import com.apihub.dto.auth.AuthResponse;
import com.apihub.dto.auth.LoginRequest;
import com.apihub.dto.auth.RegisterRequest;
import com.apihub.dto.auth.UserResponse;
import com.apihub.entity.Role;
import com.apihub.entity.User;
import com.apihub.exception.DuplicateResourceException;
import com.apihub.repository.UserRepository;
import com.apihub.security.JwtService;
import java.util.HashSet;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CategoryService categoryService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, CategoryService categoryService,
                       PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.categoryService = categoryService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            // 409 Conflict, not 400 — the request was well-formed, the state conflicts.
            throw new DuplicateResourceException("An account with this email already exists");
        }

        User user = new User(
                request.name().trim(),
                request.email().trim().toLowerCase(),
                passwordEncoder.encode(request.password()),   // BCrypt, never plaintext
                Role.USER);

        user.setInterests(new HashSet<>(categoryService.findBySlugs(request.interestSlugs())));
        User saved = userRepository.save(user);

        return issueToken(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                // Same message for "no such user" and "wrong password" so the
                // endpoint cannot be used to enumerate registered emails.
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return issueToken(user);
    }

    private AuthResponse issueToken(User user) {
        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return AuthResponse.of(token, jwtService.getExpirationSeconds(),
                UserResponse.from(user, true));
    }
}
