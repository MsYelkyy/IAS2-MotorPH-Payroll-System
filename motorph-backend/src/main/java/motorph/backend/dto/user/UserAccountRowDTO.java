package motorph.backend.dto.user;

import motorph.backend.domain.value.AccountStatus;
import motorph.backend.domain.value.Role;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Carries user account details for the IT account management table, joined
 * with the employee's display name server-side. Never carries password
 * hash/salt - those stay inside the backend.
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
