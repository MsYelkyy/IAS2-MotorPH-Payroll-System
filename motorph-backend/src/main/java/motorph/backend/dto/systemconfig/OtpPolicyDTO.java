package motorph.backend.dto.systemconfig;

/**
 * The OTP rules enforced by the backend, exposed so the system configuration screen shows the live
 * values instead of its own copy.
 */
public record OtpPolicyDTO(
        int otpLength,
        int expiryMinutes,
        int maxAttempts
) {
}
