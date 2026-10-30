package com.insurance.gw.model;

/** Role-based users of the suite - Guidewire security is role based (Producer, Underwriter, Adjuster ...). */
public enum UserRole {
    PRODUCER("Producer", "agent1"),
    UNDERWRITER("Underwriter", "uw1"),
    CLAIMS_ADJUSTER("Claims Adjuster", "adj1"),
    CLAIMS_SUPERVISOR("Claims Supervisor", "sup1"),
    BILLING_CLERK("Billing Clerk", "bill1");

    public static final String DEFAULT_PASSWORD = "gw";

    private final String label;
    private final String username;

    UserRole(String label, String username) {
        this.label = label;
        this.username = username;
    }

    public String label() {
        return label;
    }

    public String username() {
        return username;
    }

    public static UserRole fromLabel(String label) {
        for (UserRole role : values()) {
            if (role.label.equalsIgnoreCase(label)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + label);
    }
}
