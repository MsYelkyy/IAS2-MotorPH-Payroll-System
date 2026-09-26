package motorph.backend.dto.user;

import java.util.List;

/** Replaces all roles assigned to an account. Roles are given by name. */
public record UserRolesUpdateRequest(
        List<String> roles
) {
}
