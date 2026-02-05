package com.vasan12sp.ratelimiter.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class CustomerAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String uri = request.getRequestURI();

        // Skip authentication check for login/signup pages and static resources
        if (uri.equals("/customer/login") ||
                uri.equals("/customer/signup")) {
            return true;
        }

        // Check if user is authenticated
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("customerEmail") == null) {
            response.sendRedirect("/customer/login");
            return false;
        }

        return true;
    }
}
