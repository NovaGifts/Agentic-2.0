package com.giftnova.controller;

import com.giftnova.model.*;
import com.giftnova.repository.EmployeeRepository;
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
    private final EmployeeRepository           employeeRepo;

    public RecipientController(RecipientService recipientService,
                                CompanyService companyService,
                                GiftRecommendationRepository recRepo,
                                UpcomingEventRepository eventRepo,
                                EmailService emailService,
                                GiftCatalogService catalogService,
                                EmployeeRepository employeeRepo) {
        this.recipientService = recipientService;
        this.companyService   = companyService;
        this.recRepo          = recRepo;
        this.eventRepo        = eventRepo;
        this.emailService     = emailService;
        this.catalogService   = catalogService;
        this.employeeRepo     = employeeRepo;
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
            String selectedGiftName = isDonation ? "Charitable donation" :
                catalogService.findById(selectedGiftId)
                    .map(g -> (String) g.get("name")).orElse("Selected gift");
            Employee emp = employeeRepo.findById(event.getEmployeeId()).orElse(null);
            String managerEmail = emailService.resolveManagerEmail(emp, company);
            String managerName  = emailService.resolveManagerName(emp, company);
            emailService.sendApprovalRequestToManager(
                    managerEmail, managerName,
                    event.getEmployeeName(), company.getName(),
                    event.getEventType().getLabel(), selectedGiftName,
                    event.getManagerToken());
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
