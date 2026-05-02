package com.giftnova.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Immutable record of a single policy field change.
 * Written every time an HR admin saves updated policies.
 * Provides a full audit trail: what changed, from what value, to what value, and who did it.
 * Maps to the "policy_audit_log" table in PostgreSQL.
 */
@Entity
@Table(name = "policy_audit_log")
public class PolicyAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Company this change belongs to
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    // Name of the event type affected (e.g. "Birthday", "Onboarding")
    @Column(name = "event_type")
    private String eventType;

    // Name of the field that was changed (e.g. "Budget Limit", "Auto Send")
    @Column(name = "field_changed")
    private String fieldChanged;

    // Value before the change
    @Column(name = "old_value")
    private String oldValue;

    // Value after the change
    @Column(name = "new_value")
    private String newValue;

    // Email of the admin who made the change (from Company.adminEmail)
    @Column(name = "changed_by")
    private String changedBy;

    @Column(name = "changed_at", updatable = false)
    private LocalDateTime changedAt;

    @PrePersist
    void prePersist() { changedAt = LocalDateTime.now(); }

    public PolicyAuditLog() {}

    // Convenience constructor used by PolicyService when logging a change
    public PolicyAuditLog(Long companyId, String eventType, String fieldChanged,
                          String oldValue, String newValue, String changedBy) {
        this.companyId    = companyId;
        this.eventType    = eventType;
        this.fieldChanged = fieldChanged;
        this.oldValue     = oldValue;
        this.newValue     = newValue;
        this.changedBy    = changedBy;
    }

    public Long getId() { return id; }
    public Long getCompanyId() { return companyId; }
    public String getEventType() { return eventType; }
    public String getFieldChanged() { return fieldChanged; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
    public String getChangedBy() { return changedBy; }
    public LocalDateTime getChangedAt() { return changedAt; }
}
