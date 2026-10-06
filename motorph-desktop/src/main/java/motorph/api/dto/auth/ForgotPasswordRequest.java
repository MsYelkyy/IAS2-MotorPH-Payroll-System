package motorph.api.dto.auth;

/** Unauthenticated request to start an authenticator-based password recovery. */
public record ForgotPasswordRequest(
        String companyEmail
) {
}
