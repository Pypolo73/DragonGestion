package fr.dragon.admincore.core;

public enum StaffRole {
    NONE(0, "Aucun"),
    GUIDE(1, "Guide"),
    MODERATOR(2, "Modo"),
    ADMIN(3, "Admin");

    private final int power;
    private final String displayName;

    StaffRole(final int power, final String displayName) {
        this.power = power;
        this.displayName = displayName;
    }

    public boolean isStaff() {
        return this != NONE;
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }

    public boolean atLeast(final StaffRole other) {
        return this.power >= other.power;
    }

    public String displayName() {
        return this.displayName;
    }

    public static StaffRole fromInput(final String input) {
        if (input == null) {
            return NONE;
        }
        return switch (input.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "guide" -> GUIDE;
            case "modo", "moderator", "mod", "modo+" -> MODERATOR;
            case "admin", "owner" -> ADMIN;
            default -> NONE;
        };
    }
}
