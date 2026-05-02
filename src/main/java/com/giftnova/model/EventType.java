package com.giftnova.model;

/**
 * Enum representing the types of employee events that trigger a gift in GiftNova.
 * Each value stores a human-readable label used in the UI and audit log.
 */
public enum EventType {
    BIRTHDAY("Birthday"),
    WORK_ANNIVERSARY("Work Anniversary"),
    ONBOARDING("Onboarding"),
    MANAGER_APPRECIATION("Manager Appreciation");

    // Display name shown in the admin UI and audit log
    private final String label;

    EventType(String label) { this.label = label; }

    public String getLabel() { return label; }
}
