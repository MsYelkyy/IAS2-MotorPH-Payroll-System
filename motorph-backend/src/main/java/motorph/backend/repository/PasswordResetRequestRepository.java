package motorph.backend.repository;

import motorph.backend.domain.PasswordResetRequest;
import motorph.backend.domain.value.PasswordResetStatus;
import motorph.backend.exception.DataAccessException;

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
 * Handles database operations for password reset request records.
 */
@Repository
public class PasswordResetRequestRepository {

    private static final String BASE_SELECT = """
            SELECT reset_request_id,
                   user_account_id,
                   request_status,
                   requested_at,
                   completed_at,
                   cancelled_at,
                   resolved_by_employee_id
            FROM password_reset_request
            """;

    private final DataSource dataSource;

    public PasswordResetRequestRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

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

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

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

        String sql = BASE_SELECT + "WHERE reset_request_id = ?";

        return executeSingleQuery(sql,
                stmt -> stmt.setInt(1, resetRequestId),
                "Failed to find password reset request by id.");
    }

    // finds the latest pending password reset request for a user account
    public PasswordResetRequest findLatestPendingByUserAccountId(int userAccountId) {
        validatePositive(userAccountId, "User account ID must be positive.");

        String sql = BASE_SELECT + """
                WHERE user_account_id = ?
                  AND request_status = 'PENDING'
                ORDER BY requested_at DESC
                LIMIT 1
                """;

        return executeSingleQuery(sql,
                stmt -> stmt.setInt(1, userAccountId),
                "Failed to find latest pending password reset request.");
    }

    // finds all pending password reset requests
    public List<PasswordResetRequest> findPendingRequests() {
        String sql = BASE_SELECT + """
                WHERE request_status = 'PENDING'
                ORDER BY requested_at DESC
                """;

        return executeListQuery(sql, null, "Failed to find pending password reset requests.");
    }

    // finds all password reset requests
    public List<PasswordResetRequest> findAll() {
        String sql = BASE_SELECT + "ORDER BY requested_at DESC";

        return executeListQuery(sql, null, "Failed to find password reset requests.");
    }

    // counts password reset requests in one status
    public int countByStatus(PasswordResetStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Password reset status is required.");
        }

        String sql = "SELECT COUNT(*) FROM password_reset_request WHERE request_status = ?";

        return executeCountQuery(sql, stmt -> stmt.setString(1, status.name()));
    }

    // counts every password reset request
    public int countAll() {
        return executeCountQuery("SELECT COUNT(*) FROM password_reset_request", null);
    }

    // marks a password reset request as manually resolved by IT
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

        executeResolutionUpdate(sql, resolvedByEmployeeId, resetRequestId,
                "Failed to mark password reset request as manually resolved.");
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

        executeResolutionUpdate(sql, cancelledByEmployeeId, resetRequestId,
                "Failed to cancel password reset request.");
    }

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

    private int executeCountQuery(String sql, StatementBinder binder) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (binder != null) {
                binder.bind(stmt);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to count password reset requests.", e);
        }
    }

    private PasswordResetRequest executeSingleQuery(String sql, StatementBinder binder, String errorMessage) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            binder.bind(stmt);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException(errorMessage, e);
        }

        return null;
    }

    private List<PasswordResetRequest> executeListQuery(String sql, StatementBinder binder, String errorMessage) {
        List<PasswordResetRequest> results = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (binder != null) {
                binder.bind(stmt);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException(errorMessage, e);
        }

        return results;
    }

    // applies a resolution update, rejecting requests that are no longer pending
    private void executeResolutionUpdate(String sql, int employeeId, int resetRequestId, String errorMessage) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.setInt(2, resetRequestId);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new IllegalArgumentException("Pending password reset request not found.");
            }

        } catch (SQLException e) {
            throw new DataAccessException(errorMessage, e);
        }
    }

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

    private void validatePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private void setNullableTimestamp(PreparedStatement stmt, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.TIMESTAMP);
            return;
        }

        stmt.setTimestamp(index, Timestamp.valueOf(value));
    }

    private void setNullableInteger(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.INTEGER);
            return;
        }

        stmt.setInt(index, value);
    }

    private LocalDateTime getLocalDateTime(ResultSet rs, String columnName) throws SQLException {
        Timestamp value = rs.getTimestamp(columnName);
        return value == null ? null : value.toLocalDateTime();
    }

    private Integer getNullableInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    // binds prepared statement parameters
    private interface StatementBinder {
        void bind(PreparedStatement stmt) throws SQLException;
    }
}
