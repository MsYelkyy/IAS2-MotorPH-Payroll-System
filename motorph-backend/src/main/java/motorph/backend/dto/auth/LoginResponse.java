package motorph.backend.dto.auth;

/**
 * Wire response body for POST /api/v1/auth/login.
 * Never carries userAccountId, role, or any other account internal - only
 * what the client needs to complete the OTP step.
 */
public record LoginResponse(
        String challengeId,
        String maskedEmail,
        boolean temporaryPasswordChangeRequired,
        String message,
        OtpPreview otpPreview
) {
}
