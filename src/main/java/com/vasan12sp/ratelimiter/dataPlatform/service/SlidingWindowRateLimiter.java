package com.vasan12sp.ratelimiter.dataPlatform.service;


import com.vasan12sp.ratelimiter.dataPlatform.model.RateLimitKey;
import com.vasan12sp.ratelimiter.dataPlatform.model.RateLimitRule;
import com.vasan12sp.ratelimiter.dataPlatform.model.SlidingWindowResult;
import com.vasan12sp.ratelimiter.dataPlatform.redis.SlidingWindowRedisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SlidingWindowRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(SlidingWindowRateLimiter.class);

    private final SlidingWindowRedisRepository redisRepository;

    public SlidingWindowRateLimiter(SlidingWindowRedisRepository redisRepository) {
        this.redisRepository = redisRepository;
    }

    public SlidingWindowResult isAllowed(RateLimitKey key, RateLimitRule rule) {
        String redisKey = key.toRedisKey();
        long maxRequests = rule.maxRequests();
        long windowSizeSeconds = rule.windowSizeSeconds();
        long currentTimeMillis = System.currentTimeMillis();

        try {
            return redisRepository.executeRateLimitCheck(
                    redisKey,
                    maxRequests,
                    windowSizeSeconds,
                    currentTimeMillis
            );
        } catch (Exception e) {
            // FAIL-OPEN STRATEGY: If Redis is unavailable, allow the request
            // This is a design decision - in some systems you might want FAIL-CLOSED instead
            log.error("Redis unavailable, applying fail-open strategy for key: {}", redisKey, e);
            return SlidingWindowResult.allowed(0, maxRequests);
        }
    }
}
