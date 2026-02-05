package com.vasan12sp.ratelimiter.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class ApiKeyAuthentication extends AbstractAuthenticationToken {

    private final String apiKey;
    private final Long companyId;
    private final String companyName;

    public ApiKeyAuthentication(String apiKey,
                                Long companyId,
                                String companyName,
                                Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.apiKey = apiKey;
        this.companyId = companyId;
        this.companyName = companyName;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return apiKey;
    }

    @Override
    public Object getPrincipal() {
        return companyName;
    }

    public String getApiKey() {
        return apiKey;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public String getCompanyName() {
        return companyName;
    }
}
