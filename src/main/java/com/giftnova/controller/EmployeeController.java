package com.giftnova.controller;

import com.giftnova.dto.CsvUploadResult;
import com.giftnova.repository.UpcomingEventRepository;
import com.giftnova.service.CompanyService;
import com.giftnova.service.EmployeeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles Phase 3: Employee CSV upload and upcoming events display.
 *
 * Routes:
 *   GET  /companies/{id}/employees        → upload form + employee list
 *   POST /companies/{id}/employees/upload → parse CSV, validate, save
 *   GET  /companies/{id}/events           → upcoming gift events
 */
@Controller
@RequestMapping("/companies/{companyId}")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final UpcomingEventRepository eventRepo;
    private final CompanyService companyService;

    public EmployeeController(EmployeeService employeeService,
                               UpcomingEventRepository eventRepo,
                               CompanyService companyService) {
        this.employeeService = employeeService;
        this.eventRepo       = eventRepo;
        this.companyService  = companyService;
    }

    // Shows the CSV upload form and the list of already-uploaded employees
    @GetMapping("/employees")
    public String showUpload(@PathVariable Long companyId, Model model) {
        model.addAttribute("company",   companyService.findById(companyId));
        model.addAttribute("employees", employeeService.getEmployees(companyId));
        return "employees/upload";
    }

    // Processes the uploaded CSV file; shows errors inline if validation fails
    @PostMapping("/employees/upload")
    public String upload(@PathVariable Long companyId,
                         @RequestParam("file") MultipartFile file,
                         RedirectAttributes redirectAttrs) {
        if (file.isEmpty()) {
            redirectAttrs.addFlashAttribute("uploadError", "Please select a CSV file.");
            return "redirect:/companies/" + companyId + "/employees";
        }
        try {
            CsvUploadResult result = employeeService.upload(companyId, file);
            redirectAttrs.addFlashAttribute("uploadResult", result);
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("uploadError", "Failed to read file: " + e.getMessage());
        }
        return "redirect:/companies/" + companyId + "/employees";
    }

    // Shows upcoming events generated from employee birthdays and start dates
    @GetMapping("/events")
    public String showEvents(@PathVariable Long companyId, Model model) {
        model.addAttribute("company", companyService.findById(companyId));
        model.addAttribute("events",  eventRepo.findByCompanyIdOrderByEventDate(companyId));
        return "employees/events";
    }
}
