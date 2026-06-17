package motorph.application.service;


import motorph.application.exception.EntityNotFoundException;
import motorph.application.exception.UnauthorizedException;
import motorph.application.exception.ValidationException;
import motorph.domain.model.OvertimeRequest;
import motorph.domain.value.RequestStatus;
import motorph.infrastructure.config.EmailConfig;
import motorph.infrastructure.repository.AttendanceRepository;
import motorph.infrastructure.repository.DepartmentRepository;
import motorph.infrastructure.repository.EmployeeRepository;
import motorph.infrastructure.repository.OvertimeRepository;
import motorph.infrastructure.repository.PositionRepository;
import motorph.infrastructure.repository.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;


import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests overtime service using real database dependencies.
 */
@DisplayName("OvertimeService Database Tests")
class OvertimeServiceTest {


    private static final Logger logger = Logger.getLogger(OvertimeServiceTest.class.getName());


    // UT-OS-001: constructor rejects missing overtime repository
    @Test
    @DisplayName("UT-OS-001 - Negative - constructor should reject null overtime repository")
    void constructorShouldRejectNullOvertimeRepository() {
        logger.info("UT-OS-001 Testing null overtime repository validation.");


        assertThrows(
                IllegalArgumentException.class,
                () -> new OvertimeService(
                        null,
                        createAttendanceService(),
                        createEmployeeService()
                )
        );
    }


    // UT-OS-002: constructor rejects missing attendance service
    @Test
    @DisplayName("UT-OS-002 - Negative - constructor should reject null attendance service")
    void constructorShouldRejectNullAttendanceService() {
        logger.info("UT-OS-002 Testing null attendance service validation.");


        assertThrows(
                IllegalArgumentException.class,
                () -> new OvertimeService(
                        new OvertimeRepository(),
                        null,
                        createEmployeeService()
                )
        );
    }


    // UT-OS-003: constructor rejects missing employee service
    @Test
    @DisplayName("UT-OS-003 - Negative - constructor should reject null employee service")
    void constructorShouldRejectNullEmployeeService() {
        logger.info("UT-OS-003 Testing null employee service validation.");


        assertThrows(
                IllegalArgumentException.class,
                () -> new OvertimeService(
                        new OvertimeRepository(),
                        createAttendanceService(),
                        null
                )
        );
    }


    // UT-OS-004: retrieves all overtime requests
    @Test
    @DisplayName("UT-OS-004 - Positive - getAllOvertimeRequests should return overtime requests")
    void getAllOvertimeRequestsShouldReturnOvertimeRequests() {
        OvertimeService overtimeService = createOvertimeService();


        logger.info("UT-OS-004 Testing all overtime request retrieval.");


        List<OvertimeRequest> requests = overtimeService.getAllOvertimeRequests();


        assertNotNull(requests);
    }


    // UT-OS-005: retrieves overtime requests by employee
    @Test
    @DisplayName("UT-OS-005 - Positive - getOvertimeRequestsByEmployeeId should return overtime requests")
    void getOvertimeRequestsByEmployeeIdShouldReturnOvertimeRequests() {
        OvertimeService overtimeService = createOvertimeService();


        logger.info("UT-OS-005 Testing overtime request retrieval by employee.");


        List<OvertimeRequest> requests = overtimeService.getOvertimeRequestsByEmployeeId(10001);


        assertNotNull(requests);
    }


    // UT-OS-006: rejects invalid employee id
    @Test
    @DisplayName("UT-OS-006 - Negative - getOvertimeRequestsByEmployeeId should reject invalid employee ID")
    void getOvertimeRequestsByEmployeeIdShouldRejectInvalidEmployeeId() {
        OvertimeService overtimeService = createOvertimeService();


        logger.info("UT-OS-006 Testing invalid employee ID validation.");


        assertThrows(
                ValidationException.class,
                () -> overtimeService.getOvertimeRequestsByEmployeeId(0)
        );
    }


    // UT-OS-007: retrieves overtime requests by status
    @Test
    @DisplayName("UT-OS-007 - Positive - getOvertimeRequestsByStatus should return overtime requests")
    void getOvertimeRequestsByStatusShouldReturnOvertimeRequests() {
        OvertimeService overtimeService = createOvertimeService();


        logger.info("UT-OS-007 Testing overtime request retrieval by status.");


        List<OvertimeRequest> requests = overtimeService.getOvertimeRequestsByStatus(RequestStatus.PENDING);


        assertNotNull(requests);
    }


