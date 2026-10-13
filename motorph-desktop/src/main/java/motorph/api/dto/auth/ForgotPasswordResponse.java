package motorph.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Same shape for every email, so it says nothing about whether the account
 * exists or has an authenticator.
 */
public record ForgotPasswordResponse(String challengeId, long expiresInSeconds, String message) {

    @JsonCreator
    public ForgotPasswordResponse(@JsonProperty("challengeId") String challengeId,
                                  @JsonProperty("expiresInSeconds") long expiresInSeconds,
                                  @JsonProperty("message") String message) {
        this.challengeId = challengeId;
        this.expiresInSeconds = expiresInSeconds;
        this.message = message;
    }
}
