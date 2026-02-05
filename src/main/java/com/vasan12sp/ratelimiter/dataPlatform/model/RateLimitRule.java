package com.vasan12sp.ratelimiter.dataPlatform.model;

public record RateLimitRule(
        long maxRequests,
        long windowSizeSeconds,
        String endpointPattern,
        String httpMethod
) {
    public static RateLimitRule defaultRule() {
        return new RateLimitRule(100, 60, "/**", "*");
    }
}
