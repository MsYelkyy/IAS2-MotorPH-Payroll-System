package motorph.application.service;


import motorph.application.exception.ValidationException;
import motorph.domain.model.UserAccessLog;
import motorph.infrastructure.repository.UserAccessLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;


import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests user access log service using real database dependencies.
 */
@DisplayName("UserAccessLogService Database Tests")
class UserAccessLogServiceTest {


    private static final Logger logger = Logger.getLogger(UserAccessLogServiceTest.class.getName());


    // UT-UALS-001: constructor rejects missing repository
    @Test
    @DisplayName("UT-UALS-001 - Negative - constructor should reject null repository")
    void constructorShouldRejectNullRepository() {
        logger.info("UT-UALS-001 Testing null repository validation.");


        assertThrows(
                IllegalArgumentException.class,
                () -> new UserAccessLogService(null)
        );
    }


    // UT-UALS-002: retrieves all logs
    @Test
    @DisplayName("UT-UALS-002 - Positive - getAllLogs should return logs")
    void getAllLogsShouldReturnLogs() {
        UserAccessLogService service = createUserAccessLogService();


        logger.info("UT-UALS-002 Testing all logs retrieval.");


        List<UserAccessLog> logs = service.getAllLogs();


        assertNotNull(logs);
    }


    // UT-UALS-003: retrieves logs by user account id
    @Test
    @DisplayName("UT-UALS-003 - Positive - getLogsByUserAccountId should return logs")
    void getLogsByUserAccountIdShouldReturnLogs() {
        UserAccessLogService service = createUserAccessLogService();


        logger.info("UT-UALS-003 Testing logs retrieval by user account ID.");


        List<UserAccessLog> logs = service.getLogsByUserAccountId(1);


        assertNotNull(logs);
    }


    // UT-UALS-004: rejects invalid user account id
    @Test
    @DisplayName("UT-UALS-004 - Negative - getLogsByUserAccountId should reject invalid user account ID")
    void getLogsByUserAccountIdShouldRejectInvalidUserAccountId() {
        UserAccessLogService service = createUserAccessLogService();


        logger.info("UT-UALS-004 Testing invalid user account ID validation.");


        assertThrows(
                ValidationException.class,
                () -> service.getLogsByUserAccountId(0)
        );
    }


    // UT-UALS-005: retrieves logs by action
    @Test
    @DisplayName("UT-UALS-005 - Positive - getLogsByAction should return logs")
    void getLogsByActionShouldReturnLogs() {
        UserAccessLogService service = createUserAccessLogService();


        logger.info("UT-UALS-005 Testing logs retrieval by action.");


        List<UserAccessLog> logs = service.getLogsByAction(UserAccessLogService.ACTION_LOGIN_SUCCESS);


        assertNotNull(logs);
    }


    // UT-UALS-006: rejects blank action
    @Test
    @DisplayName("UT-UALS-006 - Negative - getLogsByAction should reject blank action")
    void getLogsByActionShouldRejectBlankAction() {
        UserAccessLogService service = createUserAccessLogService();


        logger.info("UT-UALS-006 Testing blank action validation.");


        assertThrows(
                ValidationException.class,
                () -> service.getLogsByAction("")
        );
    }


    // UT-UALS-007: searches logs by keyword
    @Test
    @DisplayName("UT-UALS-007 - Positive - searchLogs should return logs")
    void searchLogsShouldReturnLogs() {
        UserAccessLogService service = createUserAccessLogService();


        logger.info("UT-UALS-007 Testing logs search by keyword.");


        List<UserAccessLog> logs = service.searchLogs("LOGIN");


        assertNotNull(logs);
    }


    // UT-UALS-008: rejects blank search keyword
    @Test
    @DisplayName("UT-UALS-008 - Negative - searchLogs should reject blank keyword")
    void searchLogsShouldRejectBlankKeyword() {
        UserAccessLogService service = createUserAccessLogService();


        logger.info("UT-UALS-008 Testing blank keyword validation.");


        assertThrows(
                ValidationException.class,
                () -> service.searchLogs("")
        );
    }


