package motorph.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Authenticator code entered for a login, setup, or password recovery challenge.
 * The challenge is used to identify the account.
 *
 * <p>The checks only look at the shape of the request. The code is six digits, the same rule
 * {@code TotpCodeService} applies.
 */
public record TotpCodeRequest(
        @NotBlank @Size(max = AuthRequestRules.MAX_ID_LENGTH) @Pattern(regexp = AuthRequestRules.URL_SAFE_ID)
        String challengeId,
        @NotBlank @Pattern(regexp = "\\d{6}") String code
) {

    @Override
    public String toString() {
        return "TotpCodeRequest[challengeId=***, code=***]";
    }
}
