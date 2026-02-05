package com.vasan12sp.ratelimiter.dataPlatform.service;

import com.vasan12sp.ratelimiter.dataPlatform.dto.RateLimitRequest;
import com.vasan12sp.ratelimiter.dataPlatform.dto.RateLimitResponse;
import com.vasan12sp.ratelimiter.dataPlatform.model.RateLimitKey;
import com.vasan12sp.ratelimiter.dataPlatform.model.RateLimitRule;
import com.vasan12sp.ratelimiter.dataPlatform.model.SlidingWindowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RateLimitDecisionService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitDecisionService.class);

    private final RuleService ruleService;
    private final SlidingWindowRateLimiter rateLimiter;

    public RateLimitDecisionService(RuleService ruleService,
                                    SlidingWindowRateLimiter rateLimiter) {
        this.ruleService = ruleService;
        this.rateLimiter = rateLimiter;
    }

    public RateLimitResponse checkRateLimit(String apiKey, RateLimitRequest request) {
        // Validate API key
        if (!isValidApiKey(apiKey)) {
            log.warn("Invalid API key: {}", apiKey);
            return new RateLimitResponse(false, 0, 0);
        }

        // Fetch the appropriate rule for this request
        RateLimitRule rule = ruleService.getRuleForRequest(
                apiKey,
                request.endpoint(),
                request.method()
        );

        // Build the rate limit key
        RateLimitKey key = RateLimitKey.of(
                apiKey,
                request.endpoint(),
                request.method(),
                request.identifier()  // Changed from userId() to identifier()
        );

        // Check rate limit using sliding window algorithm
        SlidingWindowResult result = rateLimiter.isAllowed(key, rule);

        return new RateLimitResponse(
                result.allowed(),
                (int) result.retryAfterSeconds(),  // Cast to int
                (int) result.remaining()           // Cast to int
        );
    }

    private boolean isValidApiKey(String apiKey) {
        // In production, validate against API key registry
        return apiKey != null && !apiKey.isBlank() && apiKey.length() >= 8;
    }
}
