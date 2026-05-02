package com.giftnova.model;

import java.math.BigDecimal;
import java.util.List;

public class AnalyticsMetrics {

    private final long totalEvents;
    private final long giftsApproved;
    private final BigDecimal budgetUsed;
    private final long momentsPrevented;
    private final int redemptionRate;
    private final long thankYouCount;
    private final int deptCoveragePercent;
    private final BigDecimal monthlyForecast;
    private final Double avgRating;
    private final List<String> chartLabels;
    private final List<BigDecimal> chartValues;
    private final List<String> recentNotes;

    public AnalyticsMetrics(long totalEvents, long giftsApproved, BigDecimal budgetUsed,
                            long momentsPrevented, int redemptionRate, long thankYouCount,
                            int deptCoveragePercent, BigDecimal monthlyForecast, Double avgRating,
                            List<String> chartLabels, List<BigDecimal> chartValues,
                            List<String> recentNotes) {
        this.totalEvents = totalEvents;
        this.giftsApproved = giftsApproved;
        this.budgetUsed = budgetUsed;
        this.momentsPrevented = momentsPrevented;
        this.redemptionRate = redemptionRate;
        this.thankYouCount = thankYouCount;
        this.deptCoveragePercent = deptCoveragePercent;
        this.monthlyForecast = monthlyForecast;
        this.avgRating = avgRating;
        this.chartLabels = chartLabels;
        this.chartValues = chartValues;
        this.recentNotes = recentNotes;
    }

    public long getTotalEvents() { return totalEvents; }
    public long getGiftsApproved() { return giftsApproved; }
    public BigDecimal getBudgetUsed() { return budgetUsed; }
    public long getMomentsPrevented() { return momentsPrevented; }
    public int getRedemptionRate() { return redemptionRate; }
    public long getThankYouCount() { return thankYouCount; }
    public int getDeptCoveragePercent() { return deptCoveragePercent; }
    public BigDecimal getMonthlyForecast() { return monthlyForecast; }
    public Double getAvgRating() { return avgRating; }
    public List<String> getChartLabels() { return chartLabels; }
    public List<BigDecimal> getChartValues() { return chartValues; }
    public List<String> getRecentNotes() { return recentNotes; }
}
