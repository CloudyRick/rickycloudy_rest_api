package dev.rickcloudy.restapi.enums;

public enum BlogStatus {
    DRAFT,
    PUBLISHED,
    DELETED,
    ARCHIVED;


    public static BlogStatus fromString(String status) {
        try {
            return BlogStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return PUBLISHED; // Default to PUBLISHED if invalid or null
        }
    }
}
