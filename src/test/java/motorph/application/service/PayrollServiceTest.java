package motorph.application.service;

import motorph.application.dto.login.LoginRequestDTO;
import motorph.application.exception.AuthenticationException;
import motorph.application.exception.ValidationException;
import motorph.infrastructure.config.EmailConfig;
import motorph.infrastructure.repository.DepartmentRepository;
import motorph.infrastructure.repository.EmployeeBenefitRepository;
import motorph.infrastructure.repository.EmployeeRepository;
import motorph.infrastructure.repository.GovernmentDetailsRepository;
import motorph.infrastructure.repository.PositionRepository;
import motorph.infrastructure.repository.SalaryHistoryRepository;
import motorph.infrastructure.repository.TwoFactorCodeRepository;
import motorph.infrastructure.repository.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests authentication service validation rules (payroll test placeholder).
 */
@DisplayName("PayrollService - Authentication Dependency Tests")
class PayrollServiceTest {

    private static final Logger logger = Logger.getLogger(PayrollServiceTest.class.getName());

    // negative: rejects null login request
    @Test
    @DisplayName("Negative - authenticate should reject null login request")
    void authenticateShouldRejectNullLoginRequest() {
        AuthenticationService authenticationService = createAuthenticationService();
        logger.info("Testing null login request validation.");
        assertThrows(AuthenticationException.class, () -> authenticationService.authenticate(null));
    }

    // negative: rejects blank company email
    @Test
    @DisplayName("Negative - authenticate should reject blank company email")
    void authenticateShouldRejectBlankCompanyEmail() {
        AuthenticationService authenticationService = createAuthenticationService();
        logger.info("Testing blank company email validation.");
        LoginRequestDTO request = new LoginRequestDTO("", "password123");
        assertThrows(AuthenticationException.class, () -> authenticationService.authenticate(request));
    }

    // negative: rejects blank password
    @Test
    @DisplayName("Negative - authenticate should reject blank password")
    void authenticateShouldRejectBlankPassword() {
        AuthenticationService authenticationService = createAuthenticationService();
        logger.info("Testing blank password validation.");
        LoginRequestDTO request = new LoginRequestDTO("notfound@motorph.com", "");
        assertThrows(AuthenticationException.class, () -> authenticationService.authenticate(request));
    }

    // edge case: rejects unknown company email
    @Test
    @DisplayName("Edge Case - authenticate should reject unknown company email")
    void authenticateShouldRejectUnknownCompanyEmail() {
        AuthenticationService authenticationService = createAuthenticationService();
        logger.info("Testing unknown company email lookup.");
        LoginRequestDTO request = new LoginRequestDTO("unknown_user_99999@motorph.com", "password123");
        assertThrows(AuthenticationException.class, () -> authenticationService.authenticate(request));
    }

    // boundary: rejects zero user account id
    @Test
    @DisplayName("Boundary - verifyLoginOtp should reject zero user account ID")
    void verifyLoginOtpShouldRejectZeroUserAccountId() {
        AuthenticationService authenticationService = createAuthenticationService();
        logger.info("Testing zero user account ID boundary.");
        assertThrows(ValidationException.class, () -> authenticationService.verifyLoginOtp(0, "123456"));
    }

    // boundary: rejects negative user account id
    @Test
    @DisplayName("Boundary - verifyLoginOtp should reject negative user account ID")
    void verifyLoginOtpShouldRejectNegativeUserAccountId() {
        AuthenticationService authenticationService = createAuthenticationService();
        logger.info("Testing negative user account ID boundary.");
        assertThrows(ValidationException.class, () -> authenticationService.verifyLoginOtp(-1, "123456"));
    }

    // edge case: rejects missing user account during otp verification
    @Test
    @DisplayName("Edge Case - verifyLoginOtp should reject missing user account")
    void verifyLoginOtpShouldRejectMissingUserAccount() {
        AuthenticationService authenticationService = createAuthenticationService();
        logger.info("Testing missing user account during OTP verification.");
        assertThrows(AuthenticationException.class, () -> authenticationService.verifyLoginOtp(99999, "123456"));
    }

    // constructor requires user account repository
    @Test
    @DisplayName("Negative - constructor should require UserAccountRepository")
    void constructorShouldRequireUserAccountRepository() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AuthenticationService(
                        null,
                        createEmployeeService(),
                        new TwoFactorService(new TwoFactorCodeRepository()),
                        createEmailService(),
                        null
                )
        );
    }

    // constructor requires employee service
    @Test
    @DisplayName("Negative - constructor should require EmployeeService")
    void constructorShouldRequireEmployeeService() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AuthenticationService(
                        new UserAccountRepository(),
                        null,
                        new TwoFactorService(new TwoFactorCodeRepository()),
                        createEmailService(),
                        null
                )
        );
    }

    // constructor requires two factor service
    @Test
    @DisplayName("Negative - constructor should require TwoFactorService")
    void constructorShouldRequireTwoFactorService() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AuthenticationService(
                        new UserAccountRepository(),
                        createEmployeeService(),
                        null,
                        createEmailService(),
                        null
                )
        );
    }

    // constructor requires email service
    @Test
    @DisplayName("Negative - constructor should require EmailService")
    void constructorShouldRequireEmailService() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AuthenticationService(
                        new UserAccountRepository(),
                        createEmployeeService(),
                        new TwoFactorService(new TwoFactorCodeRepository()),
                        null,
                        null
                )
        );
    }

    // creates authentication service with real dependencies
    private AuthenticationService createAuthenticationService() {
        return new AuthenticationService(
                new UserAccountRepository(),
                createEmployeeService(),
                new TwoFactorService(new TwoFactorCodeRepository()),
                createEmailService(),
                null
        );
    }

    // creates employee service with real dependencies
    private EmployeeService createEmployeeService() {
        return new EmployeeService(
                new EmployeeRepository(),
                new UserAccountService(new UserAccountRepository(), createEmailService()),
                new DepartmentRepository(),
                new PositionRepository(),
                new SalaryHistoryRepository(),
                new EmployeeBenefitRepository(),
                new GovernmentDetailsRepository()
        );
    }

    // creates email service dependency
    @SuppressWarnings("deprecation")
    private EmailService createEmailService() {
        return new EmailService(EmailConfig.mailtrapSandbox());
    }
}
