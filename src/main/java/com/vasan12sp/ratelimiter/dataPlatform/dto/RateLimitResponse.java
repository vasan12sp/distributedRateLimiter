package com.vasan12sp.ratelimiter.dataPlatform.dto;


public record RateLimitResponse(
        boolean allowed,
        int retryAfterSeconds,
        int remaining
) {
    public static RateLimitResponse allowed(int remaining) {
        return new RateLimitResponse(true, 0, remaining);
    }

    public static RateLimitResponse denied(int retryAfterSeconds) {
        return new RateLimitResponse(false, retryAfterSeconds, 0);
    }
}
