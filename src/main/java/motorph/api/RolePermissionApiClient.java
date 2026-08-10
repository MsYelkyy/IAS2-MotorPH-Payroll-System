package motorph.api;

import motorph.api.dto.permission.PermissionDTO;
import motorph.api.dto.permission.RolePermissionsUpdateRequest;
import motorph.domain.value.PermissionKey;
import motorph.domain.value.Role;

import java.util.Collection;
import java.util.List;

/** Thin wrapper around the backend's permission catalogue and role grants. */
public final class RolePermissionApiClient {

    private final HttpApiClient httpApiClient;

    public RolePermissionApiClient(HttpApiClient httpApiClient) {
        this.httpApiClient = httpApiClient;
    }

    public List<PermissionDTO> getAllPermissions() {
        return httpApiClient.getList("/api/v1/permissions", PermissionDTO.class);
    }

    public List<PermissionDTO> getRolePermissions(Role role) {
        return httpApiClient.getList("/api/v1/roles/" + role.name() + "/permissions", PermissionDTO.class);
    }

    public void updateRolePermissions(Role role, Collection<PermissionKey> permissionKeys) {
        List<String> keys = permissionKeys.stream().map(PermissionKey::name).toList();
        httpApiClient.put("/api/v1/roles/" + role.name() + "/permissions",
                new RolePermissionsUpdateRequest(keys), Void.class);
    }
}
