package motorph.backend.repository;

import motorph.backend.exception.DataAccessException;
import motorph.backend.domain.UserAccessLog;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for user access log records.
 */
@Repository
public class UserAccessLogRepository {

    private static final String BASE_SELECT = """
            SELECT log_id,
                   user_account_id,
                   action,
                   details,
                   ip_address,
                   device_info,
                   logged_at
            FROM user_access_logs
            """;

    private final DataSource dataSource;

    public UserAccessLogRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

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
                    device_info
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setNullableInteger(stmt, 1, log.getUserAccountId());
            stmt.setString(2, log.getAction());
            stmt.setString(3, log.getDetails());
            stmt.setString(4, log.getIpAddress());
            stmt.setString(5, log.getDeviceInfo());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to save user access log.", e);
        }
    }

    public List<UserAccessLog> findByUserAccountId(int userAccountId) {
        String sql = BASE_SELECT + """
                WHERE user_account_id = ?
                ORDER BY logged_at DESC
                """;

        return executeListQuery(sql, stmt -> stmt.setInt(1, userAccountId));
    }

    /**
     * Returns every log entry, newest first.
     */
    public List<UserAccessLog> findAll() {
        return executeListQuery(BASE_SELECT + "ORDER BY logged_at DESC", null);
    }

    /**
     * Returns the most recent log entries for the dashboard activity feed, without loading the
     * whole table.
     */
    public List<UserAccessLog> findRecent(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive.");
        }

        String sql = BASE_SELECT + """
                ORDER BY logged_at DESC
                LIMIT ?
                """;

        return executeListQuery(sql, stmt -> stmt.setInt(1, limit));
    }

    public int countByAction(String action) {
        String sql = "SELECT COUNT(*) FROM user_access_logs WHERE action = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, action);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to count user access logs.", e);
        }
    }

    private List<UserAccessLog> executeListQuery(String sql, StatementBinder binder) {
        List<UserAccessLog> logs = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
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

    private void setNullableInteger(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.INTEGER);
            return;
        }

        stmt.setInt(index, value);
    }

    private Integer getNullableInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    private interface StatementBinder {
        void bind(PreparedStatement stmt) throws SQLException;
    }
}
