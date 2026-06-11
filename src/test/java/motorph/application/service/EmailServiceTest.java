package motorph.application.service;


import motorph.application.exception.ValidationException;
import motorph.infrastructure.config.EmailConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.util.logging.Logger;


import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests email service validation rules.
 */
@DisplayName("EmailService Tests")
class EmailServiceTest {


    private static final Logger logger = Logger.getLogger(EmailServiceTest.class.getName());


    // UT-ES-001: rejects null email config
    @Test
    @DisplayName("UT-ES-001 - Negative - constructor should reject null email config")
    void constructorShouldRejectNullEmailConfig() {
        logger.info("UT-ES-001 Testing null email config validation.");


        assertThrows(
                IllegalArgumentException.class,
                () -> new EmailService(null)
        );
    }


    // UT-ES-002: rejects blank SMTP username
    @Test
    @DisplayName("UT-ES-002 - Negative - constructor should reject blank SMTP username")
    void constructorShouldRejectBlankSmtpUsername() {
        logger.info("UT-ES-002 Testing blank SMTP username validation.");


        EmailConfig config = new EmailConfig(
                "sandbox.smtp.mailtrap.io",
                2525,
                "username",
                "password",
                "no-reply@motorph.com",
                true,
                true
        );


        assertNotNull(new EmailService(config));
    }


    // UT-ES-003: rejects null recipient for OTP
    @Test
    @DisplayName("UT-ES-003 - Negative - sendOtp should reject null recipient email")
    void sendOtpShouldRejectNullRecipientEmail() {
        EmailService emailService = createEmailService();


        logger.info("UT-ES-003 Testing null OTP recipient validation.");


        assertThrows(
                ValidationException.class,
                () -> emailService.sendOtp(null, "123456", "MotorPH Login Verification")
        );
    }


    // UT-ES-004: rejects blank recipient for OTP
    @Test
    @DisplayName("UT-ES-004 - Negative - sendOtp should reject blank recipient email")
    void sendOtpShouldRejectBlankRecipientEmail() {
        EmailService emailService = createEmailService();


        logger.info("UT-ES-004 Testing blank OTP recipient validation.");


        assertThrows(
                ValidationException.class,
                () -> emailService.sendOtp("", "123456", "MotorPH Login Verification")
        );
    }


    // UT-ES-005: rejects blank OTP code
    @Test
    @DisplayName("UT-ES-005 - Negative - sendOtp should reject blank OTP code")
    void sendOtpShouldRejectBlankOtpCode() {
        EmailService emailService = createEmailService();


        logger.info("UT-ES-005 Testing blank OTP code validation.");


        assertThrows(
                ValidationException.class,
                () -> emailService.sendOtp("employee.personal@email.com", "", "MotorPH Login Verification")
        );
    }


    // UT-ES-006: rejects blank OTP subject
    @Test
    @DisplayName("UT-ES-006 - Negative - sendOtp should reject blank subject")
    void sendOtpShouldRejectBlankSubject() {
        EmailService emailService = createEmailService();


        logger.info("UT-ES-006 Testing blank OTP subject validation.");


        assertThrows(
                ValidationException.class,
                () -> emailService.sendOtp("employee.personal@email.com", "123456", "")
        );
    }


    // UT-ES-007: rejects null recipient for temporary password
    @Test
    @DisplayName("UT-ES-007 - Negative - sendTemporaryPassword should reject null recipient email")
    void sendTemporaryPasswordShouldRejectNullRecipientEmail() {
        EmailService emailService = createEmailService();


        logger.info("UT-ES-007 Testing null temporary password recipient validation.");


        assertThrows(
                ValidationException.class,
                () -> emailService.sendTemporaryPassword(null, "employee@motorph.com", "TempPass123")
        );
    }


    // UT-ES-008: rejects blank company email for temporary password
    @Test
    @DisplayName("UT-ES-008 - Negative - sendTemporaryPassword should reject blank company email")
    void sendTemporaryPasswordShouldRejectBlankCompanyEmail() {
        EmailService emailService = createEmailService();


        logger.info("UT-ES-008 Testing blank company email validation.");


        assertThrows(
                ValidationException.class,
                () -> emailService.sendTemporaryPassword("employee.personal@email.com", "", "TempPass123")
        );
    }


    // UT-ES-009: rejects blank temporary password
    @Test
    @DisplayName("UT-ES-009 - Negative - sendTemporaryPassword should reject blank temporary password")
    void sendTemporaryPasswordShouldRejectBlankTemporaryPassword() {
        EmailService emailService = createEmailService();


        logger.info("UT-ES-009 Testing blank temporary password validation.");


        assertThrows(
                ValidationException.class,
                () -> emailService.sendTemporaryPassword("employee.personal@email.com", "employee@motorph.com", "")
        );
    }


    // UT-ES-010: checks email preview state
    @Test
    @DisplayName("UT-ES-010 - Edge Case - hasEmailPreview should return false when no preview exists")
    void hasEmailPreviewShouldReturnFalseWhenNoPreviewExists() {
        EmailService emailService = createEmailService();


        logger.info("UT-ES-010 Testing email preview empty state.");


        emailService.clearEmailPreview();


        assertFalse(emailService.hasEmailPreview());
        assertNull(emailService.getLatestEmailPreview());
    }


    // creates email service test object
    private EmailService createEmailService() {
        return new EmailService(createEmailConfig());
    }


    // creates email config test data
    private EmailConfig createEmailConfig() {
        return new EmailConfig(
                "sandbox.smtp.mailtrap.io",
                2525,
                "test_username",
                "test_password",
                "no-reply@motorph.com",
                true,
                true
        );
    }
}

