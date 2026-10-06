package motorph.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** The single-use token that authorises one password change. */
public record PasswordRecoveryVerifyResponse(String resetToken, long expiresInSeconds) {

    @JsonCreator
    public PasswordRecoveryVerifyResponse(@JsonProperty("resetToken") String resetToken,
                                          @JsonProperty("expiresInSeconds") long expiresInSeconds) {
        this.resetToken = resetToken;
        this.expiresInSeconds = expiresInSeconds;
    }

    @Override
    public String toString() {
        return "PasswordRecoveryVerifyResponse[resetToken=***]";
    }
}
