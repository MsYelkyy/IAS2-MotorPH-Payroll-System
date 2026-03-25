package motorph.domain.model;

import java.sql.Timestamp;
import motorph.domain.value.AccountStatus;

/**
 * Domain model representing one system account.
 */
public class Account {

    private final int accountId;
    private final int employeeId;
    private final String username;
    private final String companyEmail;
    private final String passwordHash;
    private final String passwordSalt;
    private final int roleId;
    private final boolean isLocked;
    private final boolean isActive;
    private final Timestamp lastLoginAt;

    // constructor
    public Account(
            int accountId,
            int employeeId,
            String username,
            String companyEmail,
            String passwordHash,
            String passwordSalt,
            int roleId,
            boolean isLocked,
            boolean isActive,
            Timestamp lastLoginAt) {

        this.accountId = accountId;
        this.employeeId = employeeId;
        this.username = requireNonBlank(username, "Username is required.");
        this.companyEmail = requireNonBlank(companyEmail, "Company Email is required.");
        this.passwordHash = clean(passwordHash);
        this.passwordSalt = clean(passwordSalt);
        this.roleId = roleId;
        this.isLocked = isLocked;
        this.isActive = isActive;
        this.lastLoginAt = lastLoginAt;

        validatePasswordState(this.passwordHash, this.passwordSalt);
    }

    // GETTERS
    public int getAccountId() {
        return accountId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public String getUsername() {
        return username;
    }

    public String getCompanyEmail() {
        return companyEmail;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public int getRoleId() {
        return roleId;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public boolean isActive() {
        return isActive;
    }

    public Timestamp getLastLoginAt() {
        return lastLoginAt;
    }

    // returns computed account status
    public AccountStatus getAccountStatus() {
        if (isLocked) return AccountStatus.LOCKED;
        if (!isActive) return AccountStatus.DISABLED;
        return AccountStatus.ACTIVE;
    }

    // checks if account can login
    public boolean canLogin() {
        return isActive && !isLocked;
    }

    // checks if password is set
    public boolean hasPassword() {
        return !passwordHash.isBlank() && !passwordSalt.isBlank();
    }

    // validates password consistency
    private void validatePasswordState(String hash, String salt) {
        boolean hasHash = !hash.isBlank();
        boolean hasSalt = !salt.isBlank();

        if (hasHash != hasSalt) {
            throw new IllegalArgumentException("Password hash and salt must both be present or both empty.");
        }
    }

    // validates required string
    private String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    // trims string safely
    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public String toString() {
        return "Account [username=" + username +
                ", companyEmail=" + companyEmail +
                ", employeeId=" + employeeId +
                ", roleId=" + roleId +
                ", active=" + isActive +
                ", locked=" + isLocked + "]";
    }
}