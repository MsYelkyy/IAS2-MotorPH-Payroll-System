package motorph.backend.dto.support;

import motorph.backend.domain.value.RoutingTeam;
import motorph.backend.domain.value.SupportTicketCategory;
import motorph.backend.domain.value.SupportTicketStatus;

import java.time.LocalDateTime;

/**
 * Support ticket row for the team ticket screens, including the submitter's name so handlers do
 * not need access to employee profiles.
 */
public record SupportTicketRowDTO(
        Integer ticketId,
        int employeeId,
        String employeeName,
        SupportTicketCategory category,
        RoutingTeam routingTeam,
        String subject,
        String description,
        SupportTicketStatus status,
        Integer resolvedByUserAccountId,
        LocalDateTime resolvedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
