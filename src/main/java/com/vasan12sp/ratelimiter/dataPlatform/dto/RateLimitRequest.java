package com.vasan12sp.ratelimiter.dataPlatform.dto;

import jakarta.validation.constraints.NotBlank;

public record RateLimitRequest(
        @NotBlank
        String identifier,  // Add this field
        @NotBlank
        String endpoint,
        @NotBlank
        String method
) {}
