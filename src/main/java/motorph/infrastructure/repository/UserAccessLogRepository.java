package motorph.infrastructure.repository;

import motorph.domain.model.UserAccessLog;
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
 * Handles database operations for user access log records.
 */
public class UserAccessLogRepository {

    // Saves a user access log record.
    public void save(UserAccessLog log) {
        String sql = """
                INSERT INTO user_access_logs (
                    employee_id,
                    role_id,
                    action,
                    details,
                    ip_address,
                    device_info,
                    logged_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (log.getEmployeeId() == null) {
                stmt.setNull(1, Types.INTEGER);
            } else {
                stmt.setInt(1, log.getEmployeeId());
            }

            if (log.getRole() == null) {
                stmt.setNull(2, Types.INTEGER);
            } else {
                stmt.setInt(2, resolveRoleId(conn, log.getRole()));
            }

            stmt.setString(3, log.getAction());
            stmt.setString(4, log.getDetails());
            stmt.setString(5, log.getIpAddress());
            stmt.setString(6, log.getDeviceInfo());
            stmt.setTimestamp(7, Timestamp.valueOf(
                    log.getTimestamp() == null ? LocalDateTime.now() : log.getTimestamp()
            ));

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save user access log.", e);
        }
    }

    // Finds all logs ordered by latest first.
    public List<UserAccessLog> findAll() {
        String sql = """
                SELECT ual.employee_id,
                       ual.role_id,
                       ual.action,
                       ual.details,
                       ual.ip_address,
                       ual.device_info,
                       ual.logged_at,
                       r.role_name
                FROM user_access_logs ual
                LEFT JOIN role r ON ual.role_id = r.role_id
                ORDER BY ual.logged_at DESC
                """;

        return executeListQuery(sql, null);
    }

    // Finds logs by employee id.
    public List<UserAccessLog> findByEmployeeId(int employeeId) {
        String sql = """
                SELECT ual.employee_id,
                       ual.role_id,
                       ual.action,
                       ual.details,
                       ual.ip_address,
                       ual.device_info,
                       ual.logged_at,
                       r.role_name
                FROM user_access_logs ual
                LEFT JOIN role r ON ual.role_id = r.role_id
                WHERE ual.employee_id = ?
                ORDER BY ual.logged_at DESC
                """;

        return executeListQuery(sql, stmt -> stmt.setInt(1, employeeId));
    }

    // Finds logs by action.
    public List<UserAccessLog> findByAction(String action) {
        String sql = """
                SELECT ual.employee_id,
                       ual.role_id,
                       ual.action,
                       ual.details,
                       ual.ip_address,
                       ual.device_info,
                       ual.logged_at,
                       r.role_name
                FROM user_access_logs ual
                LEFT JOIN role r ON ual.role_id = r.role_id
                WHERE ual.action = ?
                ORDER BY ual.logged_at DESC
                """;

        return executeListQuery(sql, stmt -> stmt.setString(1, action));
    }

    // Searches logs by keyword.
    public List<UserAccessLog> search(String keyword) {
        String sql = """
                SELECT ual.employee_id,
                       ual.role_id,
                       ual.action,
                       ual.details,
                       ual.ip_address,
                       ual.device_info,
                       ual.logged_at,
                       r.role_name
                FROM user_access_logs ual
                LEFT JOIN role r ON ual.role_id = r.role_id
                WHERE ual.action LIKE ? OR ual.details LIKE ?
                ORDER BY ual.logged_at DESC
                """;

        return executeListQuery(sql, stmt -> {
            String pattern = "%" + keyword + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
        });
    }

    // Finds logs within a date range.
    public List<UserAccessLog> findByDateRange(Timestamp start, Timestamp end) {
        String sql = """
                SELECT ual.employee_id,
                       ual.role_id,
                       ual.action,
                       ual.details,
                       ual.ip_address,
                       ual.device_info,
                       ual.logged_at,
                       r.role_name
                FROM user_access_logs ual
                LEFT JOIN role r ON ual.role_id = r.role_id
                WHERE ual.logged_at BETWEEN ? AND ?
                ORDER BY ual.logged_at DESC
                """;

        return executeListQuery(sql, stmt -> {
            stmt.setTimestamp(1, start);
            stmt.setTimestamp(2, end);
        });
    }

    // Maps one result set row to a user access log object.
    private UserAccessLog mapRow(ResultSet rs) throws SQLException {
        Integer employeeId = rs.getObject("employee_id") == null ? null : rs.getInt("employee_id");

        Role role = null;
        String roleName = rs.getString("role_name");

        if (roleName != null && !roleName.isBlank()) {
            role = Role.fromValue(roleName);
        }

        return new UserAccessLog(
                rs.getTimestamp("logged_at").toLocalDateTime(),
                employeeId,
                role,
                rs.getString("action"),
                rs.getString("details"),
                rs.getString("ip_address"),
                rs.getString("device_info")
        );
    }

    // Resolves the role id from the role table.
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

    // Executes a list query and maps all rows.
    private List<UserAccessLog> executeListQuery(String sql, StatementBinder binder) {
        List<UserAccessLog> logs = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (binder != null) {
                binder.bind(stmt);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to query user access logs.", e);
        }

        return logs;
    }

    // Binds prepared statement parameters.
    private interface StatementBinder {
        void bind(PreparedStatement stmt) throws SQLException;
    }
}