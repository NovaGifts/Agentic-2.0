package com.giftnova.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giftnova.model.Company;
import com.giftnova.model.GiftRecommendation;
import com.giftnova.model.UpcomingEvent;
import com.giftnova.repository.EmployeeRepository;
import com.giftnova.repository.UpcomingEventRepository;
import com.giftnova.service.AiRecommendationService;
import com.giftnova.service.CompanyService;
import com.giftnova.service.EmailService;
import com.giftnova.service.GiftCatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.*;

/**
 * AI Recommendation Engine
 *
 * Routes:
 *   POST /companies/{id}/recommendations/generate/{eventId} → call Claude, save recommendation
 *   GET  /companies/{id}/recommendations/{eventId}          → view recommendation
 */
@Controller
@RequestMapping("/companies/{companyId}/recommendations")
public class RecommendationController {

    private final AiRecommendationService aiService;
    private final GiftCatalogService catalogService;
    private final UpcomingEventRepository eventRepo;
    private final CompanyService companyService;
    private final EmailService emailService;
    private final EmployeeRepository employeeRepo;
    private final ObjectMapper objectMapper;

    public RecommendationController(AiRecommendationService aiService,
                                     GiftCatalogService catalogService,
                                     UpcomingEventRepository eventRepo,
                                     CompanyService companyService,
                                     EmailService emailService,
                                     EmployeeRepository employeeRepo,
                                     ObjectMapper objectMapper) {
        this.aiService      = aiService;
        this.catalogService = catalogService;
        this.eventRepo      = eventRepo;
        this.companyService = companyService;
        this.emailService   = emailService;
        this.employeeRepo   = employeeRepo;
        this.objectMapper   = objectMapper;
    }

    // Triggers AI recommendation generation and redirects to the view page
    @PostMapping("/generate/{eventId}")
    public String generate(@PathVariable Long companyId,
                            @PathVariable Long eventId,
                            RedirectAttributes redirectAttrs) {
        try {
            aiService.generate(companyId, eventId);
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("aiError", "AI recommendation failed: " + e.getMessage());
        }
        return "redirect:/companies/" + companyId + "/recommendations/" + eventId;
    }

    // Sends recommendation to employee + approval request to manager (company admin email)
    @PostMapping("/notify/{eventId}")
    public String notify(@PathVariable Long companyId,
                         @PathVariable Long eventId,
                         RedirectAttributes redirectAttrs) {
        Optional<UpcomingEvent> eventOpt = eventRepo.findById(eventId);
        if (eventOpt.isEmpty()) {
            redirectAttrs.addFlashAttribute("aiError", "Event not found.");
            return "redirect:/companies/" + companyId + "/events";
        }
        UpcomingEvent event = eventOpt.get();
        Optional<GiftRecommendation> recOpt = aiService.findByEvent(eventId);
        if (recOpt.isEmpty()) {
            redirectAttrs.addFlashAttribute("aiError", "No recommendation found. Generate one first.");
            return "redirect:/companies/" + companyId + "/recommendations/" + eventId;
        }
        GiftRecommendation rec = recOpt.get();
        Company company = companyService.findById(companyId);

        // Resolve gift details
        List<Map<String, Object>> gifts = new ArrayList<>();
        try {
            List<String> ids = objectMapper.readValue(rec.getRecommendedGiftIds(),
                    new TypeReference<List<String>>() {});
            for (String id : ids) catalogService.findById(id).ifPresent(gifts::add);
        } catch (Exception ignored) {}

        // Generate tokens if not already set
        if (event.getRecipientToken() == null) event.setRecipientToken(java.util.UUID.randomUUID().toString());
        if (event.getManagerToken()   == null) event.setManagerToken(java.util.UUID.randomUUID().toString());
        event.setStatus("LINK_SENT");
        eventRepo.save(event);

        String recipientToken = event.getRecipientToken();

        try {
            // Email employee with gift selection link
            employeeRepo.findById(event.getEmployeeId()).ifPresent(emp ->
                emailService.sendRecommendationToEmployee(
                    emp.getEmail(), emp.getName(), company.getName(),
                    event.getEventType().getLabel(), rec.getMessageDraft(), gifts, recipientToken));

            // FYI to manager — approve/reject email comes after employee picks
            emailService.sendManagerFyi(
                company.getAdminEmail(), company.getAdminName(),
                event.getEmployeeName(), company.getName(),
                event.getEventType().getLabel());

            redirectAttrs.addFlashAttribute("toast",
                emailService.isConfigured()
                    ? "Email sent to employee. Manager notified — they'll get approve/reject options once employee picks."
                    : "SMTP not configured — emails were not sent.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("aiError", "Failed to send emails: " + e.getMessage());
        }
        return "redirect:/companies/" + companyId + "/recommendations/" + eventId;
    }

    // Displays the full recommendation with gift options, message draft, and policy status
    @GetMapping("/{eventId}")
    public String view(@PathVariable Long companyId,
                        @PathVariable Long eventId,
                        Model model) {
        Optional<UpcomingEvent> eventOpt = eventRepo.findById(eventId);
        if (eventOpt.isEmpty()) {
            return "redirect:/companies/" + companyId + "/events";
        }
        UpcomingEvent event = eventOpt.get();

        Optional<GiftRecommendation> rec = aiService.findByEvent(eventId);

        List<Map<String, Object>> giftDetails  = new ArrayList<>();
        List<String>              riskFlagsList = new ArrayList<>();

        rec.ifPresent(r -> {
            // Parse gift IDs from stored JSON array
            try {
                List<String> ids = objectMapper.readValue(r.getRecommendedGiftIds(),
                        new TypeReference<List<String>>() {});
                for (String id : ids) {
                    catalogService.findById(id).ifPresent(giftDetails::add);
                }
            } catch (Exception ignored) {}

            // Parse risk flags from stored JSON array
            try {
                if (r.getRiskFlags() != null && !r.getRiskFlags().equals("[]")) {
                    riskFlagsList.addAll(objectMapper.readValue(r.getRiskFlags(),
                            new TypeReference<List<String>>() {}));
                }
            } catch (Exception ignored) {}
        });

        model.addAttribute("company",       companyService.findById(companyId));
        model.addAttribute("event",         event);
        model.addAttribute("rec",           rec.orElse(null));
        model.addAttribute("giftDetails",   giftDetails);
        model.addAttribute("riskFlagsList", riskFlagsList);
        return "recommendations/view";
    }
}
