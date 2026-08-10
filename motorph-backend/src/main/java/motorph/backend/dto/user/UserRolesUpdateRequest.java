package motorph.backend.dto.user;

import java.util.List;

/** Replaces the full role set of one account. Role names, not IDs. */
public record UserRolesUpdateRequest(
        List<String> roles
) {
}
