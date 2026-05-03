package com.giftnova.controller;

import com.giftnova.model.Company;
import com.giftnova.service.AnalyticsService;
import com.giftnova.service.CompanyService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/companies/{companyId}/analytics")
public class AnalyticsController {

    private final CompanyService companyService;
    private final AnalyticsService analyticsService;

    public AnalyticsController(CompanyService companyService, AnalyticsService analyticsService) {
        this.companyService   = companyService;
        this.analyticsService = analyticsService;
    }

    @GetMapping
    public String dashboard(@PathVariable Long companyId, Model model) {
        Company company = companyService.findById(companyId);
        model.addAttribute("company", company);
        model.addAttribute("metrics", analyticsService.compute(companyId));
        model.addAttribute("currentMonth", java.time.YearMonth.now().format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")));
        return "analytics/dashboard";
    }
}
