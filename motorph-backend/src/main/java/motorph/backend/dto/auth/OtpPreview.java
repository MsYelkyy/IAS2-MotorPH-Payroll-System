package motorph.backend.dto.auth;

/**
 * Dev-only stand-in for a sent email, returned inline on the login response
 * when no real SMTP integration exists yet (see OtpDeliveryService). Mirrors
 * the desktop's existing EmailPreviewDTO fields so LoginController can feed
 * it straight into the app's existing EmailPreviewDialog. Gated by
 * motorph.auth.otp-preview-enabled - must be off wherever a real mail
 * integration is wired up (Phase 2+).
 */
public record OtpPreview(
        String senderEmail,
        String recipientEmail,
        String subject,
        String body,
        String otpCode
) {
}
