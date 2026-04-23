package motorph.domain.value;

/**
 * Represents the role of a user in the system.
 */
public enum Role {

    ADMIN,
    HR,
    FINANCE,
    IT,
    EMPLOYEE;

    // converts text to role
    public static Role fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Role is required.");
        }

        try {
            return Role.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + value);
        }
    }
}