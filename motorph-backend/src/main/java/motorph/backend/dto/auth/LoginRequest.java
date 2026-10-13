package motorph.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/v1/auth/login}. The checks are about shape only, never about
 * whether the account exists, so a rejected request tells the caller nothing about accounts.
 */
public record LoginRequest(
        @NotBlank @Email @Size(max = 255) String companyEmail,
        @NotBlank @Size(max = 128) String password
) {

    // Spring logs the request body at DEBUG, so the password must not be in toString().
    @Override
    public String toString() {
        return "LoginRequest[companyEmail=" + companyEmail + ", password=***]";
    }
}
