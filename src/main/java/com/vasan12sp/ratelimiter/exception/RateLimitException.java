package com.vasan12sp.ratelimiter.exception;

public class RateLimitException extends RuntimeException {

    private final long retryAfterSeconds;
    private final long remaining;
    private final long limit;

    public RateLimitException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
        this.remaining = 0;
        this.limit = 0;
    }

    public RateLimitException(String message, long retryAfterSeconds, long remaining, long limit) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
        this.remaining = remaining;
        this.limit = limit;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public long getRemaining() {
        return remaining;
    }

    public long getLimit() {
        return limit;
    }
}
