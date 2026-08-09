package motorph.backend.domain.value;

/**
 * Represents the category of a support ticket.
 */
public enum SupportTicketCategory {

    PAYROLL,
    ATTENDANCE,
    ACCOUNT,
    SYSTEM,
    OTHER;

    public static SupportTicketCategory fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Support ticket category is required.");
        }

        try {
            return SupportTicketCategory.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid support ticket category: " + value);
        }
    }
}
