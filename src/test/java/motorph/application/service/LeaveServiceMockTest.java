package motorph.application.service;


import motorph.application.exception.BusinessRuleException;
import motorph.application.exception.UnauthorizedException;
import motorph.domain.model.LeaveRequest;
import motorph.domain.model.UserAccount;
import motorph.domain.value.RequestStatus;
import motorph.domain.value.Role;
import motorph.infrastructure.repository.LeaveCreditRepository;
import motorph.infrastructure.repository.LeaveRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


/**
 * Tests leave approval business rules using mocked dependencies.
 */
@DisplayName("LeaveService Mock Tests")
class LeaveServiceMockTest {


    // approve with enough credits sets status approved and deducts credits
    @Test
    @DisplayName("Positive - approveLeaveRequest should approve and deduct credits")
    void approveLeaveRequestShouldApproveAndDeductCredits() {
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
        LeaveRepository leaveRepository = mock(LeaveRepository.class);
        LeaveCreditRepository leaveCreditRepository = mock(LeaveCreditRepository.class);

        when(leaveRepository.findById(1)).thenReturn(pendingLeave(10001));

        LeaveService service = serviceWith(leaveRepository, leaveCreditRepository);

        service.cancelLeaveRequest(10001, 1);

        verify(leaveRepository).updateStatus(eq(1), eq(RequestStatus.CANCELLED), any());
    }


    // builds a leave service with mocked repositories and employee service
    private LeaveService serviceWith(LeaveRepository leaveRepository,
                                     LeaveCreditRepository leaveCreditRepository) {
        return new LeaveService(leaveRepository, leaveCreditRepository, mock(EmployeeService.class));
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
