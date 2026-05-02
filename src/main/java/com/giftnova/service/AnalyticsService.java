package com.giftnova.service;

import com.giftnova.model.AnalyticsMetrics;
import com.giftnova.model.Employee;
import com.giftnova.model.GiftRecommendation;
import com.giftnova.model.UpcomingEvent;
import com.giftnova.repository.EmployeeRepository;
import com.giftnova.repository.GiftRecommendationRepository;
import com.giftnova.repository.UpcomingEventRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Service
public class AnalyticsService {

    private final UpcomingEventRepository eventRepo;
    private final GiftRecommendationRepository recRepo;
    private final EmployeeRepository employeeRepo;

    public AnalyticsService(UpcomingEventRepository eventRepo,
                            GiftRecommendationRepository recRepo,
                            EmployeeRepository employeeRepo) {
        this.eventRepo    = eventRepo;
        this.recRepo      = recRepo;
        this.employeeRepo = employeeRepo;
    }

    public AnalyticsMetrics compute(Long companyId) {
        List<UpcomingEvent> events    = eventRepo.findByCompanyIdOrderByEventDate(companyId);
        List<GiftRecommendation> recs = recRepo.findByCompanyId(companyId);
        List<Employee> employees      = employeeRepo.findByCompanyId(companyId);

        Map<Long, GiftRecommendation> recByEvent = recs.stream()
                .collect(toMap(GiftRecommendation::getEventId, r -> r, (a, b) -> a));

        Set<String> APPROVED_STATUSES    = Set.of("APPROVED", "SENT");
        Set<String> SELECTED_STATUSES    = Set.of("SELECTION_PENDING", "APPROVED", "SENT");
        Set<String> LINK_SENT_STATUSES   = Set.of("LINK_SENT", "SELECTION_PENDING", "APPROVED", "SENT", "REJECTED");

        long totalEvents    = events.size();
        long giftsApproved  = events.stream().filter(e -> APPROVED_STATUSES.contains(e.getStatus())).count();

        BigDecimal budgetUsed = recs.stream()
                .filter(r -> {
                    UpcomingEvent e = events.stream().filter(ev -> ev.getId().equals(r.getEventId())).findFirst().orElse(null);
                    return e != null && "SENT".equals(e.getStatus());
                })
                .map(r -> r.getRecommendedBudget() != null ? r.getRecommendedBudget() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long momentsPrevented = recByEvent.size();

        long selectedCount  = events.stream().filter(e -> SELECTED_STATUSES.contains(e.getStatus())).count();
        long linkSentCount  = events.stream().filter(e -> LINK_SENT_STATUSES.contains(e.getStatus())).count();
        int redemptionRate  = linkSentCount > 0 ? (int) Math.round(100.0 * selectedCount / linkSentCount) : 0;

        long thankYouCount = recs.stream()
                .filter(r -> r.getThankYouNote() != null && !r.getThankYouNote().isBlank())
                .count();

        // Departments that have at least one approved/sent event's employee
        Set<Long> approvedEmployeeIds = events.stream()
                .filter(e -> APPROVED_STATUSES.contains(e.getStatus()))
                .map(UpcomingEvent::getEmployeeId)
                .collect(Collectors.toSet());

        Set<String> allDepts = employees.stream()
                .map(Employee::getDepartment)
                .filter(d -> d != null && !d.isBlank())
                .collect(Collectors.toSet());

        Set<String> coveredDepts = employees.stream()
                .filter(emp -> approvedEmployeeIds.contains(emp.getId()))
                .map(Employee::getDepartment)
                .filter(d -> d != null && !d.isBlank())
                .collect(Collectors.toSet());

        int deptCoveragePercent = allDepts.isEmpty() ? 0
                : (int) Math.round(100.0 * coveredDepts.size() / allDepts.size());

        LocalDate today = LocalDate.now();
        BigDecimal monthlyForecast = events.stream()
                .filter(e -> !e.getEventDate().isBefore(today) && !e.getEventDate().isAfter(today.plusDays(30)))
                .map(e -> e.getSuggestedBudget() != null ? e.getSuggestedBudget() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        OptionalDouble avgRatingOpt = recs.stream()
                .filter(r -> r.getRecipientRating() != null)
                .mapToInt(GiftRecommendation::getRecipientRating)
                .average();
        Double avgRating = avgRatingOpt.isPresent() ? avgRatingOpt.getAsDouble() : null;

        // Last 6 months chart data
        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("MMM yyyy");
        List<String> chartLabels = new ArrayList<>();
        List<BigDecimal> chartValues = new ArrayList<>();
        YearMonth now = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            chartLabels.add(ym.format(labelFmt));
            YearMonth finalYm = ym;
            BigDecimal monthTotal = events.stream()
                    .filter(e -> YearMonth.from(e.getEventDate()).equals(finalYm))
                    .map(e -> e.getSuggestedBudget() != null ? e.getSuggestedBudget() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            chartValues.add(monthTotal);
        }

        // Last 5 non-blank thank-you notes, newest first
        List<String> recentNotes = recs.stream()
                .filter(r -> r.getThankYouNote() != null && !r.getThankYouNote().isBlank())
                .sorted(Comparator.comparing(GiftRecommendation::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(GiftRecommendation::getThankYouNote)
                .collect(Collectors.toList());

        return new AnalyticsMetrics(totalEvents, giftsApproved, budgetUsed, momentsPrevented,
                redemptionRate, thankYouCount, deptCoveragePercent, monthlyForecast, avgRating,
                chartLabels, chartValues, recentNotes);
    }
}
