package motorph.application.service;


import motorph.application.exception.EntityNotFoundException;
import motorph.application.exception.UnauthorizedException;
import motorph.application.exception.ValidationException;
import motorph.domain.model.LeaveRequest;
import motorph.domain.value.RequestStatus;
import motorph.infrastructure.config.EmailConfig;
import motorph.infrastructure.repository.DepartmentRepository;
import motorph.infrastructure.repository.EmployeeBenefitRepository;
import motorph.infrastructure.repository.EmployeeRepository;
import motorph.infrastructure.repository.GovernmentDetailsRepository;
import motorph.infrastructure.repository.LeaveCreditRepository;
import motorph.infrastructure.repository.LeaveRepository;
import motorph.infrastructure.repository.PositionRepository;
import motorph.infrastructure.repository.SalaryHistoryRepository;
import motorph.infrastructure.repository.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;


import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests leave service using real database dependencies.
 */
@DisplayName("LeaveService Database Tests")
class LeaveServiceTest {


    private static final Logger logger = Logger.getLogger(LeaveServiceTest.class.getName());


    // UT-LS-001: constructor rejects missing leave repository
    @Test
    @DisplayName("UT-LS-001 - Negative - constructor should reject null leave repository")
    void constructorShouldRejectNullLeaveRepository() {
        logger.info("UT-LS-001 Testing null leave repository validation.");


        assertThrows(
                IllegalArgumentException.class,
                () -> new LeaveService(
                        null,
                        new LeaveCreditRepository(),
                        createEmployeeService()
                )
        );
    }


    // UT-LS-002: constructor rejects missing leave credit repository
    @Test
    @DisplayName("UT-LS-002 - Negative - constructor should reject null leave credit repository")
    void constructorShouldRejectNullLeaveCreditRepository() {
        logger.info("UT-LS-002 Testing null leave credit repository validation.");


        assertThrows(
                IllegalArgumentException.class,
                () -> new LeaveService(
                        new LeaveRepository(),
                        null,
                        createEmployeeService()
                )
        );
    }


    // UT-LS-003: constructor rejects missing employee service
    @Test
    @DisplayName("UT-LS-003 - Negative - constructor should reject null employee service")
    void constructorShouldRejectNullEmployeeService() {
        logger.info("UT-LS-003 Testing null employee service validation.");


        assertThrows(
                IllegalArgumentException.class,
                () -> new LeaveService(
                        new LeaveRepository(),
                        new LeaveCreditRepository(),
                        null
                )
        );
    }


    // UT-LS-004: retrieves all leave requests
    @Test
    @DisplayName("UT-LS-004 - Positive - getAllLeaveRequests should return leave requests")
    void getAllLeaveRequestsShouldReturnLeaveRequests() {
        LeaveService leaveService = createLeaveService();


        logger.info("UT-LS-004 Testing all leave request retrieval.");


        List<LeaveRequest> requests = leaveService.getAllLeaveRequests();


        assertNotNull(requests);
    }


    // UT-LS-005: retrieves leave requests by employee
    @Test
    @DisplayName("UT-LS-005 - Positive - getLeaveRequestsByEmployeeId should return leave requests")
    void getLeaveRequestsByEmployeeIdShouldReturnLeaveRequests() {
        LeaveService leaveService = createLeaveService();


        logger.info("UT-LS-005 Testing leave request retrieval by employee.");


        List<LeaveRequest> requests = leaveService.getLeaveRequestsByEmployeeId(10001);


        assertNotNull(requests);
    }


    // UT-LS-006: rejects invalid employee id
    @Test
    @DisplayName("UT-LS-006 - Negative - getLeaveRequestsByEmployeeId should reject invalid employee ID")
    void getLeaveRequestsByEmployeeIdShouldRejectInvalidEmployeeId() {
        LeaveService leaveService = createLeaveService();


        logger.info("UT-LS-006 Testing invalid employee ID validation.");


        assertThrows(
                ValidationException.class,
                () -> leaveService.getLeaveRequestsByEmployeeId(0)
        );
    }


