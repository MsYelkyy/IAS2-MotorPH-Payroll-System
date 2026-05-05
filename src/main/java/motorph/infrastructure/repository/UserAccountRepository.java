package motorph.infrastructure.repository;

import motorph.domain.model.UserAccount;
import motorph.domain.value.Role;
import motorph.infrastructure.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for user account records.
 */
public class UserAccountRepository {

    // Finds an active account by company email.
    public UserAccount findActiveByCompanyEmail(String companyEmail) {
        String sql = """
                SELECT ua.user_account_id,
                       ua.employee_id,
                       ua.company_email,
                       ua.username,
                       ua.password_hash,
                       ua.password_salt,
                       ua.role_id,
                       ua.is_locked,
                       ua.is_active,
                       ua.last_login_at,
                       r.role_name
                FROM user_account ua
                JOIN role r ON ua.role_id = r.role_id
                WHERE ua.company_email = ? AND ua.is_active = 1
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, normalizeRequired(companyEmail, "Company email is required."));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find active user account by company email.", e);
        }

        return null;
    }

    // Finds an account by company email.
    public UserAccount findByCompanyEmail(String companyEmail) {
        String sql = """
                SELECT ua.user_account_id,
                       ua.employee_id,
                       ua.company_email,
                       ua.username,
                       ua.password_hash,
                       ua.password_salt,
                       ua.role_id,
                       ua.is_locked,
                       ua.is_active,
                       ua.last_login_at,
                       r.role_name
                FROM user_account ua
                JOIN role r ON ua.role_id = r.role_id
                WHERE ua.company_email = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, normalizeRequired(companyEmail, "Company email is required."));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user account by company email.", e);
        }

        return null;
    }

    // Finds an account by user account id.
    public UserAccount findById(int userAccountId) {
        String sql = """
            SELECT ua.user_account_id,
                   ua.employee_id,
                   ua.company_email,
                   ua.username,
                   ua.password_hash,
                   ua.password_salt,
                   ua.role_id,
                   ua.is_locked,
                   ua.is_active,
                   ua.last_login_at,
                   r.role_name
            FROM user_account ua
            JOIN role r ON ua.role_id = r.role_id
            WHERE ua.user_account_id = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userAccountId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user account by id.", e);
        }

        return null;
    }

    // Finds an account by employee id.
    public UserAccount findByEmployeeId(int employeeId) {
        String sql = """
                SELECT ua.user_account_id,
                       ua.employee_id,
                       ua.company_email,
                       ua.username,
                       ua.password_hash,
                       ua.password_salt,
                       ua.role_id,
                       ua.is_locked,
                       ua.is_active,
                       ua.last_login_at,
                       r.role_name
                FROM user_account ua
                JOIN role r ON ua.role_id = r.role_id
                WHERE ua.employee_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user account by employee id.", e);
        }

        return null;
    }

    // Finds all active accounts.
    public List<UserAccount> findAllActive() {
        List<UserAccount> accounts = new ArrayList<>();

        String sql = """
                SELECT ua.user_account_id,
                       ua.employee_id,
                       ua.company_email,
                       ua.username,
                       ua.password_hash,
                       ua.password_salt,
                       ua.role_id,
                       ua.is_locked,
                       ua.is_active,
                       ua.last_login_at,
                       r.role_name
                FROM user_account ua
                JOIN role r ON ua.role_id = r.role_id
                WHERE ua.is_active = 1 AND ua.is_locked = 0
                ORDER BY ua.user_account_id
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                accounts.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find active user accounts.", e);
        }

        return accounts;
    }

    // Finds all disabled accounts.
    public List<UserAccount> findAllDisabled() {
        List<UserAccount> accounts = new ArrayList<>();

        String sql = """
                SELECT ua.user_account_id,
                       ua.employee_id,
                       ua.company_email,
                       ua.username,
                       ua.password_hash,
                       ua.password_salt,
                       ua.role_id,
                       ua.is_locked,
                       ua.is_active,
                       ua.last_login_at,
                       r.role_name
                FROM user_account ua
                JOIN role r ON ua.role_id = r.role_id
                WHERE ua.is_active = 0
                ORDER BY ua.user_account_id
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                accounts.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find disabled user accounts.", e);
        }

        return accounts;
    }

    // Finds all locked accounts.
    public List<UserAccount> findAllLocked() {
        List<UserAccount> accounts = new ArrayList<>();

        String sql = """
                SELECT ua.user_account_id,
                       ua.employee_id,
                       ua.company_email,
                       ua.username,
                       ua.password_hash,
                       ua.password_salt,
                       ua.role_id,
                       ua.is_locked,
                       ua.is_active,
                       ua.last_login_at,
                       r.role_name
                FROM user_account ua
                JOIN role r ON ua.role_id = r.role_id
                WHERE ua.is_locked = 1
                ORDER BY ua.user_account_id
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                accounts.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find locked user accounts.", e);
        }

        return accounts;
    }

    // Finds all accounts.
    public List<UserAccount> findAll() {
        List<UserAccount> accounts = new ArrayList<>();

        String sql = """
                SELECT ua.user_account_id,
                       ua.employee_id,
                       ua.company_email,
                       ua.username,
                       ua.password_hash,
                       ua.password_salt,
                       ua.role_id,
                       ua.is_locked,
                       ua.is_active,
                       ua.last_login_at,
                       r.role_name
                FROM user_account ua
                JOIN role r ON ua.role_id = r.role_id
                ORDER BY ua.user_account_id
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                accounts.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user accounts.", e);
        }

        return accounts;
    }

    // Saves a new account.
    public void save(UserAccount userAccount) {
        String sql = """
                INSERT INTO user_account (
                    employee_id,
                    company_email,
                    username,
                    password_hash,
                    password_salt,
                    role_id,
                    is_locked,
                    is_active,
                    last_login_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userAccount.getEmployeeId());
            stmt.setString(2, normalizeRequired(userAccount.getCompanyEmail(), "Company email is required."));
            stmt.setString(3, normalizeOptional(userAccount.getUsername()));
            stmt.setString(4, normalizeRequired(userAccount.getPasswordHash(), "Password hash is required."));
            stmt.setString(5, normalizeRequired(userAccount.getPasswordSalt(), "Password salt is required."));
            stmt.setInt(6, resolveRoleId(conn, userAccount.getRole()));
            stmt.setBoolean(7, userAccount.isLocked());
            stmt.setBoolean(8, userAccount.isActive());
            setNullableTimestamp(stmt, 9, userAccount.getLastLoginAt());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save user account.", e);
        }
    }

    // Updates the account role.
    public void updateRole(int userAccountId, Role role) {
        String sql = "UPDATE user_account SET role_id = ? WHERE user_account_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, resolveRoleId(conn, role));
            stmt.setInt(2, userAccountId);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new IllegalArgumentException("User account not found.");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update account role.", e);
        }
    }

    // Updates the lock status.
    public void updateLockStatus(int userAccountId, boolean isLocked) {
        String sql = "UPDATE user_account SET is_locked = ? WHERE user_account_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBoolean(1, isLocked);
            stmt.setInt(2, userAccountId);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new IllegalArgumentException("User account not found.");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update account lock status.", e);
        }
    }

    // Updates the active status.
    public void updateActiveStatus(int userAccountId, boolean isActive) {
        String sql = "UPDATE user_account SET is_active = ? WHERE user_account_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBoolean(1, isActive);
            stmt.setInt(2, userAccountId);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new IllegalArgumentException("User account not found.");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update account active status.", e);
        }
    }

    // Updates the password.
    public void updatePassword(int userAccountId, String hash, String salt) {
        String sql = """
                UPDATE user_account
                SET password_hash = ?, password_salt = ?
                WHERE user_account_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, normalizeRequired(hash, "Password hash is required."));
            stmt.setString(2, normalizeRequired(salt, "Password salt is required."));
            stmt.setInt(3, userAccountId);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new IllegalArgumentException("User account not found.");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update account password.", e);
        }
    }

    // Updates the last login time.
    public void updateLastLogin(int userAccountId) {
        String sql = "UPDATE user_account SET last_login_at = NOW() WHERE user_account_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userAccountId);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new IllegalArgumentException("User account not found.");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update last login time.", e);
        }
    }

    // Checks if a company email already exists.
    public boolean existsByCompanyEmail(String companyEmail) {
        String sql = "SELECT COUNT(*) FROM user_account WHERE company_email = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, normalizeRequired(companyEmail, "Company email is required."));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to check company email.", e);
        }

        return false;
    }

    // Maps the result set to a user account.
    private UserAccount mapRow(ResultSet rs) throws SQLException {
        Timestamp lastLoginValue = rs.getTimestamp("last_login_at");
        LocalDateTime lastLoginAt = lastLoginValue == null ? null : lastLoginValue.toLocalDateTime();

        return new UserAccount(
                rs.getInt("user_account_id"),
                rs.getInt("employee_id"),
                rs.getString("company_email"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("password_salt"),
                Role.fromValue(rs.getString("role_name")),
                rs.getBoolean("is_locked"),
                rs.getBoolean("is_active"),
                lastLoginAt
        );
    }

    // Resolves the role id from the role name.
    private int resolveRoleId(Connection conn, Role role) throws SQLException {
        if (role == null) {
            throw new IllegalArgumentException("Role is required.");
        }

        String sql = "SELECT role_id FROM role WHERE role_name = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, role.name());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("role_id");
                }
            }
        }

        throw new IllegalArgumentException("Role not found: " + role);
    }

    // Sets a nullable timestamp value.
    private void setNullableTimestamp(PreparedStatement stmt, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.TIMESTAMP);
            return;
        }

        stmt.setTimestamp(index, Timestamp.valueOf(value));
    }

    // Validates and trims a required string value.
    private String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    // Trims an optional string value.
    private String normalizeOptional(String value) {
        return value == null ? "" : value.trim();
    }
}