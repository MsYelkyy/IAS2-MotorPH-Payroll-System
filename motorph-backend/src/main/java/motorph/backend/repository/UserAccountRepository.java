package motorph.backend.repository;

import motorph.backend.exception.DataAccessException;

import motorph.backend.domain.UserAccount;
import motorph.backend.domain.value.Role;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
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
@Repository
public class UserAccountRepository implements motorph.backend.repository.Repository<UserAccount, Integer> {

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
                   ua.must_change_password,
                   ua.temporary_password_expires_at
            FROM user_account ua
            """;

    private final DataSource dataSource;

    public UserAccountRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public UserAccount findByCompanyEmail(String companyEmail) {
        String sql = BASE_SELECT + """
                WHERE ua.company_email = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, normalizeRequired(companyEmail, "Company email is required."));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(conn, rs);
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find user account by company email.", e);
        }

        return null;
    }

    @Override
    public UserAccount findById(Integer userAccountId) {
        validatePositive(userAccountId, "User account ID must be positive.");

        String sql = BASE_SELECT + """
                WHERE ua.user_account_id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userAccountId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(conn, rs);
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find user account by id.", e);
        }

        return null;
    }

    @Override
    public List<UserAccount> findAll() {
        String sql = BASE_SELECT + "ORDER BY ua.user_account_id";
        return executeListQuery(sql);
    }

    public UserAccount findByEmployeeId(int employeeId) {
        validatePositive(employeeId, "Employee ID must be positive.");

        String sql = BASE_SELECT + """
                WHERE ua.employee_id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(conn, rs);
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find user account by employee id.", e);
        }

        return null;
    }

    /**
     * Counts accounts that are active and not locked.
     */
    public int countActive() {
        return executeCountQuery("SELECT COUNT(*) FROM user_account WHERE is_active = 1 AND is_locked = 0");
    }

    public int countLocked() {
        return executeCountQuery("SELECT COUNT(*) FROM user_account WHERE is_locked = 1");
    }

    public int countDisabled() {
        return executeCountQuery("SELECT COUNT(*) FROM user_account WHERE is_active = 0");
    }

    /**
     * Saves a new account and assigns its roles in a single transaction.
     */
    // 4.2 fix: joins the caller's transaction (e.g. employee creation) instead of committing on its own
    @Override
    @Transactional
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
                    last_login_at,
                    must_change_password,
                    temporary_password_expires_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, userAccount.getEmployeeId());
            stmt.setString(2, normalizeRequired(userAccount.getCompanyEmail(), "Company email is required."));
            stmt.setString(3, normalizeOptional(userAccount.getUsername()));
            stmt.setString(4, normalizeRequired(userAccount.getPasswordHash(), "Password hash is required."));
            stmt.setString(5, normalizeRequired(userAccount.getPasswordSalt(), "Password salt is required."));
            stmt.setBoolean(6, userAccount.isLocked());
            stmt.setBoolean(7, userAccount.isActive());
            setNullableTimestamp(stmt, 8, userAccount.getLastLoginAt());
            stmt.setBoolean(9, userAccount.mustChangePassword());
            setNullableTimestamp(stmt, 10, userAccount.getTemporaryPasswordExpiresAt());

            stmt.executeUpdate();

            int userAccountId = getGeneratedId(stmt);
            assignRoles(conn, userAccountId, userAccount.getRoles());

        } catch (SQLException e) {
            throw new DataAccessException("Failed to save user account.", e);
        }
    }

    public void updateLockStatus(int userAccountId, boolean isLocked) {
        validatePositive(userAccountId, "User account ID must be positive.");

        String sql = "UPDATE user_account SET is_locked = ? WHERE user_account_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBoolean(1, isLocked);
            stmt.setInt(2, userAccountId);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new IllegalArgumentException("User account not found.");
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to update lock status.", e);
        }
    }

    public void updateActiveStatus(int userAccountId, boolean isActive) {
        validatePositive(userAccountId, "User account ID must be positive.");

        String sql = "UPDATE user_account SET is_active = ? WHERE user_account_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBoolean(1, isActive);
            stmt.setInt(2, userAccountId);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new IllegalArgumentException("User account not found.");
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to update active status.", e);
        }
    }

    public boolean existsByCompanyEmail(String companyEmail) {
        String sql = "SELECT COUNT(*) FROM user_account WHERE company_email = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, normalizeRequired(companyEmail, "Company email is required."));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to check company email.", e);
        }

        return false;
    }

    /**
     * Sets a permanent password and clears any temporary-password state.
     */
    public void updatePassword(int userAccountId, String hash, String salt) {
        validatePositive(userAccountId, "User account ID must be positive.");

        String sql = """
                UPDATE user_account
                SET password_hash = ?,
                    password_salt = ?,
                    must_change_password = 0,
                    temporary_password_expires_at = NULL
                WHERE user_account_id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, normalizeRequired(hash, "Password hash is required."));
            stmt.setString(2, normalizeRequired(salt, "Password salt is required."));
            stmt.setInt(3, userAccountId);

            if (stmt.executeUpdate() == 0) {
                throw new IllegalArgumentException("User account not found.");
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to update password.", e);
        }
    }

    /**
     * Sets an IT-issued temporary password that the user must change at next login.
     */
    public void updateTemporaryPassword(int userAccountId, String hash, String salt, LocalDateTime expiresAt) {
        validatePositive(userAccountId, "User account ID must be positive.");

        if (expiresAt == null) {
            throw new IllegalArgumentException("Temporary password expiration is required.");
        }

        if (!expiresAt.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Temporary password expiration must be in the future.");
        }

        String sql = """
                UPDATE user_account
                SET password_hash = ?,
                    password_salt = ?,
                    must_change_password = 1,
                    temporary_password_expires_at = ?
                WHERE user_account_id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, normalizeRequired(hash, "Password hash is required."));
            stmt.setString(2, normalizeRequired(salt, "Password salt is required."));
            stmt.setTimestamp(3, Timestamp.valueOf(expiresAt));
            stmt.setInt(4, userAccountId);

            if (stmt.executeUpdate() == 0) {
                throw new IllegalArgumentException("User account not found.");
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to update temporary password.", e);
        }
    }

    /**
     * Replaces all roles assigned to the account.
     */
    public void updateRoles(int userAccountId, List<Role> roles) {
        validatePositive(userAccountId, "User account ID must be positive.");
        validateRoles(roles);

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            deleteRoles(conn, userAccountId);
            assignRoles(conn, userAccountId, roles);

            conn.commit();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to update account roles.", e);
        }
    }

    public void updateLastLogin(int userAccountId) {
        validatePositive(userAccountId, "User account ID must be positive.");

        String sql = "UPDATE user_account SET last_login_at = NOW() WHERE user_account_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userAccountId);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new IllegalArgumentException("User account not found.");
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to update last login.", e);
        }
    }

    private UserAccount mapRow(Connection conn, ResultSet rs) throws SQLException {
        int userAccountId = rs.getInt("user_account_id");

        Timestamp lastLoginValue = rs.getTimestamp("last_login_at");
        LocalDateTime lastLoginAt = lastLoginValue == null ? null : lastLoginValue.toLocalDateTime();

        Timestamp temporaryPasswordExpiresValue = rs.getTimestamp("temporary_password_expires_at");
        LocalDateTime temporaryPasswordExpiresAt = temporaryPasswordExpiresValue == null
                ? null
                : temporaryPasswordExpiresValue.toLocalDateTime();

        List<Role> roles = findRolesByUserAccountId(conn, userAccountId);

        if (roles.isEmpty()) {
            throw new IllegalStateException("User account has no assigned role.");
        }

        return new UserAccount(
                userAccountId,
                rs.getInt("employee_id"),
                rs.getString("company_email"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("password_salt"),
                roles,
                rs.getBoolean("is_locked"),
                rs.getBoolean("is_active"),
                lastLoginAt,
                rs.getBoolean("must_change_password"),
                temporaryPasswordExpiresAt
        );
    }

    private List<Role> findRolesByUserAccountId(Connection conn, int userAccountId) throws SQLException {
        List<Role> roles = new ArrayList<>();

        String sql = """
                SELECT r.role_name
                FROM user_role ur
                JOIN role r ON ur.role_id = r.role_id
                WHERE ur.user_account_id = ?
                ORDER BY r.role_name
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userAccountId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    roles.add(Role.fromValue(rs.getString("role_name")));
                }
            }
        }

        return roles;
    }

    private List<UserAccount> executeListQuery(String sql) {
        List<UserAccount> accounts = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                accounts.add(mapRow(conn, rs));
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to query user accounts.", e);
        }

        return accounts;
    }

    private int executeCountQuery(String sql) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            return rs.next() ? rs.getInt(1) : 0;

        } catch (SQLException e) {
            throw new DataAccessException("Failed to count user accounts.", e);
        }
    }

    private void assignRoles(Connection conn, int userAccountId, List<Role> roles) throws SQLException {
        validateRoles(roles);

        for (Role role : roles) {
            assignRole(conn, userAccountId, role);
        }
    }

    private void deleteRoles(Connection conn, int userAccountId) throws SQLException {
        String sql = "DELETE FROM user_role WHERE user_account_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userAccountId);
            stmt.executeUpdate();
        }
    }

    private void assignRole(Connection conn, int userAccountId, Role role) throws SQLException {
        String sql = """
                INSERT IGNORE INTO user_role (user_account_id, role_id)
                VALUES (?, ?)
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userAccountId);
            stmt.setInt(2, resolveRoleId(conn, role));
            stmt.executeUpdate();
        }
    }

    private int resolveRoleId(Connection conn, Role role) throws SQLException {
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

    private int getGeneratedId(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.getGeneratedKeys()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        throw new IllegalStateException("Failed to retrieve generated user account ID.");
    }

    private void validateUserAccount(UserAccount userAccount) {
        if (userAccount == null) {
            throw new IllegalArgumentException("User account is required.");
        }

        validatePositive(userAccount.getEmployeeId(), "Employee ID must be positive.");
        normalizeRequired(userAccount.getCompanyEmail(), "Company email is required.");
        normalizeRequired(userAccount.getPasswordHash(), "Password hash is required.");
        normalizeRequired(userAccount.getPasswordSalt(), "Password salt is required.");
        validateRoles(userAccount.getRoles());
    }

    private void validateRoles(List<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("At least one role is required.");
        }

        for (Role role : roles) {
            if (role == null) {
                throw new IllegalArgumentException("Role is required.");
            }
        }
    }

    private void setNullableTimestamp(PreparedStatement stmt, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.TIMESTAMP);
            return;
        }

        stmt.setTimestamp(index, Timestamp.valueOf(value));
    }

    private void validatePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }

    private String normalizeOptional(String value) {
        return value == null ? "" : value.trim();
    }
}
