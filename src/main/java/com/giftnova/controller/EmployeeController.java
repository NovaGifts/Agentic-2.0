package com.giftnova.controller;

import com.giftnova.dto.CsvUploadResult;
import com.giftnova.model.UpcomingEvent;
import com.giftnova.repository.EmployeeRepository;
import com.giftnova.repository.GiftRecommendationRepository;
import com.giftnova.repository.UpcomingEventRepository;
import com.giftnova.service.*;
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
    private final AiRecommendationService aiService;
    private final EmailService emailService;
    private final EmployeeRepository employeeRepo;
    private final GiftRecommendationRepository recRepo;

    public EmployeeController(EmployeeService employeeService,
                               UpcomingEventRepository eventRepo,
                               CompanyService companyService,
                               AiRecommendationService aiService,
                               EmailService emailService,
                               EmployeeRepository employeeRepo,
                               GiftRecommendationRepository recRepo) {
        this.employeeService = employeeService;
        this.eventRepo       = eventRepo;
        this.companyService  = companyService;
        this.aiService       = aiService;
        this.emailService    = emailService;
        this.employeeRepo    = employeeRepo;
        this.recRepo         = recRepo;
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
            Long cid = companyId;
            new Thread(() -> runAutomation(cid)).start();
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

    private void runAutomation(Long companyId) {
        com.giftnova.model.Company company = companyService.findById(companyId);
        List<UpcomingEvent> events = eventRepo.findByCompanyIdOrderByEventDate(companyId);
        java.time.LocalDate today = java.time.LocalDate.now();
        for (UpcomingEvent event : events) {
            if (recRepo.findByEventId(event.getId()).isPresent()) continue;
            try {
                com.giftnova.model.GiftRecommendation rec = aiService.generate(companyId, event.getId());

                if (event.getRecipientToken() == null) event.setRecipientToken(java.util.UUID.randomUUID().toString());
                if (event.getManagerToken() == null) event.setManagerToken(java.util.UUID.randomUUID().toString());

                boolean isToday = today.equals(event.getEventDate());
                event.setStatus(isToday ? "LINK_SENT" : "NEEDS_REVIEW");
                eventRepo.save(event);

                if (isToday) {
                    employeeRepo.findById(event.getEmployeeId()).ifPresent(emp -> {
                        emailService.sendRecommendationToEmployee(
                            emp.getEmail(), emp.getName(), company.getName(),
                            event.getEventType().getLabel(), rec.getMessageDraft(),
                            event.getRecipientToken());

                        String managerEmail = emailService.resolveManagerEmail(emp, company);
                        String managerName  = emailService.resolveManagerName(emp, company);
                        emailService.sendManagerFyi(managerEmail, managerName,
                            event.getEmployeeName(), company.getName(), event.getEventType().getLabel());
                        if (!managerEmail.equals(company.getAdminEmail())) {
                            emailService.sendHrFyi(company.getAdminEmail(), company.getAdminName(),
                                event.getEmployeeName(), event.getEventType().getLabel());
                        }
                    });
                }
            } catch (Exception e) {
                System.err.println("Automation failed for event " + event.getId() + ": " + e.getMessage());
            }
        }
    }
}
