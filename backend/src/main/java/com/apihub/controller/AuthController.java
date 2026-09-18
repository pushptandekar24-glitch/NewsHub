package com.apihub.controller;

import com.apihub.common.ApiResult;
import com.apihub.dto.auth.AuthResponse;
import com.apihub.dto.auth.LoginRequest;
import com.apihub.dto.auth.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Register, login and token issuance")
public class AuthController {

    private final com.apihub.service.AuthService authService;

    public AuthController(com.apihub.service.AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Create an account and receive a JWT")
    public ResponseEntity<ApiResult<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        // 201 Created — a new resource now exists.
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.ok(authService.register(request), "Account created"));
    }

    @PostMapping("/login")
    @Operation(summary = "Exchange credentials for a JWT")
    public ResponseEntity<ApiResult<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResult.ok(authService.login(request), "Logged in"));
    }

    /**
     * Logout is client-side: the React app discards the token. A stateless JWT
     * cannot be invalidated server-side without a denylist, which is deliberately
     * out of scope here — see JwtService for the trade-off.
     */
    @PostMapping("/logout")
    @Operation(summary = "No-op endpoint; the client discards its token")
    public ResponseEntity<ApiResult<Void>> logout() {
        return ResponseEntity.ok(ApiResult.ok(null, "Discard the token on the client"));
    }
}
