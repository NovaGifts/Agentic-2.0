package com.giftnova.controller;

import com.giftnova.model.Company;
import com.giftnova.service.CompanyService;
import com.giftnova.service.PolicyService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles the company onboarding setup flow.
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
    private final BCryptPasswordEncoder encoder;

    public CompanySetupController(CompanyService companyService, PolicyService policyService,
                                   BCryptPasswordEncoder encoder) {
        this.companyService = companyService;
        this.policyService  = policyService;
        this.encoder        = encoder;
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
                         HttpSession session,
                         RedirectAttributes redirectAttrs) {
        if (company.getPassword() == null || company.getPassword().isBlank()) {
            result.rejectValue("password", "required", "Password is required.");
            return "setup";
        }
        if (company.getPassword().length() < 6) {
            result.rejectValue("password", "size", "Password must be at least 6 characters.");
            return "setup";
        }
        if (result.hasErrors()) {
            return "setup";
        }
        if (companyService.exists(company.getName())) {
            result.rejectValue("name", "duplicate", "A company with this name already exists.");
            return "setup";
        }
        company.setPasswordHash(encoder.encode(company.getPassword()));
        Company saved = companyService.save(company);
        policyService.initDefaults(saved.getId());

        // Auto-login after signup
        session.setAttribute("companyId", saved.getId());

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
