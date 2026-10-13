package motorph.backend.controller.v1;

import jakarta.validation.Valid;
import motorph.backend.domain.value.PermissionKey;
import motorph.backend.domain.value.Role;
import motorph.backend.dto.user.AccountCountsDTO;
import motorph.backend.dto.user.AccountSummaryDTO;
import motorph.backend.dto.user.UserAccountRowDTO;
import motorph.backend.dto.user.UserRolesUpdateRequest;
import motorph.backend.exception.ValidationException;
import motorph.backend.security.CurrentUserProvider;
import motorph.backend.service.PermissionService;
import motorph.backend.service.UserAccountService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * IT user account management. Viewing accounts requires {@code VIEW_USERS} or {@code MANAGE_USERS};
 * any status or role change requires {@code MANAGE_USERS}. The acting user is taken from the access
 * token so audit entries cannot be forged by the client.
 * Refactored to include @Valid checking on role updates (UserRolesUpdateRequest).
 */
@RestController
@RequestMapping("/api/v1/user-accounts")
public class UserAccountController {

    private final UserAccountService userAccountService;
    private final PermissionService permissionService;
    private final CurrentUserProvider currentUserProvider;

    public UserAccountController(UserAccountService userAccountService,
                                 PermissionService permissionService,
                                 CurrentUserProvider currentUserProvider) {
        this.userAccountService = userAccountService;
        this.permissionService = permissionService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public List<UserAccountRowDTO> getUserAccounts() {
        permissionService.requireAnyPermission(PermissionKey.VIEW_USERS, PermissionKey.MANAGE_USERS);
        return userAccountService.getUserAccountRows();
    }

    @GetMapping("/counts")
    public AccountCountsDTO getAccountCounts() {
        permissionService.requireAnyPermission(PermissionKey.VIEW_USERS, PermissionKey.MANAGE_USERS);
        return userAccountService.getAccountCounts();
    }

    /**
     * Returns a display-only summary of an account. HR screens use this to show who approved a
     * request or an employee's company email, so employee-module permissions are accepted as well.
     */
    @GetMapping("/{userAccountId}/summary")
    public AccountSummaryDTO getAccountSummary(@PathVariable int userAccountId) {
        validatePositiveId(userAccountId, "User Account ID");
        requireAccountSummaryAccess();
        return userAccountService.getAccountSummary(userAccountId);
    }

    @GetMapping("/by-employee/{employeeId}/summary")
    public AccountSummaryDTO getAccountSummaryByEmployee(@PathVariable int employeeId) {
        validatePositiveId(employeeId, "Employee ID");
        requireAccountSummaryAccess();
        return userAccountService.getAccountSummaryByEmployeeId(employeeId);
    }

    @PostMapping("/{userAccountId}/lock")
    public void lockAccount(@PathVariable int userAccountId) {
        validatePositiveId(userAccountId, "User Account ID");
        permissionService.requirePermission(PermissionKey.MANAGE_USERS);
        userAccountService.lockAccount(actorUserAccountId(), userAccountId);
    }

    /**
     * Removes a user's authenticator enrolment so they must enrol again at their next login. Admin
     * only: the {@code RESET_USER_TOTP} permission is granted to the Admin role alone.
     */
    @PostMapping("/{userAccountId}/reset-totp")
    public void resetTotp(@PathVariable int userAccountId) {
        permissionService.requirePermission(PermissionKey.RESET_USER_TOTP);
        userAccountService.resetTotp(actorUserAccountId(), userAccountId);
    }

    @PostMapping("/{userAccountId}/unlock")
    public void unlockAccount(@PathVariable int userAccountId) {
        validatePositiveId(userAccountId, "User Account ID");
        permissionService.requirePermission(PermissionKey.MANAGE_USERS);
        userAccountService.unlockAccount(actorUserAccountId(), userAccountId);
    }

    @PostMapping("/{userAccountId}/enable")
    public void enableAccount(@PathVariable int userAccountId) {
        validatePositiveId(userAccountId, "User Account ID");
        permissionService.requirePermission(PermissionKey.MANAGE_USERS);
        userAccountService.enableAccount(actorUserAccountId(), userAccountId);
    }

    @PostMapping("/{userAccountId}/disable")
    public void disableAccount(@PathVariable int userAccountId) {
        validatePositiveId(userAccountId, "User Account ID");
        permissionService.requirePermission(PermissionKey.MANAGE_USERS);
        userAccountService.disableAccount(actorUserAccountId(), userAccountId);
    }

    @PutMapping("/{userAccountId}/roles")
    public void updateRoles(@PathVariable int userAccountId, @RequestBody @Valid UserRolesUpdateRequest request) {
        validatePositiveId(userAccountId, "User Account ID");
        permissionService.requirePermission(PermissionKey.MANAGE_USERS);

        List<Role> roles = request.roles() == null
                ? List.of()
                : request.roles().stream().map(Role::fromValue).toList();

        userAccountService.updateRoles(actorUserAccountId(), userAccountId, roles);
    }

    private void requireAccountSummaryAccess() {
        permissionService.requireAnyPermission(
                PermissionKey.VIEW_USERS,
                PermissionKey.MANAGE_USERS,
                PermissionKey.VIEW_EMPLOYEE,
                PermissionKey.MANAGE_EMPLOYEE);
    }

    private int actorUserAccountId() {
        return currentUserProvider.getCurrentUserAccountId();
    }

    private void validatePositiveId(int id, String name) {
        if (id <= 0) {
            throw new ValidationException(name + " must be a positive integer.");
        }
    }
}
