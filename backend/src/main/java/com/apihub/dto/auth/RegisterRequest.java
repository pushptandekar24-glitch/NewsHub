package com.apihub.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Bean Validation runs BEFORE the controller body executes, so invalid input
 * never reaches the service layer. Failures are turned into a 400 by
 * GlobalExceptionHandler#handleValidation.
 */
public record RegisterRequest(

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email address")
        @Size(max = 180)
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be at least 8 characters")
        String password,

        /** Optional onboarding step: category slugs the user cares about. */
        List<String> interestSlugs
) { }
