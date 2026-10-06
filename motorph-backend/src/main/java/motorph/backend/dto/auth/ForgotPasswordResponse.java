package motorph.backend.dto.auth;

/**
 * Response for {@code POST /api/v1/auth/forgot-password}.
 * The same response is returned for all email addresses so the system does not reveal whether an account exists.
 */
public record ForgotPasswordResponse(
        String challengeId,
        long expiresInSeconds,
        String message
) {
}
