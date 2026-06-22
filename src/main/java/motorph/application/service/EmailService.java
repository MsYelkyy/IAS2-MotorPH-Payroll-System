package motorph.application.service;

import motorph.application.dto.auth.EmailPreviewDTO;
import motorph.application.dto.auth.EmailPreviewType;
import motorph.application.exception.ValidationException;
import motorph.infrastructure.config.EmailConfig;
import motorph.infrastructure.config.EmailPreviewConfig;

import java.time.LocalDateTime;

/**
 * Handles system email notifications through in-app preview delivery.
 */
public class EmailService {

    private final EmailConfig emailConfig;
    private final EmailPreviewService emailPreviewService;

    // constructors
    public EmailService(EmailConfig emailConfig) {
        this(emailConfig, new EmailPreviewService());
    }

    public EmailService(EmailConfig emailConfig, EmailPreviewService emailPreviewService) {
        if (emailConfig == null) {
            throw new IllegalArgumentException("EmailConfig is required.");
        }

        validateRequired(emailConfig.getSenderEmail(), "Sender email is required.");
        if (emailPreviewService == null) {
            throw new IllegalArgumentException("EmailPreviewService is required.");
        }

        this.emailConfig = emailConfig;
        this.emailPreviewService = emailPreviewService;
    }

    // sends an OTP email
    public void sendOtp(String recipientEmail, String otpCode, String subject) {
        validateRequired(recipientEmail, "Recipient email is required.");
        validateRequired(otpCode, "OTP code is required.");
        validateRequired(subject, "Email subject is required.");

        sendEmail(
                EmailPreviewType.LOGIN_OTP,
                recipientEmail.trim(),
                subject.trim(),
                buildOtpMessage(otpCode.trim()),
                "OTP Code",
                otpCode.trim()
        );
    }

    // sends a password reset OTP email
    public void sendPasswordResetOtp(String recipientEmail, String otpCode, String subject) {
        validateRequired(recipientEmail, "Recipient email is required.");
        validateRequired(otpCode, "OTP code is required.");
        validateRequired(subject, "Email subject is required.");

        sendEmail(
                EmailPreviewType.PASSWORD_RESET_OTP,
                recipientEmail.trim(),
                subject.trim(),
                buildOtpMessage(otpCode.trim()),
                "OTP Code",
                otpCode.trim()
        );
    }

    // sends a temporary password email
    public void sendTemporaryPassword(String recipientEmail, String companyEmail, String temporaryPassword) {
        sendTemporaryPassword(
                EmailPreviewType.TEMPORARY_PASSWORD,
                recipientEmail,
                companyEmail,
                temporaryPassword
        );
    }

    // sends a temporary password email for employee account creation
    public void sendEmployeeAccountCredentials(String recipientEmail, String companyEmail, String temporaryPassword) {
        sendTemporaryPassword(
                EmailPreviewType.EMPLOYEE_ACCOUNT_CREDENTIALS,
                recipientEmail,
                companyEmail,
                temporaryPassword
        );
    }

    // sends a temporary password email for IT-approved password reset
    public void sendPasswordResetTemporaryPassword(String recipientEmail, String companyEmail, String temporaryPassword) {
        sendTemporaryPassword(
                EmailPreviewType.TEMPORARY_PASSWORD,
                recipientEmail,
                companyEmail,
                temporaryPassword
        );
    }

    private void sendTemporaryPassword(EmailPreviewType type,
                                       String recipientEmail,
                                       String companyEmail,
                                       String temporaryPassword) {
        validateRequired(recipientEmail, "Recipient email is required.");
        validateRequired(companyEmail, "Company email is required.");
        validateRequired(temporaryPassword, "Temporary password is required.");

        String cleanRecipient = recipientEmail.trim();
        String cleanCompanyEmail = companyEmail.trim();
        String cleanTemporaryPassword = temporaryPassword.trim();
        String subject = "MotorPH Employee Account Credentials";
        String body = buildTemporaryPasswordMessage(cleanCompanyEmail, cleanTemporaryPassword);

        sendEmail(
                type,
                cleanRecipient,
                subject,
                body,
                "Temporary Password",
                cleanTemporaryPassword
        );
    }

    // checks if an email preview is available
    public boolean hasEmailPreview() {
        return emailPreviewService.hasPreview();
    }

    // returns the latest email preview
    public EmailPreviewDTO getLatestEmailPreview() {
        return emailPreviewService.getLatestPreview();
    }

    // clears the latest email preview
    public void clearEmailPreview() {
        emailPreviewService.clearPreview();
    }

    // publishes a plain text email preview
    private void sendEmail(EmailPreviewType type,
                           String recipientEmail,
                           String subject,
                           String body,
                           String highlightLabel,
                           String highlightValue) {
        if (!EmailPreviewConfig.isPreviewEnabled()) {
            return;
        }

        EmailPreviewDTO preview = new EmailPreviewDTO(
                type,
                emailConfig.getSenderEmail(),
                recipientEmail,
                subject,
                body,
                highlightLabel,
                highlightValue,
                LocalDateTime.now()
        );

        emailPreviewService.savePreview(preview);
    }

    // builds the OTP email message
    private String buildOtpMessage(String otpCode) {
        return "Your MotorPH verification code is: " + otpCode + "\n\n"
                + "This code will expire soon. Do not share this code with anyone.\n\n"
                + "If you did not request this code, please report it to IT/Admin immediately.";
    }

    // builds the temporary password email message
    private String buildTemporaryPasswordMessage(String companyEmail, String temporaryPassword) {
        return "Welcome to MotorPH!\n\n"
                + "Your employee account has been created.\n\n"
                + "Company Email: " + companyEmail + "\n"
                + "Temporary Password: " + temporaryPassword + "\n\n"
                + "This temporary password is active for 24 hours only.\n"
                + "Please log in and change your password immediately.\n\n"
                + "If you did not expect this account, please report it to IT/Admin immediately.";
    }

    // validates required text
    private void validateRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(message);
        }
    }
}
