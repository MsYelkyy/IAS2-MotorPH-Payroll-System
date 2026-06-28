package motorph.infrastructure.repository;

import motorph.application.exception.DataAccessException;

import motorph.domain.model.UserAccessLog;
import motorph.infrastructure.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for user access log records.
 */
public class UserAccessLogRepository {

    // saves a user access log record
    public void save(UserAccessLog log) {
        if (log == null) {
            throw new IllegalArgumentException("User access log is required.");
        }

        String sql = """
                INSERT INTO user_access_logs (
                    user_account_id,
                    action,
                    details,
                    ip_address,
                    device_info,
                    logged_at
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setNullableInteger(stmt, 1, log.getUserAccountId());
            stmt.setString(2, log.getAction());
            stmt.setString(3, log.getDetails());
            stmt.setString(4, log.getIpAddress());
            stmt.setString(5, log.getDeviceInfo());
            stmt.setTimestamp(6, Timestamp.valueOf(log.getLoggedAt()));

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to save user access log.", e);
        }
    }

    // finds all logs ordered by latest first
    public List<UserAccessLog> findAll() {
        String sql = """
                SELECT log_id,
                       user_account_id,
                       action,
                       details,
                       ip_address,
                       device_info,
                       logged_at
                FROM user_access_logs
                ORDER BY logged_at DESC
                """;

        return executeListQuery(sql, null);
    }

    // finds logs by user account id
    public List<UserAccessLog> findByUserAccountId(int userAccountId) {
        String sql = """
                SELECT log_id,
                       user_account_id,
                       action,
                       details,
                       ip_address,
                       device_info,
                       logged_at
                FROM user_access_logs
                WHERE user_account_id = ?
                ORDER BY logged_at DESC
                """;

        return executeListQuery(sql, stmt -> stmt.setInt(1, userAccountId));
    }

    // finds logs by action
    public List<UserAccessLog> findByAction(String action) {
        String sql = """
                SELECT log_id,
                       user_account_id,
                       action,
                       details,
                       ip_address,
                       device_info,
                       logged_at
                FROM user_access_logs
                WHERE action = ?
                ORDER BY logged_at DESC
                """;

        return executeListQuery(sql, stmt -> stmt.setString(1, action));
    }

    // searches logs by keyword
    public List<UserAccessLog> search(String keyword) {
        String sql = """
                SELECT log_id,
                       user_account_id,
                       action,
                       details,
                       ip_address,
                       device_info,
                       logged_at
                FROM user_access_logs
                WHERE action LIKE ? OR details LIKE ?
                ORDER BY logged_at DESC
                """;

        return executeListQuery(sql, stmt -> {
            String pattern = "%" + keyword + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
        });
    }

    // finds logs within a date range
    public List<UserAccessLog> findByDateRange(Timestamp start, Timestamp end) {
        String sql = """
                SELECT log_id,
                       user_account_id,
                       action,
                       details,
                       ip_address,
                       device_info,
                       logged_at
                FROM user_access_logs
                WHERE logged_at BETWEEN ? AND ?
                ORDER BY logged_at DESC
                """;

        return executeListQuery(sql, stmt -> {
            stmt.setTimestamp(1, start);
            stmt.setTimestamp(2, end);
        });
    }

    // maps one result set row to a user access log object
    private UserAccessLog mapRow(ResultSet rs) throws SQLException {
        return new UserAccessLog(
                rs.getInt("log_id"),
                getNullableInteger(rs, "user_account_id"),
                rs.getString("action"),
                rs.getString("details"),
                rs.getString("ip_address"),
                rs.getString("device_info"),
                rs.getTimestamp("logged_at").toLocalDateTime()
        );
    }

    // executes a list query and maps all rows
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
            throw new DataAccessException("Failed to query user access logs.", e);
        }

        return logs;
    }

    // sets a nullable integer value
    private void setNullableInteger(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.INTEGER);
            return;
        }

        stmt.setInt(index, value);
    }

    // gets a nullable integer value
    private Integer getNullableInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    // binds prepared statement parameters
    private interface StatementBinder {
        void bind(PreparedStatement stmt) throws SQLException;
    }
}