package motorph.backend.domain.value;

/**
 * Represents the lifecycle state of a password reset request.
 */
public enum PasswordResetStatus {

    PENDING,
    COMPLETED,
    EXPIRED,
    CANCELLED;

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
