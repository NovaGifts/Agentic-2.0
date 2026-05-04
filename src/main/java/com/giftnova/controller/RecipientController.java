package com.giftnova.controller;

import com.giftnova.model.*;
import com.giftnova.repository.EmployeeRepository;
import com.giftnova.repository.GiftRecommendationRepository;
import com.giftnova.repository.UpcomingEventRepository;
import com.giftnova.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
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

        // 15-day expiry from event date
        if ("LINK_SENT".equals(event.getStatus()) &&
                LocalDate.now().isAfter(event.getEventDate().plusDays(15))) {
            model.addAttribute("error", "This gift link has expired. Please contact your HR team.");
            return "recipient/choose";
        }

        // Already submitted — go to confirmation
        if ("SELECTION_PENDING".equals(event.getStatus()) || "SENT".equals(event.getStatus())) {
            return "redirect:/gift/" + token + "/confirmed";
        }

        Company company = companyService.findById(event.getCompanyId());
        GiftRecommendation rec = recRepo.findByEventId(event.getId()).orElse(null);
        List<Map<String, Object>> gifts = recipientService.getGiftsForEvent(rec);

        // Cheapest gift gets the "Recommended" badge
        String cheapestId = gifts.stream()
                .min(Comparator.comparing(g -> new BigDecimal(g.get("price").toString())))
                .map(g -> (String) g.get("id"))
                .orElse(null);

        model.addAttribute("event",         event);
        model.addAttribute("company",       company);
        model.addAttribute("rec",           rec);
        model.addAttribute("gifts",         gifts);
        model.addAttribute("recommendedId", cheapestId);
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
        boolean isSurprise = "surprise".equals(selectedGiftId);

        // For "Surprise Me", randomly pick one of the recommended gifts
        String resolvedGiftId = selectedGiftId;
        if (isSurprise) {
            GiftRecommendation rec = eventRepo.findByRecipientToken(token)
                    .flatMap(e -> recRepo.findByEventId(e.getId())).orElse(null);
            List<Map<String, Object>> gifts = recipientService.getGiftsForEvent(rec);
            if (!gifts.isEmpty()) {
                resolvedGiftId = (String) gifts.get(new Random().nextInt(gifts.size())).get("id");
            }
        }

        final String finalGiftId = resolvedGiftId;
        recipientService.saveSelection(token,
                isDonation ? null : finalGiftId,
                shippingAddress, thankYouNote, isDonation, isSurprise, recipientRating);

        // Send manager the approve/reject email
        eventRepo.findByRecipientToken(token).ifPresent(event -> {
            Company company = companyService.findById(event.getCompanyId());
            String giftNameForManager = isDonation ? "Charitable donation" :
                    catalogService.findById(finalGiftId)
                            .map(g -> (String) g.get("name")).orElse("Selected gift");
            Employee emp = employeeRepo.findById(event.getEmployeeId()).orElse(null);
            String managerEmail = emailService.resolveManagerEmail(emp, company);
            String managerName  = emailService.resolveManagerName(emp, company);
            emailService.sendApprovalRequestToManager(
                    managerEmail, managerName,
                    event.getEmployeeName(), company.getName(),
                    event.getEventType().getLabel(), giftNameForManager,
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
