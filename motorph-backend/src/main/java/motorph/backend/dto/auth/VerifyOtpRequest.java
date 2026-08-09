package motorph.backend.dto.auth;

/** Wire request body for POST /api/v1/auth/verify-otp. */
public record VerifyOtpRequest(String challengeId, String otp) {
}
