package motorph.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import motorph.domain.value.AccountStatus;
import motorph.domain.value.Role;

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

    // constructors
    public UserAccount(Integer userAccountId,
                       int employeeId,
                       String companyEmail,
                       String username,
                       String passwordHash,
                       String passwordSalt,
                       Role role,
                       boolean locked,
                       boolean active,
                       LocalDateTime lastLoginAt) {

        this(
                userAccountId,
                employeeId,
                companyEmail,
                username,
                passwordHash,
                passwordSalt,
                List.of(role),
                role,
                locked,
                active,
                lastLoginAt,
                false,
                null
        );
    }

    // constructors
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

    // constructors
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

    // getters
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

    public Role getRole() {
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

    // checks if the account has the given role
    public boolean hasRole(Role role) {
        return role != null && roles.contains(role);
    }

    // checks if the account has more than one role
    public boolean hasMultipleRoles() {
        return roles.size() > 1;
    }

    // checks if the active role matches the given role
    public boolean isUsingRole(Role role) {
        return activeRole == role;
    }

    // checks if the account has any allowed role
    public boolean hasAnyRole(Role... allowedRoles) {
        if (allowedRoles == null) {
            return false;
        }

        for (Role role : allowedRoles) {
            if (hasRole(role)) {
                return true;
            }
        }

        return false;
    }

    // returns computed account status
    public AccountStatus getAccountStatus() {
        if (locked) {
            return AccountStatus.LOCKED;
        }

        if (!active) {
            return AccountStatus.DISABLED;
        }

        return AccountStatus.ACTIVE;
    }

    // checks if the account is allowed to log in
    public boolean canLogin() {
        return active && !locked;
    }

    // checks if password credentials exist
    public boolean hasPassword() {
        return passwordHash != null && !passwordHash.isBlank()
                && passwordSalt != null && !passwordSalt.isBlank();
    }

    // checks if the account is using a temporary password
    public boolean hasTemporaryPassword() {
        return mustChangePassword && temporaryPasswordExpiresAt != null;
    }

    // checks if the temporary password is expired
    public boolean isTemporaryPasswordExpired() {
        return hasTemporaryPassword() && LocalDateTime.now().isAfter(temporaryPasswordExpiresAt);
    }

    // checks if the temporary password is still usable
    public boolean isTemporaryPasswordActive() {
        return hasTemporaryPassword() && !isTemporaryPasswordExpired();
    }

    // setters
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

    public void setRole(Role role) {
        setRoles(List.of(role));
        setActiveRole(role);
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

    public void switchRole(Role role) {
        setActiveRole(role);
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

    // updates password hash and salt together
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

    // marks the account as using a temporary password
    public void markTemporaryPassword(LocalDateTime expiresAt) {
        if (expiresAt == null) {
            throw new IllegalArgumentException("Temporary password expiration is required.");
        }

        if (!expiresAt.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Temporary password expiration must be in the future.");
        }

        this.mustChangePassword = true;
        this.temporaryPasswordExpiresAt = expiresAt;
    }

    // clears the temporary password state
    public void clearTemporaryPasswordState() {
        this.mustChangePassword = false;
        this.temporaryPasswordExpiresAt = null;
    }

    // updates last login timestamp to current time
    public void markLastLoginNow() {
        this.lastLoginAt = LocalDateTime.now();
    }

    // validates temporary password fields
    private void validateTemporaryPasswordState() {
        if (!mustChangePassword && temporaryPasswordExpiresAt != null) {
            throw new IllegalArgumentException("Temporary password expiration requires must change password to be enabled.");
        }
    }

    // validates required string
    private String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }

    // cleans optional string
    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    // returns the user account summary
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
                + ", mustChangePassword=" + mustChangePassword
                + ", temporaryPasswordExpiresAt=" + temporaryPasswordExpiresAt
                + '}';
    }
}