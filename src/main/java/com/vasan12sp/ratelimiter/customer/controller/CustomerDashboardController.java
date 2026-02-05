package com.vasan12sp.ratelimiter.customer.controller;

import com.vasan12sp.ratelimiter.admin.model.ApiKey;
import com.vasan12sp.ratelimiter.admin.model.RateLimitRuleEntity;
import com.vasan12sp.ratelimiter.customer.model.User;
import com.vasan12sp.ratelimiter.customer.service.CustomerService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/customer")
public class CustomerDashboardController {

    private final CustomerService customerService;

    public CustomerDashboardController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/customer/login";

        model.addAttribute("user", user);
        model.addAttribute("company", user.getCompany());
        return "customer/dashboard";
    }

    @GetMapping("/apikeys")
    public String apiKeys(HttpSession session, Model model) {
        User user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/customer/login";

        List<ApiKey> apiKeys = customerService.getApiKeysForUser(user);
        model.addAttribute("apiKeys", apiKeys);
        model.addAttribute("company", user.getCompany());
        return "customer/apikeys";
    }

    @PostMapping("/apikeys/generate")
    public String generateApiKey(HttpSession session, RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/customer/login";

        try {
            ApiKey apiKey = customerService.generateApiKey(user);
            redirectAttributes.addFlashAttribute("successMessage",
                    "API Key generated: " + apiKey.getKeyValue());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/customer/apikeys";
    }

    @PostMapping("/apikeys/{keyId}/revoke")
    public String revokeApiKey(@PathVariable Long keyId,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/customer/login";

        try {
            customerService.revokeApiKey(user, keyId);
            redirectAttributes.addFlashAttribute("successMessage", "API Key revoked");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/customer/apikeys";
    }

    @GetMapping("/rules")
    public String rules(HttpSession session, Model model) {
        User user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/customer/login";

        List<RateLimitRuleEntity> rules = customerService.getRulesForUser(user);
        model.addAttribute("rules", rules);
        return "customer/rules";
    }

    @GetMapping("/rules/new")
    public String newRuleForm(HttpSession session, Model model) {
        User user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/customer/login";

        model.addAttribute("rule", new RateLimitRuleEntity());
        return "customer/rule-form";
    }

    @PostMapping("/rules/create")
    public String createRule(@RequestParam String endpoint,
                             @RequestParam String httpMethod,
                             @RequestParam int allowedRequestCount,
                             @RequestParam int windowSizeSeconds,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/customer/login";

        try {
            customerService.createRule(user, endpoint, httpMethod, allowedRequestCount, windowSizeSeconds);
            redirectAttributes.addFlashAttribute("successMessage", "Rule created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/customer/rules";
    }

    @PostMapping("/rules/{ruleId}/delete")
    public String deleteRule(@PathVariable Long ruleId,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/customer/login";

        try {
            customerService.deleteRule(user, ruleId);
            redirectAttributes.addFlashAttribute("successMessage", "Rule deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/customer/rules";
    }


    private User getAuthenticatedUser(HttpSession session) {
        String email = (String) session.getAttribute("customerEmail");
        if (email == null) return null;
        return customerService.findByEmail(email);
    }
}
