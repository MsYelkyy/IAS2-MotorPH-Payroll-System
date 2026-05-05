package motorph.infrastructure.repository;

import motorph.domain.model.PasswordResetRequest;
import motorph.domain.value.PasswordResetStatus;
import motorph.infrastructure.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;

/**
 * Handles database operations for password reset request records.
 */
public class PasswordResetRequestRepository {

    // Saves a new password reset request and returns the generated id.
    public int saveAndReturnId(PasswordResetRequest request) {
        String sql = """
                INSERT INTO password_reset_request (
                    user_account_id,
                    request_status,
                    requested_at,
                    completed_at,
                    cancelled_at,
                    resolved_by_employee_id
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, request.getUserAccountId());
            stmt.setString(2, request.getRequestStatus().name());
            stmt.setTimestamp(3, Timestamp.valueOf(request.getRequestedAt()));
            setNullableTimestamp(stmt, 4, request.getCompletedAt());
            setNullableTimestamp(stmt, 5, request.getCancelledAt());
            setNullableInteger(stmt, 6, request.getResolvedByEmployeeId());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save password reset request.", e);
        }

        throw new IllegalStateException("Failed to retrieve generated reset request ID.");
    }

    // Finds one password reset request by id.
    public PasswordResetRequest findById(int resetRequestId) {
        String sql = """
                SELECT reset_request_id,
                       user_account_id,
                       request_status,
                       requested_at,
                       completed_at,
                       cancelled_at,
                       resolved_by_employee_id
                FROM password_reset_request
                WHERE reset_request_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, resetRequestId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find password reset request by id.", e);
        }

        return null;
    }

    // Finds the latest pending password reset request for a user account.
    public PasswordResetRequest findLatestPendingByUserAccountId(int userAccountId) {
        String sql = """
                SELECT reset_request_id,
                       user_account_id,
                       request_status,
                       requested_at,
                       completed_at,
                       cancelled_at,
                       resolved_by_employee_id
                FROM password_reset_request
                WHERE user_account_id = ?
                  AND request_status = 'PENDING'
                ORDER BY requested_at DESC
                LIMIT 1
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
            throw new RuntimeException("Failed to find latest pending password reset request.", e);
        }

        return null;
    }

    // Marks a password reset request as completed.
    public void markCompleted(int resetRequestId) {
        String sql = """
                UPDATE password_reset_request
                SET request_status = 'COMPLETED',
                    completed_at = NOW()
                WHERE reset_request_id = ?
                """;

        executeStatusUpdate(sql, resetRequestId, "Failed to mark password reset request as completed.");
    }

    // Marks a password reset request as expired.
    public void markExpired(int resetRequestId) {
        String sql = """
                UPDATE password_reset_request
                SET request_status = 'EXPIRED'
                WHERE reset_request_id = ?
                """;

        executeStatusUpdate(sql, resetRequestId, "Failed to mark password reset request as expired.");
    }

    // Marks a password reset request as cancelled.
    public void markCancelled(int resetRequestId) {
        String sql = """
                UPDATE password_reset_request
                SET request_status = 'CANCELLED',
                    cancelled_at = NOW()
                WHERE reset_request_id = ?
                """;

        executeStatusUpdate(sql, resetRequestId, "Failed to mark password reset request as cancelled.");
    }

    // Marks a password reset request as manually resolved.
    public void markManuallyResolved(int resetRequestId, int resolvedByEmployeeId) {
        String sql = """
                UPDATE password_reset_request
                SET request_status = 'COMPLETED',
                    completed_at = NOW(),
                    resolved_by_employee_id = ?
                WHERE reset_request_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, resolvedByEmployeeId);
            stmt.setInt(2, resetRequestId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to mark password reset request as manually resolved.", e);
        }
    }

    // Maps one result set row to a password reset request.
    private PasswordResetRequest mapRow(ResultSet rs) throws SQLException {
        return new PasswordResetRequest(
                rs.getInt("reset_request_id"),
                rs.getInt("user_account_id"),
                PasswordResetStatus.fromValue(rs.getString("request_status")),
                getLocalDateTime(rs, "requested_at"),
                getLocalDateTime(rs, "completed_at"),
                getLocalDateTime(rs, "cancelled_at"),
                getNullableInteger(rs, "resolved_by_employee_id")
        );
    }

    // Executes a simple status update.
    private void executeStatusUpdate(String sql, int resetRequestId, String errorMessage) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, resetRequestId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(errorMessage, e);
        }
    }

    // Sets a nullable timestamp value.
    private void setNullableTimestamp(PreparedStatement stmt, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.TIMESTAMP);
            return;
        }

        stmt.setTimestamp(index, Timestamp.valueOf(value));
    }

    // Sets a nullable integer value.
    private void setNullableInteger(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.INTEGER);
            return;
        }

        stmt.setInt(index, value);
    }

    // Gets a nullable timestamp as local date time.
    private LocalDateTime getLocalDateTime(ResultSet rs, String columnName) throws SQLException {
        Timestamp value = rs.getTimestamp(columnName);
        return value == null ? null : value.toLocalDateTime();
    }

    // Gets a nullable integer value.
    private Integer getNullableInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }
}