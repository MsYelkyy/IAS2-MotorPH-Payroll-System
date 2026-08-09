package motorph.backend.repository;

import motorph.backend.exception.DataAccessException;
import motorph.backend.domain.UserAccessLog;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
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
@Repository
public class UserAccessLogRepository {

    private final DataSource dataSource;

    public UserAccessLogRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

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

        try (Connection conn = dataSource.getConnection();
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

        List<UserAccessLog> logs = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userAccountId);

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
}
