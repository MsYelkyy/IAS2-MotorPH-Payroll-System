package motorph.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/v1/auth/forgot-password/reset}. Only the reset token's shape is
 * checked here. The password rules stay in the service.
 */
public record ResetPasswordRequest(
        @NotBlank @Size(max = AuthRequestRules.MAX_ID_LENGTH) @Pattern(regexp = AuthRequestRules.URL_SAFE_ID)
        String resetToken,
        String newPassword,
        String confirmPassword
) {

    @Override
    public String toString() {
        return "ResetPasswordRequest[***]";
    }
}
