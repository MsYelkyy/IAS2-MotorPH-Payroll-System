package motorph.api.dto.systemconfig;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Wire copy of the OTP policy the backend enforces. */
public record OtpPolicyDTO(
        int otpLength,
        int expiryMinutes,
        int maxAttempts
) {

    @JsonCreator
    public OtpPolicyDTO(@JsonProperty("otpLength") int otpLength,
                        @JsonProperty("expiryMinutes") int expiryMinutes,
                        @JsonProperty("maxAttempts") int maxAttempts) {
        this.otpLength = otpLength;
        this.expiryMinutes = expiryMinutes;
        this.maxAttempts = maxAttempts;
    }
}
