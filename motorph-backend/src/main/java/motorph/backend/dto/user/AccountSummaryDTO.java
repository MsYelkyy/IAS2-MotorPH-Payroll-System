package motorph.backend.dto.user;

/**
 * Display-only summary of an account: the employee who holds it and the company email. Screens that
 * show who approved a request only need these fields, so roles and status are deliberately left
 * out.
 */
public record AccountSummaryDTO(
        int userAccountId,
        int employeeId,
        String fullName,
        String companyEmail
) {
}
