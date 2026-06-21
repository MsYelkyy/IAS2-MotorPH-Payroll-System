package motorph.application.service;

import motorph.application.exception.EntityNotFoundException;
import motorph.application.exception.ValidationException;
import motorph.domain.model.PasswordResetRequest;
import motorph.infrastructure.config.EmailConfig;
import motorph.infrastructure.repository.DepartmentRepository;
import motorph.infrastructure.repository.EmployeeBenefitRepository;
import motorph.infrastructure.repository.EmployeeRepository;
import motorph.infrastructure.repository.GovernmentDetailsRepository;
import motorph.infrastructure.repository.PasswordResetRequestRepository;
import motorph.infrastructure.repository.PositionRepository;
import motorph.infrastructure.repository.SalaryHistoryRepository;
import motorph.infrastructure.repository.TwoFactorCodeRepository;
import motorph.infrastructure.repository.UserAccessLogRepository;
import motorph.infrastructure.repository.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests password reset service using real database dependencies.
 */
@DisplayName("PasswordResetService Database Tests")
class PasswordResetServiceTest {

    private static final Logger logger = Logger.getLogger(PasswordResetServiceTest.class.getName());

    // UT-PRS-001: constructor rejects missing password reset repository
    @Test
    @DisplayName("UT-PRS-001 - Negative - constructor should reject null password reset repository")
    void constructorShouldRejectNullPasswordResetRepository() {
        logger.info("UT-PRS-001 Testing null password reset repository validation.");

        assertThrows(
                IllegalArgumentException.class,
                () -> new PasswordResetService(
                        null,
                        new UserAccountRepository(),
                        createEmployeeService(),
                        createTwoFactorService(),
                        createEmailService(),
                        createUserAccessLogService()
                )
        );
    }

    // UT-PRS-002: constructor rejects missing user account repository
    @Test
    @DisplayName("UT-PRS-002 - Negative - constructor should reject null user account repository")
    void constructorShouldRejectNullUserAccountRepository() {
        logger.info("UT-PRS-002 Testing null user account repository validation.");

        assertThrows(
                IllegalArgumentException.class,
                () -> new PasswordResetService(
                        new PasswordResetRequestRepository(),
                        null,
                        createEmployeeService(),
                        createTwoFactorService(),
                        createEmailService(),
                        createUserAccessLogService()
                )
        );
    }

    // UT-PRS-003: constructor rejects missing employee service
    @Test
    @DisplayName("UT-PRS-003 - Negative - constructor should reject null employee service")
    void constructorShouldRejectNullEmployeeService() {
        logger.info("UT-PRS-003 Testing null employee service validation.");

        assertThrows(
                IllegalArgumentException.class,
                () -> new PasswordResetService(
                        new PasswordResetRequestRepository(),
                        new UserAccountRepository(),
                        null,
                        createTwoFactorService(),
                        createEmailService(),
                        createUserAccessLogService()
                )
        );
    }

    // UT-PRS-004: constructor rejects missing two-factor service
    @Test
    @DisplayName("UT-PRS-004 - Negative - constructor should reject null two-factor service")
    void constructorShouldRejectNullTwoFactorService() {
        logger.info("UT-PRS-004 Testing null two-factor service validation.");

        assertThrows(
                IllegalArgumentException.class,
                () -> new PasswordResetService(
                        new PasswordResetRequestRepository(),
                        new UserAccountRepository(),
                        createEmployeeService(),
                        null,
                        createEmailService(),
                        createUserAccessLogService()
                )
        );
    }

    // UT-PRS-005: constructor rejects missing email service
    @Test
    @DisplayName("UT-PRS-005 - Negative - constructor should reject null email service")
    void constructorShouldRejectNullEmailService() {
        logger.info("UT-PRS-005 Testing null email service validation.");

        assertThrows(
                IllegalArgumentException.class,
                () -> new PasswordResetService(
                        new PasswordResetRequestRepository(),
                        new UserAccountRepository(),
                        createEmployeeService(),
                        createTwoFactorService(),
                        null,
                        createUserAccessLogService()
                )
        );
    }

    // UT-PRS-006: constructor rejects missing access log service
    @Test
    @DisplayName("UT-PRS-006 - Negative - constructor should reject null access log service")
    void constructorShouldRejectNullAccessLogService() {
        logger.info("UT-PRS-006 Testing null user access log service validation.");

        assertThrows(
                IllegalArgumentException.class,
                () -> new PasswordResetService(
                        new PasswordResetRequestRepository(),
                        new UserAccountRepository(),
                        createEmployeeService(),
                        createTwoFactorService(),
                        createEmailService(),
                        null
                )
        );
    }

    // UT-PRS-007: retrieves pending password reset requests
    @Test
    @DisplayName("UT-PRS-007 - Positive - listPendingRequests should return pending requests")
    void listPendingRequestsShouldReturnPendingRequests() {
        PasswordResetService passwordResetService = createPasswordResetService();

        logger.info("UT-PRS-007 Testing pending password reset request retrieval.");

        List<PasswordResetRequest> requests = passwordResetService.listPendingRequests();

        assertNotNull(requests);
    }

