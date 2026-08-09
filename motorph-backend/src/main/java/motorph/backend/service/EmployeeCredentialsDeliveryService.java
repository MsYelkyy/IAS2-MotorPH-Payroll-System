package motorph.backend.service;

import motorph.backend.dto.auth.CredentialsPreview;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Phase-3-scoped credentials delivery for newly created employee accounts.
 * Mirrors OtpDeliveryService: no real EmailService/SMTP integration exists
 * on the backend yet, so this logs server-side always and returns a preview
 * for the client to display only when no real delivery channel exists.
 */
@Service
public class EmployeeCredentialsDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeCredentialsDeliveryService.class);
    private static final String DEV_SENDER_EMAIL = "no-reply@motorph.com";

    private final boolean previewEnabled;

    public EmployeeCredentialsDeliveryService(
            @Value("${motorph.auth.otp-preview-enabled:true}") boolean previewEnabled) {
        this.previewEnabled = previewEnabled;
    }

    public CredentialsPreview sendEmployeeAccountCredentials(
            String personalEmail, String companyEmail, String temporaryPassword) {

        log.info("[DEV CREDENTIALS DELIVERY] Temporary password for {} ({}) is {}",
                companyEmail, personalEmail, temporaryPassword);

        if (!previewEnabled) {
            return null;
        }

        return new CredentialsPreview(
                DEV_SENDER_EMAIL,
                personalEmail,
                "MotorPH Employee Account Credentials",
                buildMessage(companyEmail, temporaryPassword),
                companyEmail,
                temporaryPassword
        );
    }

    private String buildMessage(String companyEmail, String temporaryPassword) {
        return "Welcome to MotorPH!\n\n"
                + "Your employee account has been created.\n\n"
                + "Company Email: " + companyEmail + "\n"
                + "Temporary Password: " + temporaryPassword + "\n\n"
                + "This temporary password is active for 24 hours only.\n"
                + "Please log in and change your password immediately.\n\n"
                + "If you did not expect this account, please report it to IT/Admin immediately.";
    }
}
