package com.vasan12sp.ratelimiter.admin.repository;

import com.vasan12sp.ratelimiter.admin.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    boolean existsByName(String name);

    // Remove the incorrect findByApiKey() method
    // Add this correct method instead:
    @Query("SELECT c FROM Company c JOIN FETCH c.apiKeys k WHERE k.keyValue = :apiKey AND k.active = true")
    Optional<Company> findByApiKeyWithDetails(@Param("apiKey") String apiKey);
}
