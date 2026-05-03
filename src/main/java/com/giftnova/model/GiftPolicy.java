package com.giftnova.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Defines the gifting rule for one event type within a company.
 * For example: "Birthday gifts are capped at $40, no approval needed, auto-send allowed."
 * Maps to the "gift_policies" table in PostgreSQL.
 */
@Entity
@Table(name = "gift_policies")
public class GiftPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Links this policy to the company it belongs to
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    // The type of event this rule applies to (Birthday, Work Anniversary, etc.)
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    // Maximum gift value allowed for this event type
    @Column(name = "budget_limit", nullable = false, precision = 10, scale = 2)
    private BigDecimal budgetLimit;

    // Whether manager approval is needed before the gift is sent
    @Column(name = "approval_required", nullable = false)
    private boolean approvalRequired;

    // If true, GiftNova sends the gift automatically without manual HR action
    @Column(name = "auto_send", nullable = false)
    private boolean autoSend;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    public BigDecimal getBudgetLimit() { return budgetLimit; }
    public void setBudgetLimit(BigDecimal budgetLimit) { this.budgetLimit = budgetLimit; }
    public boolean isApprovalRequired() { return approvalRequired; }
    public void setApprovalRequired(boolean approvalRequired) { this.approvalRequired = approvalRequired; }
    public boolean isAutoSend() { return autoSend; }
    public void setAutoSend(boolean autoSend) { this.autoSend = autoSend; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
