package motorph.application.service;


import motorph.application.exception.BusinessRuleException;
import motorph.application.exception.UnauthorizedException;
import motorph.domain.model.Employee;
import motorph.domain.model.LeaveRequest;
import motorph.domain.model.UserAccount;
import motorph.domain.value.EmploymentStatus;
import motorph.domain.value.Gender;
import motorph.domain.value.RequestStatus;
import motorph.domain.value.Role;
import motorph.infrastructure.repository.LeaveCreditRepository;
import motorph.infrastructure.repository.LeaveRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


/**
 * Tests leave approval business rules using mocked dependencies.
 */
@DisplayName("LeaveService Mock Tests")
class LeaveServiceMockTest {


    private static final Logger logger = Logger.getLogger(LeaveServiceMockTest.class.getName());


    // approve with enough credits sets status approved and deducts credits
    @Test
    @DisplayName("Positive - approveLeaveRequest should approve and deduct credits")
    void approveLeaveRequestShouldApproveAndDeductCredits() {
        logger.info("Testing leave approval deducts credits.");

        LeaveRepository leaveRepository = mock(LeaveRepository.class);
        LeaveCreditRepository leaveCreditRepository = mock(LeaveCreditRepository.class);

        when(leaveRepository.findById(1)).thenReturn(pendingLeave(10002));
        when(leaveCreditRepository.hasEnoughCredits(anyInt(), anyInt(), anyInt(), anyDouble()))
                .thenReturn(true);

        LeaveService service = serviceWith(leaveRepository, leaveCreditRepository);

        service.approveLeaveRequest(hrActor(), 1);

        verify(leaveRepository).updateStatus(eq(1), eq(RequestStatus.APPROVED), anyInt());
        verify(leaveCreditRepository).deductCredits(anyInt(), anyInt(), anyInt(), anyDouble());
    }


    // approve with insufficient credits is blocked
    @Test
    @DisplayName("Negative - approveLeaveRequest should block when credits are insufficient")
    void approveLeaveRequestShouldBlockWhenInsufficientCredits() {
        logger.info("Testing leave approval blocked on insufficient credits.");

        LeaveRepository leaveRepository = mock(LeaveRepository.class);
        LeaveCreditRepository leaveCreditRepository = mock(LeaveCreditRepository.class);

        when(leaveRepository.findById(1)).thenReturn(pendingLeave(10002));
        when(leaveCreditRepository.hasEnoughCredits(anyInt(), anyInt(), anyInt(), anyDouble()))
                .thenReturn(false);

        LeaveService service = serviceWith(leaveRepository, leaveCreditRepository);

        assertThrows(BusinessRuleException.class, () -> service.approveLeaveRequest(hrActor(), 1));
        verify(leaveCreditRepository, never()).deductCredits(anyInt(), anyInt(), anyInt(), anyDouble());
    }


    // a user cannot approve their own leave request
    @Test
    @DisplayName("Negative - approveLeaveRequest should block self-approval")
    void approveLeaveRequestShouldBlockSelfApproval() {
        logger.info("Testing leave self-approval is blocked.");

        LeaveRepository leaveRepository = mock(LeaveRepository.class);
        LeaveCreditRepository leaveCreditRepository = mock(LeaveCreditRepository.class);

        // actor employee id (10001) matches the request owner
        when(leaveRepository.findById(1)).thenReturn(pendingLeave(10001));

        LeaveService service = serviceWith(leaveRepository, leaveCreditRepository);

        assertThrows(UnauthorizedException.class, () -> service.approveLeaveRequest(hrActor(), 1));
    }


