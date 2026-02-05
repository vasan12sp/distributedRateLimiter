package com.vasan12sp.ratelimiter.dataPlatform.redis;


import com.vasan12sp.ratelimiter.dataPlatform.model.SlidingWindowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;

@Repository
public class SlidingWindowRedisRepository {

    private static final Logger log = LoggerFactory.getLogger(SlidingWindowRedisRepository.class);

    private final RedisTemplate<String, String> redisTemplate;
    private DefaultRedisScript<List> slidingWindowScript;

    public SlidingWindowRedisRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        slidingWindowScript = new DefaultRedisScript<>();
        slidingWindowScript.setScriptSource(
                new ResourceScriptSource(new ClassPathResource("scripts/SlidingWindowLuaScript.lua"))
        );
        slidingWindowScript.setResultType(List.class);

        log.info("Sliding Window Counter Lua script loaded successfully");
    }

    private SlidingWindowResult mapToResult(List<Long> luaResult, long maxRequests) {
        if (luaResult == null || luaResult.size() < 3) {
            log.warn("Invalid Lua script result, applying fail-open");
            return SlidingWindowResult.allowed(0, maxRequests);
        }

        // Lua script returns: [allowed (0/1), current_count, retry_after_seconds]
        long allowed = luaResult.get(0);
        long currentCount = luaResult.get(1);
        long retryAfterSeconds = luaResult.get(2);

        if (allowed == 1) {
            long remaining = Math.max(0, maxRequests - currentCount);
            return SlidingWindowResult.allowed(currentCount, remaining);
        } else {
            return SlidingWindowResult.denied(currentCount, retryAfterSeconds);
        }
    }


    public SlidingWindowResult executeRateLimitCheck(
            String key,
            long maxRequests,
            long windowSizeSeconds,
            long currentTimeMillis) {

        List<String> keys = Collections.singletonList(key);

        // Execute the Lua script atomically
        @SuppressWarnings("unchecked")
        List<Long> result = redisTemplate.execute(
                slidingWindowScript,
                keys,
                String.valueOf(maxRequests),
                String.valueOf(windowSizeSeconds),
                String.valueOf(currentTimeMillis)
        );

        return mapToResult(result, maxRequests);
    }

    public boolean allowRequest(String key, int maxRequests, int windowSeconds, long currentTimeMillis) {
        try {
            SlidingWindowResult result = executeRateLimitCheck(
                    key,
                    maxRequests,
                    windowSeconds,
                    currentTimeMillis
            );
            return result.allowed();
        } catch (Exception e) {
            log.error("Redis error during rate limiting for key: {}", key, e);
            return true; // Fail open to prevent service disruption
        }
    }

}
