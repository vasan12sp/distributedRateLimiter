package com.vasan12sp.ratelimiter.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class AdminSecretKeyFilter extends OncePerRequestFilter {

    @Value("${admin.secret.key}")
    private String adminSecretKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // Skip filter for login page
        if (requestURI.equals("/admin/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check if already authenticated via session
        HttpSession session = request.getSession(false);
        if (session != null && Boolean.TRUE.equals(session.getAttribute("adminAuthenticated"))) {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                setAdminAuthentication(request);
            }
            filterChain.doFilter(request, response);
            return;
        }

        // Check for secret key in header (for programmatic access)
        String secretKey = request.getHeader("X-Admin-Secret");
        if (secretKey != null && secretKey.equals(adminSecretKey)) {
            setAdminAuthentication(request);
            filterChain.doFilter(request, response);
            return;
        }

        // No authentication found - let Spring Security handle (redirect to login)
        filterChain.doFilter(request, response);
    }

    private void setAdminAuthentication(HttpServletRequest request) {
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_ADMIN");
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "admin",
                        null,
                        Collections.singletonList(authority)
                );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