    // a non-HR/Admin role cannot approve
    @Test
    @DisplayName("Negative - approveLeaveRequest should deny non-HR/Admin role")
    void approveLeaveRequestShouldDenyNonHrAdmin() {
        logger.info("Testing leave approval denied for non-HR/Admin role.");

        LeaveRepository leaveRepository = mock(LeaveRepository.class);
        LeaveCreditRepository leaveCreditRepository = mock(LeaveCreditRepository.class);

        when(leaveRepository.findById(1)).thenReturn(pendingLeave(10002));

        LeaveService service = serviceWith(leaveRepository, leaveCreditRepository);

        assertThrows(UnauthorizedException.class, () -> service.approveLeaveRequest(employeeActor(), 1));
    }


    // a non-pending request cannot be approved
    @Test
    @DisplayName("Negative - approveLeaveRequest should reject a non-pending request")
    void approveLeaveRequestShouldRejectNonPending() {
        logger.info("Testing approval rejected for a non-pending request.");

        LeaveRepository leaveRepository = mock(LeaveRepository.class);
        LeaveCreditRepository leaveCreditRepository = mock(LeaveCreditRepository.class);

        when(leaveRepository.findById(1)).thenReturn(approvedLeave(10002));

        LeaveService service = serviceWith(leaveRepository, leaveCreditRepository);

        assertThrows(BusinessRuleException.class, () -> service.approveLeaveRequest(hrActor(), 1));
    }


    // an employee cannot cancel someone else's leave request
    @Test
    @DisplayName("Negative - cancelLeaveRequest should block cancelling another employee's request")
    void cancelLeaveRequestShouldBlockNonOwner() {
        logger.info("Testing cancelling another employee's leave is blocked.");

        LeaveRepository leaveRepository = mock(LeaveRepository.class);
        LeaveCreditRepository leaveCreditRepository = mock(LeaveCreditRepository.class);

        when(leaveRepository.findById(1)).thenReturn(pendingLeave(10002));

        LeaveService service = serviceWith(leaveRepository, leaveCreditRepository);

        // actor 10001 tries to cancel request owned by 10002
        assertThrows(UnauthorizedException.class, () -> service.cancelLeaveRequest(10001, 1));
    }


    // cancel by the owner sets status cancelled
    @Test
    @DisplayName("Positive - cancelLeaveRequest should cancel the owner's pending request")
    void cancelLeaveRequestShouldCancelOwnRequest() {
        logger.info("Testing the owner can cancel their own pending leave.");

        LeaveRepository leaveRepository = mock(LeaveRepository.class);
        LeaveCreditRepository leaveCreditRepository = mock(LeaveCreditRepository.class);

        when(leaveRepository.findById(1)).thenReturn(pendingLeave(10001));

        LeaveService service = serviceWith(leaveRepository, leaveCreditRepository);

        service.cancelLeaveRequest(10001, 1);

        verify(leaveRepository).updateStatus(eq(1), eq(RequestStatus.CANCELLED), any());
    }


    // reverse restores credits and updates the status of an approved leave
    @Test
    @DisplayName("Positive - reverseApprovedLeave should restore credits and update status")
    void reverseApprovedLeaveShouldRestoreCredits() {
        logger.info("Testing reversing approved leave restores credits.");

        LeaveRepository leaveRepository = mock(LeaveRepository.class);
        LeaveCreditRepository leaveCreditRepository = mock(LeaveCreditRepository.class);

        when(leaveRepository.findById(1)).thenReturn(approvedLeave(10002));

        LeaveService service = serviceWith(leaveRepository, leaveCreditRepository);

        service.reverseApprovedLeave(hrActor(), 1, RequestStatus.CANCELLED);

        verify(leaveCreditRepository).restoreCredits(anyInt(), anyInt(), anyInt(), anyDouble());
        verify(leaveRepository).updateStatus(eq(1), eq(RequestStatus.CANCELLED), anyInt());
    }


    // reverse only accepts rejected or cancelled as the new status
    @Test
    @DisplayName("Negative - reverseApprovedLeave should reject an invalid new status")
    void reverseApprovedLeaveShouldRejectInvalidStatus() {
        logger.info("Testing reverse rejects an invalid new status.");

        LeaveService service = serviceWith(mock(LeaveRepository.class), mock(LeaveCreditRepository.class));

        assertThrows(
                motorph.application.exception.ValidationException.class,
                () -> service.reverseApprovedLeave(hrActor(), 1, RequestStatus.APPROVED));
    }