    // UT-OS-008: rejects null request status
    @Test
    @DisplayName("UT-OS-008 - Negative - getOvertimeRequestsByStatus should reject null status")
    void getOvertimeRequestsByStatusShouldRejectNullStatus() {
        OvertimeService overtimeService = createOvertimeService();


        logger.info("UT-OS-008 Testing null request status validation.");


        assertThrows(
                ValidationException.class,
                () -> overtimeService.getOvertimeRequestsByStatus(null)
        );
    }


    // UT-OS-009: retrieves pending overtime requests
    @Test
    @DisplayName("UT-OS-009 - Positive - getPendingOvertimeRequests should return pending requests")
    void getPendingOvertimeRequestsShouldReturnPendingRequests() {
        OvertimeService overtimeService = createOvertimeService();


        logger.info("UT-OS-009 Testing pending overtime request retrieval.");


        List<OvertimeRequest> requests = overtimeService.getPendingOvertimeRequests();


        assertNotNull(requests);
    }


    // UT-OS-010: rejects invalid overtime request id
    @Test
    @DisplayName("UT-OS-010 - Negative - getOvertimeRequestById should reject invalid overtime request ID")
    void getOvertimeRequestByIdShouldRejectInvalidOvertimeRequestId() {
        OvertimeService overtimeService = createOvertimeService();


        logger.info("UT-OS-010 Testing invalid overtime request ID validation.");


        assertThrows(
                ValidationException.class,
                () -> overtimeService.getOvertimeRequestById(0)
        );
    }


    // UT-OS-011: rejects missing overtime request
    @Test
    @DisplayName("UT-OS-011 - Edge Case - getOvertimeRequestById should reject missing overtime request")
    void getOvertimeRequestByIdShouldRejectMissingOvertimeRequest() {
        OvertimeService overtimeService = createOvertimeService();


        logger.info("UT-OS-011 Testing missing overtime request lookup.");


        assertThrows(
                EntityNotFoundException.class,
                () -> overtimeService.getOvertimeRequestById(99999)
        );
    }


    // UT-OS-012: retrieves approved overtime hours
    @Test
    @DisplayName("UT-OS-012 - Positive - getApprovedOvertimeHours should return approved hours")
    void getApprovedOvertimeHoursShouldReturnApprovedHours() {
        OvertimeService overtimeService = createOvertimeService();


        logger.info("UT-OS-012 Testing approved overtime hours retrieval.");


        double approvedHours = overtimeService.getApprovedOvertimeHours(
                10001,
                LocalDate.of(2024, 6, 1),
                LocalDate.of(2024, 6, 30)
        );


        assertTrue(approvedHours >= 0);
    }


    // UT-OS-013: rejects invalid date range
    @Test
    @DisplayName("UT-OS-013 - Boundary - getApprovedOvertimeHours should reject invalid date range")
    void getApprovedOvertimeHoursShouldRejectInvalidDateRange() {
        OvertimeService overtimeService = createOvertimeService();


        logger.info("UT-OS-013 Testing invalid date range boundary.");


        assertThrows(
                ValidationException.class,
                () -> overtimeService.getApprovedOvertimeHours(
                        10001,
                        LocalDate.of(2024, 6, 30),
                        LocalDate.of(2024, 6, 1)
                )
        );
    }


    // UT-OS-014: rejects invalid attendance id for submit
    @Test
    @DisplayName("UT-OS-014 - Negative - submitOvertimeRequest should reject invalid attendance ID")
    void submitOvertimeRequestShouldRejectInvalidAttendanceId() {
        OvertimeService overtimeService = createOvertimeService();


        logger.info("UT-OS-014 Testing invalid attendance ID validation.");


        assertThrows(
                ValidationException.class,
                () -> overtimeService.submitOvertimeRequest(
                        10001,
                        0,
                        1,
                        LocalDate.of(2024, 6, 3),
                        2,
                        "Overtime work"
                )
        );
    }


    // UT-OS-015: rejects invalid overtime type id for submit
    @Test
    @DisplayName("UT-OS-015 - Negative - submitOvertimeRequest should reject invalid overtime type ID")
    void submitOvertimeRequestShouldRejectInvalidOvertimeTypeId() {
        OvertimeService overtimeService = createOvertimeService();


        logger.info("UT-OS-015 Testing invalid overtime type ID validation.");


        assertThrows(
                ValidationException.class,
                () -> overtimeService.submitOvertimeRequest(
                        10001,
                        1,
                        0,
                        LocalDate.of(2024, 6, 3),
                        2,
                        "Overtime work"
                )
        );
    }


