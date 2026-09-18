package com.apihub.dto.auth;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateInterestsRequest(
        @NotNull(message = "interestSlugs is required (send an empty list to clear)")
        List<String> interestSlugs
) { }
