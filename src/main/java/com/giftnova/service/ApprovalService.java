package com.giftnova.service;

import com.giftnova.model.*;
import com.giftnova.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class ApprovalService {

    private final UpcomingEventRepository      eventRepo;
    private final GiftRecommendationRepository recRepo;
    private final EmployeeRepository           employeeRepo;
    private final GiftCatalogService           catalogService;
    private final CompanyRepository            companyRepo;
    private final EmailService                 emailService;
    private final GiftPolicyRepository         policyRepo;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public ApprovalService(UpcomingEventRepository eventRepo,
                            GiftRecommendationRepository recRepo,
                            EmployeeRepository employeeRepo,
                            GiftCatalogService catalogService,
                            CompanyRepository companyRepo,
                            EmailService emailService,
                            GiftPolicyRepository policyRepo) {
        this.eventRepo      = eventRepo;
        this.recRepo        = recRepo;
        this.employeeRepo   = employeeRepo;
        this.catalogService = catalogService;
        this.companyRepo    = companyRepo;
        this.emailService   = emailService;
        this.policyRepo     = policyRepo;
    }

    public List<ApprovalItem> getQueue(Long companyId) {
        List<UpcomingEvent> events = eventRepo.findByCompanyIdOrderByEventDate(companyId);
        List<GiftRecommendation> recs = recRepo.findByCompanyId(companyId);

        Map<Long, GiftRecommendation> recByEventId = new HashMap<>();
        for (GiftRecommendation rec : recs) recByEventId.put(rec.getEventId(), rec);

        List<ApprovalItem> queue = new ArrayList<>();
        for (UpcomingEvent event : events) {
            String status = event.getStatus();
            if (!"LINK_SENT".equals(status) && !"SELECTION_PENDING".equals(status)
                    && !"APPROVED".equals(status) && !"REJECTED".equals(status)) continue;

            GiftRecommendation rec = recByEventId.get(event.getId());
            if (rec != null) {
                ApprovalItem item = new ApprovalItem();
                item.setEvent(event);
                item.setRecommendation(rec);
                item.setGiftNames(resolveGiftNames(rec));
                if (rec.isDonationChosen()) {
                    item.setSelectedGiftName("Charitable donation");
                } else if (rec.getSelectedGiftId() != null) {
                    item.setSelectedGiftName(catalogService.findById(rec.getSelectedGiftId())
                            .map(g -> (String) g.get("name")).orElse(null));
                }
                policyRepo.findByCompanyIdAndEventType(companyId, event.getEventType())
                        .ifPresent(p -> item.setPolicyBudget(p.getBudgetLimit()));
                queue.add(item);
            }
        }
        return queue;
    }

    public Optional<ApprovalItem> getItem(Long eventId) {
        return eventRepo.findById(eventId).map(event -> {
            ApprovalItem item = new ApprovalItem();
            item.setEvent(event);
            recRepo.findByEventId(eventId).ifPresent(rec -> {
                item.setRecommendation(rec);
                item.setGiftNames(resolveGiftNames(rec));
            });
            return item;
        });
    }

    public void approve(Long eventId) {
        eventRepo.findById(eventId).ifPresent(e -> {
            e.setStatus("APPROVED");
            eventRepo.save(e);
        });
    }

    // Generates a secure token, sets status to LINK_SENT, and emails the employee
    public String sendLink(Long companyId, Long eventId) {
        return eventRepo.findById(eventId).map(event -> {
            if (event.getRecipientToken() == null) {
                event.setRecipientToken(UUID.randomUUID().toString());
            }
            event.setStatus("LINK_SENT");
            eventRepo.save(event);

            String url = baseUrl + "/gift/" + event.getRecipientToken();

            // Send email if SMTP is configured; otherwise HR copies the link manually
            employeeRepo.findById(event.getEmployeeId()).ifPresent(emp ->
                companyRepo.findById(companyId).ifPresent(company ->
                    emailService.sendGiftLink(emp.getEmail(), emp.getName(),
                            company.getName(), event.getEventType().getLabel(), url)));

            return url;
        }).orElse(null);
    }

    // HR confirms the employee's gift selection → gift is sent
    public void approveSelection(Long eventId) {
        eventRepo.findById(eventId).ifPresent(e -> {
            e.setStatus("SENT");
            eventRepo.save(e);
        });
    }

    public void reject(Long eventId) {
        eventRepo.findById(eventId).ifPresent(e -> {
            e.setStatus("REJECTED");
            eventRepo.save(e);
        });
    }

    public void requestReview(Long eventId) {
        eventRepo.findById(eventId).ifPresent(e -> {
            e.setStatus("APPROVAL_NEEDED");
            eventRepo.save(e);
        });
    }

    public void updateDraft(Long eventId, String messageDraft, BigDecimal budget) {
        recRepo.findByEventId(eventId).ifPresent(rec -> {
            if (messageDraft != null && !messageDraft.isBlank()) rec.setMessageDraft(messageDraft);
            if (budget != null) rec.setRecommendedBudget(budget);
            recRepo.save(rec);
        });
        eventRepo.findById(eventId).ifPresent(e -> {
            e.setStatus("DRAFT_READY");
            eventRepo.save(e);
        });
    }


    private List<String> resolveGiftNames(GiftRecommendation rec) {
        List<String> names = new ArrayList<>();
        if (rec.getRecommendedGiftIds() == null) return names;
        String cleaned = rec.getRecommendedGiftIds().replaceAll("[\\[\\]\"\\s]", "");
        for (String id : cleaned.split(",")) {
            catalogService.findById(id.trim()).ifPresent(g -> names.add((String) g.get("name")));
        }
        return names;
    }

}
