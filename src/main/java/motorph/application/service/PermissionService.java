package motorph.application.service;

import motorph.application.session.UserSession;
import motorph.application.dto.permission.MenuPermissionDTO;
import motorph.domain.value.PermissionKey;
import motorph.domain.value.Role;
import motorph.infrastructure.repository.PermissionRepository;

import java.util.Set;

/**
 * Handles permission checks based on the active user role.
 */
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final UserSession userSession;

    // constructors
    public PermissionService(PermissionRepository permissionRepository) {
        if (permissionRepository == null) {
            throw new IllegalArgumentException("PermissionRepository is required.");
        }

        this.permissionRepository = permissionRepository;
        this.userSession = UserSession.getInstance();
    }

    // checks if the active role has the permission key
    public boolean hasPermission(String permissionKey) {
        validatePermissionKey(permissionKey);
        Role activeRole = getActiveRoleOrThrow();

        return permissionRepository.roleHasPermission(activeRole, permissionKey);
    }

    // checks if the active role has any permission key
    public boolean hasAnyPermission(String... permissionKeys) {
        if (permissionKeys == null || permissionKeys.length == 0) {
            return false;
        }

        for (String permissionKey : permissionKeys) {
            if (permissionKey != null && !permissionKey.isBlank()
                    && hasPermission(permissionKey)) {
                return true;
            }
        }

        return false;
    }

    // checks if the active role has all permission keys
    public boolean hasAllPermissions(String... permissionKeys) {
        if (permissionKeys == null || permissionKeys.length == 0) {
            return false;
        }

        for (String permissionKey : permissionKeys) {
            if (permissionKey == null || permissionKey.isBlank()
                    || !hasPermission(permissionKey)) {
                return false;
            }
        }

        return true;
    }

    // returns permission keys for the active role
    public Set<String> getActiveRolePermissionKeys() {
        Role activeRole = getActiveRoleOrThrow();
        return permissionRepository.findPermissionKeysByRole(activeRole);
    }

    // returns menu visibility permissions for the active role
    public MenuPermissionDTO getMenuPermissions() {
        return new MenuPermissionDTO(
                hasPermission(PermissionKey.VIEW_DASHBOARD),
                hasAnyPermission(PermissionKey.VIEW_EMPLOYEE, PermissionKey.MANAGE_EMPLOYEE),
                hasAnyPermission(PermissionKey.VIEW_ATTENDANCE, PermissionKey.MANAGE_ATTENDANCE),
                hasAnyPermission(PermissionKey.VIEW_LEAVE, PermissionKey.SUBMIT_LEAVE, PermissionKey.APPROVE_LEAVE),
                hasAnyPermission(PermissionKey.VIEW_OVERTIME, PermissionKey.SUBMIT_OVERTIME, PermissionKey.APPROVE_OVERTIME),
                hasAnyPermission(PermissionKey.VIEW_PAYROLL, PermissionKey.PROCESS_PAYROLL, PermissionKey.VIEW_PAYSLIP),
                hasAnyPermission(PermissionKey.VIEW_REPORTS, PermissionKey.GENERATE_REPORTS),
                hasAnyPermission(PermissionKey.VIEW_ROLES, PermissionKey.MANAGE_ROLES),
                hasAnyPermission(PermissionKey.VIEW_USERS, PermissionKey.MANAGE_USERS),
                hasAnyPermission(PermissionKey.VIEW_ACCESS_LOGS, PermissionKey.MANAGE_2FA, PermissionKey.MANAGE_PASSWORD_RESET),
                hasAnyPermission(PermissionKey.VIEW_DATABASE_TOOLS, PermissionKey.BACKUP_DATABASE),
                hasPermission(PermissionKey.CHANGE_OWN_PASSWORD)
        );
    }

    // checks if the active role can view a module
    public boolean canView(String permissionKey) {
        return hasPermission(permissionKey);
    }

    // checks if the active role can manage a module
    public boolean canManage(String permissionKey) {
        return hasPermission(permissionKey);
    }

    // gets active role from user session
    private Role getActiveRoleOrThrow() {
        if (!userSession.isLoggedIn()) {
            throw new IllegalStateException("User must be logged in.");
        }

        Role activeRole = userSession.getActiveRole();

        if (activeRole == null) {
            throw new IllegalStateException("Active role is required.");
        }

        return activeRole;
    }

    // validates permission key input
    private void validatePermissionKey(String permissionKey) {
        if (permissionKey == null || permissionKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Permission key is required.");
        }
    }

    // checks if the active role has the permission key
    public boolean hasPermission(PermissionKey permissionKey) {
        if (permissionKey == null) {
            throw new IllegalArgumentException("Permission key is required.");
        }

        return hasPermission(permissionKey.name());
    }

    // checks if the active role has any permission key
    public boolean hasAnyPermission(PermissionKey... permissionKeys) {
        if (permissionKeys == null || permissionKeys.length == 0) {
            return false;
        }

        for (PermissionKey permissionKey : permissionKeys) {
            if (permissionKey != null && hasPermission(permissionKey)) {
                return true;
            }
        }

        return false;
    }
}