    // UT-LS-007: retrieves leave requests by status
    @Test
    @DisplayName("UT-LS-007 - Positive - getLeaveRequestsByStatus should return leave requests")
    void getLeaveRequestsByStatusShouldReturnLeaveRequests() {
        LeaveService leaveService = createLeaveService();


        logger.info("UT-LS-007 Testing leave request retrieval by status.");


        List<LeaveRequest> requests = leaveService.getLeaveRequestsByStatus(RequestStatus.PENDING);


        assertNotNull(requests);
    }


    // UT-LS-008: rejects null request status
    @Test
    @DisplayName("UT-LS-008 - Negative - getLeaveRequestsByStatus should reject null status")
    void getLeaveRequestsByStatusShouldRejectNullStatus() {
        LeaveService leaveService = createLeaveService();


        logger.info("UT-LS-008 Testing null request status validation.");


        assertThrows(
                ValidationException.class,
                () -> leaveService.getLeaveRequestsByStatus(null)
        );
    }


    // UT-LS-009: retrieves pending leave requests
    @Test
    @DisplayName("UT-LS-009 - Positive - getPendingLeaveRequests should return pending requests")
    void getPendingLeaveRequestsShouldReturnPendingRequests() {
        LeaveService leaveService = createLeaveService();


        logger.info("UT-LS-009 Testing pending leave request retrieval.");


        List<LeaveRequest> requests = leaveService.getPendingLeaveRequests();


        assertNotNull(requests);
    }


    // UT-LS-010: retrieves leave requests by date range
    @Test
    @DisplayName("UT-LS-010 - Positive - getLeaveRequestsByDateRange should return leave requests")
    void getLeaveRequestsByDateRangeShouldReturnLeaveRequests() {
        LeaveService leaveService = createLeaveService();


        logger.info("UT-LS-010 Testing leave request retrieval by date range.");


        List<LeaveRequest> requests = leaveService.getLeaveRequestsByDateRange(
                LocalDate.of(2024, 6, 1),
                LocalDate.of(2024, 6, 30)
        );


        assertNotNull(requests);
    }


    // UT-LS-011: rejects invalid date range
    @Test
    @DisplayName("UT-LS-011 - Boundary - getLeaveRequestsByDateRange should reject invalid date range")
    void getLeaveRequestsByDateRangeShouldRejectInvalidDateRange() {
        LeaveService leaveService = createLeaveService();


        logger.info("UT-LS-011 Testing invalid date range boundary.");


        assertThrows(
                ValidationException.class,
                () -> leaveService.getLeaveRequestsByDateRange(
                        LocalDate.of(2024, 6, 30),
                        LocalDate.of(2024, 6, 1)
                )
        );
    }


    // UT-LS-012: rejects invalid leave request id
    @Test
    @DisplayName("UT-LS-012 - Negative - getLeaveRequestById should reject invalid leave request ID")
    void getLeaveRequestByIdShouldRejectInvalidLeaveRequestId() {
        LeaveService leaveService = createLeaveService();


        logger.info("UT-LS-012 Testing invalid leave request ID validation.");


        assertThrows(
                ValidationException.class,
                () -> leaveService.getLeaveRequestById(0)
        );
    }


    // UT-LS-013: rejects missing leave request
    @Test
    @DisplayName("UT-LS-013 - Edge Case - getLeaveRequestById should reject missing leave request")
    void getLeaveRequestByIdShouldRejectMissingLeaveRequest() {
        LeaveService leaveService = createLeaveService();


        logger.info("UT-LS-013 Testing missing leave request lookup.");


        assertThrows(
                EntityNotFoundException.class,
                () -> leaveService.getLeaveRequestById(99999)
        );
    }


    // UT-LS-014: retrieves remaining leave credits
    @Test
    @DisplayName("UT-LS-014 - Positive - getRemainingLeaveCredits should return remaining credits")
    void getRemainingLeaveCreditsShouldReturnRemainingCredits() {
        LeaveService leaveService = createLeaveService();


        logger.info("UT-LS-014 Testing remaining leave credit retrieval.");


        double credits = leaveService.getRemainingLeaveCredits(10001, 1, 2024);


        assertTrue(credits >= 0);
    }


