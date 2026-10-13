package motorph.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request to replace an IT-issued temporary password during login. The account is identified by the
 * challenge ID from {@code /login}, never by user account ID. Only the challenge ID's shape is
 * checked here. The password rules stay in the service.
 */
public record CompleteTemporaryPasswordRequest(
        @NotBlank @Size(max = AuthRequestRules.MAX_ID_LENGTH) @Pattern(regexp = AuthRequestRules.URL_SAFE_ID)
        String challengeId,
        String newPassword,
        String confirmPassword
) {

    @Override
    public String toString() {
        return "CompleteTemporaryPasswordRequest[***]";
    }
}
