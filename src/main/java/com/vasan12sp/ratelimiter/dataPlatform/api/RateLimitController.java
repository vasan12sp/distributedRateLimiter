package com.vasan12sp.ratelimiter.dataPlatform.api;

import com.vasan12sp.ratelimiter.admin.model.RateLimitRuleEntity;
import com.vasan12sp.ratelimiter.admin.repository.RuleRepository;
import com.vasan12sp.ratelimiter.admin.repository.ApiKeyRepository;
import com.vasan12sp.ratelimiter.dataPlatform.dto.RateLimitRequest;
import com.vasan12sp.ratelimiter.dataPlatform.dto.RateLimitResponse;
import com.vasan12sp.ratelimiter.dataPlatform.dto.ErrorResponse;
import com.vasan12sp.ratelimiter.dataPlatform.redis.SlidingWindowRedisRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class RateLimitController {

    private final SlidingWindowRedisRepository redisRepository;
    private final RuleRepository ruleRepository;
    private final ApiKeyRepository apiKeyRepository;

    private static final int DEFAULT_MAX_REQUESTS = 100;
    private static final int DEFAULT_WINDOW_SECONDS = 60;

    public RateLimitController(SlidingWindowRedisRepository redisRepository,
                               RuleRepository ruleRepository,
                               ApiKeyRepository apiKeyRepository) {
        this.redisRepository = redisRepository;
        this.ruleRepository = ruleRepository;
        this.apiKeyRepository = apiKeyRepository;
    }

    @PostMapping("/check")
    public ResponseEntity<?> checkRateLimit(
            @RequestHeader("X-API-Key") String apiKey,
            @RequestBody RateLimitRequest request) {

        Long companyId = apiKeyRepository.findByKeyValue(apiKey)
                .filter(key -> key.isActive())
                .map(key -> key.getCompany().getId())
                .orElse(null);

        if (companyId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid or inactive API key"));
        }

        RateLimitRuleEntity rule = findMatchingRule(
                companyId,
                request.endpoint(),
                request.method()
        );

        int maxRequests = (rule != null) ? rule.getAllowedRequestCount() : DEFAULT_MAX_REQUESTS;
        int windowSeconds = (rule != null) ? rule.getWindowSizeSeconds() : DEFAULT_WINDOW_SECONDS;

        String key = String.format("rate_limit:%d:%s:%s:%s",
                companyId,
                request.identifier(),
                request.endpoint(),
                request.method()
        );

        long currentTimeMillis = System.currentTimeMillis();

        // Use the Lua script execution method that returns everything
        var result = redisRepository.executeRateLimitCheck(
                key,
                maxRequests,
                windowSeconds,
                currentTimeMillis
        );

        return ResponseEntity.ok(new RateLimitResponse(
                result.allowed(),
                (int) result.retryAfterSeconds(),
                (int) result.remaining()
        ));
    }


    private RateLimitRuleEntity findMatchingRule(Long companyId, String endpoint, String httpMethod) {
        List<RateLimitRuleEntity> rules = ruleRepository.findByCompanyId(companyId);

        Optional<RateLimitRuleEntity> match = rules.stream()
                .filter(rule -> matchesEndpoint(rule.getEndpoint(), endpoint))
                .filter(rule -> rule.getHttpMethod().equals(httpMethod) || rule.getHttpMethod().equals("*"))
                .findFirst();

        return match.orElse(null);
    }

    private boolean matchesEndpoint(String pattern, String endpoint) {
        if (pattern.equals(endpoint)) {
            return true;
        }
        if (pattern.contains("*")) {
            String regex = pattern.replace("*", ".*");
            return endpoint.matches(regex);
        }
        return false;
    }
}
