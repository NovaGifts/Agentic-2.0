package com.giftnova.controller;

import com.giftnova.dto.CsvUploadResult;
import com.giftnova.model.UpcomingEvent;
import com.giftnova.repository.UpcomingEventRepository;
import com.giftnova.service.CompanyService;
import com.giftnova.service.EmployeeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles Employee CSV upload and upcoming events display.
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

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy");

    @GetMapping("/events")
    public String showEvents(@PathVariable Long companyId, Model model) {
        List<UpcomingEvent> all = eventRepo.findByCompanyIdOrderByEventDate(companyId);
        Map<String, List<UpcomingEvent>> byMonth = all.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getEventDate().format(MONTH_FMT),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        model.addAttribute("company",       companyService.findById(companyId));
        model.addAttribute("eventsByMonth", byMonth);
        model.addAttribute("totalEvents",   all.size());
        return "employees/events";
    }

    @PostMapping("/events/{eventId}/status")
    public String updateEventStatus(@PathVariable Long companyId,
                                    @PathVariable Long eventId,
                                    @RequestParam String status) {
        UpcomingEvent ev = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        ev.setStatus(status);
        eventRepo.save(ev);
        return "redirect:/companies/" + companyId + "/events";
    }
}
