package motorph.backend.controller.v1;

import motorph.backend.domain.Permission;
import motorph.backend.domain.value.PermissionKey;
import motorph.backend.domain.value.Role;
import motorph.backend.dto.permission.RolePermissionsUpdateRequest;
import motorph.backend.service.PermissionService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Role and permission administration. Viewing requires {@code VIEW_ROLES} or {@code MANAGE_ROLES};
 * changing a role's permissions requires {@code MANAGE_ROLES}. Changes apply on the affected users'
 * next request because permissions are loaded from the database each time.
 */
@RestController
public class RolePermissionController {

    private final PermissionService permissionService;

    public RolePermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping("/api/v1/permissions")
    public List<Permission> getAllPermissions() {
        permissionService.requireAnyPermission(PermissionKey.VIEW_ROLES, PermissionKey.MANAGE_ROLES);
        return permissionService.getAllPermissions();
    }

    @GetMapping("/api/v1/roles/{role}/permissions")
    public Set<Permission> getRolePermissions(@PathVariable String role) {
        permissionService.requireAnyPermission(PermissionKey.VIEW_ROLES, PermissionKey.MANAGE_ROLES);
        return permissionService.getPermissionsByRole(Role.fromValue(role));
    }

    @PutMapping("/api/v1/roles/{role}/permissions")
    public void updateRolePermissions(@PathVariable String role,
                                      @RequestBody RolePermissionsUpdateRequest request) {
        permissionService.requirePermission(PermissionKey.MANAGE_ROLES);

        Set<PermissionKey> keys = request.permissionKeys() == null
                ? Set.of()
                : request.permissionKeys().stream()
                        .map(PermissionKey::fromValue)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        permissionService.updateRolePermissions(Role.fromValue(role), keys);
    }
}
