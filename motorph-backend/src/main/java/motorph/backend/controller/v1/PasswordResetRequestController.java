package motorph.backend.controller.v1;

import motorph.backend.domain.value.PasswordResetStatus;
import motorph.backend.domain.value.PermissionKey;
import motorph.backend.dto.auth.CredentialsPreview;
import motorph.backend.dto.user.PasswordResetCountsDTO;
import motorph.backend.dto.user.PasswordResetRequestRowDTO;
import motorph.backend.security.AuthenticatedPrincipal;
import motorph.backend.security.CurrentUserProvider;
import motorph.backend.service.PasswordResetService;
import motorph.backend.service.PermissionService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * IT resolution of password reset requests, gated MANAGE_PASSWORD_RESET.
 * Who processed or cancelled a request is taken from the caller's token, not
 * from the request - the desktop version passed the acting employee ID in
 * from the client, which a modified client could have forged.
 */
@RestController
@RequestMapping("/api/v1/password-reset-requests")
public class PasswordResetRequestController {

    private final PasswordResetService passwordResetService;
    private final PermissionService permissionService;
    private final CurrentUserProvider currentUserProvider;

    public PasswordResetRequestController(PasswordResetService passwordResetService,
                                          PermissionService permissionService,
                                          CurrentUserProvider currentUserProvider) {
        this.passwordResetService = passwordResetService;
        this.permissionService = permissionService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public List<PasswordResetRequestRowDTO> getRequests(@RequestParam(required = false) String status) {
        permissionService.requirePermission(PermissionKey.MANAGE_PASSWORD_RESET);

        PasswordResetStatus filter = status == null || status.isBlank()
                ? null
                : PasswordResetStatus.fromValue(status);

        return passwordResetService.getRequestRows(filter);
    }

    @GetMapping("/counts")
    public PasswordResetCountsDTO getCounts() {
        permissionService.requirePermission(PermissionKey.MANAGE_PASSWORD_RESET);
        return passwordResetService.getCounts();
    }

    @PostMapping("/{resetRequestId}/process")
    public CredentialsPreview processRequest(@PathVariable int resetRequestId) {
        permissionService.requirePermission(PermissionKey.MANAGE_PASSWORD_RESET);

        AuthenticatedPrincipal principal = currentUserProvider.getCurrentPrincipal();

        return passwordResetService.processPasswordResetRequest(
                resetRequestId, principal.userAccountId(), principal.employeeId());
    }

    @PostMapping("/{resetRequestId}/cancel")
    public void cancelRequest(@PathVariable int resetRequestId) {
        permissionService.requirePermission(PermissionKey.MANAGE_PASSWORD_RESET);

        passwordResetService.cancelPasswordResetRequest(
                resetRequestId, currentUserProvider.getCurrentPrincipal().employeeId());
    }
}
