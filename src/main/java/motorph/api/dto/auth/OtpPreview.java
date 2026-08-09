package motorph.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Dev-only stand-in for a sent OTP email, present on LoginResponse only when
 * the backend has no real SMTP integration wired up (see the backend's
 * OtpDeliveryService / motorph.auth.otp-preview-enabled). Mirrors the shape
 * the app's existing EmailPreviewDialog already knows how to render.
 */
public record OtpPreview(
        String senderEmail,
        String recipientEmail,
        String subject,
        String body,
        String otpCode
) {

    @JsonCreator
    public OtpPreview(@JsonProperty("senderEmail") String senderEmail,
                      @JsonProperty("recipientEmail") String recipientEmail,
                      @JsonProperty("subject") String subject,
                      @JsonProperty("body") String body,
                      @JsonProperty("otpCode") String otpCode) {
        this.senderEmail = senderEmail;
        this.recipientEmail = recipientEmail;
        this.subject = subject;
        this.body = body;
        this.otpCode = otpCode;
    }
}
