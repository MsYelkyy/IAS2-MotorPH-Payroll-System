package motorph.backend.service;

import motorph.backend.dto.permission.MenuPermissionDTO;
import motorph.backend.exception.UnauthorizedException;
import motorph.backend.exception.ValidationException;
import motorph.backend.domain.Permission;
import motorph.backend.domain.UserAccount;
import motorph.backend.domain.value.PermissionKey;
import motorph.backend.domain.value.Role;
import motorph.backend.repository.PermissionRepository;
import motorph.backend.repository.UserAccountRepository;
import motorph.backend.security.CurrentUserProvider;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Checks permissions for the caller's active role. The role is loaded from the database on every
 * request and is never taken from the client.
 *
 * <p>Known limitation: accounts with more than one role are always checked against their first
 * role, because the client cannot yet tell the backend which role it is acting as.
 */
@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final UserAccountRepository userAccountRepository;
    private final CurrentUserProvider currentUserProvider;

    public PermissionService(PermissionRepository permissionRepository,
                             UserAccountRepository userAccountRepository,
                             CurrentUserProvider currentUserProvider) {
        this.permissionRepository = permissionRepository;
        this.userAccountRepository = userAccountRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public boolean hasPermission(String permissionKey) {
        validatePermissionKey(permissionKey);
        Role activeRole = getActiveRoleOrThrow();

        return permissionRepository.roleHasPermission(activeRole, permissionKey);
    }

    public boolean hasAnyPermission(String... permissionKeys) {
        if (permissionKeys == null || permissionKeys.length == 0) {
            return false;
        }

        for (String permissionKey : permissionKeys) {
            if (permissionKey != null && !permissionKey.isBlank() && hasPermission(permissionKey)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Throws {@link UnauthorizedException} unless the caller's active role has the permission.
     * Controllers call this before any protected operation.
     */
    public void requirePermission(PermissionKey permissionKey) {
        if (!hasPermission(permissionKey)) {
            throw new UnauthorizedException("You do not have permission to perform this action.");
        }
    }

    public void requireAnyPermission(PermissionKey... permissionKeys) {
        if (!hasAnyPermission(permissionKeys)) {
            throw new UnauthorizedException("You do not have permission to perform this action.");
        }
    }

    public Set<String> getActiveRolePermissionKeys() {
        Role activeRole = getActiveRoleOrThrow();
        return permissionRepository.findPermissionKeysByRole(activeRole);
    }

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

    public Set<Permission> getPermissionsByRole(Role role) {
        if (role == null) {
            throw new ValidationException("Role is required.");
        }
        return permissionRepository.findPermissionsByRole(role);
    }

    public List<Permission> getAllPermissions() {
        return permissionRepository.findAllPermissions();
    }

    /**
     * Replaces the full permission set of a role.
     */
    public void updateRolePermissions(Role role, Set<PermissionKey> permissionKeys) {
        if (role == null) {
            throw new ValidationException("Role is required.");
        }
        if (permissionKeys == null) {
            throw new ValidationException("Permission keys are required.");
        }
        permissionRepository.updateRolePermissions(role, permissionKeys);
    }

    public boolean hasPermission(PermissionKey permissionKey) {
        if (permissionKey == null) {
            throw new ValidationException("Permission key is required.");
        }

        return hasPermission(permissionKey.name());
    }

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

    /**
     * Returns the caller's active role. Used for per-request checks that cannot be expressed as a
     * permission key, such as whether the caller supervises a particular employee.
     */
    public Role getActiveRole() {
        return getActiveRoleOrThrow();
    }

    private Role getActiveRoleOrThrow() {
        int userAccountId = currentUserProvider.getCurrentUserAccountId();
        UserAccount account = userAccountRepository.findById(userAccountId);

        if (account == null || account.getActiveRole() == null) {
            throw new UnauthorizedException("Active role is required.");
        }

        return account.getActiveRole();
    }

    private void validatePermissionKey(String permissionKey) {
        if (permissionKey == null || permissionKey.trim().isEmpty()) {
            throw new ValidationException("Permission key is required.");
        }
    }
}
