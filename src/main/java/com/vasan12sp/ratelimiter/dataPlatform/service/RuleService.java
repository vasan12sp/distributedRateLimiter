package com.vasan12sp.ratelimiter.dataPlatform.service;

import com.vasan12sp.ratelimiter.admin.model.ApiKey;
import com.vasan12sp.ratelimiter.admin.model.Company;
import com.vasan12sp.ratelimiter.admin.repository.ApiKeyRepository;
import com.vasan12sp.ratelimiter.dataPlatform.model.RateLimitRule;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RuleService {

    private final ApiKeyRepository apiKeyRepository;

    // Cache rules per API key - populated from database/Redis
    private final Map<String, Map<String, RateLimitRule>> ruleCache = new ConcurrentHashMap<>();

    public RuleService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    public RateLimitRule getRuleForRequest(String apiKey, String endpoint, String method) {
        // Find API key and its associated company (eagerly fetched)
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyValueWithCompany(apiKey);
        if (apiKeyOpt.isEmpty()) {
            return null; // Invalid API key
        }

        ApiKey apiKeyEntity = apiKeyOpt.get();
        Company company = apiKeyEntity.getCompany();

        // Get company's rules from cache or load from DB
        Map<String, RateLimitRule> companyRules = ruleCache.computeIfAbsent(
                apiKey,
                k -> loadCompanyRules(company)
        );

        // Try specific endpoint + method
        String specificKey = buildRuleKey(endpoint, method);
        RateLimitRule rule = companyRules.get(specificKey);
        if (rule != null) return rule;

        // Try endpoint with any method
        String endpointKey = buildRuleKey(endpoint, "*");
        rule = companyRules.get(endpointKey);
        if (rule != null) return rule;

        // Return company's default based on subscription plan tier
        String tier = "STARTER";
        return getDefaultRuleForTier(tier);
    }

    private Map<String, RateLimitRule> loadCompanyRules(Company company) {
        // In production: load from database table like `company_rate_rules`
        // For now, return empty - company uses tier defaults
        return new ConcurrentHashMap<>();
    }

    private RateLimitRule getDefaultRuleForTier(String tier) {
        return switch (tier.toUpperCase()) {
            case "ENTERPRISE" -> new RateLimitRule(10000, 60, "/**", "*");
            case "PROFESSIONAL" -> new RateLimitRule(1000, 60, "/**", "*");
            case "STARTER" -> new RateLimitRule(100, 60, "/**", "*");
            default -> RateLimitRule.defaultRule();
        };
    }

    private String buildRuleKey(String endpoint, String method) {
        return String.format("%s:%s", endpoint, method);
    }

    // Called when company updates their rules via admin portal
    public void invalidateCache(String apiKey) {
        ruleCache.remove(apiKey);
    }
}
