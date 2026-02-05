package com.vasan12sp.ratelimiter.dataPlatform.model;

import java.util.Objects;

public record RateLimitKey(
        String apiKey,
        String endpoint,
        String httpMethod,
        String userId
) {
    public String toRedisKey() {
        String userPart = (userId != null && !userId.isBlank()) ? userId : "anonymous";
        String methodPart = (httpMethod != null && !httpMethod.isBlank()) ? httpMethod : "ALL";
        return String.format("rate_limit:%s:%s:%s:%s", apiKey, endpoint, methodPart, userPart);
    }

    public static RateLimitKey of(String apiKey, String endpoint, String httpMethod, String userId) {
        Objects.requireNonNull(apiKey, "API key cannot be null");
        Objects.requireNonNull(endpoint, "Endpoint cannot be null");
        return new RateLimitKey(apiKey, endpoint, httpMethod, userId);
    }
}
