package motorph.api.dto.auth;

/** Unauthenticated request to have IT issue a temporary password. */
public record ForgotPasswordRequest(
        String companyEmail
) {
}
