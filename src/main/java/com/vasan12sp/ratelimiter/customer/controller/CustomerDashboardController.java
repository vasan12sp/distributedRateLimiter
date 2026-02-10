package com.vasan12sp.ratelimiter.customer.controller;

import com.vasan12sp.ratelimiter.admin.model.ApiKey;
import com.vasan12sp.ratelimiter.admin.model.RateLimitRuleEntity;
import com.vasan12sp.ratelimiter.customer.model.User;
import com.vasan12sp.ratelimiter.customer.service.CustomerService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        List<ApiKey> activeKeys = customerService.getActiveApiKeysForUser(user);
        List<ApiKey> revokedKeys = customerService.getRevokedApiKeysForUser(user);
        model.addAttribute("activeApiKeys", activeKeys);
        model.addAttribute("revokedApiKeys", revokedKeys);
        model.addAttribute("company", user.getCompany());
        model.addAttribute("totalKeyCount", activeKeys.size() + revokedKeys.size());
        return "customer/apikeys";
    }

    @PostMapping("/apikeys/generate")
    public Object generateApiKey(HttpSession session,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(session);
        boolean isAjax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With")) ||
                (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"));

        if (user == null) {
            if (isAjax) {
                Map<String, String> body = new HashMap<>();
                body.put("error", "unauthenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
            }
            return "redirect:/customer/login";
        }

        try {
            ApiKey apiKey = customerService.generateApiKey(user);
            if (isAjax) {
                Map<String, String> body = new HashMap<>();
                body.put("apiKey", apiKey.getKeyValue());
                return ResponseEntity.ok(body);
            } else {
                // Avoid placing raw key in flash; show a generic notice instead
                redirectAttributes.addFlashAttribute("successMessage", "API Key generated");
            }
        } catch (Exception e) {
            if (isAjax) {
                Map<String, String> body = new HashMap<>();
                body.put("error", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            }
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

    @GetMapping("/rules/edit/{id}")
    public String editRuleForm(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/customer/login";

        try {
            RateLimitRuleEntity rule = customerService.getRuleForUser(user, id);
            model.addAttribute("rule", rule);
            return "customer/rule-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/customer/rules";
        }
    }

    @PostMapping("/rules/{id}/update")
    public String updateRule(@PathVariable Long id,
                             @RequestParam String endpoint,
                             @RequestParam String httpMethod,
                             @RequestParam int allowedRequestCount,
                             @RequestParam int windowSizeSeconds,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/customer/login";

        try {
            customerService.updateRule(user, id, endpoint, httpMethod, allowedRequestCount, windowSizeSeconds);
            redirectAttributes.addFlashAttribute("successMessage", "Rule updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/customer/rules";
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
    public Object deleteRule(@PathVariable Long ruleId,
                             HttpSession session,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(session);
        boolean isAjax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With")) ||
                (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"));
        if (user == null) {
            if (isAjax) {
                Map<String, String> body = new HashMap<>();
                body.put("error", "unauthenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
            }
            return "redirect:/customer/login";
        }

        try {
            customerService.deleteRule(user, ruleId);
            if (isAjax) {
                Map<String, String> body = new HashMap<>();
                body.put("status", "deleted");
                return ResponseEntity.ok(body);
            }
            redirectAttributes.addFlashAttribute("successMessage", "Rule deleted successfully");
        } catch (Exception e) {
            if (isAjax) {
                Map<String, String> body = new HashMap<>();
                body.put("error", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            }
        }
        return "redirect:/customer/rules";
    }


    private User getAuthenticatedUser(HttpSession session) {
        String email = (String) session.getAttribute("customerEmail");
        if (email == null) return null;
        return customerService.findByEmail(email);
    }
}
