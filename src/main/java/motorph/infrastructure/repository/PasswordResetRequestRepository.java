package motorph.infrastructure.repository;

import motorph.application.exception.DataAccessException;

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
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for password reset request records.
 */
public class PasswordResetRequestRepository {

    // saves a new password reset request and returns the generated id
    public int saveAndReturnId(PasswordResetRequest request) {
        validateRequest(request);

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
            throw new DataAccessException("Failed to save password reset request.", e);
        }

        throw new IllegalStateException("Failed to retrieve generated reset request ID.");
    }

    // finds one password reset request by id
    public PasswordResetRequest findById(int resetRequestId) {
        validatePositive(resetRequestId, "Reset request ID must be positive.");

        String sql = baseSelect() + """
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
            throw new DataAccessException("Failed to find password reset request by id.", e);
        }

        return null;
    }

    // finds the latest pending password reset request for a user account
    public PasswordResetRequest findLatestPendingByUserAccountId(int userAccountId) {
        validatePositive(userAccountId, "User account ID must be positive.");

        String sql = baseSelect() + """
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
            throw new DataAccessException("Failed to find latest pending password reset request.", e);
        }

        return null;
    }

    // finds all pending password reset requests
    public List<PasswordResetRequest> findPendingRequests() {
        List<PasswordResetRequest> results = new ArrayList<>();

        String sql = baseSelect() + """
                WHERE request_status = 'PENDING'
                ORDER BY requested_at DESC
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                results.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find pending password reset requests.", e);
        }

        return results;
    }

    // finds all password reset requests
public List<PasswordResetRequest> findAll() {
    List<PasswordResetRequest> results = new ArrayList<>();

    String sql = baseSelect() + """
            ORDER BY requested_at DESC
            """;

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {

        while (rs.next()) {
            results.add(mapRow(rs));
        }

    } catch (SQLException e) {
        throw new DataAccessException("Failed to find password reset requests.", e);
    }

    return results;
}

// finds password reset requests by status
public List<PasswordResetRequest> findByStatus(PasswordResetStatus status) {
    if (status == null) {
        throw new IllegalArgumentException("Password reset status is required.");
    }

    List<PasswordResetRequest> results = new ArrayList<>();

    String sql = baseSelect() + """
            WHERE request_status = ?
            ORDER BY requested_at DESC
            """;

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

        stmt.setString(1, status.name());

        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                results.add(mapRow(rs));
            }
        }

    } catch (SQLException e) {
        throw new DataAccessException("Failed to find password reset requests by status.", e);
    }

    return results;
}

    // marks a password reset request as completed
    public void markCompleted(int resetRequestId) {
        validatePositive(resetRequestId, "Reset request ID must be positive.");

        String sql = """
                UPDATE password_reset_request
                SET request_status = 'COMPLETED',
                    completed_at = NOW()
                WHERE reset_request_id = ?
                  AND request_status = 'PENDING'
                """;

        executeStatusUpdate(sql, resetRequestId, "Failed to mark password reset request as completed.");
    }

    // marks a password reset request as manually resolved
    public void markManuallyResolved(int resetRequestId, int resolvedByEmployeeId) {
        validatePositive(resetRequestId, "Reset request ID must be positive.");
        validatePositive(resolvedByEmployeeId, "Resolver employee ID must be positive.");

        String sql = """
                UPDATE password_reset_request
                SET request_status = 'COMPLETED',
                    completed_at = NOW(),
                    resolved_by_employee_id = ?
                WHERE reset_request_id = ?
                  AND request_status = 'PENDING'
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, resolvedByEmployeeId);
            stmt.setInt(2, resetRequestId);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new IllegalArgumentException("Pending password reset request not found.");
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to mark password reset request as manually resolved.", e);
        }
    }

    // marks a password reset request as expired
    public void markExpired(int resetRequestId) {
        validatePositive(resetRequestId, "Reset request ID must be positive.");

        String sql = """
                UPDATE password_reset_request
                SET request_status = 'EXPIRED'
                WHERE reset_request_id = ?
                  AND request_status = 'PENDING'
                """;

        executeStatusUpdate(sql, resetRequestId, "Failed to mark password reset request as expired.");
    }

    // marks a password reset request as cancelled
    public void markCancelled(int resetRequestId, int cancelledByEmployeeId) {
        validatePositive(resetRequestId, "Reset request ID must be positive.");
        validatePositive(cancelledByEmployeeId, "Canceller employee ID must be positive.");

        String sql = """
                UPDATE password_reset_request
                SET request_status = 'CANCELLED',
                    cancelled_at = NOW(),
                    resolved_by_employee_id = ?
                WHERE reset_request_id = ?
                  AND request_status = 'PENDING'
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, cancelledByEmployeeId);
            stmt.setInt(2, resetRequestId);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new IllegalArgumentException("Pending password reset request not found.");
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to cancel password reset request.", e);
        }
    }

    // builds the base select query
    private String baseSelect() {
        return """
                SELECT reset_request_id,
                       user_account_id,
                       request_status,
                       requested_at,
                       completed_at,
                       cancelled_at,
                       resolved_by_employee_id
                FROM password_reset_request
                """;
    }

    // maps one result set row to a password reset request
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

    // executes a simple status update
    private void executeStatusUpdate(String sql, int resetRequestId, String errorMessage) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, resetRequestId);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new IllegalArgumentException("Pending password reset request not found.");
            }

        } catch (SQLException e) {
            throw new DataAccessException(errorMessage, e);
        }
    }

    // validates password reset request input
    private void validateRequest(PasswordResetRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Password reset request is required.");
        }

        validatePositive(request.getUserAccountId(), "User account ID must be positive.");

        if (request.getRequestStatus() == null) {
            throw new IllegalArgumentException("Request status is required.");
        }

        if (request.getRequestedAt() == null) {
            throw new IllegalArgumentException("Requested date and time is required.");
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

    // sets a nullable integer value
    private void setNullableInteger(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.INTEGER);
            return;
        }

        stmt.setInt(index, value);
    }

    // gets a nullable timestamp as local date time
    private LocalDateTime getLocalDateTime(ResultSet rs, String columnName) throws SQLException {
        Timestamp value = rs.getTimestamp(columnName);
        return value == null ? null : value.toLocalDateTime();
    }

    // gets a nullable integer value
    private Integer getNullableInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }
}