    // UT-LS-015: rejects invalid leave type id
    @Test
    @DisplayName("UT-LS-015 - Negative - getRemainingLeaveCredits should reject invalid leave type ID")
    void getRemainingLeaveCreditsShouldRejectInvalidLeaveTypeId() {
        LeaveService leaveService = createLeaveService();


        logger.info("UT-LS-015 Testing invalid leave type ID validation.");


        assertThrows(
                ValidationException.class,
                () -> leaveService.getRemainingLeaveCredits(10001, 0, 2024)
        );
    }


    // UT-LS-016: rejects invalid year
    @Test
    @DisplayName("UT-LS-016 - Boundary - getRemainingLeaveCredits should reject invalid year")
    void getRemainingLeaveCreditsShouldRejectInvalidYear() {
        LeaveService leaveService = createLeaveService();


        logger.info("UT-LS-016 Testing invalid leave credit year boundary.");


        assertThrows(
                ValidationException.class,
                () -> leaveService.getRemainingLeaveCredits(10001, 1, 1999)
        );
    }


    // UT-LS-017: rejects invalid leave hours
    @Test
    @DisplayName("UT-LS-017 - Negative - submitLeaveRequest should reject invalid leave hours")
    void submitLeaveRequestShouldRejectInvalidLeaveHours() {
        LeaveService leaveService = createLeaveService();


        logger.info("UT-LS-017 Testing invalid leave hours validation.");


        assertThrows(
                ValidationException.class,
                () -> leaveService.submitLeaveRequest(
                        10001,
                        1,
                        LocalDate.now().plusDays(1),
                        LocalDate.now().plusDays(1),
                        0,
                        "Vacation leave"
                )
        );
    }


    // UT-LS-018: rejects blank leave reason
    @Test
    @DisplayName("UT-LS-018 - Negative - submitLeaveRequest should reject blank reason")
    void submitLeaveRequestShouldRejectBlankReason() {
        LeaveService leaveService = createLeaveService();


        logger.info("UT-LS-018 Testing blank leave reason validation.");


        assertThrows(
                ValidationException.class,
                () -> leaveService.submitLeaveRequest(
                        10001,
                        1,
                        LocalDate.now().plusDays(1),
                        LocalDate.now().plusDays(1),
                        8,
                        ""
                )
        );
    }


    // UT-LS-019: rejects null actor account for approval
    @Test
    @DisplayName("UT-LS-019 - Negative - approveLeaveRequest should reject null actor")
    void approveLeaveRequestShouldRejectNullActor() {
        LeaveService leaveService = createLeaveService();


        logger.info("UT-LS-019 Testing null actor account for approval.");


        assertThrows(
                UnauthorizedException.class,
                () -> leaveService.approveLeaveRequest(null, 1)
        );
    }


    // UT-LS-020: rejects null actor account for rejection
    @Test
    @DisplayName("UT-LS-020 - Negative - rejectLeaveRequest should reject null actor")
    void rejectLeaveRequestShouldRejectNullActor() {
        LeaveService leaveService = createLeaveService();


        logger.info("UT-LS-020 Testing null actor account for rejection.");


        assertThrows(
                UnauthorizedException.class,
                () -> leaveService.rejectLeaveRequest(null, 1)
        );
    }


    // UT-LS-021: rejects invalid actor employee id for cancellation
    @Test
    @DisplayName("UT-LS-021 - Negative - cancelLeaveRequest should reject invalid actor employee ID")
    void cancelLeaveRequestShouldRejectInvalidActorEmployeeId() {
        LeaveService leaveService = createLeaveService();


        logger.info("UT-LS-021 Testing invalid actor employee ID for cancellation.");


        assertThrows(
                ValidationException.class,
                () -> leaveService.cancelLeaveRequest(0, 1)
        );
    }


    // creates leave service with real dependencies
    private LeaveService createLeaveService() {
        return new LeaveService(
                new LeaveRepository(),
                new LeaveCreditRepository(),
                createEmployeeService()
        );
    }


    // creates employee service with real dependencies
    private EmployeeService createEmployeeService() {
        return new EmployeeService(
                new EmployeeRepository(),
                createUserAccountService(),
                new DepartmentRepository(),
                new PositionRepository(),
                new SalaryHistoryRepository(),
                new EmployeeBenefitRepository(),
                new GovernmentDetailsRepository()
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

