package motorph.backend.dto.auth;

/**
 * Response body for {@code POST /api/v1/auth/login}. It never includes the user account ID or
 * roles, only what the client needs to complete the OTP step.
 */
public record LoginResponse(
        String challengeId,
        String maskedEmail,
        boolean temporaryPasswordChangeRequired,
        String message,
        OtpPreview otpPreview
) {
}
