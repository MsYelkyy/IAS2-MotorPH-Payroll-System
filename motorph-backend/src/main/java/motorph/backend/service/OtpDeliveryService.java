package motorph.backend.service;

import motorph.backend.dto.auth.OtpPreview;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Phase-1-scoped OTP delivery. The full EmailService (jakarta.mail, SMTP
 * config) moves to the backend in Phase 2 alongside the rest of the employee
 * module. Until then, this mirrors the desktop app's existing behavior:
 * EmailPreviewConfig.previewEnabled defaults to true there because no real
 * SMTP is configured, so every "sent" email is actually just shown in-app.
 * Same default here - motorph.auth.otp-preview-enabled=true unless a real
 * mail integration exists, in which case set it false so the OTP is never
 * echoed back over the API.
 */
@Service
public class OtpDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(OtpDeliveryService.class);
    private static final String DEV_SENDER_EMAIL = "no-reply@motorph.com";

    private final boolean previewEnabled;

    public OtpDeliveryService(@Value("${motorph.auth.otp-preview-enabled:true}") boolean previewEnabled) {
        this.previewEnabled = previewEnabled;

        if (!previewEnabled) {
            log.warn("OTP preview mode is disabled, but the backend has no real email "
                    + "integration yet (EmailService moves in Phase 2). OTPs will only be "
                    + "visible in server logs until then.");
        }
    }

    // logs the OTP server-side always, and returns preview details for the
    // client to display only when no real delivery channel exists
    public OtpPreview sendLoginOtp(String companyEmail, String rawOtp) {
        log.info("[DEV OTP DELIVERY] Login OTP for {} is {}", companyEmail, rawOtp);

        if (!previewEnabled) {
            return null;
        }

        return new OtpPreview(
                DEV_SENDER_EMAIL,
                companyEmail,
                "MotorPH Login Verification",
                buildOtpMessage(rawOtp),
                rawOtp
        );
    }

    private String buildOtpMessage(String otpCode) {
        return "Your MotorPH verification code is: " + otpCode + "\n\n"
                + "This code will expire soon. Do not share this code with anyone.\n\n"
                + "If you did not request this code, please report it to IT/Admin immediately.";
    }
}