    // a valid submission saves the leave request
    @Test
    @DisplayName("Positive - submitLeaveRequest should save a valid request")
    void submitLeaveRequestShouldSaveValidRequest() {
        logger.info("Testing a valid leave submission is saved.");

        LeaveRepository leaveRepository = mock(LeaveRepository.class);
        LeaveCreditRepository leaveCreditRepository = mock(LeaveCreditRepository.class);
        EmployeeService employeeService = mock(EmployeeService.class);

        when(employeeService.getEmployeeById(10001)).thenReturn(regularEmployee());
        when(leaveRepository.hasConflict(anyInt(), any(), any())).thenReturn(false);
        when(leaveCreditRepository.hasEnoughCredits(anyInt(), anyInt(), anyInt(), anyDouble()))
                .thenReturn(true);

        LeaveService service = new LeaveService(leaveRepository, leaveCreditRepository, employeeService);

        LocalDate start = LocalDate.now().plusDays(7);
        service.submitLeaveRequest(10001, 1, start, start.plusDays(1), 8.0, "Family event");

        verify(leaveRepository).save(any(LeaveRequest.class));
    }


    // computes leave hours as eight hours per day for a future range
    @Test
    @DisplayName("Positive - computeLeaveHours should return eight hours per day")
    void computeLeaveHoursShouldReturnEightHoursPerDay() {
        logger.info("Testing leave hours computation at eight hours per day.");

        LeaveService service = serviceWith(mock(LeaveRepository.class), mock(LeaveCreditRepository.class));

        LocalDate start = LocalDate.now().plusDays(10);
        double hours = service.computeLeaveHours(start, start.plusDays(2));

        // 3 days * 8 hours
        assertEquals(24.0, hours, 0.01);
    }


    // builds a leave service with mocked repositories and employee service
    private LeaveService serviceWith(LeaveRepository leaveRepository,
                                     LeaveCreditRepository leaveCreditRepository) {
        return new LeaveService(leaveRepository, leaveCreditRepository, mock(EmployeeService.class));
    }

    // builds a regular (eligible) employee
    private Employee regularEmployee() {
        return new Employee(
                10001, "Juan", "Dela Cruz", LocalDate.of(1990, 1, 1), Gender.MALE,
                null, null, null, null, EmploymentStatus.REGULAR, 1, 1,
                LocalDate.of(2020, 1, 1), null, true, null);
    }

    // builds a pending leave request owned by the given employee
    private LeaveRequest pendingLeave(int ownerEmployeeId) {
        return new LeaveRequest(
                1, ownerEmployeeId, 1,
                LocalDate.of(2025, 6, 10), LocalDate.of(2025, 6, 11),
                8.0, "Vacation", LocalDateTime.of(2025, 6, 1, 9, 0));
    }

    // builds an already-approved leave request
    private LeaveRequest approvedLeave(int ownerEmployeeId) {
        return new LeaveRequest(
                1, ownerEmployeeId, 1,
                LocalDate.of(2025, 6, 10), LocalDate.of(2025, 6, 11),
                8.0, "Vacation", RequestStatus.APPROVED,
                LocalDateTime.of(2025, 6, 1, 9, 0), 5, null, null);
    }

    // builds an HR actor account (employee id 10001)
    private UserAccount hrActor() {
        return actor(Role.HR);
    }

    // builds a plain employee actor account
    private UserAccount employeeActor() {
        return actor(Role.EMPLOYEE);
    }

    // builds an actor user account with the given active role
    private UserAccount actor(Role role) {
        return new UserAccount(
                5, 10001, "hr@motorph.com", "hruser",
                "hash", "salt", List.of(role), role,
                false, true, null, false, null);
    }
}
