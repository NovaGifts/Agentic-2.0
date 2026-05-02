package com.giftnova.controller;

import com.giftnova.model.*;
import com.giftnova.repository.GiftRecommendationRepository;
import com.giftnova.repository.UpcomingEventRepository;
import com.giftnova.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Employee-facing gift selection flow.
 * /gift/{token}           — choose a gift (public, no auth required)
 * /gift/{token}/confirmed — confirmation after selection
 */
@Controller
@RequestMapping("/gift")
public class RecipientController {

    private final RecipientService             recipientService;
    private final CompanyService               companyService;
    private final GiftRecommendationRepository recRepo;
    private final UpcomingEventRepository      eventRepo;
    private final EmailService                 emailService;
    private final GiftCatalogService           catalogService;

    public RecipientController(RecipientService recipientService,
                                CompanyService companyService,
                                GiftRecommendationRepository recRepo,
                                UpcomingEventRepository eventRepo,
                                EmailService emailService,
                                GiftCatalogService catalogService) {
        this.recipientService = recipientService;
        this.companyService   = companyService;
        this.recRepo          = recRepo;
        this.eventRepo        = eventRepo;
        this.emailService     = emailService;
        this.catalogService   = catalogService;
    }

    @GetMapping("/{token}")
    public String showChoice(@PathVariable String token, Model model) {
        Optional<UpcomingEvent> opt = recipientService.findByToken(token);
        if (opt.isEmpty()) {
            model.addAttribute("error", "This gift link is invalid or has expired.");
            return "recipient/choose";
        }
        UpcomingEvent event = opt.get();

        // Already submitted — go to confirmation
        if ("SELECTION_PENDING".equals(event.getStatus()) || "SENT".equals(event.getStatus())) {
            return "redirect:/gift/" + token + "/confirmed";
        }

        Company company = companyService.findById(event.getCompanyId());
        GiftRecommendation rec = recRepo.findByEventId(event.getId()).orElse(null);

        model.addAttribute("event",   event);
        model.addAttribute("company", company);
        model.addAttribute("rec",     rec);
        model.addAttribute("gifts",   recipientService.getGiftsForEvent(rec));
        return "recipient/choose";
    }

    @PostMapping("/{token}")
    public String saveChoice(@PathVariable String token,
                             @RequestParam(required = false) String selectedGiftId,
                             @RequestParam(required = false) String shippingAddress,
                             @RequestParam(required = false) String thankYouNote,
                             @RequestParam(required = false) Integer recipientRating) {
        if (selectedGiftId == null || selectedGiftId.isBlank()) {
            return "redirect:/gift/" + token;
        }
        boolean isDonation = "donation".equals(selectedGiftId);
        recipientService.saveSelection(token,
                isDonation ? null : selectedGiftId,
                shippingAddress, thankYouNote, isDonation, recipientRating);

        // After selection — send manager the approve/reject email
        eventRepo.findByRecipientToken(token).ifPresent(event -> {
            Company company = companyService.findById(event.getCompanyId());
            recRepo.findByEventId(event.getId()).ifPresent(rec -> {
                // Resolve gift details for the email
                List<Map<String, Object>> gifts = new ArrayList<>();
                String cleaned = rec.getRecommendedGiftIds() == null ? "" :
                        rec.getRecommendedGiftIds().replaceAll("[\\[\\]\"\\s]", "");
                for (String id : cleaned.split(",")) {
                    if (!id.isBlank()) catalogService.findById(id.trim()).ifPresent(gifts::add);
                }
                emailService.sendApprovalRequestToManager(
                        company.getAdminEmail(), company.getAdminName(),
                        event.getEmployeeName(), company.getName(),
                        event.getEventType().getLabel(), rec.getMessageDraft(),
                        gifts, rec.getRecommendedBudget(), event.getManagerToken());
            });
        });

        return "redirect:/gift/" + token + "/confirmed";
    }

    @GetMapping("/{token}/confirmed")
    public String confirmed(@PathVariable String token, Model model) {
        Optional<UpcomingEvent> opt = recipientService.findByToken(token);
        if (opt.isEmpty()) return "redirect:/";

        UpcomingEvent event = opt.get();
        Company company = companyService.findById(event.getCompanyId());
        GiftRecommendation rec = recRepo.findByEventId(event.getId()).orElse(null);

        model.addAttribute("event",        event);
        model.addAttribute("company",      company);
        model.addAttribute("selectedName", recipientService.getSelectedGiftName(rec));
        model.addAttribute("thankYouNote", rec != null ? rec.getThankYouNote() : null);
        return "recipient/confirmed";
    }
}
