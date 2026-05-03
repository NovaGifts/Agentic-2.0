package com.giftnova.model;

import java.math.BigDecimal;
import java.util.List;

public class ApprovalItem {

    private UpcomingEvent event;
    private GiftRecommendation recommendation;
    private List<String> giftNames;
    private String selectedGiftName;
    private BigDecimal policyBudget;

    public UpcomingEvent getEvent() { return event; }
    public void setEvent(UpcomingEvent event) { this.event = event; }
    public GiftRecommendation getRecommendation() { return recommendation; }
    public void setRecommendation(GiftRecommendation recommendation) { this.recommendation = recommendation; }
    public List<String> getGiftNames() { return giftNames; }
    public void setGiftNames(List<String> giftNames) { this.giftNames = giftNames; }
    public String getSelectedGiftName() { return selectedGiftName; }
    public void setSelectedGiftName(String selectedGiftName) { this.selectedGiftName = selectedGiftName; }
    public BigDecimal getPolicyBudget() { return policyBudget; }
    public void setPolicyBudget(BigDecimal policyBudget) { this.policyBudget = policyBudget; }
}
