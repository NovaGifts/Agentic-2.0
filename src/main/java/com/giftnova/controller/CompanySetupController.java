package com.giftnova.controller;

import com.giftnova.model.Company;
import com.giftnova.service.CompanyService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/setup")
public class CompanySetupController {

    private final CompanyService companyService;

    public CompanySetupController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    public String showForm(Model model) {
        model.addAttribute("company", new Company());
        return "setup";
    }

    @PostMapping
    public String submit(@Valid @ModelAttribute("company") Company company,
                         BindingResult result,
                         RedirectAttributes redirectAttrs) {
        if (result.hasErrors()) {
            return "setup";
        }
        if (companyService.exists(company.getName())) {
            result.rejectValue("name", "duplicate", "A company with this name already exists.");
            return "setup";
        }
        Company saved = companyService.save(company);
        redirectAttrs.addFlashAttribute("successMessage",
                "Welcome aboard, " + saved.getName() + "! Your pilot is ready.");
        return "redirect:/setup/success";
    }

    @GetMapping("/success")
    public String success() {
        return "success";
    }
}
