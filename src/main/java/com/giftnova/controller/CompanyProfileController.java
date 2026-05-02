package com.giftnova.controller;

import com.giftnova.model.Company;
import com.giftnova.service.CompanyService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/companies/{companyId}")
public class CompanyProfileController {

    private final CompanyService companyService;
    private final BCryptPasswordEncoder encoder;

    public CompanyProfileController(CompanyService companyService, BCryptPasswordEncoder encoder) {
        this.companyService = companyService;
        this.encoder        = encoder;
    }

    @GetMapping("/change-password")
    public String changePasswordForm(@PathVariable Long companyId, Model model) {
        model.addAttribute("company", companyService.findById(companyId));
        return "company/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@PathVariable Long companyId,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes ra) {
        Company company = companyService.findById(companyId);

        if (!encoder.matches(currentPassword, company.getPasswordHash())) {
            ra.addFlashAttribute("error", "Current password is incorrect.");
            return "redirect:/companies/" + companyId + "/change-password";
        }
        if (newPassword == null || newPassword.length() < 6) {
            ra.addFlashAttribute("error", "New password must be at least 6 characters.");
            return "redirect:/companies/" + companyId + "/change-password";
        }
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Passwords do not match.");
            return "redirect:/companies/" + companyId + "/change-password";
        }

        company.setPasswordHash(encoder.encode(newPassword));
        companyService.save(company);
        ra.addFlashAttribute("toast", "Password updated successfully.");
        return "redirect:/companies/" + companyId + "/events";
    }

    @PostMapping("/delete")
    public String deleteAccount(@PathVariable Long companyId,
                                @RequestParam String confirmPassword,
                                HttpSession session,
                                RedirectAttributes ra) {
        Company company = companyService.findById(companyId);
        if (!encoder.matches(confirmPassword, company.getPasswordHash())) {
            ra.addFlashAttribute("deleteError", "Incorrect password. Account was not deleted.");
            return "redirect:/companies/" + companyId + "/events";
        }
        companyService.deleteCompany(companyId);
        session.invalidate();
        return "redirect:/login";
    }
}
