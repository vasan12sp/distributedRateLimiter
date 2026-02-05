package com.vasan12sp.ratelimiter.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

@Service
public class MetricsService {

    private final MeterRegistry meterRegistry;
    @Getter
    private final RateLimitMetrics rateLimitMetrics;

    public MetricsService(MeterRegistry meterRegistry, RateLimitMetrics rateLimitMetrics) {
        this.meterRegistry = meterRegistry;
        this.rateLimitMetrics = rateLimitMetrics;
    }

    /**
     * Record a rate limit decision with tags for the endpoint and company
     */
    public void recordRateLimitDecision(String endpoint, String companyId, boolean allowed) {
        List<Tag> tags = List.of(
                Tag.of("endpoint", sanitizeEndpoint(endpoint)),
                Tag.of("company_id", companyId),
                Tag.of("decision", allowed ? "allowed" : "blocked")
        );

        meterRegistry.counter("ratelimiter.decisions", tags).increment();
    }

    /**
     * Time a Redis operation with tags
     */
    public <T> T timeRedisOperation(String operation, Supplier<T> supplier) {
        Timer.Sample sample = rateLimitMetrics.startRedisTimer();
        try {
            return supplier.get();
        } finally {
            rateLimitMetrics.stopRedisTimer(sample);
        }
    }

    /**
     * Time a callable operation
     */
    public <T> T timeOperation(String name, Callable<T> operation) throws Exception {
        Timer timer = Timer.builder("ratelimiter.operation." + name)
                .description("Timing for " + name + " operation")
                .register(meterRegistry);

        return timer.recordCallable(operation);
    }

    /**
     * Record a custom counter
     */
    public void incrementCounter(String name, String... tags) {
        meterRegistry.counter("ratelimiter." + name, tags).increment();
    }

    /**
     * Sanitize endpoint for use as a metric tag value
     */
    private String sanitizeEndpoint(String endpoint) {
        if (endpoint == null) {
            return "unknown";
        }
        // Replace path variables with placeholders
        return endpoint.replaceAll("/[0-9]+", "/{id}")
                .replaceAll("[^a-zA-Z0-9/_{}]", "_");
    }

}
