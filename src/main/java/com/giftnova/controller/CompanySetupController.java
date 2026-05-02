package com.giftnova.controller;

import com.giftnova.model.Company;
import com.giftnova.service.CompanyService;
import com.giftnova.service.PolicyService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles the company onboarding setup flow (Phase 1).
 * After a company is created, default gift policies are seeded automatically.
 *
 * Routes:
 *   GET  /setup          → show the company setup form
 *   POST /setup          → validate + save company, seed policies, redirect to success
 *   GET  /setup/success  → confirmation page
 */
@Controller
@RequestMapping("/setup")
public class CompanySetupController {

    private final CompanyService companyService;
    private final PolicyService policyService;

    public CompanySetupController(CompanyService companyService, PolicyService policyService) {
        this.companyService = companyService;
        this.policyService  = policyService;
    }

    // Shows the blank company setup form
    @GetMapping
    public String showForm(Model model) {
        model.addAttribute("company", new Company());
        return "setup";
    }

    @PostMapping
    public String submit(@Valid @ModelAttribute("company") Company company,
                         BindingResult result,
                         RedirectAttributes redirectAttrs) {
        // Return to form if validation annotations (@NotBlank, @Email, etc.) failed
        if (result.hasErrors()) {
            return "setup";
        }
        // Prevent duplicate company names
        if (companyService.exists(company.getName())) {
            result.rejectValue("name", "duplicate", "A company with this name already exists.");
            return "setup";
        }
        Company saved = companyService.save(company);

        // Seed the four default gift policies (Birthday, Work Anniversary, Onboarding, Manager Appreciation)
        policyService.initDefaults(saved.getId());

        // Pass companyId via flash so the success page can link to the policy builder
        redirectAttrs.addFlashAttribute("successMessage",
                "Welcome aboard, " + saved.getName() + "! Your pilot is ready.");
        redirectAttrs.addFlashAttribute("companyId", saved.getId());
        return "redirect:/setup/success";
    }

    // Success confirmation page — shown after company is created
    @GetMapping("/success")
    public String success() {
        return "success";
    }
}
