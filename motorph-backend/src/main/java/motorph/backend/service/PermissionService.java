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
 * Handles permission checks for the caller's active role. Ported from the
 * desktop's PermissionService, with one deliberate change: the active role
 * is resolved fresh per request from the DB via CurrentUserProvider +
 * UserAccountRepository - never from a static singleton (see Phase 1 notes
 * on UserSession) and never from anything the client claims.
 *
 * Known gap: multi-role accounts always evaluate against their first
 * assigned role (UserAccount's own default) since the client has no way yet
 * to tell the backend "I'm currently acting as role X" the way the desktop's
 * old in-memory role switch did. Closing this (an X-Active-Role-style header,
 * validated against the account's actual assigned roles before being
 * honored) is follow-up work, not a Phase 2 blocker since it only affects
 * accounts with more than one role.
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
            if (permissionKey != null && !permissionKey.isBlank() && hasPermission(permissionKey)) {
                return true;
            }
        }

        return false;
    }

    // throws UnauthorizedException unless the active role has the permission key -
    // the standard first line of every gated controller method
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

    // returns all permissions assigned to a specific role (for admin role editor)
    public Set<Permission> getPermissionsByRole(Role role) {
        if (role == null) {
            throw new ValidationException("Role is required.");
        }
        return permissionRepository.findPermissionsByRole(role);
    }

    // returns all permissions defined in the system (for admin role editor)
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAllPermissions();
    }

    // replaces the full permission set for a role (admin-only operation)
    public void updateRolePermissions(Role role, Set<PermissionKey> permissionKeys) {
        if (role == null) {
            throw new ValidationException("Role is required.");
        }
        if (permissionKeys == null) {
            throw new ValidationException("Permission keys are required.");
        }
        permissionRepository.updateRolePermissions(role, permissionKeys);
    }

    // checks if the active role has the permission key
    public boolean hasPermission(PermissionKey permissionKey) {
        if (permissionKey == null) {
            throw new ValidationException("Permission key is required.");
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

    // returns the caller's active role, resolved fresh from the DB every call -
    // used where a dynamic per-request check (e.g. "is this actor an admin,
    // or specifically this employee's assigned supervisor") can't be expressed
    // as a static permission key
    public Role getActiveRole() {
        return getActiveRoleOrThrow();
    }

    // resolves the caller's active role fresh from the DB every call
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
