package motorph.domain.model;

import java.time.LocalDateTime;

import motorph.domain.value.AccountStatus;
import motorph.domain.value.Role;

/**
 * Represents a system login account and maps to the user_account table.
 */
public class UserAccount {

    private Integer userAccountId;
    private int employeeId;
    private String companyEmail;
    private String username;
    private String passwordHash;
    private String passwordSalt;
    private Role role;
    private boolean locked;
    private boolean active;
    private LocalDateTime lastLoginAt;

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

        setUserAccountId(userAccountId);
        setEmployeeId(employeeId);
        setCompanyEmail(companyEmail);
        setUsername(username);
        setRole(role);
        setLocked(locked);
        setActive(active);
        setLastLoginAt(lastLoginAt);
        updatePasswordCredentials(passwordHash, passwordSalt);
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

    public Role getRole() {
        return role;
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

    // Returns computed account status.
    public AccountStatus getAccountStatus() {
        if (locked) return AccountStatus.LOCKED;
        if (!active) return AccountStatus.DISABLED;
        return AccountStatus.ACTIVE;
    }

    // Checks if the account is allowed to log in.
    public boolean canLogin() {
        return active && !locked;
    }

    // Checks if password credentials exist.
    public boolean hasPassword() {
        return passwordHash != null && !passwordHash.isBlank()
                && passwordSalt != null && !passwordSalt.isBlank();
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

    public void setRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role is required.");
        }
        this.role = role;
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

    // Updates password hash and salt together.
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

    // Updates last login timestamp to current time.
    public void markLastLoginNow() {
        this.lastLoginAt = LocalDateTime.now();
    }

    // Validates required string.
    private String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    // Cleans optional string.
    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public String toString() {
        return "UserAccount{userAccountId=" + userAccountId
                + ", employeeId=" + employeeId
                + ", companyEmail='" + companyEmail + '\''
                + ", role=" + role
                + ", locked=" + locked
                + ", active=" + active
                + '}';
    }
}