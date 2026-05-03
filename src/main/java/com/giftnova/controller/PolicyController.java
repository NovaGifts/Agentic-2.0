package com.giftnova.controller;

import com.giftnova.dto.PolicyForm;
import com.giftnova.model.Company;
import com.giftnova.service.CompanyService;
import com.giftnova.service.PolicyService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles the Policy Builder admin screens.
 *
 * Routes:
 *   GET  /admin                        → redirects to latest company's policies (demo shortcut)
 *   GET  /admin/{companyId}/policies   → show policy builder form
 *   POST /admin/{companyId}/policies   → save policy changes + audit log
 *   GET  /admin/{companyId}/audit      → show full audit log
 */
@Controller
@RequestMapping("/admin")
public class PolicyController {

    private final PolicyService policyService;
    private final CompanyService companyService;

    public PolicyController(PolicyService policyService, CompanyService companyService) {
        this.policyService  = policyService;
        this.companyService = companyService;
    }

    // Convenience redirect — goes to latest company's policies, or setup if none exist yet
    @GetMapping
    public String adminHome() {
        try {
            Company latest = companyService.findLatest();
            return "redirect:/admin/" + latest.getId() + "/policies";
        } catch (IllegalStateException e) {
            return "redirect:/setup";
        }
    }

    // Loads the policy builder page for a specific company
    @GetMapping("/{companyId}/policies")
    public String showPolicies(@PathVariable Long companyId, Model model) {
        Company company = companyService.findById(companyId);

        // Build the form object that Thymeleaf will bind to
        PolicyForm form = new PolicyForm();
        form.setPolicies(policyService.getPolicies(companyId));
        form.setRestrictedCategories(company.getRestrictedCategories());
        form.setAllowedDepartments(company.getAllowedDepartments());
        form.setAllowedLocations(company.getAllowedLocations());
        form.setMonthlyBudget(company.getMonthlyBudget());

        model.addAttribute("company", company);
        model.addAttribute("policyForm", form);
        return "admin/policies";
    }

    // Receives the submitted policy form, saves changes, and logs them
    @PostMapping("/{companyId}/policies")
    public String savePolicies(@PathVariable Long companyId,
                                @ModelAttribute PolicyForm policyForm,
                                RedirectAttributes redirectAttrs) {
        try {
            Company company = companyService.findById(companyId);
            policyService.updatePolicies(companyId, policyForm, company.getAdminEmail());
            if (policyForm.getMonthlyBudget() != null) company.setMonthlyBudget(policyForm.getMonthlyBudget());
            companyService.save(company);
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", "Could not save policies: " + e.getMessage());
            return "redirect:/admin/" + companyId + "/policies";
        }
        return "redirect:/companies/" + companyId + "/employees";
    }

    // Shows the full audit trail of all policy changes for this company
    @GetMapping("/{companyId}/audit")
    public String showAuditLog(@PathVariable Long companyId, Model model) {
        model.addAttribute("company", companyService.findById(companyId));
        model.addAttribute("logs", policyService.getAuditLog(companyId));
        return "admin/audit";
    }
}
