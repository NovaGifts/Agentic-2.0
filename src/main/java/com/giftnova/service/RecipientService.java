package com.giftnova.service;

import com.giftnova.model.*;
import com.giftnova.repository.*;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Handles the employee-facing gift selection flow:
 *   1. Look up event by secure token
 *   2. Resolve gift catalog items from the stored IDs
 *   3. Save the employee's choice and mark event as SELECTION_PENDING
 */
@Service
public class RecipientService {

    private final UpcomingEventRepository eventRepo;
    private final GiftRecommendationRepository recRepo;
    private final GiftCatalogService catalogService;

    public RecipientService(UpcomingEventRepository eventRepo,
                             GiftRecommendationRepository recRepo,
                             GiftCatalogService catalogService) {
        this.eventRepo      = eventRepo;
        this.recRepo        = recRepo;
        this.catalogService = catalogService;
    }

    public Optional<UpcomingEvent> findByToken(String token) {
        return eventRepo.findByRecipientToken(token);
    }

    // Parses the stored JSON gift-ID array and returns full catalog objects
    public List<Map<String, Object>> getGiftsForEvent(GiftRecommendation rec) {
        if (rec == null || rec.getRecommendedGiftIds() == null) return List.of();
        String cleaned = rec.getRecommendedGiftIds().replaceAll("[\\[\\]\"\\s]", "");
        List<Map<String, Object>> gifts = new ArrayList<>();
        for (String id : cleaned.split(",")) {
            catalogService.findById(id.trim()).ifPresent(gifts::add);
        }
        return gifts;
    }

    // Saves the employee's choice and moves the event into the HR review queue
    public void saveSelection(String token, String selectedGiftId,
                               String shippingAddress, String thankYouNote,
                               boolean donationChosen, Integer recipientRating) {
        eventRepo.findByRecipientToken(token).ifPresent(event -> {
            event.setStatus("SELECTION_PENDING");
            eventRepo.save(event);

            recRepo.findByEventId(event.getId()).ifPresent(rec -> {
                rec.setSelectedGiftId(donationChosen ? "donation" : selectedGiftId);
                rec.setShippingAddress(shippingAddress);
                rec.setThankYouNote(thankYouNote);
                rec.setDonationChosen(donationChosen);
                rec.setRecipientRating(recipientRating);
                recRepo.save(rec);
            });
        });
    }

    public String getSelectedGiftName(GiftRecommendation rec) {
        if (rec == null) return "Your gift";
        if (rec.isDonationChosen()) return "Charitable donation";
        if (rec.getSelectedGiftId() != null) {
            return catalogService.findById(rec.getSelectedGiftId())
                    .map(g -> (String) g.get("name")).orElse("Selected gift");
        }
        return "Your gift";
    }
}
