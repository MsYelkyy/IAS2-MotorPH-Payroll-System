package motorph.backend.service;

import motorph.backend.dto.auth.CredentialsPreview;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Delivers login credentials for newly created employee accounts. There is no mail integration yet,
 * so the credentials are only returned, while preview mode is enabled, for the client to display.
 * They are never written to the application log.
 */
@Service
public class EmployeeCredentialsDeliveryService {

    private static final String DEV_SENDER_EMAIL = "no-reply@motorph.com";

    private final boolean previewEnabled;

    public EmployeeCredentialsDeliveryService(
            @Value("${motorph.auth.otp-preview-enabled:true}") boolean previewEnabled) {
        this.previewEnabled = previewEnabled;
    }

    public CredentialsPreview sendEmployeeAccountCredentials(
            String personalEmail, String companyEmail, String temporaryPassword) {

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
