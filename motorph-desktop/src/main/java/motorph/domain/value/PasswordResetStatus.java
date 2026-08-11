package motorph.domain.value;

/**
 * Represents the status of a password reset request.
 */
public enum PasswordResetStatus {

    PENDING,
    COMPLETED,
    EXPIRED,
    CANCELLED;

    // Converts text to password reset status.
    public static PasswordResetStatus fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Password reset status is required.");
        }

        try {
            return PasswordResetStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid password reset status: " + value);
        }
    }
}