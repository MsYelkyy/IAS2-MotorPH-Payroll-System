package motorph.backend.dto.auth;

import java.util.List;

/**
 * Response body for {@code GET /api/v1/auth/me}. Intended for display only; the backend never
 * relies on it for authorization.
 */
public record MeResponse(
        int userAccountId,
        int employeeId,
        String companyEmail,
        List<String> roles,
        String activeRole
) {
}
