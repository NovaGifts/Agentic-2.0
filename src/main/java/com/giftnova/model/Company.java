package com.giftnova.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Company name is required")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Website is required")
    @Column(nullable = false)
    private String website;

    @NotBlank(message = "Industry is required")
    @Column(nullable = false)
    private String industry;

    @Min(value = 1, message = "Employee count must be at least 1")
    @Column(name = "employee_count", nullable = false)
    private int employeeCount;

    @NotBlank(message = "Country is required")
    @Column(nullable = false)
    private String country;

    @NotBlank(message = "Currency is required")
    @Column(nullable = false, length = 3)
    private String currency;

    @NotBlank(message = "Admin name is required")
    @Column(name = "admin_name", nullable = false)
    private String adminName;

    @NotBlank(message = "Admin email is required")
    @Email(message = "Enter a valid email address")
    @Column(name = "admin_email", nullable = false)
    private String adminEmail;

    @NotBlank(message = "Timezone is required")
    @Column(nullable = false)
    private String timezone;

    @NotNull(message = "Monthly budget is required")
    @DecimalMin(value = "0.01", message = "Budget must be greater than 0")
    @Column(name = "monthly_budget", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyBudget;

    @NotNull(message = "Approval threshold is required")
    @DecimalMin(value = "0.01", message = "Threshold must be greater than 0")
    @Column(name = "approval_threshold", nullable = false, precision = 10, scale = 2)
    private BigDecimal approvalThreshold;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }
    public int getEmployeeCount() { return employeeCount; }
    public void setEmployeeCount(int employeeCount) { this.employeeCount = employeeCount; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getAdminName() { return adminName; }
    public void setAdminName(String adminName) { this.adminName = adminName; }
    public String getAdminEmail() { return adminEmail; }
    public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public BigDecimal getMonthlyBudget() { return monthlyBudget; }
    public void setMonthlyBudget(BigDecimal monthlyBudget) { this.monthlyBudget = monthlyBudget; }
    public BigDecimal getApprovalThreshold() { return approvalThreshold; }
    public void setApprovalThreshold(BigDecimal approvalThreshold) { this.approvalThreshold = approvalThreshold; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
