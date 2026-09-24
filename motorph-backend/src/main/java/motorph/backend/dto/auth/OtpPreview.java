package motorph.backend.dto.auth;

/**
 * Development stand-in for the login OTP email. It is returned in the login response only while
 * {@code motorph.auth.otp-preview-enabled} is true, which must be turned off once real email
 * delivery is configured.
 */
public record OtpPreview(
        String senderEmail,
        String recipientEmail,
        String subject,
        String body,
        String otpCode
) {
}
