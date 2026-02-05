package com.vasan12sp.ratelimiter.admin.controller;

import com.vasan12sp.ratelimiter.admin.model.Company;
import com.vasan12sp.ratelimiter.admin.model.RateLimitRuleEntity;
import com.vasan12sp.ratelimiter.admin.service.AdminConsoleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/rules")
public class RuleController {

    private final AdminConsoleService adminConsoleService;

    public RuleController(AdminConsoleService adminConsoleService) {
        this.adminConsoleService = adminConsoleService;
    }

    @GetMapping
    public String listRules(Model model) {
        List<RateLimitRuleEntity> rules = adminConsoleService.getAllRules();
        List<Company> companies = adminConsoleService.getAllCompanies();

        model.addAttribute("rules", rules);
        model.addAttribute("companies", companies);
        model.addAttribute("httpMethods", List.of("GET", "POST", "PUT", "DELETE", "PATCH"));
        return "rules";
    }

    @PostMapping
    public String createRule(@RequestParam("companyId") Long companyId,
                             @RequestParam("endpoint") String endpoint,
                             @RequestParam("httpMethod") String httpMethod,
                             @RequestParam("allowedRequestCount") int allowedRequestCount,
                             @RequestParam("windowSizeSeconds") int windowSizeSeconds,
                             RedirectAttributes redirectAttributes) {
        try {
            adminConsoleService.createRule(companyId, endpoint, httpMethod,
                    allowedRequestCount, windowSizeSeconds);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Rate limit rule created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to create rule: " + e.getMessage());
        }
        return "redirect:/admin/rules";
    }

    @PostMapping("/{id}/update")
    public String updateRule(@PathVariable Long id,
                             @RequestParam("allowedRequestCount") int allowedRequestCount,
                             @RequestParam("windowSizeSeconds") int windowSizeSeconds,
                             RedirectAttributes redirectAttributes) {
        try {
            adminConsoleService.updateRule(id, allowedRequestCount, windowSizeSeconds);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Rule updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to update rule: " + e.getMessage());
        }
        return "redirect:/admin/rules";
    }

    @PostMapping("/{id}/delete")
    public String deleteRule(@PathVariable Long id,
                             RedirectAttributes redirectAttributes) {
        try {
            adminConsoleService.deleteRule(id);
            redirectAttributes.addFlashAttribute("successMessage", "Rule deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to delete rule: " + e.getMessage());
        }
        return "redirect:/admin/rules";
    }
}
