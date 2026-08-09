package motorph.api.dto.support;

/** Wire request body for POST /api/v1/support-tickets. Outbound only - never deserialized. */
public record TicketSubmitRequest(
        String category,
        String subject,
        String description
) {
}
