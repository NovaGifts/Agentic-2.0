package com.giftnova.model;

import java.util.List;

public class ApprovalItem {

    private UpcomingEvent event;
    private GiftRecommendation recommendation;
    private List<String> giftNames;

    public UpcomingEvent getEvent() { return event; }
    public void setEvent(UpcomingEvent event) { this.event = event; }
    public GiftRecommendation getRecommendation() { return recommendation; }
    public void setRecommendation(GiftRecommendation recommendation) { this.recommendation = recommendation; }
    public List<String> getGiftNames() { return giftNames; }
    public void setGiftNames(List<String> giftNames) { this.giftNames = giftNames; }
}
