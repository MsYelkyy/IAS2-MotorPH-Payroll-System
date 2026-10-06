package motorph.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from the login endpoint.
 * It tells the client the next login step and provides a challenge ID.
 * No user ID, role, or tokens are returned yet.
 */
public record LoginResponse(
        String status,
        String challengeId,
        String maskedEmail,
        String message
) {

    public static final String TOTP_REQUIRED = "TOTP_REQUIRED";
    public static final String TOTP_SETUP_REQUIRED = "TOTP_SETUP_REQUIRED";
    public static final String TEMPORARY_PASSWORD_CHANGE_REQUIRED = "TEMPORARY_PASSWORD_CHANGE_REQUIRED";

    @JsonCreator
    public LoginResponse(@JsonProperty("status") String status,
                         @JsonProperty("challengeId") String challengeId,
                         @JsonProperty("maskedEmail") String maskedEmail,
                         @JsonProperty("message") String message) {
        this.status = status;
        this.challengeId = challengeId;
        this.maskedEmail = maskedEmail;
        this.message = message;
    }
}
