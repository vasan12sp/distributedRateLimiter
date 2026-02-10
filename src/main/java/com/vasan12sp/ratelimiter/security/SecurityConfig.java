package com.vasan12sp.ratelimiter.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ApiKeyAuthFilter apiKeyAuthFilter;
    private final SessionAuthFilter sessionAuthFilter;
    private final AdminSecretKeyFilter adminSecretKeyFilter;

    public SecurityConfig(ApiKeyAuthFilter apiKeyAuthFilter,
                          SessionAuthFilter sessionAuthFilter,
                          AdminSecretKeyFilter adminSecretKeyFilter) {
        this.apiKeyAuthFilter = apiKeyAuthFilter;
        this.sessionAuthFilter = sessionAuthFilter;
        this.adminSecretKeyFilter = adminSecretKeyFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers("/api/**")
                )

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(3)
                        .maxSessionsPreventsLogin(false)
                )

                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/", "/home", "/customer/signup", "/customer/login").permitAll()
                        .requestMatchers("/admin/login").permitAll()
                        .requestMatchers("/error", "/css/**", "/js/**", "/images/**").permitAll()

                        // API endpoints (API Key authentication)
                        .requestMatchers("/api/**").authenticated()

                        // Admin endpoints (Secret key authentication)
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Customer endpoints (Session authentication)
                        .requestMatchers("/customer/**").authenticated()

                        .anyRequest().permitAll()
                )

                .formLogin(form -> form.disable())  // Disable Spring's form login
                .httpBasic(httpBasic -> httpBasic.disable())  // Disable basic auth

                .logout(logout -> logout
                        .logoutUrl("/customer/logout")
                        .logoutSuccessUrl("/customer/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .clearAuthentication(true)
                        .permitAll()
                )

                // Add exception handling for unauthorized access
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (request.getRequestURI().startsWith("/customer/")) {
                                response.sendRedirect("/customer/login");
                            } else if (request.getRequestURI().startsWith("/admin/")) {
                                response.sendRedirect("/admin/login");
                            } else {
                                response.sendRedirect("/customer/login");
                            }
                        })
                )

                // Filter order matters!
                .addFilterBefore(adminSecretKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(sessionAuthFilter, AdminSecretKeyFilter.class)
                .addFilterBefore(apiKeyAuthFilter, SessionAuthFilter.class);

        return http.build();
    }



    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
