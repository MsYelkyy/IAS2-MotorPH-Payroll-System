package motorph.backend.dto.auth;

/** Request body for {@code POST /api/v1/auth/forgot-password/reset}. */
public record ResetPasswordRequest(
        String resetToken,
        String newPassword,
        String confirmPassword
) {

    @Override
    public String toString() {
        return "ResetPasswordRequest[***]";
    }
}
