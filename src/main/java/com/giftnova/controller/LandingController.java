package com.giftnova.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LandingController {

    @GetMapping("/")
    public String home(HttpSession session) {
        if (session.getAttribute("companyId") != null) {
            return "redirect:/companies/" + session.getAttribute("companyId") + "/calendar";
        }
        return "redirect:/login";
    }
}
