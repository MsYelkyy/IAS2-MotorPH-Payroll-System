package motorph.domain.value;

/**
 * Represents the status of a leave request.
 */
public enum LeaveStatus {

    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED;

    // Converts text to leave status.
    public static LeaveStatus fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Leave status is required.");
        }

        try {
            return LeaveStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid leave status: " + value);
        }
    }
}