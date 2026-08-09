package motorph.backend.domain.value;

/**
 * Represents the status of a support ticket.
 */
public enum SupportTicketStatus {

    OPEN,
    RESOLVED,
    CANCELLED;

    public static SupportTicketStatus fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Support ticket status is required.");
        }

        try {
            return SupportTicketStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid support ticket status: " + value);
        }
    }
}