    // UT-UALS-009: retrieves logs by date range
    @Test
    @DisplayName("UT-UALS-009 - Positive - getLogsByDateRange should return logs")
    void getLogsByDateRangeShouldReturnLogs() {
        UserAccessLogService service = createUserAccessLogService();


        logger.info("UT-UALS-009 Testing logs retrieval by date range.");


        List<UserAccessLog> logs = service.getLogsByDateRange(
                LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.now()
        );


        assertNotNull(logs);
    }


    // UT-UALS-010: rejects null date range
    @Test
    @DisplayName("UT-UALS-010 - Negative - getLogsByDateRange should reject null date range")
    void getLogsByDateRangeShouldRejectNullDateRange() {
        UserAccessLogService service = createUserAccessLogService();


        logger.info("UT-UALS-010 Testing null date range validation.");


        assertThrows(
                ValidationException.class,
                () -> service.getLogsByDateRange(null, LocalDateTime.now())
        );
    }


    // UT-UALS-011: rejects invalid date range
    @Test
    @DisplayName("UT-UALS-011 - Boundary - getLogsByDateRange should reject invalid date range")
    void getLogsByDateRangeShouldRejectInvalidDateRange() {
        UserAccessLogService service = createUserAccessLogService();


        logger.info("UT-UALS-011 Testing invalid date range boundary.");


        assertThrows(
                ValidationException.class,
                () -> service.getLogsByDateRange(
                        LocalDateTime.now(),
                        LocalDateTime.of(2024, 1, 1, 0, 0)
                )
        );
    }


    // UT-UALS-012: rejects invalid login success user account id
    @Test
    @DisplayName("UT-UALS-012 - Negative - logLoginSuccess should reject invalid user account ID")
    void logLoginSuccessShouldRejectInvalidUserAccountId() {
        UserAccessLogService service = createUserAccessLogService();


        logger.info("UT-UALS-012 Testing invalid login success user account ID.");


        assertThrows(
                ValidationException.class,
                () -> service.logLoginSuccess(0, "Login successful.")
        );
    }


    // UT-UALS-013: rejects blank company email for failed login
    @Test
    @DisplayName("UT-UALS-013 - Negative - logLoginFailed should reject blank company email")
    void logLoginFailedShouldRejectBlankCompanyEmail() {
        UserAccessLogService service = createUserAccessLogService();


        logger.info("UT-UALS-013 Testing blank failed login company email.");


        assertThrows(
                ValidationException.class,
                () -> service.logLoginFailed("", "Invalid password.")
        );
    }


    // UT-UALS-014: rejects blank view name
    @Test
    @DisplayName("UT-UALS-014 - Negative - logOpenView should reject blank view name")
    void logOpenViewShouldRejectBlankViewName() {
        UserAccessLogService service = createUserAccessLogService();


        logger.info("UT-UALS-014 Testing blank view name validation.");


        assertThrows(
                ValidationException.class,
                () -> service.logOpenView(1, "")
        );
    }


    // UT-UALS-015: rejects invalid target employee id
    @Test
    @DisplayName("UT-UALS-015 - Negative - logPasswordReset should reject invalid target employee ID")
    void logPasswordResetShouldRejectInvalidTargetEmployeeId() {
        UserAccessLogService service = createUserAccessLogService();


        logger.info("UT-UALS-015 Testing invalid target employee ID for password reset log.");


        assertThrows(
                ValidationException.class,
                () -> service.logPasswordReset(1, 0)
        );
    }


    // UT-UALS-016: rejects blank report name
    @Test
    @DisplayName("UT-UALS-016 - Negative - logGeneratedReport should reject blank report name")
    void logGeneratedReportShouldRejectBlankReportName() {
        UserAccessLogService service = createUserAccessLogService();


        logger.info("UT-UALS-016 Testing blank report name validation.");


        assertThrows(
                ValidationException.class,
                () -> service.logGeneratedReport(1, "")
        );
    }


    // creates user access log service with real dependency
    private UserAccessLogService createUserAccessLogService() {
        return new UserAccessLogService(new UserAccessLogRepository());
    }
}

