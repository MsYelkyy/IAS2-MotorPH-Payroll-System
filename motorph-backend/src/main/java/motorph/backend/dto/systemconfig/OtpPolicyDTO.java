package motorph.backend.dto.systemconfig;

/**
 * The OTP rules the backend actually enforces, surfaced so the Admin system
 * config screen displays the live policy rather than its own copy of the
 * numbers.
 */
public record OtpPolicyDTO(
        int otpLength,
        int expiryMinutes,
        int maxAttempts
) {
}
