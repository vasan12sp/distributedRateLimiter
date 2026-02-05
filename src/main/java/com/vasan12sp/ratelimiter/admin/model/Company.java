package com.vasan12sp.ratelimiter.admin.model;


import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApiKey> apiKeys = new ArrayList<>();

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RateLimitRuleEntity> rules = new ArrayList<>();

    public Company() {
        this.createdAt = LocalDateTime.now();
    }

    public Company(String name) {
        this.name = name;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<ApiKey> getApiKeys() {
        return apiKeys;
    }

    public void setApiKeys(List<ApiKey> apiKeys) {
        this.apiKeys = apiKeys;
    }

    public List<RateLimitRuleEntity> getRules() {
        return rules;
    }

    public void setRules(List<RateLimitRuleEntity> rules) {
        this.rules = rules;
    }

    public void addApiKey(ApiKey apiKey) {
        apiKeys.add(apiKey);
        apiKey.setCompany(this);
    }

    public void addRule(RateLimitRuleEntity rule) {
        rules.add(rule);
        rule.setCompany(this);
    }
}
