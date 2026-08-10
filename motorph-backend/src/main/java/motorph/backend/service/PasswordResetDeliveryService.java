package motorph.backend.service;

import motorph.backend.dto.auth.CredentialsPreview;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Delivery of IT-issued temporary passwords. Same shape as
 * OtpDeliveryService/EmployeeCredentialsDeliveryService: no real SMTP
 * integration exists on the backend yet, so this logs server-side always and
 * returns a preview only while preview mode is on. The preview goes to the IT
 * user resolving the request, not to the account holder, so switch preview
 * mode off as soon as real mail delivery exists.
 */
@Service
public class PasswordResetDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetDeliveryService.class);
    private static final String DEV_SENDER_EMAIL = "no-reply@motorph.com";

    private final boolean previewEnabled;

    public PasswordResetDeliveryService(
            @Value("${motorph.auth.otp-preview-enabled:true}") boolean previewEnabled) {
        this.previewEnabled = previewEnabled;
    }

    public CredentialsPreview sendTemporaryPassword(String personalEmail,
                                                    String companyEmail,
                                                    String temporaryPassword) {
        log.info("[DEV PASSWORD RESET DELIVERY] Temporary password for {} ({}) is {}",
                companyEmail, personalEmail, temporaryPassword);

        if (!previewEnabled) {
            return null;
        }

        return new CredentialsPreview(
                DEV_SENDER_EMAIL,
                personalEmail,
                "MotorPH Password Reset",
                buildMessage(companyEmail, temporaryPassword),
                companyEmail,
                temporaryPassword
        );
    }

    private String buildMessage(String companyEmail, String temporaryPassword) {
        return "A password reset was processed for your MotorPH account.\n\n"
                + "Company Email: " + companyEmail + "\n"
                + "Temporary Password: " + temporaryPassword + "\n\n"
                + "This temporary password expires in one hour.\n"
                + "Log in with it and set a new password immediately.\n\n"
                + "If you did not request this reset, report it to IT/Admin immediately.";
    }
}
