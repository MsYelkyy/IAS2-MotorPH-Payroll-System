package motorph.backend.service;

import motorph.backend.dto.auth.CredentialsPreview;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Delivers IT-issued temporary passwords. There is no mail integration yet, so the password is only
 * returned, in preview mode, to the IT user handling the request rather than to the account holder.
 * It is never written to the application log. Turn preview mode off once real email delivery is
 * available.
 */
@Service
public class PasswordResetDeliveryService {

    private static final String DEV_SENDER_EMAIL = "no-reply@motorph.com";

    private final boolean previewEnabled;

    public PasswordResetDeliveryService(
            @Value("${motorph.auth.otp-preview-enabled:true}") boolean previewEnabled) {
        this.previewEnabled = previewEnabled;
    }

    public CredentialsPreview sendTemporaryPassword(String personalEmail,
                                                    String companyEmail,
                                                    String temporaryPassword) {
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
