package com.vasan12sp.ratelimiter.customer.service;

import com.vasan12sp.ratelimiter.admin.model.ApiKey;
import com.vasan12sp.ratelimiter.admin.model.Company;
import com.vasan12sp.ratelimiter.admin.model.RateLimitRuleEntity;
import com.vasan12sp.ratelimiter.admin.repository.ApiKeyRepository;
import com.vasan12sp.ratelimiter.admin.repository.CompanyRepository;
import com.vasan12sp.ratelimiter.admin.repository.RuleRepository;
import com.vasan12sp.ratelimiter.customer.model.User;
import com.vasan12sp.ratelimiter.customer.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;

@Service
public class
CustomerService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final RuleRepository rateLimitRuleRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerService(UserRepository userRepository,
                           CompanyRepository companyRepository,
                           ApiKeyRepository apiKeyRepository,
                           RuleRepository rateLimitRuleRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.rateLimitRuleRepository = rateLimitRuleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(String email, String password, String name, String companyName) {
        // Normalize inputs
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        String normalizedCompany = companyName == null ? null : companyName.trim();

        if (normalizedEmail == null || normalizedEmail.isEmpty()) {
            throw new IllegalArgumentException("Invalid email");
        }

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("Email already exists — please login");
        }
        if (companyRepository.existsByName(normalizedCompany)) {
            throw new IllegalArgumentException("Company name already taken");
        }

        // Create company
        Company company = new Company();
        company.setName(normalizedCompany);

        try {
            company = companyRepository.save(company);

            // Create user
            User user = new User();
            user.setEmail(normalizedEmail);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setName(name);
            user.setCompany(company);
            user.setRole("OWNER");

            return userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            // Likely a unique constraint violation (email or company) due to a race condition
            throw new IllegalArgumentException("Email or company already exists — please login or choose another company name");
        }
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public List<ApiKey> getApiKeysForUser(User user) {
        return apiKeyRepository.findByCompanyId(user.getCompany().getId());
    }

    @Transactional
    public ApiKey generateApiKey(User user) {
        ApiKey apiKey = new ApiKey();
        apiKey.setKeyValue(UUID.randomUUID().toString());
        apiKey.setCompany(user.getCompany());
        apiKey.setActive(true);
        return apiKeyRepository.save(apiKey);
    }

    @Transactional
    public void revokeApiKey(User user, Long keyId) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API Key not found"));

        if (!apiKey.getCompany().getId().equals(user.getCompany().getId())) {
            throw new SecurityException("Not authorized to revoke this key");
        }

        apiKey.setActive(false);
        apiKeyRepository.save(apiKey);
    }

    public List<RateLimitRuleEntity> getRulesForUser(User user) {
        return rateLimitRuleRepository.findByCompanyIdOrderByCreatedAtDesc(user.getCompany().getId());
    }

    public List<ApiKey> getActiveApiKeysForUser(User user) {
        return apiKeyRepository.findByCompanyIdAndActiveOrderByCreatedAtDesc(user.getCompany().getId(), true);
    }

    public List<ApiKey> getRevokedApiKeysForUser(User user) {
        return apiKeyRepository.findByCompanyIdAndActiveOrderByCreatedAtDesc(user.getCompany().getId(), false);
    }

    @Transactional
    public RateLimitRuleEntity createRule(User user, String endpoint, String httpMethod,
                                          int allowedRequestCount, int windowSizeSeconds) {
        RateLimitRuleEntity rule = new RateLimitRuleEntity();
        rule.setCompany(user.getCompany());
        rule.setEndpoint(endpoint);
        rule.setHttpMethod(httpMethod);
        rule.setAllowedRequestCount(allowedRequestCount);
        rule.setWindowSizeSeconds(windowSizeSeconds);
        return rateLimitRuleRepository.save(rule);
    }

    @Transactional
    public void deleteRule(User user, Long ruleId) {
        RateLimitRuleEntity rule = rateLimitRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found"));

        if (!rule.getCompany().getId().equals(user.getCompany().getId())) {
            throw new SecurityException("Not authorized to delete this rule");
        }

        rateLimitRuleRepository.delete(rule);
    }

    @Transactional(readOnly = true)
    public RateLimitRuleEntity getRuleForUser(User user, Long ruleId) {
        RateLimitRuleEntity rule = rateLimitRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found"));

        if (!rule.getCompany().getId().equals(user.getCompany().getId())) {
            throw new SecurityException("Not authorized to view this rule");
        }

        return rule;
    }

    @Transactional
    public RateLimitRuleEntity updateRule(User user, Long ruleId, String endpoint, String httpMethod,
                                          int allowedRequestCount, int windowSizeSeconds) {
        RateLimitRuleEntity rule = rateLimitRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found"));

        if (!rule.getCompany().getId().equals(user.getCompany().getId())) {
            throw new SecurityException("Not authorized to update this rule");
        }

        rule.setEndpoint(endpoint);
        rule.setHttpMethod(httpMethod);
        rule.setAllowedRequestCount(allowedRequestCount);
        rule.setWindowSizeSeconds(windowSizeSeconds);

        return rateLimitRuleRepository.save(rule);
    }

    public User authenticateUser(String email, String password) {
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return null;
        }

        // Verify password
        if (passwordEncoder.matches(password, user.getPasswordHash())) {
            return user;
        }

        return null;
    }


}
