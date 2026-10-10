package motorph.backend.service;

import motorph.backend.domain.UserAccessLog;
import motorph.backend.exception.ValidationException;
import motorph.backend.repository.UserAccessLogRepository;
import motorph.backend.security.RequestClientInfo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Checks that every audit log entry gets the request IP, device, and target (Control 5.3).
 */
class UserAccessLogServiceTest {

    private static final int ACTOR_ID = 3;
    private static final String IP = "192.168.1.25";
    private static final String DEVICE = "MotorPH-Desktop/1.0 (Windows 11 10.0; amd64; Java 21)";

    private UserAccessLogRepository repository;
    private RequestClientInfo requestClientInfo;
    private UserAccessLogService service;

    @BeforeEach
    void setUp() {
        repository = mock(UserAccessLogRepository.class);
        requestClientInfo = mock(RequestClientInfo.class);
        when(requestClientInfo.getIpAddress()).thenReturn(IP);
        when(requestClientInfo.getDeviceInfo()).thenReturn(DEVICE);

        service = new UserAccessLogService(repository, requestClientInfo);
    }

    @Test
    void loginSuccessRecordsIpAndDevice() {
        service.logLoginSuccess(ACTOR_ID, "Login successful.");

        assertSaved(UserAccessLogService.ACTION_LOGIN_SUCCESS, null, null);
    }

    @Test
    void loginFailedRecordsIpAndDeviceWithoutUser() {
        service.logLoginFailed("juan.cruz@motorph.com", "Wrong password.");

        UserAccessLog log = saved();
        assertThat(log.getUserAccountId()).isNull();
        assertThat(log.getIpAddress()).isEqualTo(IP);
        assertThat(log.getDeviceInfo()).isEqualTo(DEVICE);
    }

    @Test
    void automaticAccountLockTargetsTheAccountWithoutAttributingItAsActor() {
        service.logAutomaticAccountLock(ACTOR_ID);

        UserAccessLog log = saved();
        assertThat(log.getUserAccountId()).isNull();
        assertThat(log.getAction()).isEqualTo(UserAccessLogService.ACTION_ACCOUNT_LOCKED);
        assertThat(log.getTargetType()).isEqualTo("USER_ACCOUNT");
        assertThat(log.getTargetId()).isEqualTo(String.valueOf(ACTOR_ID));
        assertThat(log.getDetails())
                .isEqualTo("Automatic lock applied after repeated failed passwords.");
    }

    @Test
    void logoutRecordsIpAndDevice() {
        service.logLogout(ACTOR_ID);

        assertSaved(UserAccessLogService.ACTION_LOGOUT, null, null);
    }

    @Test
    void eventWithoutTargetRecordsIpAndDevice() {
        service.logEvent(ACTOR_ID, UserAccessLogService.ACTION_PASSWORD_CHANGED, "Changed own password.");

        assertSaved(UserAccessLogService.ACTION_PASSWORD_CHANGED, null, null);
    }

    @Test
    void eventWithTargetRecordsTarget() {
        service.logEvent(ACTOR_ID, UserAccessLogService.ACTION_EMPLOYEE_UPDATED, "Updated employee ID 5",
                UserAccessLogService.TARGET_EMPLOYEE, 5);

        assertSaved(UserAccessLogService.ACTION_EMPLOYEE_UPDATED, "EMPLOYEE", "5");
    }

    @Test
    void eventWithTargetTypeButNoIdIsAllowed() {
        service.logEvent(ACTOR_ID, "CONFIG_SSS_ADDED", "Added SSS rate",
                UserAccessLogService.TARGET_GOVERNMENT_RATE, null);

        assertSaved("CONFIG_SSS_ADDED", "GOVERNMENT_RATE", null);
    }

    @Test
    void passwordResetTargetsTheEmployee() {
        service.logPasswordReset(ACTOR_ID, 42);

        assertSaved(UserAccessLogService.ACTION_PASSWORD_RESET, "EMPLOYEE", "42");
    }

    @Test
    void accountStatusChangeTargetsTheEmployee() {
        service.logAccountStatusChanged(ACTOR_ID, 42, "ACTIVE", "LOCKED");

        assertSaved(UserAccessLogService.ACTION_ACCOUNT_STATUS_CHANGED, "EMPLOYEE", "42");
    }

    @Test
    void backupCreatedTargetsTheFile() {
        service.logDatabaseBackupCreated(ACTOR_ID, "motorph_backup_20261001.sql");
        assertSaved(UserAccessLogService.ACTION_DATABASE_BACKUP_CREATED, "BACKUP_FILE", "motorph_backup_20261001.sql");
    }

