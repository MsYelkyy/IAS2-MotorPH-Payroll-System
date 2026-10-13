package motorph.backend.domain.value;

/**
 * Represents the status of a user account.
 */
public enum AccountStatus {

    ACTIVE,
    DISABLED,
    LOCKED;

    public static AccountStatus fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Account status is required.");
        }

        try {
            return AccountStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid account status: " + value);
        }
    }
}
