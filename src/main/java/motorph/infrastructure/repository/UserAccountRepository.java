package motorph.infrastructure.repository;

import motorph.domain.model.UserAccount;
import motorph.domain.value.Role;
import motorph.infrastructure.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for user account records.
 */
public class UserAccountRepository {

    private static final String BASE_SELECT = """
            SELECT ua.user_account_id,
                   ua.employee_id,
                   ua.company_email,
                   ua.username,
                   ua.password_hash,
                   ua.password_salt,
                   ua.is_locked,
                   ua.is_active,
                   ua.last_login_at,
                   MIN(r.role_name) AS role_name
            FROM user_account ua
            LEFT JOIN user_role ur ON ua.user_account_id = ur.user_account_id
            LEFT JOIN role r ON ur.role_id = r.role_id
            """;

    private static final String GROUP_BY_ACCOUNT = """
            GROUP BY ua.user_account_id,
                     ua.employee_id,
                     ua.company_email,
                     ua.username,
                     ua.password_hash,
                     ua.password_salt,
                     ua.is_locked,
                     ua.is_active,
                     ua.last_login_at
            """;

    // finds an active account by company email
    public UserAccount findActiveByCompanyEmail(String companyEmail) {
        String sql = BASE_SELECT + """
                WHERE ua.company_email = ?
                  AND ua.is_active = 1
                """ + GROUP_BY_ACCOUNT;

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

    // finds an account by company email
    public UserAccount findByCompanyEmail(String companyEmail) {
        String sql = BASE_SELECT + """
                WHERE ua.company_email = ?
                """ + GROUP_BY_ACCOUNT;

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

    // finds an account by user account id
    public UserAccount findById(int userAccountId) {
        validatePositive(userAccountId, "User account ID must be positive.");

        String sql = BASE_SELECT + """
                WHERE ua.user_account_id = ?
                """ + GROUP_BY_ACCOUNT;

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

    // finds an account by employee id
    public UserAccount findByEmployeeId(int employeeId) {
        validatePositive(employeeId, "Employee ID must be positive.");

        String sql = BASE_SELECT + """
                WHERE ua.employee_id = ?
                """ + GROUP_BY_ACCOUNT;

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

    // finds all active accounts
    public List<UserAccount> findAllActive() {
        String sql = BASE_SELECT + """
                WHERE ua.is_active = 1
                  AND ua.is_locked = 0
                """ + GROUP_BY_ACCOUNT + """
                ORDER BY ua.user_account_id
                """;

        return executeListQuery(sql);
    }

    // finds all disabled accounts
    public List<UserAccount> findAllDisabled() {
        String sql = BASE_SELECT + """
                WHERE ua.is_active = 0
                """ + GROUP_BY_ACCOUNT + """
                ORDER BY ua.user_account_id
                """;

        return executeListQuery(sql);
    }

    // finds all locked accounts
    public List<UserAccount> findAllLocked() {
        String sql = BASE_SELECT + """
                WHERE ua.is_locked = 1
                """ + GROUP_BY_ACCOUNT + """
                ORDER BY ua.user_account_id
                """;

        return executeListQuery(sql);
    }

    // finds all accounts
    public List<UserAccount> findAll() {
        String sql = BASE_SELECT + GROUP_BY_ACCOUNT + """
                ORDER BY ua.user_account_id
                """;

        return executeListQuery(sql);
    }

    // saves a new account and assigns its role
    public void save(UserAccount userAccount) {
        validateUserAccount(userAccount);

        String sql = """
                INSERT INTO user_account (
                    employee_id,
                    company_email,
                    username,
                    password_hash,
                    password_salt,
                    is_locked,
                    is_active,
                    last_login_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, userAccount.getEmployeeId());
                stmt.setString(2, normalizeRequired(userAccount.getCompanyEmail(), "Company email is required."));
                stmt.setString(3, normalizeOptional(userAccount.getUsername()));
                stmt.setString(4, normalizeRequired(userAccount.getPasswordHash(), "Password hash is required."));
                stmt.setString(5, normalizeRequired(userAccount.getPasswordSalt(), "Password salt is required."));
                stmt.setBoolean(6, userAccount.isLocked());
                stmt.setBoolean(7, userAccount.isActive());
                setNullableTimestamp(stmt, 8, userAccount.getLastLoginAt());

                stmt.executeUpdate();

                int userAccountId = getGeneratedId(stmt);
                assignRole(conn, userAccountId, userAccount.getRole());
            }

            conn.commit();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save user account.", e);
        }
    }

    // updates the account role
    public void updateRole(int userAccountId, Role role) {
        validatePositive(userAccountId, "User account ID must be positive.");
        validateRole(role);

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            deleteRoles(conn, userAccountId);
            assignRole(conn, userAccountId, role);

            conn.commit();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update account role.", e);
        }
    }

    // updates the lock status
    public void updateLockStatus(int userAccountId, boolean isLocked) {
        validatePositive(userAccountId, "User account ID must be positive.");

        String sql = "UPDATE user_account SET is_locked = ? WHERE user_account_id = ?";

        executeAccountUpdate(sql, stmt -> {
            stmt.setBoolean(1, isLocked);
            stmt.setInt(2, userAccountId);
        });
    }

    // updates the active status
    public void updateActiveStatus(int userAccountId, boolean isActive) {
        validatePositive(userAccountId, "User account ID must be positive.");

        String sql = "UPDATE user_account SET is_active = ? WHERE user_account_id = ?";

        executeAccountUpdate(sql, stmt -> {
            stmt.setBoolean(1, isActive);
            stmt.setInt(2, userAccountId);
        });
    }

    // updates the password
    public void updatePassword(int userAccountId, String hash, String salt) {
        validatePositive(userAccountId, "User account ID must be positive.");

        String sql = """
                UPDATE user_account
                SET password_hash = ?,
                    password_salt = ?
                WHERE user_account_id = ?
                """;

        executeAccountUpdate(sql, stmt -> {
            stmt.setString(1, normalizeRequired(hash, "Password hash is required."));
            stmt.setString(2, normalizeRequired(salt, "Password salt is required."));
            stmt.setInt(3, userAccountId);
        });
    }

    // updates the last login time
    public void updateLastLogin(int userAccountId) {
        validatePositive(userAccountId, "User account ID must be positive.");

        String sql = "UPDATE user_account SET last_login_at = NOW() WHERE user_account_id = ?";

        executeAccountUpdate(sql, stmt -> stmt.setInt(1, userAccountId));
    }

    // checks if a company email already exists
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

    // maps the result set to a user account
    private UserAccount mapRow(ResultSet rs) throws SQLException {
        Timestamp lastLoginValue = rs.getTimestamp("last_login_at");
        LocalDateTime lastLoginAt = lastLoginValue == null ? null : lastLoginValue.toLocalDateTime();

        String roleName = rs.getString("role_name");

        if (roleName == null || roleName.isBlank()) {
            throw new IllegalStateException("User account has no assigned role.");
        }

        return new UserAccount(
                rs.getInt("user_account_id"),
                rs.getInt("employee_id"),
                rs.getString("company_email"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("password_salt"),
                Role.fromValue(roleName),
                rs.getBoolean("is_locked"),
                rs.getBoolean("is_active"),
                lastLoginAt
        );
    }

    // executes a list query and maps all rows
    private List<UserAccount> executeListQuery(String sql) {
        List<UserAccount> accounts = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                accounts.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to query user accounts.", e);
        }

        return accounts;
    }

    // assigns one role to the user account
    private void assignRole(Connection conn, int userAccountId, Role role) throws SQLException {
        validateRole(role);

        String sql = """
                INSERT INTO user_role (user_account_id, role_id)
                VALUES (?, ?)
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userAccountId);
            stmt.setInt(2, resolveRoleId(conn, role));
            stmt.executeUpdate();
        }
    }

    // deletes existing roles for the user account
    private void deleteRoles(Connection conn, int userAccountId) throws SQLException {
        String sql = "DELETE FROM user_role WHERE user_account_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userAccountId);
            stmt.executeUpdate();
        }
    }

    // resolves the role id from the role name
    private int resolveRoleId(Connection conn, Role role) throws SQLException {
        validateRole(role);

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

    // gets the generated user account id
    private int getGeneratedId(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.getGeneratedKeys()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        throw new IllegalStateException("Failed to retrieve generated user account ID.");
    }

    // executes an account update
    private void executeAccountUpdate(String sql, StatementBinder binder) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            binder.bind(stmt);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new IllegalArgumentException("User account not found.");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update user account.", e);
        }
    }

    // validates user account input
    private void validateUserAccount(UserAccount userAccount) {
        if (userAccount == null) {
            throw new IllegalArgumentException("User account is required.");
        }

        validatePositive(userAccount.getEmployeeId(), "Employee ID must be positive.");
        normalizeRequired(userAccount.getCompanyEmail(), "Company email is required.");
        normalizeRequired(userAccount.getPasswordHash(), "Password hash is required.");
        normalizeRequired(userAccount.getPasswordSalt(), "Password salt is required.");
        validateRole(userAccount.getRole());
    }

    // validates role input
    private void validateRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role is required.");
        }
    }

    // validates positive number input
    private void validatePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    // sets a nullable timestamp value
    private void setNullableTimestamp(PreparedStatement stmt, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.TIMESTAMP);
            return;
        }

        stmt.setTimestamp(index, Timestamp.valueOf(value));
    }

    // validates and trims a required string value
    private String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }

    // trims an optional string value
    private String normalizeOptional(String value) {
        return value == null ? "" : value.trim();
    }

    // binds prepared statement parameters
    private interface StatementBinder {
        void bind(PreparedStatement stmt) throws SQLException;
    }
}