package motorph.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wire response body for POST /api/v1/auth/login. Deliberately carries no
 * account internals (no userAccountId, no role) - just what the client
 * needs to complete the OTP step.
 */
public record LoginResponse(
        String challengeId,
        String maskedEmail,
        boolean temporaryPasswordChangeRequired,
        String message,
        OtpPreview otpPreview
) {

    @JsonCreator
    public LoginResponse(@JsonProperty("challengeId") String challengeId,
                         @JsonProperty("maskedEmail") String maskedEmail,
                         @JsonProperty("temporaryPasswordChangeRequired") boolean temporaryPasswordChangeRequired,
                         @JsonProperty("message") String message,
                         @JsonProperty("otpPreview") OtpPreview otpPreview) {
        this.challengeId = challengeId;
        this.maskedEmail = maskedEmail;
        this.temporaryPasswordChangeRequired = temporaryPasswordChangeRequired;
        this.message = message;
        this.otpPreview = otpPreview;
    }
}
