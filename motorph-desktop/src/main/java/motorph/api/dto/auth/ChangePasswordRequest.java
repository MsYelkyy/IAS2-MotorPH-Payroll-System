package motorph.api.dto.auth;

/** Authenticated change of the caller's own password. */
public record ChangePasswordRequest(
        String currentPassword,
        String newPassword
) {
}
