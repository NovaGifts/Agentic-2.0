package com.giftnova.controller;

import com.giftnova.model.Company;
import com.giftnova.service.CompanyService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
public class LoginController {

    private final CompanyService companyService;
    private final BCryptPasswordEncoder encoder;

    public LoginController(CompanyService companyService, BCryptPasswordEncoder encoder) {
        this.companyService = companyService;
        this.encoder        = encoder;
    }

    @GetMapping("/login")
    public String showLogin(HttpSession session) {
        if (session.getAttribute("companyId") != null) {
            return "redirect:/companies/" + session.getAttribute("companyId") + "/events";
        }
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String companyName,
                          @RequestParam String password,
                          HttpSession session,
                          Model model) {
        Optional<Company> company = companyService.findByName(companyName);
        if (company.isPresent()
                && company.get().getPasswordHash() != null
                && encoder.matches(password, company.get().getPasswordHash())) {
            session.setAttribute("companyId", company.get().getId());
            return "redirect:/companies/" + company.get().getId() + "/events";
        }
        model.addAttribute("error", "Invalid company name or password.");
        model.addAttribute("companyName", companyName);
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
