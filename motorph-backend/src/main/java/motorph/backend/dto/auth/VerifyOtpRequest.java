package motorph.backend.dto.auth;

/** Request body for {@code POST /api/v1/auth/verify-otp}. */
public record VerifyOtpRequest(String challengeId, String otp) {
}
