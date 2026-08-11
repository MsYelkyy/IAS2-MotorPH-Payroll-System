package motorph.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Wire response body carrying newly issued tokens. */
public record TokenResponse(String accessToken, String refreshToken, long expiresIn) {

    @JsonCreator
    public TokenResponse(@JsonProperty("accessToken") String accessToken,
                         @JsonProperty("refreshToken") String refreshToken,
                         @JsonProperty("expiresIn") long expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }
}
