package motorph.backend.dto.auth;

/**
 * Request to replace an IT-issued temporary password during login. The account is identified by the
 * challenge ID from {@code /login}, never by user account ID.
 */
public record CompleteTemporaryPasswordRequest(
        String challengeId,
        String newPassword,
        String confirmPassword
) {
}
