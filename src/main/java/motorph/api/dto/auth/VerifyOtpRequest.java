package motorph.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Wire request body for POST /api/v1/auth/verify-otp. */
public record VerifyOtpRequest(String challengeId, String otp) {

    @JsonCreator
    public VerifyOtpRequest(@JsonProperty("challengeId") String challengeId,
                            @JsonProperty("otp") String otp) {
        this.challengeId = challengeId;
        this.otp = otp;
    }
}
