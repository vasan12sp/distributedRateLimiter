package com.vasan12sp.ratelimiter.admin.controller;

import com.vasan12sp.ratelimiter.admin.model.ApiKey;
import com.vasan12sp.ratelimiter.admin.model.Company;
import com.vasan12sp.ratelimiter.admin.service.AdminConsoleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/companies")
public class CompanyController {

    private final AdminConsoleService adminConsoleService;

    public CompanyController(AdminConsoleService adminConsoleService) {
        this.adminConsoleService = adminConsoleService;
    }

    @GetMapping
    public String listCompanies(Model model) {
        List<Company> companies = adminConsoleService.getAllCompanies();
        model.addAttribute("companies", companies);
        model.addAttribute("newCompany", new Company());
        return "companies";
    }

    @PostMapping
    public String createCompany(@RequestParam("name") String name,
                                RedirectAttributes redirectAttributes) {
        try {
            adminConsoleService.createCompany(name);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Company '" + name + "' created successfully");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/companies";
    }

    @PostMapping("/{id}/delete")
    public String deleteCompany(@PathVariable Long id,
                                RedirectAttributes redirectAttributes) {
        try {
            adminConsoleService.deleteCompany(id);
            redirectAttributes.addFlashAttribute("successMessage", "Company deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to delete company: " + e.getMessage());
        }
        return "redirect:/admin/companies";
    }

    @GetMapping("/{id}/apikeys")
    public String viewApiKeys(@PathVariable Long id, Model model) {
        Company company = adminConsoleService.getCompanyById(id)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));
        List<ApiKey> apiKeys = adminConsoleService.getApiKeysForCompany(id);

        model.addAttribute("company", company);
        model.addAttribute("apiKeys", apiKeys);
        return "apikeys";
    }

    @PostMapping("/{id}/apikeys/generate")
    public String generateApiKey(@PathVariable Long id,
                                 RedirectAttributes redirectAttributes) {
        try {
            ApiKey apiKey = adminConsoleService.generateApiKey(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "API Key generated: " + apiKey.getKeyValue());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to generate API key: " + e.getMessage());
        }
        return "redirect:/admin/companies/" + id + "/apikeys";
    }

    @PostMapping("/{companyId}/apikeys/{keyId}/revoke")
    public String revokeApiKey(@PathVariable Long companyId,
                               @PathVariable Long keyId,
                               RedirectAttributes redirectAttributes) {
        adminConsoleService.revokeApiKey(keyId);
        redirectAttributes.addFlashAttribute("successMessage", "API Key revoked successfully");
        return "redirect:/admin/companies/" + companyId + "/apikeys";
    }
}
