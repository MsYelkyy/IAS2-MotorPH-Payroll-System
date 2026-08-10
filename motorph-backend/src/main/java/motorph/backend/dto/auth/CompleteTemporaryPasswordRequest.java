package motorph.backend.dto.auth;

/**
 * Replaces an IT-issued temporary password. Carries the opaque challenge
 * issued by /login rather than a user account ID - the client is never told
 * which account it is acting on before authentication completes.
 */
public record CompleteTemporaryPasswordRequest(
        String challengeId,
        String newPassword,
        String confirmPassword
) {
}
