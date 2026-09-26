package motorph.backend.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import motorph.backend.domain.value.AccountStatus;
import motorph.backend.domain.value.Role;

/**
 * Represents a system login account with assigned roles.
 */
public class UserAccount {

    private Integer userAccountId;
    private int employeeId;
    private String companyEmail;
    private String username;
    private String passwordHash;
    private String passwordSalt;
    private List<Role> roles;
    private Role activeRole;
    private boolean locked;
    private boolean active;
    private LocalDateTime lastLoginAt;
    private boolean mustChangePassword;
    private LocalDateTime temporaryPasswordExpiresAt;

    public UserAccount(Integer userAccountId,
                       int employeeId,
                       String companyEmail,
                       String username,
                       String passwordHash,
                       String passwordSalt,
                       List<Role> roles,
                       boolean locked,
                       boolean active,
                       LocalDateTime lastLoginAt,
                       boolean mustChangePassword,
                       LocalDateTime temporaryPasswordExpiresAt) {

        this(
                userAccountId,
                employeeId,
                companyEmail,
                username,
                passwordHash,
                passwordSalt,
                roles,
                roles == null || roles.isEmpty() ? null : roles.get(0),
                locked,
                active,
                lastLoginAt,
                mustChangePassword,
                temporaryPasswordExpiresAt
        );
    }

    public UserAccount(Integer userAccountId,
                       int employeeId,
                       String companyEmail,
                       String username,
                       String passwordHash,
                       String passwordSalt,
                       List<Role> roles,
                       Role activeRole,
                       boolean locked,
                       boolean active,
                       LocalDateTime lastLoginAt,
                       boolean mustChangePassword,
                       LocalDateTime temporaryPasswordExpiresAt) {

        setUserAccountId(userAccountId);
        setEmployeeId(employeeId);
        setCompanyEmail(companyEmail);
        setUsername(username);
        setRoles(roles);
        setActiveRole(activeRole);
        setLocked(locked);
        setActive(active);
        setLastLoginAt(lastLoginAt);
        setMustChangePassword(mustChangePassword);
        setTemporaryPasswordExpiresAt(temporaryPasswordExpiresAt);
        updatePasswordCredentials(passwordHash, passwordSalt);
        validateTemporaryPasswordState();
    }

    public Integer getUserAccountId() {
        return userAccountId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public String getCompanyEmail() {
        return companyEmail;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public List<Role> getRoles() {
        return Collections.unmodifiableList(roles);
    }

    public Role getActiveRole() {
        return activeRole;
    }

    public boolean isLocked() {
        return locked;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public boolean mustChangePassword() {
        return mustChangePassword;
    }

    public LocalDateTime getTemporaryPasswordExpiresAt() {
        return temporaryPasswordExpiresAt;
    }

    public boolean hasRole(Role role) {
        return role != null && roles.contains(role);
    }

    public boolean hasMultipleRoles() {
        return roles.size() > 1;
    }

    /**
     * Derives the account status from the locked and active flags. A locked account is reported as
     * {@code LOCKED} even if it is also disabled.
     */
    public AccountStatus getAccountStatus() {
        if (locked) {
            return AccountStatus.LOCKED;
        }

        if (!active) {
            return AccountStatus.DISABLED;
        }

        return AccountStatus.ACTIVE;
    }

    public boolean canLogin() {
        return active && !locked;
    }

    public boolean hasPassword() {
        return passwordHash != null && !passwordHash.isBlank()
                && passwordSalt != null && !passwordSalt.isBlank();
    }

    public boolean hasTemporaryPassword() {
        return mustChangePassword && temporaryPasswordExpiresAt != null;
    }

    public boolean isTemporaryPasswordExpired() {
        return hasTemporaryPassword() && LocalDateTime.now().isAfter(temporaryPasswordExpiresAt);
    }

    public void setUserAccountId(Integer userAccountId) {
        if (userAccountId != null && userAccountId <= 0) {
            throw new IllegalArgumentException("User account ID must be positive.");
        }

        this.userAccountId = userAccountId;
    }

    public void setEmployeeId(int employeeId) {
        if (employeeId <= 0) {
            throw new IllegalArgumentException("Employee ID must be positive.");
        }

        this.employeeId = employeeId;
    }

    public void setCompanyEmail(String companyEmail) {
        String value = requireNonBlank(companyEmail, "Company email is required.");

        if (!value.contains("@")) {
            throw new IllegalArgumentException("Company email must be valid.");
        }

        this.companyEmail = value;
    }

    public void setUsername(String username) {
        this.username = requireNonBlank(username, "Username is required.");
    }

    public void setRoles(List<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("At least one role is required.");
        }

        List<Role> cleanRoles = new ArrayList<>();

        for (Role role : roles) {
            if (role == null) {
                throw new IllegalArgumentException("Role cannot be null.");
            }

            if (!cleanRoles.contains(role)) {
                cleanRoles.add(role);
            }
        }

        this.roles = cleanRoles;

        if (activeRole == null || !this.roles.contains(activeRole)) {
            this.activeRole = this.roles.get(0);
        }
    }

    public void setActiveRole(Role activeRole) {
        if (activeRole == null) {
            throw new IllegalArgumentException("Active role is required.");
        }

        if (roles == null || !roles.contains(activeRole)) {
            throw new IllegalArgumentException("Active role must be assigned to the account.");
        }

        this.activeRole = activeRole;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public void setTemporaryPasswordExpiresAt(LocalDateTime temporaryPasswordExpiresAt) {
        this.temporaryPasswordExpiresAt = temporaryPasswordExpiresAt;
    }

    public void updatePasswordCredentials(String passwordHash, String passwordSalt) {
        String hashValue = clean(passwordHash);
        String saltValue = clean(passwordSalt);

        boolean hasHash = !hashValue.isBlank();
        boolean hasSalt = !saltValue.isBlank();

        if (hasHash != hasSalt) {
            throw new IllegalArgumentException("Password hash and salt must both be present or both be empty.");
        }

        this.passwordHash = hashValue;
        this.passwordSalt = saltValue;
    }

    private void validateTemporaryPasswordState() {
        if (!mustChangePassword && temporaryPasswordExpiresAt != null) {
            throw new IllegalArgumentException("Temporary password expiration requires must change password to be enabled.");
        }
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public String toString() {
        return "UserAccount{"
                + "userAccountId=" + userAccountId
                + ", employeeId=" + employeeId
                + ", companyEmail='" + companyEmail + '\''
                + ", roles=" + roles
                + ", activeRole=" + activeRole
                + ", locked=" + locked
                + ", active=" + active
                + '}';
    }
}
