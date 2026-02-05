package com.vasan12sp.ratelimiter.metrics;

import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RateLimitMetrics {

    private static final String METRIC_PREFIX = "ratelimiter";

    private final Counter totalRequests;
    private final Counter allowedRequests;
    private final Counter blockedRequests;
    private final Counter redisErrors;
    private final Timer redisLatency;
    private final Timer requestLatency;

    public RateLimitMetrics(MeterRegistry meterRegistry) {
        // Total requests counter
        this.totalRequests = Counter.builder(METRIC_PREFIX + ".requests.total")
                .description("Total number of rate limit check requests")
                .register(meterRegistry);

        // Allowed requests counter
        this.allowedRequests = Counter.builder(METRIC_PREFIX + ".requests.allowed")
                .description("Number of requests that were allowed")
                .register(meterRegistry);

        // Blocked requests counter
        this.blockedRequests = Counter.builder(METRIC_PREFIX + ".requests.blocked")
                .description("Number of requests that were blocked due to rate limiting")
                .register(meterRegistry);

        // Redis errors counter
        this.redisErrors = Counter.builder(METRIC_PREFIX + ".redis.errors")
                .description("Number of Redis operation errors")
                .register(meterRegistry);

        // Redis operation latency timer
        this.redisLatency = Timer.builder(METRIC_PREFIX + ".redis.latency")
                .description("Latency of Redis operations")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(meterRegistry);

        // Overall request processing latency
        this.requestLatency = Timer.builder(METRIC_PREFIX + ".request.latency")
                .description("End-to-end latency of rate limit check requests")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(meterRegistry);

        // Gauge for current rate (updated periodically)
        Gauge.builder(METRIC_PREFIX + ".current.rate", this, RateLimitMetrics::getCurrentRate)
                .description("Current request rate (requests per second)")
                .register(meterRegistry);
    }

    /**
     * Record a rate limit check request
     */
    public void recordRequest() {
        totalRequests.increment();
    }

    /**
     * Record an allowed request
     */
    public void recordAllowed() {
        allowedRequests.increment();
    }

    /**
     * Record a blocked request
     */
    public void recordBlocked() {
        blockedRequests.increment();
    }

    /**
     * Record a Redis error
     */
    public void recordRedisError() {
        redisErrors.increment();
    }

    /**
     * Record Redis operation latency
     */
    public void recordRedisLatency(long durationNanos) {
        redisLatency.record(durationNanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Record Redis operation latency in milliseconds
     */
    public void recordRedisLatencyMs(long durationMs) {
        redisLatency.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Record overall request processing latency
     */
    public void recordRequestLatency(long durationNanos) {
        requestLatency.record(durationNanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Time a Redis operation
     */
    public Timer.Sample startRedisTimer() {
        return Timer.start();
    }

    /**
     * Stop a Redis timer and record the duration
     */
    public void stopRedisTimer(Timer.Sample sample) {
        sample.stop(redisLatency);
    }

    /**
     * Time a request operation
     */
    public Timer.Sample startRequestTimer() {
        return Timer.start();
    }

    /**
     * Stop a request timer and record the duration
     */
    public void stopRequestTimer(Timer.Sample sample) {
        sample.stop(requestLatency);
    }

    /**
     * Get the current request rate (approximate)
     */
    private double getCurrentRate() {
        return totalRequests.count();
    }

    /**
     * Get total request count
     */
    public double getTotalRequests() {
        return totalRequests.count();
    }

    /**
     * Get allowed request count
     */
    public double getAllowedRequests() {
        return allowedRequests.count();
    }

    /**
     * Get blocked request count
     */
    public double getBlockedRequests() {
        return blockedRequests.count();
    }

    /**
     * Get Redis error count
     */
    public double getRedisErrors() {
        return redisErrors.count();
    }

    /**
     * Calculate block rate percentage
     */
    public double getBlockRate() {
        double total = totalRequests.count();
        if (total == 0) {
            return 0.0;
        }
        return (blockedRequests.count() / total) * 100;
    }
}
