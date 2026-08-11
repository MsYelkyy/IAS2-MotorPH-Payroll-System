package motorph.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Wire request body for POST /api/v1/auth/logout. */
public record LogoutRequest(String refreshToken) {

    @JsonCreator
    public LogoutRequest(@JsonProperty("refreshToken") String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
