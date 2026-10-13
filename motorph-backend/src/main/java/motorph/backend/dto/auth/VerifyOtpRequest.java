package motorph.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/v1/auth/verify-otp}. The challenge ID is the URL-safe Base64
 * value issued by {@code /login}, and the OTP is six digits, the same rule {@code TwoFactorService}
 * applies.
 */
public record VerifyOtpRequest(
        @NotBlank @Size(max = 128) @Pattern(regexp = "[A-Za-z0-9_-]+") String challengeId,
        @NotBlank @Pattern(regexp = "\\d{6}") String otp
) {
}
