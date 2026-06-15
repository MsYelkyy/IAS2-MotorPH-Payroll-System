package motorph.application.service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import motorph.application.dto.auth.EmailPreviewDTO;
import motorph.application.exception.ValidationException;
import motorph.infrastructure.config.EmailConfig;
import motorph.infrastructure.config.EmailPreviewConfig;

import java.time.LocalDateTime;
import java.util.Properties;

/**
 * Handles sending system email notifications.
 */
public class EmailService {

    private final EmailConfig emailConfig;
    private final EmailPreviewService emailPreviewService;

    // constructors
    public EmailService(EmailConfig emailConfig) {
        if (emailConfig == null) {
            throw new IllegalArgumentException("EmailConfig is required.");
        }

        validateRequired(emailConfig.getUsername(), "SMTP username is required.");
        validateRequired(emailConfig.getPassword(), "SMTP password is required.");
        validateRequired(emailConfig.getSenderEmail(), "Sender email is required.");

        this.emailConfig = emailConfig;
        this.emailPreviewService = new EmailPreviewService();
    }

    // sends an OTP email
    public void sendOtp(String recipientEmail, String otpCode, String subject) {
        validateRequired(recipientEmail, "Recipient email is required.");
        validateRequired(otpCode, "OTP code is required.");
        validateRequired(subject, "Email subject is required.");

        sendEmail(
                recipientEmail.trim(),
                subject.trim(),
                buildOtpMessage(otpCode.trim())
        );
    }

    // sends a temporary password email
    public void sendTemporaryPassword(String recipientEmail, String companyEmail, String temporaryPassword) {
        validateRequired(recipientEmail, "Recipient email is required.");
        validateRequired(companyEmail, "Company email is required.");
        validateRequired(temporaryPassword, "Temporary password is required.");

        String cleanRecipient = recipientEmail.trim();
        String cleanCompanyEmail = companyEmail.trim();
        String cleanTemporaryPassword = temporaryPassword.trim();
        String subject = "MotorPH Employee Account Credentials";
        String body = buildTemporaryPasswordMessage(cleanCompanyEmail, cleanTemporaryPassword);

        sendEmail(cleanRecipient, subject, body);
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

    // sends a plain text email
    private void sendEmail(String recipientEmail, String subject, String body) {
        saveEmailPreview(recipientEmail, subject, body);

        try {
            Message message = buildMessage(recipientEmail, subject, body);
            Transport.send(message);

        } catch (MessagingException e) {
            System.out.println("=================================");
            System.out.println("EMAIL DELIVERY FAILED");
            System.out.println("Recipient: " + recipientEmail);
            System.out.println("Subject: " + subject);
            System.out.println("Reason: " + e.getMessage());
            System.out.println("=================================");

            // do not block login while Mailtrap is unavailable
        }
    }

    // builds an email message
    private Message buildMessage(String recipientEmail, String subject, String body) throws MessagingException {
        Session session = Session.getInstance(buildMailProperties(), new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                        emailConfig.getUsername(),
                        emailConfig.getPassword()
                );
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(emailConfig.getSenderEmail()));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
        message.setSubject(subject);
        message.setText(body);

        return message;
    }

    // saves email preview when QA preview is enabled
    private void saveEmailPreview(String recipientEmail, String subject, String body) {
        if (!EmailPreviewConfig.isPreviewEnabled()) {
            return;
        }

        EmailPreviewDTO preview = new EmailPreviewDTO(
                emailConfig.getSenderEmail(),
                recipientEmail,
                subject,
                body,
                LocalDateTime.now()
        );

        emailPreviewService.savePreview(preview);
    }

    // builds SMTP properties
    private Properties buildMailProperties() {
        Properties properties = new Properties();

        properties.put("mail.smtp.auth", String.valueOf(emailConfig.isAuthEnabled()));
        properties.put("mail.smtp.starttls.enable", String.valueOf(emailConfig.isStartTlsEnabled()));
        properties.put("mail.smtp.host", emailConfig.getHost());
        properties.put("mail.smtp.port", String.valueOf(emailConfig.getPort()));

        return properties;
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