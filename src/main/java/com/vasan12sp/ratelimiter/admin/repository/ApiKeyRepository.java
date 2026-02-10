package com.vasan12sp.ratelimiter.admin.repository;

import com.vasan12sp.ratelimiter.admin.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByKeyValue(String keyValue);

    @Query("SELECT a FROM ApiKey a JOIN FETCH a.company WHERE a.keyValue = :keyValue")
    Optional<ApiKey> findByKeyValueWithCompany(@Param("keyValue") String keyValue);

    List<ApiKey> findByCompanyId(Long companyId);

    // New convenience methods
    List<ApiKey> findByCompanyIdAndActiveOrderByCreatedAtDesc(Long companyId, boolean active);
}