    // UT-PRS-008: rejects blank company email
    @Test
    @DisplayName("UT-PRS-008 - Negative - requestPasswordReset should reject blank company email")
    void requestPasswordResetShouldRejectBlankCompanyEmail() {
        PasswordResetService passwordResetService = createPasswordResetService();

        logger.info("UT-PRS-008 Testing blank company email validation.");

        assertThrows(
                ValidationException.class,
                () -> passwordResetService.requestPasswordReset("")
        );
    }

    // UT-PRS-009: rejects null company email
    @Test
    @DisplayName("UT-PRS-009 - Negative - requestPasswordReset should reject null company email")
    void requestPasswordResetShouldRejectNullCompanyEmail() {
        PasswordResetService passwordResetService = createPasswordResetService();

        logger.info("UT-PRS-009 Testing null company email validation.");

        assertThrows(
                ValidationException.class,
                () -> passwordResetService.requestPasswordReset(null)
        );
    }

    // UT-PRS-010: rejects unknown company email
    @Test
    @DisplayName("UT-PRS-010 - Edge Case - requestPasswordReset should reject unknown company email")
    void requestPasswordResetShouldRejectUnknownCompanyEmail() {
        PasswordResetService passwordResetService = createPasswordResetService();

        logger.info("UT-PRS-010 Testing unknown company email lookup.");

        assertThrows(
                EntityNotFoundException.class,
                () -> passwordResetService.requestPasswordReset("unknown_user_99999@motorph.com")
        );
    }

    // UT-PRS-011: rejects invalid reset request id for processing
    @Test
    @DisplayName("UT-PRS-011 - Negative - processPasswordResetRequest should reject invalid reset request ID")
    void processPasswordResetRequestShouldRejectInvalidResetRequestId() {
        PasswordResetService passwordResetService = createPasswordResetService();

        logger.info("UT-PRS-011 Testing invalid reset request ID for processing.");

        assertThrows(
                ValidationException.class,
                () -> passwordResetService.processPasswordResetRequest(0, 10001)
        );
    }

    // UT-PRS-012: rejects invalid resolver employee id
    @Test
    @DisplayName("UT-PRS-012 - Negative - processPasswordResetRequest should reject invalid resolver employee ID")
    void processPasswordResetRequestShouldRejectInvalidResolverEmployeeId() {
        PasswordResetService passwordResetService = createPasswordResetService();

        logger.info("UT-PRS-012 Testing invalid resolver employee ID.");

        assertThrows(
                ValidationException.class,
                () -> passwordResetService.processPasswordResetRequest(1, 0)
        );
    }

    // UT-PRS-013: rejects missing reset request
    @Test
    @DisplayName("UT-PRS-013 - Edge Case - processPasswordResetRequest should reject missing reset request")
    void processPasswordResetRequestShouldRejectMissingResetRequest() {
        PasswordResetService passwordResetService = createPasswordResetService();

        logger.info("UT-PRS-013 Testing missing reset request lookup.");

        assertThrows(
                EntityNotFoundException.class,
                () -> passwordResetService.processPasswordResetRequest(99999, 10001)
        );
    }

    // UT-PRS-014: rejects invalid reset request id for processing (approver path)
    @Test
    @DisplayName("UT-PRS-014 - Negative - processPasswordResetRequest should reject invalid reset request ID")
    void processPasswordResetRequestShouldRejectInvalidResetRequestIdApproverPath() {
        PasswordResetService passwordResetService = createPasswordResetService();

        logger.info("UT-PRS-014 Testing invalid reset request ID for approver path.");

        assertThrows(
                ValidationException.class,
                () -> passwordResetService.processPasswordResetRequest(0, 10001)
        );
    }

    // UT-PRS-015: rejects invalid reset request id for cancellation
    @Test
    @DisplayName("UT-PRS-015 - Negative - cancelPasswordResetRequest should reject invalid reset request ID")
    void cancelPasswordResetRequestShouldRejectInvalidResetRequestId() {
        PasswordResetService passwordResetService = createPasswordResetService();

        logger.info("UT-PRS-015 Testing invalid reset request ID for cancellation.");

        assertThrows(
                ValidationException.class,
                () -> passwordResetService.cancelPasswordResetRequest(0, 10001)
        );
    }

    // UT-PRS-016: rejects invalid canceller employee id
    @Test
    @DisplayName("UT-PRS-016 - Negative - cancelPasswordResetRequest should reject invalid canceller employee ID")
    void cancelPasswordResetRequestShouldRejectInvalidCancellerEmployeeId() {
        PasswordResetService passwordResetService = createPasswordResetService();

        logger.info("UT-PRS-016 Testing invalid canceller employee ID.");

        assertThrows(
                ValidationException.class,
                () -> passwordResetService.cancelPasswordResetRequest(1, 0)
        );
    }

    // creates password reset service with real dependencies
    private PasswordResetService createPasswordResetService() {
        return new PasswordResetService(
                new PasswordResetRequestRepository(),
                new UserAccountRepository(),
                createEmployeeService(),
                createTwoFactorService(),
                createEmailService(),
                createUserAccessLogService()
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

    // creates two-factor service dependency
    private TwoFactorService createTwoFactorService() {
        return new TwoFactorService(new TwoFactorCodeRepository());
    }

    // creates email service dependency
    private EmailService createEmailService() {
        return new EmailService(createEmailConfig());
    }

    // creates access log service dependency
    private UserAccessLogService createUserAccessLogService() {
        return new UserAccessLogService(new UserAccessLogRepository());
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
