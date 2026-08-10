package motorph.api.dto.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import motorph.domain.value.AccountStatus;
import motorph.domain.value.Role;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Wire copy of the backend's UserAccountRowDTO, keeping the getter names the
 * IT account-management table already binds to.
 */
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

    @JsonCreator
    public UserAccountRowDTO(@JsonProperty("userAccountId") int userAccountId,
                             @JsonProperty("employeeId") int employeeId,
                             @JsonProperty("fullName") String fullName,
                             @JsonProperty("companyEmail") String companyEmail,
                             @JsonProperty("roles") List<Role> roles,
                             @JsonProperty("activeRole") Role activeRole,
                             @JsonProperty("accountStatus") AccountStatus accountStatus,
                             @JsonProperty("locked") boolean locked,
                             @JsonProperty("active") boolean active,
                             @JsonProperty("multipleRoles") boolean multipleRoles,
                             @JsonProperty("lastLoginAt") LocalDateTime lastLoginAt) {
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
