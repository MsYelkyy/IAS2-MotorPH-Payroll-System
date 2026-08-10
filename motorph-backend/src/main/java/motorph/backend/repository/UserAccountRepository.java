package motorph.backend.repository;

import motorph.backend.exception.DataAccessException;

import motorph.backend.domain.UserAccount;
import motorph.backend.domain.value.Role;

import org.springframework.stereotype.Repository;

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

    // finds an account by company email
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

    // finds an account by user account id
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

    // finds an account by employee id
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

    // counts accounts that can currently log in
    public int countActive() {
        return executeCountQuery("SELECT COUNT(*) FROM user_account WHERE is_active = 1 AND is_locked = 0");
    }

    // counts locked accounts
    public int countLocked() {
        return executeCountQuery("SELECT COUNT(*) FROM user_account WHERE is_locked = 1");
    }

    // counts disabled accounts
    public int countDisabled() {
        return executeCountQuery("SELECT COUNT(*) FROM user_account WHERE is_active = 0");
    }

    // saves a new account and assigns its roles
    @Override
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

        try (Connection conn = dataSource.getConnection()) {
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
                stmt.setBoolean(9, userAccount.mustChangePassword());
                setNullableTimestamp(stmt, 10, userAccount.getTemporaryPasswordExpiresAt());

                stmt.executeUpdate();

                int userAccountId = getGeneratedId(stmt);
                assignRoles(conn, userAccountId, userAccount.getRoles());
            }

            conn.commit();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to save user account.", e);
        }
    }

    // updates the lock status
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

    // updates the active status
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

    // checks if a company email already exists
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

    // replaces every role assigned to the account
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

    // updates the last login time
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

    // maps the result set to a user account
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

    // finds all roles assigned to one user account
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

    // assigns all roles to the user account
    private void assignRoles(Connection conn, int userAccountId, List<Role> roles) throws SQLException {
        validateRoles(roles);

        for (Role role : roles) {
            assignRole(conn, userAccountId, role);
        }
    }

    // deletes every existing role assignment for the user account
    private void deleteRoles(Connection conn, int userAccountId) throws SQLException {
        String sql = "DELETE FROM user_role WHERE user_account_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userAccountId);
            stmt.executeUpdate();
        }
    }

    // assigns one role to the user account
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

    // resolves the role id from the role name
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

    // gets the generated user account id
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
