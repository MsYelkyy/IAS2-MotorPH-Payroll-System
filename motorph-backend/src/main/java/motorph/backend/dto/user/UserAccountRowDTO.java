package motorph.backend.dto.user;

import motorph.backend.domain.value.AccountStatus;
import motorph.backend.domain.value.Role;

import java.time.LocalDateTime;
import java.util.List;

/**
 * User account row for the IT account management table, including the employee's display name.
 * Password hashes and salts are never included.
 */
public record UserAccountRowDTO(
        int userAccountId,
        int employeeId,
        String fullName,
        String companyEmail,
        List<Role> roles,
        Role activeRole,
        AccountStatus accountStatus,
        boolean locked,
        boolean active,
        boolean multipleRoles,
        LocalDateTime lastLoginAt
) {
}
