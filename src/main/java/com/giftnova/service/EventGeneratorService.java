package com.giftnova.service;

import com.giftnova.model.*;
import com.giftnova.repository.GiftPolicyRepository;
import com.giftnova.repository.UpcomingEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Generates upcoming gift events from employee data.
 * Called after every CSV upload — wipes old events and regenerates fresh ones.
 *
 * Rules:
 *   Birthday        → next occurrence within 60 days
 *   Work Anniversary → next occurrence within 60 days (only if employed ≥ 1 year)
 *   Onboarding      → if start_date is within the next 30 days (new hire)
 */
@Service
public class EventGeneratorService {

    private static final int UPCOMING_DAYS = 60;

    private final UpcomingEventRepository eventRepo;
    private final GiftPolicyRepository policyRepo;

    public EventGeneratorService(UpcomingEventRepository eventRepo,
                                  GiftPolicyRepository policyRepo) {
        this.eventRepo  = eventRepo;
        this.policyRepo = policyRepo;
    }

    @Transactional
    public void regenerateEvents(Long companyId, List<Employee> employees) {
        // Delete all existing events so we start fresh after each CSV upload
        eventRepo.deleteByCompanyId(companyId);

        LocalDate today = LocalDate.now();

        for (Employee emp : employees) {
            // --- Birthday ---
            if (emp.getBirthday() != null) {
                LocalDate next = nextOccurrence(emp.getBirthday(), today);
                if (!next.isAfter(today.plusDays(UPCOMING_DAYS))) {
                    eventRepo.save(buildEvent(companyId, emp, EventType.BIRTHDAY, next));
                }
            }

            // --- Work Anniversary (only if they've been here at least 1 year) ---
            if (emp.getStartDate() != null && emp.getStartDate().isBefore(today.minusYears(1).plusDays(1))) {
                LocalDate next = nextOccurrence(emp.getStartDate(), today);
                if (!next.isAfter(today.plusDays(UPCOMING_DAYS))) {
                    eventRepo.save(buildEvent(companyId, emp, EventType.WORK_ANNIVERSARY, next));
                }
            }

            // --- New Hire Onboarding (start_date is upcoming within 30 days) ---
            if (emp.getStartDate() != null
                    && !emp.getStartDate().isBefore(today)
                    && !emp.getStartDate().isAfter(today.plusDays(30))) {
                eventRepo.save(buildEvent(companyId, emp, EventType.ONBOARDING, emp.getStartDate()));
            }
        }
    }

    // Returns the next calendar occurrence of a recurring date (birthday/anniversary)
    private LocalDate nextOccurrence(LocalDate original, LocalDate today) {
        LocalDate thisYear = original.withYear(today.getYear());
        // If already passed this year, use next year
        return thisYear.isBefore(today) ? thisYear.plusYears(1) : thisYear;
    }

    // Builds an UpcomingEvent, looking up the budget from the company's GiftPolicy
    private UpcomingEvent buildEvent(Long companyId, Employee emp,
                                      EventType type, LocalDate date) {
        UpcomingEvent event = new UpcomingEvent();
        event.setCompanyId(companyId);
        event.setEmployeeId(emp.getId());
        event.setEmployeeName(emp.getName());
        event.setEventType(type);
        event.setEventDate(date);
        event.setSuggestedBudget(getBudget(companyId, type));
        return event;
    }

    // Looks up the budget from GiftPolicy; falls back to $0 if no policy found
    private BigDecimal getBudget(Long companyId, EventType type) {
        return policyRepo.findByCompanyIdAndEventType(companyId, type)
                .map(GiftPolicy::getBudgetLimit)
                .orElse(BigDecimal.ZERO);
    }
}
