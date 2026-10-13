package motorph.backend.security;

import java.time.Instant;

/**
 * Identity of the caller, taken from a validated, non-revoked access token. This is the only source
 * the backend uses to determine who is making a request.
 */
public record AuthenticatedPrincipal(
        int userAccountId,
        int employeeId,
        String jti,
        Instant tokenExpiresAt
) {
}
