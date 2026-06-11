package motorph.domain.value;

/**
 * Represents the approval status of employee requests.
 */
public enum RequestStatus {

    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED;

    // Converts text to request status.
    public static RequestStatus fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Request status is required.");
        }

        try {
            return RequestStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid request status: " + value);
        }
    }
}