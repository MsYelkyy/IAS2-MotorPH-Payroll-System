package motorph.backend.dto.user;

/**
 * Minimal, display-only view of one account: who holds it and its company
 * email. Deliberately narrower than UserAccountRowDTO - no roles, no status,
 * no login history - because the screens that need it (showing which
 * employee approved a request, filling an employee's company email field)
 * only ever render these fields.
 */
public record AccountSummaryDTO(
        int userAccountId,
        int employeeId,
        String fullName,
        String companyEmail
) {
}
