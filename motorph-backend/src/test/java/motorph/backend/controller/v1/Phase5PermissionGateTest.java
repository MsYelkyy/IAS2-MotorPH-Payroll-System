package motorph.backend.controller.v1;

import motorph.backend.domain.value.PermissionKey;
import motorph.backend.dto.permission.RolePermissionsUpdateRequest;
import motorph.backend.dto.user.UserRolesUpdateRequest;
import motorph.backend.exception.UnauthorizedException;
import motorph.backend.security.CurrentUserProvider;
import motorph.backend.service.DatabaseBackupService;
import motorph.backend.service.PasswordResetService;
import motorph.backend.service.PermissionService;
import motorph.backend.service.UserAccessLogService;
import motorph.backend.service.UserAccountService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Verifies that protected endpoints reject users without the required permission.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Phase5PermissionGateTest {

    @Mock private CurrentUserProvider currentUserProvider;

    private PermissionService denyAll() {
        PermissionService permissionService = mock(PermissionService.class);

        doThrow(new UnauthorizedException("You do not have permission to perform this action."))
                .when(permissionService).requirePermission(any(PermissionKey.class));
        doThrow(new UnauthorizedException("You do not have permission to perform this action."))
                .when(permissionService).requireAnyPermission(any(PermissionKey[].class));

        return permissionService;
    }

    private void assertAllDenied(Object service, Map<String, ThrowingCallable> calls) {
        calls.forEach((name, call) ->
                assertThatThrownBy(call)
                        .describedAs("%s must refuse a caller without the permission", name)
                        .isInstanceOf(UnauthorizedException.class));

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("User account endpoints refuse a caller without user-administration rights")
    void userAccountEndpointsAreGated() {
        UserAccountService userAccountService = mock(UserAccountService.class);
        UserAccountController controller =
                new UserAccountController(userAccountService, denyAll(), currentUserProvider);

        assertAllDenied(userAccountService, Map.of(
                "GET /user-accounts", controller::getUserAccounts,
                "GET /user-accounts/counts", controller::getAccountCounts,
                "GET /user-accounts/{id}/summary", () -> controller.getAccountSummary(1),
                "GET /user-accounts/by-employee/{id}/summary", () -> controller.getAccountSummaryByEmployee(1),
                "POST /user-accounts/{id}/lock", () -> controller.lockAccount(1),
                "POST /user-accounts/{id}/unlock", () -> controller.unlockAccount(1),
                "POST /user-accounts/{id}/enable", () -> controller.enableAccount(1),
                "POST /user-accounts/{id}/disable", () -> controller.disableAccount(1),
                "PUT /user-accounts/{id}/roles",
                () -> controller.updateRoles(1, new UserRolesUpdateRequest(List.of("ADMIN")))
        ));
    }

    @Test
    @DisplayName("Access log endpoints refuse a caller without VIEW_ACCESS_LOGS")
    void accessLogEndpointsAreGated() {
        UserAccessLogService userAccessLogService = mock(UserAccessLogService.class);
        AccessLogController controller =
                new AccessLogController(userAccessLogService, denyAll(), currentUserProvider);

        assertAllDenied(userAccessLogService, Map.of(
                "GET /access-logs", controller::getAllLogs,
                "GET /access-logs/recent", () -> controller.getRecentLogs(10),
                "GET /access-logs/counts", controller::getLoginActivityCounts
        ));
    }

    @Test
    @DisplayName("Role and permission endpoints refuse a caller without role-administration rights")
    void rolePermissionEndpointsAreGated() {
        PermissionService permissionService = denyAll();
        RolePermissionController controller = new RolePermissionController(permissionService);

        assertThatThrownBy(controller::getAllPermissions).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> controller.getRolePermissions("ADMIN")).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> controller.updateRolePermissions(
                "ADMIN", new RolePermissionsUpdateRequest(List.of("VIEW_USERS"))))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Password reset endpoints refuse a caller without MANAGE_PASSWORD_RESET")
    void passwordResetEndpointsAreGated() {
        PasswordResetService passwordResetService = mock(PasswordResetService.class);
        PasswordResetRequestController controller =
                new PasswordResetRequestController(passwordResetService, denyAll(), currentUserProvider);

        assertAllDenied(passwordResetService, Map.of(
                "GET /password-reset-requests", () -> controller.getRequests(null),
                "GET /password-reset-requests/counts", controller::getCounts,
                "POST /password-reset-requests/{id}/process", () -> controller.processRequest(1),
                "POST /password-reset-requests/{id}/cancel", () -> controller.cancelRequest(1)
        ));
    }

    @Test
    @DisplayName("Database backup endpoints refuse a caller without BACKUP_DATABASE")
    void databaseBackupEndpointsAreGated() {
        DatabaseBackupService databaseBackupService = mock(DatabaseBackupService.class);
        DatabaseBackupController controller =
                new DatabaseBackupController(databaseBackupService, denyAll(), currentUserProvider);

        assertAllDenied(databaseBackupService, Map.of(
                "GET /system/backups", controller::listBackups,
                "POST /system/backups", controller::createBackup,
                "GET /system/backups/{file}/download", () -> controller.downloadBackup("motorph_backup_2026-08-10_09-30-00.sql"),
                "DELETE /system/backups/{file}", () -> controller.deleteBackup("motorph_backup_2026-08-10_09-30-00.sql"),
                "POST /system/backups/{file}/restore", () -> controller.restoreBackup("motorph_backup_2026-08-10_09-30-00.sql")
        ));
    }
}
