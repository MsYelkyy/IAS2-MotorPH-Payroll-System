package motorph.domain.value;

/**
 * Represents the purpose of a two-factor authentication code.
 */
public enum OtpPurpose {

    LOGIN_2FA,
    PASSWORD_RESET;

    // Converts text to OTP purpose.
    public static OtpPurpose fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("OTP purpose is required.");
        }

        try {
            return OtpPurpose.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid OTP purpose: " + value);
        }
    }
}