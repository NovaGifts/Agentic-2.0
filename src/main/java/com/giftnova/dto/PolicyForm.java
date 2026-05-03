package com.giftnova.dto;

import com.giftnova.model.GiftPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for the Policy Builder form submission.
 * Wraps the list of per-event policies together with company-level
 * global settings so everything can be submitted in one form POST.
 */
public class PolicyForm {

    // One entry per event type (Birthday, Work Anniversary, Onboarding, Manager Appreciation)
    private List<GiftPolicy> policies = new ArrayList<>();

    private java.math.BigDecimal monthlyBudget;

    // Comma-separated gift categories that are blocked company-wide (e.g. "Alcohol, Tobacco")
    private String restrictedCategories;

    // Comma-separated departments eligible for gifting; empty = all departments
    private String allowedDepartments;

    // Comma-separated office locations eligible for gifting; empty = all locations
    private String allowedLocations;

    public java.math.BigDecimal getMonthlyBudget() { return monthlyBudget; }
    public void setMonthlyBudget(java.math.BigDecimal monthlyBudget) { this.monthlyBudget = monthlyBudget; }
    public List<GiftPolicy> getPolicies() { return policies; }
    public void setPolicies(List<GiftPolicy> policies) { this.policies = policies; }
    public String getRestrictedCategories() { return restrictedCategories; }
    public void setRestrictedCategories(String restrictedCategories) { this.restrictedCategories = restrictedCategories; }
    public String getAllowedDepartments() { return allowedDepartments; }
    public void setAllowedDepartments(String allowedDepartments) { this.allowedDepartments = allowedDepartments; }
    public String getAllowedLocations() { return allowedLocations; }
    public void setAllowedLocations(String allowedLocations) { this.allowedLocations = allowedLocations; }
}
