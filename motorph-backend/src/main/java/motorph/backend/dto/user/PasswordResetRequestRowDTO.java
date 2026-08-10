package motorph.backend.dto.user;

import motorph.backend.domain.value.PasswordResetStatus;

import java.time.LocalDateTime;

/**
 * Carries a password reset request for the IT review table, joined
 * server-side with the requesting employee's name and company email.
 */
public record PasswordResetRequestRowDTO(
        int resetRequestId,
        int userAccountId,
        String employeeName,
        String companyEmail,
        PasswordResetStatus status,
        LocalDateTime requestedAt,
        String resolvedBy,
        LocalDateTime completedAt,
        LocalDateTime cancelledAt
) {
}
