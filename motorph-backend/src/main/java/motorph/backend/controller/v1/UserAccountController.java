package motorph.backend.controller.v1;

import motorph.backend.domain.value.PermissionKey;
import motorph.backend.domain.value.Role;
import motorph.backend.dto.user.AccountCountsDTO;
import motorph.backend.dto.user.UserAccountRowDTO;
import motorph.backend.dto.user.UserRolesUpdateRequest;
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
 * IT user account management. Reads accept either VIEW_USERS or MANAGE_USERS;
 * every status/role change requires MANAGE_USERS. The acting IT user is always
 * resolved from the validated token, never from the request body, so the audit
 * trail can't be forged by a modified client.
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

    @PostMapping("/{userAccountId}/lock")
    public void lockAccount(@PathVariable int userAccountId) {
        permissionService.requirePermission(PermissionKey.MANAGE_USERS);
        userAccountService.lockAccount(actorUserAccountId(), userAccountId);
    }

    @PostMapping("/{userAccountId}/unlock")
    public void unlockAccount(@PathVariable int userAccountId) {
        permissionService.requirePermission(PermissionKey.MANAGE_USERS);
        userAccountService.unlockAccount(actorUserAccountId(), userAccountId);
    }

    @PostMapping("/{userAccountId}/enable")
    public void enableAccount(@PathVariable int userAccountId) {
        permissionService.requirePermission(PermissionKey.MANAGE_USERS);
        userAccountService.enableAccount(actorUserAccountId(), userAccountId);
    }

    @PostMapping("/{userAccountId}/disable")
    public void disableAccount(@PathVariable int userAccountId) {
        permissionService.requirePermission(PermissionKey.MANAGE_USERS);
        userAccountService.disableAccount(actorUserAccountId(), userAccountId);
    }

    @PutMapping("/{userAccountId}/roles")
    public void updateRoles(@PathVariable int userAccountId, @RequestBody UserRolesUpdateRequest request) {
        permissionService.requirePermission(PermissionKey.MANAGE_USERS);

        List<Role> roles = request.roles() == null
                ? List.of()
                : request.roles().stream().map(Role::fromValue).toList();

        userAccountService.updateRoles(userAccountId, roles);
    }

    private int actorUserAccountId() {
        return currentUserProvider.getCurrentUserAccountId();
    }
}
