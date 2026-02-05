package com.vasan12sp.ratelimiter.customer.controller;

import com.vasan12sp.ratelimiter.customer.model.User;
import com.vasan12sp.ratelimiter.customer.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;


@Controller
@RequestMapping("/customer")
public class CustomerAuthController {

    private final CustomerService customerService;
    private final PasswordEncoder passwordEncoder;

    public CustomerAuthController(CustomerService customerService,
                                  PasswordEncoder passwordEncoder) {
        this.customerService = customerService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/signup")
    public String signupPage() {
        return "customer/signup";
    }

    @PostMapping("/signup")
    public String signup(@RequestParam String email,
                         @RequestParam String password,
                         @RequestParam String name,
                         @RequestParam String companyName,
                         RedirectAttributes redirectAttributes) {
        try {
            customerService.registerUser(email, password, name, companyName);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Account created! Please login.");
            return "redirect:/customer/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/customer/signup";
        }
    }

    @GetMapping("/login")
    public String loginPage() {
        return "customer/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {

        // Validate input
        if (email == null || email.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Email and password are required");
            return "redirect:/customer/login";
        }

        try {
            User user = customerService.authenticateUser(email.trim(), password);

            if (user != null) {
                // Set session attributes
                session.setAttribute("customerEmail", user.getEmail());
                session.setAttribute("userId", user.getId());
                session.setAttribute("companyId", user.getCompany().getId());

                // Manually set authentication for Spring Security
                List<GrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_CUSTOMER"),
                        new SimpleGrantedAuthority("ROLE_USER")
                );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user.getEmail(), null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);

                return "redirect:/customer/dashboard";
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid email or password");
                return "redirect:/customer/login";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Login failed: " + e.getMessage());
            return "redirect:/customer/login";
        }
    }


    @PostMapping("/logout")
    public String logout(HttpSession session,
                         HttpServletRequest request,
                         RedirectAttributes redirectAttributes) {
        session.invalidate();

        // Check if request is AJAX
        String ajaxHeader = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equals(ajaxHeader)) {
            return "redirect:/customer/login"; // AJAX will handle this
        }

        redirectAttributes.addFlashAttribute("successMessage", "Logged out successfully");
        return "redirect:/customer/login";
    }

}
