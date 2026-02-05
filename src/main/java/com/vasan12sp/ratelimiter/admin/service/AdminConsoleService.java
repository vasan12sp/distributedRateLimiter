package com.vasan12sp.ratelimiter.admin.service;

import com.vasan12sp.ratelimiter.admin.model.ApiKey;
import com.vasan12sp.ratelimiter.admin.model.Company;
import com.vasan12sp.ratelimiter.admin.model.RateLimitRuleEntity;
import com.vasan12sp.ratelimiter.admin.repository.ApiKeyRepository;
import com.vasan12sp.ratelimiter.admin.repository.CompanyRepository;
import com.vasan12sp.ratelimiter.admin.repository.RuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AdminConsoleService {

    private static final Logger logger = LoggerFactory.getLogger(AdminConsoleService.class);

    private final CompanyRepository companyRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final RuleRepository ruleRepository;

    public AdminConsoleService(CompanyRepository companyRepository,
                               ApiKeyRepository apiKeyRepository,
                               RuleRepository ruleRepository) {
        this.companyRepository = companyRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.ruleRepository = ruleRepository;
    }

    // Company operations
    public Company createCompany(String name) {
        if (companyRepository.existsByName(name)) {
            throw new IllegalArgumentException("Company with name '" + name + "' already exists");
        }
        Company company = new Company(name);
        Company saved = companyRepository.save(company);
        logger.info("Created company: {} with ID: {}", name, saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Company> getCompanyById(Long id) {
        return companyRepository.findById(id);
    }

    public void deleteCompany(Long id) {
        companyRepository.deleteById(id);
        logger.info("Deleted company with ID: {}", id);
    }

    // API Key operations
    public ApiKey generateApiKey(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));

        ApiKey apiKey = new ApiKey(company);
        company.addApiKey(apiKey);
        ApiKey saved = apiKeyRepository.save(apiKey);
        logger.info("Generated API key for company: {}", companyId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ApiKey> getApiKeysForCompany(Long companyId) {
        return apiKeyRepository.findByCompanyId(companyId);
    }

    public void revokeApiKey(Long apiKeyId) {
        apiKeyRepository.findById(apiKeyId).ifPresent(apiKey -> {
            apiKey.setActive(false);
            apiKeyRepository.save(apiKey);
            logger.info("Revoked API key: {}", apiKeyId);
        });
    }

    // Rule operations
    public RateLimitRuleEntity createRule(Long companyId, String endpoint,
                                          String httpMethod, int allowedRequestCount,
                                          int windowSizeSeconds) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));

        RateLimitRuleEntity rule = new RateLimitRuleEntity(
                endpoint, httpMethod, allowedRequestCount, windowSizeSeconds);
        rule.setCompany(company);
        company.addRule(rule);

        RateLimitRuleEntity saved = ruleRepository.save(rule);
        logger.info("Created rate limit rule for company: {}, endpoint: {}", companyId, endpoint);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<RateLimitRuleEntity> getAllRules() {
        return ruleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<RateLimitRuleEntity> getRulesForCompany(Long companyId) {
        return ruleRepository.findByCompanyId(companyId);
    }

    public RateLimitRuleEntity updateRule(Long ruleId, int allowedRequestCount,
                                          int windowSizeSeconds) {
        RateLimitRuleEntity rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleId));

        rule.setAllowedRequestCount(allowedRequestCount);
        rule.setWindowSizeSeconds(windowSizeSeconds);

        RateLimitRuleEntity saved = ruleRepository.save(rule);
        logger.info("Updated rule: {}", ruleId);
        return saved;
    }

    public void deleteRule(Long ruleId) {
        ruleRepository.deleteById(ruleId);
        logger.info("Deleted rule: {}", ruleId);
    }
}
