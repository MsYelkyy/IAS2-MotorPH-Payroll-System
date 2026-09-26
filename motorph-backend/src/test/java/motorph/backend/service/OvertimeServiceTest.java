package motorph.backend.service;

import motorph.backend.domain.OvertimeRequest;
import motorph.backend.domain.value.RequestStatus;
import motorph.backend.exception.UnauthorizedException;
import motorph.backend.repository.OvertimeRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Holders of APPROVE_OVERTIME (HR and Admin) can decide any overtime request except their own.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OvertimeServiceTest {

    private static final int REQUEST_ID = 5;
    private static final int REQUESTER_EMPLOYEE_ID = 10028;
    private static final int HR_EMPLOYEE_ID = 10006;
    private static final int HR_ACCOUNT_ID = 6;

    @Mock private OvertimeRepository overtimeRepository;
    @Mock private AttendanceService attendanceService;
    @Mock private EmployeeService employeeService;
    @Mock private UserAccessLogService userAccessLogService;

    @InjectMocks private OvertimeService overtimeService;

    @Test
    @DisplayName("HR can approve another employee's overtime without being their supervisor")
    void approverCanApproveAnyRequest() {
        when(overtimeRepository.findById(REQUEST_ID)).thenReturn(pendingRequest());

        overtimeService.approveOvertimeRequest(HR_EMPLOYEE_ID, HR_ACCOUNT_ID, true, REQUEST_ID, null);

        verify(overtimeRepository).updateStatus(eq(REQUEST_ID), eq(RequestStatus.APPROVED), eq(HR_ACCOUNT_ID), any());
    }

    @Test
    @DisplayName("Nobody can approve their own overtime request")
    void cannotApproveOwnRequest() {
        when(overtimeRepository.findById(REQUEST_ID)).thenReturn(pendingRequest());

        assertThatThrownBy(() -> overtimeService.approveOvertimeRequest(
                REQUESTER_EMPLOYEE_ID, HR_ACCOUNT_ID, true, REQUEST_ID, null))
                .isInstanceOf(UnauthorizedException.class);

        verify(overtimeRepository, never()).updateStatus(anyInt(), any(), anyInt(), any());
    }

    private OvertimeRequest pendingRequest() {
        return new OvertimeRequest(REQUEST_ID, REQUESTER_EMPLOYEE_ID, 900, 1,
                LocalDate.now().minusDays(1), 2.0, "Month-end close", LocalDateTime.now());
    }
}
