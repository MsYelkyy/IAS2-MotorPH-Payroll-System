package motorph.api.dto.auth;

/**
 * Replaces an IT-issued temporary password, identified by the opaque
 * challenge login returned - never by a user account ID.
 */
public record CompleteTemporaryPasswordRequest(
        String challengeId,
        String newPassword,
        String confirmPassword
) {
}
