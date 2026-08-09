package motorph.backend.dto.support;

/** Wire request body for POST /api/v1/support-tickets. employeeId is never included - always the caller. */
public record TicketSubmitRequest(
        String category,
        String subject,
        String description
) {
}