    @Test
    void restoreTargetsTheFile() {
        service.logDatabaseRestore(ACTOR_ID, "motorph_backup_20261001.sql");
        assertSaved(UserAccessLogService.ACTION_DATABASE_RESTORE, "BACKUP_FILE", "motorph_backup_20261001.sql");
    }

    @Test
    void outsideARequestIpAndDeviceAreNull() {
        when(requestClientInfo.getIpAddress()).thenReturn(null);
        when(requestClientInfo.getDeviceInfo()).thenReturn(null);

        service.logLogout(ACTOR_ID);

        UserAccessLog log = saved();
        assertThat(log.getIpAddress()).isNull();
        assertThat(log.getDeviceInfo()).isNull();
    }

    @Test
    void unknownTargetTypeIsRejected() {
        assertThatThrownBy(() -> service.logEvent(ACTOR_ID, "EMPLOYEE_UPDATED", "x", "EMPLOYEES", 5))
                .isInstanceOf(ValidationException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void targetIdWithoutTypeIsRejected() {
        assertThatThrownBy(() -> service.logEvent(ACTOR_ID, "EMPLOYEE_UPDATED", "x", null, 5))
                .isInstanceOf(ValidationException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void tooLongTargetIdIsRejected() {
        String longId = "r".repeat(65);

        assertThatThrownBy(() -> service.logEvent(ACTOR_ID, "REPORT_EXPORTED", "x",
                UserAccessLogService.TARGET_REPORT, longId))
                .isInstanceOf(ValidationException.class);
    }

    // Control 5.4

    @Test
    void supportTicketTargetRecordsTheTicketId() {
        service.logEvent(ACTOR_ID, "SUPPORT_TICKET_RESOLVED", "Resolved support ticket ID 7",
                UserAccessLogService.TARGET_SUPPORT_TICKET, 7);

        assertSaved("SUPPORT_TICKET_RESOLVED", "SUPPORT_TICKET", "7");
    }

    @Test
    void supportTicketTargetWithoutIdIsAllowed() {
        service.logEvent(ACTOR_ID, "SUPPORT_TICKET_SUBMITTED", "Submitted a support ticket",
                UserAccessLogService.TARGET_SUPPORT_TICKET, null);

        assertSaved("SUPPORT_TICKET_SUBMITTED", "SUPPORT_TICKET", null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACCESS_DENIED", "OTP_FAILED", "ACCESS_LOG_VIEWED"})
    void eventsWithoutATargetRecordIpAndDevice(String action) {
        service.logEvent(ACTOR_ID, action, "Some details.");

        assertSaved(action, null, null);
    }

    @Test
    void controlFiveFourActionNamesAreStable() {
        assertThat(UserAccessLogService.ACTION_SUPPORT_TICKET_SUBMITTED).isEqualTo("SUPPORT_TICKET_SUBMITTED");
        assertThat(UserAccessLogService.ACTION_SUPPORT_TICKET_RESOLVED).isEqualTo("SUPPORT_TICKET_RESOLVED");
        assertThat(UserAccessLogService.ACTION_SUPPORT_TICKET_CANCELLED).isEqualTo("SUPPORT_TICKET_CANCELLED");
        assertThat(UserAccessLogService.ACTION_PROFILE_PHOTO_UPDATED).isEqualTo("PROFILE_PHOTO_UPDATED");
        assertThat(UserAccessLogService.ACTION_ACCESS_DENIED).isEqualTo("ACCESS_DENIED");
        assertThat(UserAccessLogService.ACTION_OTP_FAILED).isEqualTo("OTP_FAILED");
        assertThat(UserAccessLogService.ACTION_ACCESS_LOG_VIEWED).isEqualTo("ACCESS_LOG_VIEWED");
        assertThat(UserAccessLogService.TARGET_SUPPORT_TICKET).isEqualTo("SUPPORT_TICKET");
    }

    private void assertSaved(String action, String targetType, String targetId) {
        UserAccessLog log = saved();

        assertThat(log.getAction()).isEqualTo(action);
        assertThat(log.getIpAddress()).isEqualTo(IP);
        assertThat(log.getDeviceInfo()).isEqualTo(DEVICE);
        assertThat(log.getTargetType()).isEqualTo(targetType);
        assertThat(log.getTargetId()).isEqualTo(targetId);
        assertThat(log.getLoggedAt()).isNull();
    }

    private UserAccessLog saved() {
        ArgumentCaptor<UserAccessLog> captor = ArgumentCaptor.forClass(UserAccessLog.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }
}
