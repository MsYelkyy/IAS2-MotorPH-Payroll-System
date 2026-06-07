package motorph.application.service;

import motorph.application.exception.ValidationException;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

/**
 * Handles sending system email notifications.
 */
public class EmailService {

    private final String senderEmail;
    private final String senderPassword;
    private final boolean demoMode;

    // constructors
    public EmailService(String senderEmail, String senderPassword, boolean demoMode) {
        validateRequired(senderEmail, "Sender email is required.");

        if (!demoMode) {
            validateRequired(senderPassword, "Sender password is required.");
        }

        this.senderEmail = senderEmail.trim();
        this.senderPassword = senderPassword == null ? "" : senderPassword.trim();
        this.demoMode = demoMode;
    }

    // sends an OTP email
    public void sendOtp(String recipientEmail, String otpCode, String subject) {
        validateRequired(recipientEmail, "Recipient email is required.");
        validateRequired(otpCode, "OTP code is required.");
        validateRequired(subject, "Email subject is required.");

        String cleanRecipient = recipientEmail.trim();
        String cleanOtp = otpCode.trim();
        String cleanSubject = subject.trim();

        String body = buildOtpMessage(cleanOtp);

        if (demoMode) {
            printDemoEmail(cleanRecipient, cleanSubject, body);
            return;
        }

        sendEmail(cleanRecipient, cleanSubject, body);
    }

    // sends a temporary password email
    public void sendTemporaryPassword(String recipientEmail, String temporaryPassword) {
        validateRequired(recipientEmail, "Recipient email is required.");
        validateRequired(temporaryPassword, "Temporary password is required.");

        String cleanRecipient = recipientEmail.trim();
        String cleanTemporaryPassword = temporaryPassword.trim();
        String subject = "MotorPH Temporary Password";
        String body = buildTemporaryPasswordMessage(cleanTemporaryPassword);

        if (demoMode) {
            printDemoEmail(cleanRecipient, subject, body);
            return;
        }

        sendEmail(cleanRecipient, subject, body);
    }

    // sends a plain text email
    private void sendEmail(String recipientEmail, String subject, String body) {
        Properties properties = buildMailProperties();

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email.", e);
        }
    }

    // builds Gmail SMTP properties
    private Properties buildMailProperties() {
        Properties properties = new Properties();

        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");

        return properties;
    }

    // builds the OTP email message
    private String buildOtpMessage(String otpCode) {
        return "Your MotorPH verification code is: " + otpCode + "\n\n"
                + "This code will expire soon. Do not share this code with anyone.\n\n"
                + "If you did not request this code, please ignore this email.";
    }

    // builds the temporary password email message
    private String buildTemporaryPasswordMessage(String temporaryPassword) {
        return "Your MotorPH temporary password is: " + temporaryPassword + "\n\n"
                + "This temporary password is only active for 5 minutes.\n"
                + "Please log in and change your password immediately.\n\n"
                + "If you did not request this password reset, please contact IT/Admin.";
    }

    // prints email details for demo mode
    private void printDemoEmail(String recipientEmail, String subject, String body) {
        System.out.println("----- MOTORPH DEMO EMAIL -----");
        System.out.println("From: " + senderEmail);
        System.out.println("To: " + recipientEmail);
        System.out.println("Subject: " + subject);
        System.out.println();
        System.out.println(body);
        System.out.println("------------------------------");
    }

    // validates required text
    private void validateRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(message);
        }
    }
}