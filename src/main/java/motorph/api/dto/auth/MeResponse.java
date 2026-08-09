package motorph.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Wire response body for GET /api/v1/auth/me - DB-loaded at request time,
 * for UI display purposes only. Never treat this as an authorization source;
 * the backend re-checks permissions independently on every request.
 */
public record MeResponse(
        int userAccountId,
        int employeeId,
        String companyEmail,
        List<String> roles,
        String activeRole
) {

    @JsonCreator
    public MeResponse(@JsonProperty("userAccountId") int userAccountId,
                      @JsonProperty("employeeId") int employeeId,
                      @JsonProperty("companyEmail") String companyEmail,
                      @JsonProperty("roles") List<String> roles,
                      @JsonProperty("activeRole") String activeRole) {
        this.userAccountId = userAccountId;
        this.employeeId = employeeId;
        this.companyEmail = companyEmail;
        this.roles = roles;
        this.activeRole = activeRole;
    }
}
