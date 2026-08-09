package motorph.backend.dto.auth;

import java.util.List;

/**
 * Wire response body for GET /api/v1/auth/me - always DB-loaded at request
 * time, for UI display purposes only. Never treat this as an authorization
 * source; the backend re-checks permissions independently on every request.
 */
public record MeResponse(
        int userAccountId,
        int employeeId,
        String companyEmail,
        List<String> roles,
        String activeRole
) {
}
