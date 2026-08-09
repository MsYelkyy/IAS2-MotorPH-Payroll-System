package motorph.backend.security;

import java.time.Instant;

/**
 * The per-request identity resolved from a validated, non-revoked access
 * token. This - not any static/singleton session - is the sole source of
 * truth for "who is making this request" on the backend.
 */
public record AuthenticatedPrincipal(
        int userAccountId,
        int employeeId,
        String jti,
        Instant tokenExpiresAt
) {
}
