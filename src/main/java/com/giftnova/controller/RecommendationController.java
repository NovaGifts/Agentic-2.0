package com.giftnova.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giftnova.model.GiftRecommendation;
import com.giftnova.model.UpcomingEvent;
import com.giftnova.repository.UpcomingEventRepository;
import com.giftnova.service.AiRecommendationService;
import com.giftnova.service.CompanyService;
import com.giftnova.service.GiftCatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.*;

/**
 * Phase 5: AI Recommendation Engine
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
    private final ObjectMapper objectMapper;

    public RecommendationController(AiRecommendationService aiService,
                                     GiftCatalogService catalogService,
                                     UpcomingEventRepository eventRepo,
                                     CompanyService companyService,
                                     ObjectMapper objectMapper) {
        this.aiService      = aiService;
        this.catalogService = catalogService;
        this.eventRepo      = eventRepo;
        this.companyService = companyService;
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

    // Displays the full recommendation with gift options, message draft, and policy status
    @GetMapping("/{eventId}")
    public String view(@PathVariable Long companyId,
                        @PathVariable Long eventId,
                        Model model) {
        UpcomingEvent event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

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
