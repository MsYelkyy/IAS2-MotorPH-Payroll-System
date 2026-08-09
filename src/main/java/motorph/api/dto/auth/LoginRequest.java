package motorph.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Wire request body for POST /api/v1/auth/login. */
public record LoginRequest(String companyEmail, String password) {

    @JsonCreator
    public LoginRequest(@JsonProperty("companyEmail") String companyEmail,
                        @JsonProperty("password") String password) {
        this.companyEmail = companyEmail;
        this.password = password;
    }
}
