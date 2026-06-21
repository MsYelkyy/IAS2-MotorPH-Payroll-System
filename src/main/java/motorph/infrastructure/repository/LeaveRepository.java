package motorph.infrastructure.repository;

import motorph.domain.model.LeaveRequest;
import motorph.domain.value.RequestStatus;
import motorph.infrastructure.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for leave request records.
 */
public class LeaveRepository {

    private static final String BASE_SELECT = """
            SELECT lr.leave_request_id,
                   lr.employee_id,
                   lr.leave_type_id,
                   lr.start_date,
                   lr.end_date,
                   lr.leave_hours,
                   lr.reason,
                   lr.leave_status_id,
                   lr.date_submitted,
                   lr.approved_by,
                   lr.updated_at,
                   ls.leave_status_name
            FROM leave_request lr
            JOIN leave_status ls
              ON lr.leave_status_id = ls.leave_status_id
            """;

    // finds all leave requests
    public List<LeaveRequest> findAll() {
        String sql = BASE_SELECT + " ORDER BY lr.date_submitted DESC";
        return executeListQuery(sql, null);
    }

    // finds leave request by id
    public LeaveRequest findById(int leaveRequestId) {
        validatePositive(leaveRequestId, "Leave request ID must be positive.");

        String sql = BASE_SELECT + " WHERE lr.leave_request_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, leaveRequestId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find leave request by id.", e);
        }

        return null;
    }

    // finds leave requests by employee
    public List<LeaveRequest> findByEmployeeId(int employeeId) {
        validatePositive(employeeId, "Employee ID must be positive.");

        String sql = BASE_SELECT + """
                WHERE lr.employee_id = ?
                ORDER BY lr.date_submitted DESC
                """;

        return executeListQuery(sql, stmt -> stmt.setInt(1, employeeId));
    }

    // finds leave requests by status
    public List<LeaveRequest> findByStatus(RequestStatus status) {
        validateStatus(status);

        String sql = BASE_SELECT + """
                WHERE ls.leave_status_name = ?
                ORDER BY lr.date_submitted DESC
                """;

        return executeListQuery(sql, stmt -> stmt.setString(1, status.name()));
    }

    // finds pending leave requests
    public List<LeaveRequest> findPending() {
        return findByStatus(RequestStatus.PENDING);
    }

    // finds leave requests within date range
    public List<LeaveRequest> findByDateRange(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);

        String sql = BASE_SELECT + """
                WHERE lr.start_date >= ?
                  AND lr.end_date <= ?
                ORDER BY lr.start_date
                """;

        return executeListQuery(sql, stmt -> {
            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));
        });
    }

    // checks if employee has overlapping approved leave
    public boolean hasConflict(int employeeId, LocalDate startDate, LocalDate endDate) {
        validatePositive(employeeId, "Employee ID must be positive.");
        validateDateRange(startDate, endDate);

        String sql = """
                SELECT COUNT(*)
                FROM leave_request lr
                JOIN leave_status ls
                  ON lr.leave_status_id = ls.leave_status_id
                WHERE lr.employee_id = ?
                  AND ls.leave_status_name = 'APPROVED'
                  AND lr.start_date <= ?
                  AND lr.end_date >= ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.setDate(2, Date.valueOf(endDate));
            stmt.setDate(3, Date.valueOf(startDate));

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to check leave conflict.", e);
        }
    }

    // saves a leave request
    public void save(LeaveRequest request) {
        validateRequest(request);

        String sql = """
                INSERT INTO leave_request (
                    employee_id,
                    leave_type_id,
                    start_date,
                    end_date,
                    leave_hours,
                    reason,
                    leave_status_id,
                    date_submitted,
                    approved_by
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, request.getEmployeeId());
            stmt.setInt(2, request.getLeaveTypeId());
            stmt.setDate(3, Date.valueOf(request.getStartDate()));
            stmt.setDate(4, Date.valueOf(request.getEndDate()));
            stmt.setDouble(5, request.getLeaveHours());
            stmt.setString(6, request.getReason().trim());
            stmt.setInt(7, resolveLeaveStatusId(conn, request.getStatus()));
            stmt.setTimestamp(8, Timestamp.valueOf(request.getRequestDate()));

            if (request.getApproverAccountId() == null) {
                stmt.setNull(9, Types.INTEGER);
            } else {
                stmt.setInt(9, request.getApproverAccountId());
            }

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save leave request.", e);
        }
    }

    // updates leave request status and approver
    public void updateStatus(int leaveRequestId, RequestStatus status, Integer approvedBy) {
        validatePositive(leaveRequestId, "Leave request ID must be positive.");
        validateStatus(status);

        String sql = """
                UPDATE leave_request
                SET leave_status_id = ?,
                    approved_by = ?
                WHERE leave_request_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, resolveLeaveStatusId(conn, status));

            if (approvedBy == null) {
                stmt.setNull(2, Types.INTEGER);
            } else {
                stmt.setInt(2, approvedBy);
            }

            stmt.setInt(3, leaveRequestId);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new IllegalArgumentException("Leave request not found.");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update leave request status.", e);
        }
    }

    // cancels a leave request
    public void cancel(int leaveRequestId) {
        updateStatus(leaveRequestId, RequestStatus.CANCELLED, null);
    }

    // executes a list query and maps all rows
    private List<LeaveRequest> executeListQuery(String sql, StatementBinder binder) {
        List<LeaveRequest> requests = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (binder != null) {
                binder.bind(stmt);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to query leave requests.", e);
        }

        return requests;
    }

    // maps result set to leave request
    private LeaveRequest mapRow(ResultSet rs) throws SQLException {
        Timestamp submittedAt = rs.getTimestamp("date_submitted");

        int approvedBy = rs.getInt("approved_by");
        Integer approverAccountId = rs.wasNull() ? null : approvedBy;

        // updated_at acts as the reviewed timestamp, but only for reviewed requests
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        LocalDateTime dateReviewed = (approverAccountId != null && updatedAt != null)
                ? updatedAt.toLocalDateTime()
                : null;

        return new LeaveRequest(
                rs.getInt("leave_request_id"),
                rs.getInt("employee_id"),
                rs.getInt("leave_type_id"),
                rs.getDate("start_date").toLocalDate(),
                rs.getDate("end_date").toLocalDate(),
                rs.getDouble("leave_hours"),
                rs.getString("reason"),
                RequestStatus.fromValue(rs.getString("leave_status_name")),
                submittedAt.toLocalDateTime(),
                approverAccountId,
                null,
                dateReviewed
        );
    }

    // resolves leave status id
    private int resolveLeaveStatusId(Connection conn, RequestStatus status) throws SQLException {
        String sql = "SELECT leave_status_id FROM leave_status WHERE leave_status_name = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("leave_status_id");
                }
            }
        }

        throw new IllegalArgumentException("Leave status not found: " + status);
    }

    // validates leave request input
    private void validateRequest(LeaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Leave request is required.");
        }

        validatePositive(request.getEmployeeId(), "Employee ID must be positive.");
        validatePositive(request.getLeaveTypeId(), "Leave type ID must be positive.");
        validateDateRange(request.getStartDate(), request.getEndDate());

        if (request.getLeaveHours() <= 0) {
            throw new IllegalArgumentException("Leave hours must be greater than zero.");
        }

        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Leave reason is required.");
        }

        if (request.getRequestDate() == null) {
            throw new IllegalArgumentException("Request date is required.");
        }

        validateStatus(request.getStatus());
    }

    // validates positive number input
    private void validatePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    // validates request status input
    private void validateStatus(RequestStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Request status is required.");
        }
    }

    // validates date range input
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("Start date is required.");
        }

        if (endDate == null) {
            throw new IllegalArgumentException("End date is required.");
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date.");
        }
    }

    // binds prepared statement parameters
    private interface StatementBinder {
        void bind(PreparedStatement stmt) throws SQLException;
    }
}