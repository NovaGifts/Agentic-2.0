package com.giftnova.service;

import com.giftnova.dto.PolicyForm;
import com.giftnova.model.*;
import com.giftnova.repository.GiftPolicyRepository;
import com.giftnova.repository.PolicyAuditLogRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Core business logic for the Policy Builder.
 * Handles seeding default policies on company creation,
 * saving admin policy edits, and writing the audit log.
 */
@Service
public class PolicyService {

    private final GiftPolicyRepository policyRepo;
    private final PolicyAuditLogRepository auditRepo;
    private final CompanyService companyService;

    public PolicyService(GiftPolicyRepository policyRepo,
                         PolicyAuditLogRepository auditRepo,
                         CompanyService companyService) {
        this.policyRepo     = policyRepo;
        this.auditRepo      = auditRepo;
        this.companyService = companyService;
    }

    /**
     * Seeds four default gift policies for a newly created company.
     * Skips if policies already exist (idempotent — safe to call multiple times).
     */
    public void initDefaults(Long companyId) {
        if (policyRepo.countByCompanyId(companyId) > 0) return;

        List<GiftPolicy> defaults = new ArrayList<>();
        defaults.add(build(companyId, EventType.BIRTHDAY,             new BigDecimal("40"),  false, null,                 true));
        defaults.add(build(companyId, EventType.WORK_ANNIVERSARY,     new BigDecimal("75"),  true,  new BigDecimal("75"), false));
        defaults.add(build(companyId, EventType.ONBOARDING,           new BigDecimal("100"), true,  null,                 false));
        defaults.add(build(companyId, EventType.MANAGER_APPRECIATION, new BigDecimal("50"),  true,  null,                 false));
        policyRepo.saveAll(defaults);
    }

    // Returns all policies for a company, ordered by event type name
    public List<GiftPolicy> getPolicies(Long companyId) {
        return policyRepo.findByCompanyIdOrderByEventType(companyId);
    }

    // Returns audit log entries newest-first for display in the admin UI
    public List<PolicyAuditLog> getAuditLog(Long companyId) {
        return auditRepo.findByCompanyIdOrderByChangedAtDesc(companyId);
    }

    /**
     * Saves the admin's policy edits and records every changed field in the audit log.
     * Only fields that actually changed get an audit entry — unchanged fields are skipped.
     * Also saves company-level settings (restricted categories, departments, locations).
     */
    public void updatePolicies(Long companyId, PolicyForm form, String changedBy) {
        for (GiftPolicy update : form.getPolicies()) {
            GiftPolicy existing = policyRepo.findById(update.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + update.getId()));

            // Diff each field and write an audit log entry for any that changed
            logIfChanged(companyId, existing.getEventType().getLabel(),
                    "Budget Limit", str(existing.getBudgetLimit()), str(update.getBudgetLimit()), changedBy);
            logIfChanged(companyId, existing.getEventType().getLabel(),
                    "Approval Required", str(existing.isApprovalRequired()), str(update.isApprovalRequired()), changedBy);
            logIfChanged(companyId, existing.getEventType().getLabel(),
                    "Approval Threshold", str(existing.getApprovalThreshold()), str(update.getApprovalThreshold()), changedBy);
            logIfChanged(companyId, existing.getEventType().getLabel(),
                    "Auto Send", str(existing.isAutoSend()), str(update.isAutoSend()), changedBy);

            existing.setBudgetLimit(update.getBudgetLimit());
            existing.setApprovalRequired(update.isApprovalRequired());
            // Clear threshold when approval is turned off
            existing.setApprovalThreshold(update.isApprovalRequired() ? update.getApprovalThreshold() : null);
            existing.setAutoSend(update.isAutoSend());
            policyRepo.save(existing);
        }

        // Update and audit company-level global settings
        Company company = companyService.findById(companyId);
        logIfChanged(companyId, "Company", "Restricted Categories",
                str(company.getRestrictedCategories()), str(form.getRestrictedCategories()), changedBy);
        logIfChanged(companyId, "Company", "Allowed Departments",
                str(company.getAllowedDepartments()), str(form.getAllowedDepartments()), changedBy);
        logIfChanged(companyId, "Company", "Allowed Locations",
                str(company.getAllowedLocations()), str(form.getAllowedLocations()), changedBy);

        company.setRestrictedCategories(form.getRestrictedCategories());
        company.setAllowedDepartments(form.getAllowedDepartments());
        company.setAllowedLocations(form.getAllowedLocations());
        companyService.save(company);
    }

    // Only creates an audit log entry when the value actually changed
    private void logIfChanged(Long companyId, String eventType, String field,
                               String oldVal, String newVal, String changedBy) {
        if (!Objects.equals(oldVal, newVal)) {
            auditRepo.save(new PolicyAuditLog(companyId, eventType, field, oldVal, newVal, changedBy));
        }
    }

    // Helper to build a GiftPolicy with all required fields set
    private GiftPolicy build(Long companyId, EventType type, BigDecimal budget,
                              boolean approvalRequired, BigDecimal threshold, boolean autoSend) {
        GiftPolicy p = new GiftPolicy();
        p.setCompanyId(companyId);
        p.setEventType(type);
        p.setBudgetLimit(budget);
        p.setApprovalRequired(approvalRequired);
        p.setApprovalThreshold(threshold);
        p.setAutoSend(autoSend);
        return p;
    }

    // Converts any value to string for audit log comparison; null becomes empty string
    private String str(Object val) {
        return val == null ? "" : val.toString();
    }
}
