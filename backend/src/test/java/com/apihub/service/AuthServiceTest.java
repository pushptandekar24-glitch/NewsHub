package com.apihub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.apihub.dto.auth.LoginRequest;
import com.apihub.dto.auth.RegisterRequest;
import com.apihub.entity.Role;
import com.apihub.entity.User;
import com.apihub.exception.DuplicateResourceException;
import com.apihub.repository.UserRepository;
import com.apihub.security.JwtService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock CategoryService categoryService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;

    @InjectMocks AuthService authService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new User("Asha", "asha@example.com", "hashed-pw", Role.USER);
    }

    @Test
    @DisplayName("registration hashes the password and never stores plaintext")
    void registerHashesPassword() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode("supersecret1")).thenReturn("bcrypt-hash");
        when(categoryService.findBySlugs(any())).thenReturn(List.of());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any(), anyString(), anyString())).thenReturn("jwt-token");

        var response = authService.register(
                new RegisterRequest("Asha", "Asha@Example.com", "supersecret1", List.of()));

        assertThat(response.token()).isEqualTo("jwt-token");
        verify(passwordEncoder).encode("supersecret1");
    }

    @Test
    @DisplayName("duplicate email registration is a 409, not a 400")
    void duplicateEmailRejected() {
        when(userRepository.existsByEmailIgnoreCase("asha@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Asha", "asha@example.com", "supersecret1", List.of())))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login with a wrong password fails with the same message as an unknown email")
    void wrongPasswordRejected() {
        when(userRepository.findByEmailIgnoreCase("asha@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrong", "hashed-pw")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("asha@example.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    @DisplayName("unknown email yields the same error — no account enumeration")
    void unknownEmailRejected() {
        when(userRepository.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "whatever")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
    }
}
