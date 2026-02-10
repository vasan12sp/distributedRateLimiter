package com.vasan12sp.ratelimiter.dataPlatform.filter;

import com.vasan12sp.ratelimiter.admin.model.RateLimitRuleEntity;
import com.vasan12sp.ratelimiter.admin.repository.RuleRepository;
import com.vasan12sp.ratelimiter.dataPlatform.redis.SlidingWindowRedisRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final SlidingWindowRedisRepository slidingWindowRedisRepository;
    private final RuleRepository ruleRepository;

    private static final int DEFAULT_ALLOWED_REQUESTS = 100;
    private static final int DEFAULT_WINDOW_SECONDS = 60;

    public RateLimitingFilter(SlidingWindowRedisRepository slidingWindowRedisRepository,
                              RuleRepository ruleRepository) {
        this.slidingWindowRedisRepository = slidingWindowRedisRepository;
        this.ruleRepository = ruleRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/customer") ||
                path.startsWith("/admin") ||
                path.startsWith("/h2-console") ||
                path.equals("/") ||
                path.equals("/docs") ||
                path.equals("/home") ||
                path.startsWith("/actuator") ||
                path.equals("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
            return;
        }

        String companyId = (String) authentication.getPrincipal();
        String endpoint = request.getRequestURI();
        String httpMethod = request.getMethod();

        String userId = extractUserId(request);

        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"User identification required\"}");
            return;
        }

        RateLimitRuleEntity rule = findMatchingRule(Long.parseLong(companyId), endpoint, httpMethod);

        int allowedRequests = (rule != null) ? rule.getAllowedRequestCount() : DEFAULT_ALLOWED_REQUESTS;
        int windowSeconds = (rule != null) ? rule.getWindowSizeSeconds() : DEFAULT_WINDOW_SECONDS;

        String key = "rate_limit:" + companyId + ":" + userId + ":" + endpoint + ":" + httpMethod;
        long currentTime = System.currentTimeMillis();

        boolean allowed = slidingWindowRedisRepository.allowRequest(
                key,
                allowedRequests,
                windowSeconds,
                currentTime
        );

        if (!allowed) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractUserId(HttpServletRequest request) {
        // Primary method: X-User-Id header
        String userId = request.getHeader("X-User-Id");
        if (userId != null && !userId.isEmpty()) {
            return userId;
        }

        // Fallback: Use API key as user identifier
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null && !apiKey.isEmpty()) {
            return apiKey;
        }

        return null;
    }


    private RateLimitRuleEntity findMatchingRule(Long companyId, String endpoint, String httpMethod) {
        List<RateLimitRuleEntity> rules = ruleRepository.findByCompanyId(companyId);

        Optional<RateLimitRuleEntity> exactMatch = rules.stream()
                .filter(rule -> matchesEndpoint(rule.getEndpoint(), endpoint))
                .filter(rule -> rule.getHttpMethod().equals(httpMethod) || rule.getHttpMethod().equals("*"))
                .findFirst();

        return exactMatch.orElse(null);
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
