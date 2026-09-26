package motorph.backend.service;

import motorph.backend.dto.auth.OtpPreview;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Delivers login OTPs. There is no mail integration yet, so the OTP is logged on the server and,
 * while {@code motorph.auth.otp-preview-enabled} is true, also returned to the client for display.
 * Turn preview mode off once real email delivery is set up so OTPs are never sent back over the
 * API.
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
