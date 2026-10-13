package motorph.api.dto.auth;

/** Sets the new password with the single-use reset token. */
public record ResetPasswordRequest(String resetToken, String newPassword, String confirmPassword) {

    @Override
    public String toString() {
        return "ResetPasswordRequest[***]";
    }
}