    // UT-OS-016: rejects null overtime date for submit
    @Test
    @DisplayName("UT-OS-016 - Negative - submitOvertimeRequest should reject null overtime date")
    void submitOvertimeRequestShouldRejectNullOvertimeDate() {
        OvertimeService overtimeService = createOvertimeService();


        logger.info("UT-OS-016 Testing null overtime date validation.");


        assertThrows(
                ValidationException.class,
                () -> overtimeService.submitOvertimeRequest(
                        10001,
                        1,
                        1,
                        null,
                        2,
                        "Overtime work"
                )
        );
    }


    // UT-OS-017: rejects invalid requested hours
    @Test
    @DisplayName("UT-OS-017 - Negative - submitOvertimeRequest should reject invalid requested hours")
    void submitOvertimeRequestShouldRejectInvalidRequestedHours() {
        OvertimeService overtimeService = createOvertimeService();


        logger.info("UT-OS-017 Testing invalid requested hours validation.");


        assertThrows(
                ValidationException.class,
                () -> overtimeService.submitOvertimeRequest(
                        10001,
                        1,
                        1,
                        LocalDate.of(2024, 6, 3),
                        0,
                        "Overtime work"
                )
        );
    }


    // UT-OS-018: rejects blank reason
    @Test
    @DisplayName("UT-OS-018 - Negative - submitOvertimeRequest should reject blank reason")
    void submitOvertimeRequestShouldRejectBlankReason() {
        OvertimeService overtimeService = createOvertimeService();


        logger.info("UT-OS-018 Testing blank overtime reason validation.");


        assertThrows(
                ValidationException.class,
                () -> overtimeService.submitOvertimeRequest(
                        10001,
                        1,
                        1,
                        LocalDate.of(2024, 6, 3),
                        2,
                        ""
                )
        );
    }


    // UT-OS-019: rejects null actor account for approve
    @Test
    @DisplayName("UT-OS-019 - Negative - approveOvertimeRequest should reject null actor")
    void approveOvertimeRequestShouldRejectNullActor() {
        OvertimeService overtimeService = createOvertimeService();


        logger.info("UT-OS-019 Testing null actor account for overtime approval.");


        assertThrows(
                UnauthorizedException.class,
                () -> overtimeService.approveOvertimeRequest(null, 1, "Approved")
        );
    }


    // UT-OS-020: rejects null actor account for reject
    @Test
    @DisplayName("UT-OS-020 - Negative - rejectOvertimeRequest should reject null actor")
    void rejectOvertimeRequestShouldRejectNullActor() {
        OvertimeService overtimeService = createOvertimeService();


        logger.info("UT-OS-020 Testing null actor account for overtime rejection.");


        assertThrows(
                UnauthorizedException.class,
                () -> overtimeService.rejectOvertimeRequest(null, 1, "Rejected")
        );
    }


    // UT-OS-021: rejects invalid actor employee id for cancellation
    @Test
    @DisplayName("UT-OS-021 - Negative - cancelOvertimeRequest should reject invalid actor employee ID")
    void cancelOvertimeRequestShouldRejectInvalidActorEmployeeId() {
        OvertimeService overtimeService = createOvertimeService();


        logger.info("UT-OS-021 Testing invalid actor employee ID for overtime cancellation.");


        assertThrows(
                ValidationException.class,
                () -> overtimeService.cancelOvertimeRequest(0, 1)
        );
    }


    // creates overtime service with real dependencies
    private OvertimeService createOvertimeService() {
        return new OvertimeService(
                new OvertimeRepository(),
                createAttendanceService(),
                createEmployeeService()
        );
    }


    // creates attendance service with real dependency
    private AttendanceService createAttendanceService() {
        return new AttendanceService(new AttendanceRepository());
    }


        // creates employee service with real dependencies
        private EmployeeService createEmployeeService() {
        return new EmployeeService(
                new EmployeeRepository(),
                createUserAccountService(),
                new DepartmentRepository(),
                new PositionRepository()
        );
        }


    // creates user account service dependency
    private UserAccountService createUserAccountService() {
        return new UserAccountService(
                new UserAccountRepository(),
                new EmailService(createEmailConfig())
        );
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

