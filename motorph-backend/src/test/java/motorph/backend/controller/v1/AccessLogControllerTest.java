package motorph.backend.controller.v1;

import motorph.backend.domain.UserAccessLog;
import motorph.backend.domain.value.PermissionKey;
import motorph.backend.exception.DataAccessException;
import motorph.backend.exception.UnauthorizedException;
import motorph.backend.repository.UserAccessLogRepository;
import motorph.backend.security.CurrentUserProvider;
import motorph.backend.security.RequestClientInfo;
import motorph.backend.service.PermissionService;
import motorph.backend.service.UserAccessLogService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Only a successful view of the full audit log is itself audited (Control 5.4). The dashboard
 * widgets that load automatically are not, and viewing never triggers another read. These tests use
 * the real {@link UserAccessLogService} over a mocked repository so they see exactly which reads
 * and writes reach the database.
 */
class AccessLogControllerTest {

    private static final int VIEWER_ACCOUNT_ID = 11;
    private static final String IP = "192.168.1.25";
    private static final String DEVICE = "MotorPH-Desktop/1.0 (Windows 11 10.0; amd64; Java 21)";

    private UserAccessLogRepository repository;
    private PermissionService permissionService;
    private AccessLogController controller;

    @BeforeEach
    void setUp() {
        repository = mock(UserAccessLogRepository.class);
        when(repository.findAll()).thenReturn(List.of(entry(1), entry(2)));

        RequestClientInfo requestClientInfo = mock(RequestClientInfo.class);
        when(requestClientInfo.getIpAddress()).thenReturn(IP);
        when(requestClientInfo.getDeviceInfo()).thenReturn(DEVICE);

        CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
        when(currentUserProvider.getCurrentUserAccountId()).thenReturn(VIEWER_ACCOUNT_ID);

        permissionService = mock(PermissionService.class);

        controller = new AccessLogController(
                new UserAccessLogService(repository, requestClientInfo), permissionService, currentUserProvider);
    }

    @Test
    @DisplayName("Viewing the full log returns it and records one ACCESS_LOG_VIEWED entry for the viewer")
    void fullViewIsAudited() {
        List<UserAccessLog> logs = controller.getAllLogs();

        assertThat(logs).hasSize(2);

        UserAccessLog audit = savedLog();
        assertThat(audit.getAction()).isEqualTo("ACCESS_LOG_VIEWED");
        assertThat(audit.getUserAccountId()).isEqualTo(VIEWER_ACCOUNT_ID);
        assertThat(audit.getDetails()).isEqualTo("Viewed the full audit log (2 entries).");
        assertThat(audit.getTargetType()).isNull();
        assertThat(audit.getTargetId()).isNull();
    }

    @Test
    @DisplayName("IP and device of the view come from RequestClientInfo")
    void viewRecordsRequestContext() {
        controller.getAllLogs();

        UserAccessLog audit = savedLog();
        assertThat(audit.getIpAddress()).isEqualTo(IP);
        assertThat(audit.getDeviceInfo()).isEqualTo(DEVICE);
        assertThat(audit.getLoggedAt()).isNull();
    }

    @Test
    @DisplayName("One view is exactly one read and one insert, so logging a view cannot loop")
    void viewCannotRecurse() {
        controller.getAllLogs();

        verify(repository).findAll();
        verify(repository).save(any(UserAccessLog.class));
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("The entry count in the details is the number of rows returned to the viewer")
    void emptyLogIsStillAudited() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(controller.getAllLogs()).isEmpty();

        assertThat(savedLog().getDetails()).isEqualTo("Viewed the full audit log (0 entries).");
    }

    @Test
    @DisplayName("The recent and counts widgets load automatically and are not audited")
    void dashboardWidgetsAreNotAudited() {
        controller.getRecentLogs(10);
        controller.getLoginActivityCounts();

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("A caller without VIEW_ACCESS_LOGS reads nothing and writes no ACCESS_LOG_VIEWED entry")
    void deniedViewIsNotAuditedHere() {
        doThrow(new UnauthorizedException("You do not have permission to perform this action."))
                .when(permissionService).requirePermission(PermissionKey.VIEW_ACCESS_LOGS);

        assertThatThrownBy(controller::getAllLogs).isInstanceOf(UnauthorizedException.class);

        // The denial itself is recorded as ACCESS_DENIED by GlobalExceptionHandler.
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("A view whose read fails is not audited")
    void failedReadIsNotAudited() {
        when(repository.findAll()).thenThrow(
                new DataAccessException("Failed to query user access logs.", new SQLException("database down")));

        assertThatThrownBy(controller::getAllLogs).isInstanceOf(DataAccessException.class);

        verify(repository, never()).save(any());
    }

    private UserAccessLog savedLog() {
        ArgumentCaptor<UserAccessLog> captor = ArgumentCaptor.forClass(UserAccessLog.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    private UserAccessLog entry(int logId) {
        return new UserAccessLog(logId, 3, "LOGIN_SUCCESS", "Login successful.", IP, DEVICE,
                LocalDateTime.of(2026, 10, 1, 9, 0), null, null);
    }
}
