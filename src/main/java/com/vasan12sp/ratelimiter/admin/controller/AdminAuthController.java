package com.vasan12sp.ratelimiter.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminAuthController {

    @Value("${admin.secret.key}")
    private String adminSecretKey;

    @GetMapping("/login")
    public String loginPage() {
        return "admin/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String secretKey,
                        HttpSession session,
                        RedirectAttributes redirectAttributes,
                        HttpServletRequest request) {
        if (adminSecretKey.equals(secretKey)) {
            // Invalidate old session (prevent session fixation)
            session.invalidate();
            session = request.getSession(true);

            session.setAttribute("adminAuthenticated", true);
            session.setMaxInactiveInterval(60 * 60); // 1 hour

            return "redirect:/admin/dashboard";
        } else {
            redirectAttributes.addFlashAttribute("error", "Invalid secret key");
            return "redirect:/admin/login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session,
                         HttpServletRequest request,
                         RedirectAttributes redirectAttributes) {
        // Clear all session attributes first
        if (session != null) {
            session.removeAttribute("userId");
            session.removeAttribute("companyId");
            session.removeAttribute("customerEmail");
            session.removeAttribute("userEmail");
            session.invalidate();
        }

        redirectAttributes.addFlashAttribute("successMessage", "Logged out successfully");
        return "redirect:/customer/login";
    }


    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        return "admin/dashboard";
    }
}
