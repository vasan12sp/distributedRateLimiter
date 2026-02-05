package com.vasan12sp.ratelimiter.dataPlatform.model;

public record SlidingWindowResult(
        boolean allowed,
        long currentCount,
        long remaining,
        long retryAfterSeconds
) {
    public static SlidingWindowResult allowed(long currentCount, long remaining) {
        return new SlidingWindowResult(true, currentCount, remaining, 0);
    }

    public static SlidingWindowResult denied(long currentCount, long retryAfterSeconds) {
        return new SlidingWindowResult(false, currentCount, 0, retryAfterSeconds);
    }
}
