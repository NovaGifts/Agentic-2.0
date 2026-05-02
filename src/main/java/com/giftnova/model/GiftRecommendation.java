package com.giftnova.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Stores the AI-generated gift recommendation for one upcoming event.
 * AI fills the suggestion fields; deterministic policy engine sets
 * policyStatus, approvalRequired, and riskFlags.
 * Maps to the "gift_recommendations" table.
 */
@Entity
@Table(name = "gift_recommendations")
public class GiftRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // One recommendation per event
    @Column(name = "event_id", nullable = false, unique = true)
    private Long eventId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    // Comma-separated gift IDs from the catalog (e.g. "gift_03,gift_11,gift_18")
    @Column(name = "recommended_gift_ids")
    private String recommendedGiftIds;

    // The suggested total spend — must not exceed policy budget limit
    @Column(name = "recommended_budget", precision = 10, scale = 2)
    private BigDecimal recommendedBudget;

    // Warm message draft written by AI for the HR admin to review/edit
    @Column(name = "message_draft", length = 1000)
    private String messageDraft;

    // AI's explanation of why these gifts were chosen
    @Column(length = 2000)
    private String reasoning;

    // "compliant" or "non_compliant" — set by policy engine, not AI
    @Column(name = "policy_status")
    private String policyStatus;

    // Set deterministically from GiftPolicy — AI cannot override this
    @Column(name = "approval_required")
    private boolean approvalRequired;

    @Column(name = "risk_flags", length = 1000)
    private String riskFlags;

    // Fields populated by the employee on the recipient choice page
    @Column(name = "selected_gift_id")
    private String selectedGiftId;

    @Column(name = "shipping_address", length = 500)
    private String shippingAddress;

    @Column(name = "thank_you_note", length = 1000)
    private String thankYouNote;

    @Column(name = "donation_chosen")
    private boolean donationChosen;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public String getRecommendedGiftIds() { return recommendedGiftIds; }
    public void setRecommendedGiftIds(String recommendedGiftIds) { this.recommendedGiftIds = recommendedGiftIds; }
    public BigDecimal getRecommendedBudget() { return recommendedBudget; }
    public void setRecommendedBudget(BigDecimal recommendedBudget) { this.recommendedBudget = recommendedBudget; }
    public String getMessageDraft() { return messageDraft; }
    public void setMessageDraft(String messageDraft) { this.messageDraft = messageDraft; }
    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }
    public String getPolicyStatus() { return policyStatus; }
    public void setPolicyStatus(String policyStatus) { this.policyStatus = policyStatus; }
    public boolean isApprovalRequired() { return approvalRequired; }
    public void setApprovalRequired(boolean approvalRequired) { this.approvalRequired = approvalRequired; }
    public String getRiskFlags() { return riskFlags; }
    public void setRiskFlags(String riskFlags) { this.riskFlags = riskFlags; }
    public String getSelectedGiftId() { return selectedGiftId; }
    public void setSelectedGiftId(String selectedGiftId) { this.selectedGiftId = selectedGiftId; }
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
    public String getThankYouNote() { return thankYouNote; }
    public void setThankYouNote(String thankYouNote) { this.thankYouNote = thankYouNote; }
    public boolean isDonationChosen() { return donationChosen; }
    public void setDonationChosen(boolean donationChosen) { this.donationChosen = donationChosen; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
