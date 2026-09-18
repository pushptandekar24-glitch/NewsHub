package com.apihub.controller;

import com.apihub.common.ApiResult;
import com.apihub.dto.auth.UpdateInterestsRequest;
import com.apihub.dto.auth.UserResponse;
import com.apihub.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Profile and interest management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Current user's profile and interests")
    public ResponseEntity<ApiResult<UserResponse>> me() {
        return ResponseEntity.ok(ApiResult.ok(userService.getCurrentUserProfile()));
    }

    @PutMapping("/me/interests")
    @Operation(summary = "Replace the current user's interest categories")
    public ResponseEntity<ApiResult<UserResponse>> updateInterests(
            @Valid @RequestBody UpdateInterestsRequest request) {
        return ResponseEntity.ok(
                ApiResult.ok(userService.updateInterests(request.interestSlugs()), "Interests updated"));
    }
}
