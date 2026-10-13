package motorph.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Replaces all roles assigned to an account. Roles are given by name. Whether the list is empty
 * and whether each name is a real role are checked by the service.
 */
public record UserRolesUpdateRequest(
        @NotNull @Size(max = 10) List<@NotBlank @Size(max = 45) String> roles
) {
}
