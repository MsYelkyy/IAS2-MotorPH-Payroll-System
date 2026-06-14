package motorph.application.dto.user;

import motorph.domain.value.AccountStatus;
import motorph.domain.value.Role;

import java.time.LocalDateTime;
import java.util.List;

/** Carries user account details for IT account management tables. */
public class UserAccountRowDTO {

    private final int userAccountId;
    private final int employeeId;
    private final String fullName;
    private final String companyEmail;
    private final List<Role> roles;
    private final Role activeRole;
    private final AccountStatus accountStatus;
    private final boolean locked;
    private final boolean active;
    private final boolean multipleRoles;
    private final LocalDateTime lastLoginAt;

    // constructors
    public UserAccountRowDTO(
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
            LocalDateTime lastLoginAt) {

        this.userAccountId = userAccountId;
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.companyEmail = companyEmail;
        this.roles = roles;
        this.activeRole = activeRole;
        this.accountStatus = accountStatus;
        this.locked = locked;
        this.active = active;
        this.multipleRoles = multipleRoles;
        this.lastLoginAt = lastLoginAt;
    }

    // getters
    public int getUserAccountId() {
        return userAccountId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getCompanyEmail() {
        return companyEmail;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public Role getActiveRole() {
        return activeRole;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public boolean isLocked() {
        return locked;
    }

    public boolean isActive() {
        return active;
    }

    public boolean hasMultipleRoles() {
        return multipleRoles;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }
}