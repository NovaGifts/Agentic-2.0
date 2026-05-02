package com.giftnova.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Auto-generated gift event for an employee.
 * Created after CSV upload by scanning each employee's birthday and start_date.
 * Refreshed on every upload so it's always in sync with employee data.
 */
@Entity
@Table(name = "upcoming_events")
public class UpcomingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "employee_name", nullable = false)
    private String employeeName;

    // BIRTHDAY, WORK_ANNIVERSARY, or ONBOARDING
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    // The actual date the event falls on (e.g. May 18 2026)
    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    // Budget pulled from the company's GiftPolicy for this event type
    @Column(name = "suggested_budget", precision = 10, scale = 2)
    private BigDecimal suggestedBudget;

    // PENDING → APPROVED → SENT (approval workflow comes in Phase 4)
    @Column(nullable = false)
    private String status = "PENDING";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }
    public BigDecimal getSuggestedBudget() { return suggestedBudget; }
    public void setSuggestedBudget(BigDecimal suggestedBudget) { this.suggestedBudget = suggestedBudget; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
