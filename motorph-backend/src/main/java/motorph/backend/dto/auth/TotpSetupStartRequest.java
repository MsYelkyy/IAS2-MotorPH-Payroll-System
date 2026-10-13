package motorph.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request body for {@code POST /api/v1/auth/totp/setup/start}. */
public record TotpSetupStartRequest(
        @NotBlank @Size(max = AuthRequestRules.MAX_ID_LENGTH) @Pattern(regexp = AuthRequestRules.URL_SAFE_ID)
        String challengeId
) {

    @Override
    public String toString() {
        return "TotpSetupStartRequest[challengeId=***]";
    }
}
