package com.vasan12sp.ratelimiter.security;

import com.vasan12sp.ratelimiter.customer.model.User;
import com.vasan12sp.ratelimiter.customer.service.CustomerService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class SessionAuthFilter extends OncePerRequestFilter {

    private final CustomerService customerService;

    public SessionAuthFilter(@Lazy CustomerService customerService) {
        this.customerService = customerService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        String requestURI = request.getRequestURI();

        // Skip filter for public endpoints
        if (requestURI.equals("/customer/login") ||
                requestURI.equals("/customer/signup") ||
                requestURI.startsWith("/css") ||
                requestURI.startsWith("/js") ||
                requestURI.startsWith("/images") ||
                requestURI.equals("/") ||
                requestURI.equals("/home") ||
                requestURI.equals("/docs") ||
                requestURI.equals("/error")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check if this is a customer route that requires authentication
        if (requestURI.startsWith("/customer/")) {
            if (session == null || session.getAttribute("customerEmail") == null) {
                // No valid session - redirect to login
                response.sendRedirect("/customer/login");
                return;
            }

            // Valid session - set authentication
            String email = (String) session.getAttribute("customerEmail");
            List<GrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_CUSTOMER"),
                    new SimpleGrantedAuthority("ROLE_USER")
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(email, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }


}
