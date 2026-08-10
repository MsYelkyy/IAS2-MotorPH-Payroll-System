package motorph.api.dto.permission;

import java.util.List;

/** Replaces the full permission set granted to one role. */
public record RolePermissionsUpdateRequest(
        List<String> permissionKeys
) {
}
