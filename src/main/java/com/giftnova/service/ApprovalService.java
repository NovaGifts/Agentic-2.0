package com.giftnova.service;

import com.giftnova.model.*;
import com.giftnova.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class ApprovalService {

    private final UpcomingEventRepository    eventRepo;
    private final GiftRecommendationRepository recRepo;
    private final EmployeeRepository         employeeRepo;
    private final GiftCatalogService         catalogService;

    public ApprovalService(UpcomingEventRepository eventRepo,
                            GiftRecommendationRepository recRepo,
                            EmployeeRepository employeeRepo,
                            GiftCatalogService catalogService) {
        this.eventRepo      = eventRepo;
        this.recRepo        = recRepo;
        this.employeeRepo   = employeeRepo;
        this.catalogService = catalogService;
    }

    public List<ApprovalItem> getQueue(Long companyId) {
        List<UpcomingEvent> events = eventRepo.findByCompanyIdOrderByEventDate(companyId);
        List<GiftRecommendation> recs = recRepo.findByCompanyId(companyId);

        Map<Long, GiftRecommendation> recByEventId = new HashMap<>();
        for (GiftRecommendation rec : recs) recByEventId.put(rec.getEventId(), rec);

        List<ApprovalItem> queue = new ArrayList<>();
        for (UpcomingEvent event : events) {
            GiftRecommendation rec = recByEventId.get(event.getId());
            if (rec != null) {
                ApprovalItem item = new ApprovalItem();
                item.setEvent(event);
                item.setRecommendation(rec);
                item.setGiftNames(resolveGiftNames(rec));
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

    public void seedDemo(Long companyId) {
        if (!eventRepo.findByCompanyIdOrderByEventDate(companyId).isEmpty()) return;

        String[][] empData = {
            {"Priya Sharma",    "priya@demo.com",   "Engineering", "US", "1990-05-15", "2021-03-10"},
            {"Daniel Park",     "daniel@demo.com",  "Product",     "US", "1988-11-20", "2019-05-01"},
            {"Ava Chen",        "ava@demo.com",     "Design",      "US", "1995-07-08", "2026-04-15"},
            {"Marcus Johnson",  "marcus@demo.com",  "Engineering", "US", "1986-09-12", "2018-01-05"},
            {"Sofia Rodriguez", "sofia@demo.com",   "Marketing",   "US", "1992-03-22", "2020-08-15"}
        };

        EventType[] types = {
            EventType.BIRTHDAY, EventType.WORK_ANNIVERSARY, EventType.ONBOARDING,
            EventType.MANAGER_APPRECIATION, EventType.BIRTHDAY
        };

        LocalDate base = LocalDate.of(2026, 5, 1);
        LocalDate[] dates    = {base.plusDays(14), base, base, base.plusDays(19), base.plusDays(21)};
        String[]    statuses = {"NEEDS_REVIEW", "APPROVAL_NEEDED", "NEEDS_REVIEW", "NEEDS_REVIEW", "DRAFT_READY"};

        BigDecimal[] suggestedBudgets = {bd("50"), bd("75"), bd("100"), bd("85"), bd("50")};
        BigDecimal[] recBudgets       = {bd("40"), bd("75"), bd("100"), bd("85"), bd("35")};
        boolean[]    needsApproval    = {false, true, false, false, false};

        String[][] gifts = {
            {"gift_11", "gift_03", "gift_08"},
            {"gift_02", "gift_14", "gift_09"},
            {"gift_12", "gift_24", "gift_15"},
            {"gift_06", "gift_23", "gift_18"},
            {"gift_05", "gift_03", "gift_04"}
        };

        String[] messages = {
            "Happy Birthday Priya! Your curiosity and dedication make our team shine every day. Enjoy your special day!",
            "Congratulations Daniel on your work anniversary! Seven years of innovation and leadership — you're the backbone of our product team. Here's to many more!",
            "Welcome to the team, Ava! We're so excited to have your creative energy on board. This small gift is just a sign of how thrilled we are to have you with us.",
            "Marcus, your mentorship and calm under pressure make this team extraordinary. Thank you for everything you bring — this is a small token of our appreciation.",
            "Happy Birthday Sofia! Your marketing creativity keeps GiftNova growing every day. Wishing you a wonderful birthday celebration!"
        };

        String[] reasonings = {
            "Birthday gifts selected for warmth and daily enjoyment — coffee for a morning ritual, Kindle credit for relaxation, DoorDash for a treat-yourself dinner.",
            "Anniversary milestone gifts — Airbnb experience for a memorable adventure, Patagonia for quality lifestyle, Spotify for daily enjoyment. Exceeds approval threshold.",
            "Practical onboarding gifts to set up a new hire for success — USB-C hub for productivity, Amazon Prime for convenience, Yeti tumbler for the desk.",
            "Wellness-focused gifts for a manager who gives so much to the team — Headspace and Calm for mental wellness, aromatherapy for relaxation.",
            "Learning-focused birthday gifts for a growth-minded marketer — Coursera for skill building, Kindle for deep reading, Udemy for practical skills."
        };

        for (int i = 0; i < 5; i++) {
            Employee emp = new Employee();
            emp.setCompanyId(companyId);
            emp.setName(empData[i][0]);
            emp.setEmail(empData[i][1]);
            emp.setDepartment(empData[i][2]);
            emp.setCountry(empData[i][3]);
            emp.setBirthday(LocalDate.parse(empData[i][4]));
            emp.setStartDate(LocalDate.parse(empData[i][5]));
            Employee saved = employeeRepo.save(emp);

            UpcomingEvent event = new UpcomingEvent();
            event.setCompanyId(companyId);
            event.setEmployeeId(saved.getId());
            event.setEmployeeName(empData[i][0]);
            event.setEventType(types[i]);
            event.setEventDate(dates[i]);
            event.setSuggestedBudget(suggestedBudgets[i]);
            event.setStatus(statuses[i]);
            UpcomingEvent savedEvent = eventRepo.save(event);

            GiftRecommendation rec = new GiftRecommendation();
            rec.setEventId(savedEvent.getId());
            rec.setCompanyId(companyId);
            rec.setRecommendedGiftIds("[\"" + String.join("\",\"", gifts[i]) + "\"]");
            rec.setRecommendedBudget(recBudgets[i]);
            rec.setMessageDraft(messages[i]);
            rec.setReasoning(reasonings[i]);
            rec.setPolicyStatus("compliant");
            rec.setApprovalRequired(needsApproval[i]);
            rec.setRiskFlags(needsApproval[i]
                    ? "[\"Budget $75 exceeds approval threshold — manager sign-off required\"]"
                    : "[]");
            recRepo.save(rec);
        }
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

    private static BigDecimal bd(String val) { return new BigDecimal(val); }
}
