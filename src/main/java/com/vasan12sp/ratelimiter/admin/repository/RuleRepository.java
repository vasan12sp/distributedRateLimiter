package com.vasan12sp.ratelimiter.admin.repository;

import com.vasan12sp.ratelimiter.admin.model.RateLimitRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RuleRepository extends JpaRepository<RateLimitRuleEntity, Long> {

    List<RateLimitRuleEntity> findByCompanyId(Long companyId);

    Optional<RateLimitRuleEntity> findByCompanyIdAndEndpointAndHttpMethod(
            Long companyId, String endpoint, String httpMethod);

    List<RateLimitRuleEntity> findByEndpointAndHttpMethod(String endpoint, String httpMethod);
}

