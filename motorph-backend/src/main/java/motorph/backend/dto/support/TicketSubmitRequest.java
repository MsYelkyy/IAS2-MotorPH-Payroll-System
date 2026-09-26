package motorph.backend.dto.support;

/**
 * Request body for {@code POST /api/v1/support-tickets}. The employee is always the caller, so no
 * employee ID is accepted.
 */
public record TicketSubmitRequest(
        String category,
        String subject,
        String description
) {
}
