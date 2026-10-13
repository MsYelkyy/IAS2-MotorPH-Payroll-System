package motorph.backend.dto.auth;

/** Request body for {@code POST /api/v1/auth/forgot-password}. */
public record ForgotPasswordRequest(
        String companyEmail
) {
}
