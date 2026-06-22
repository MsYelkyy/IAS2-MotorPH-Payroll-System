package motorph.infrastructure.repository;

import motorph.domain.model.Attendance;
import motorph.domain.model.OvertimeRequest;
import motorph.domain.value.AttendanceStatus;
import motorph.domain.value.RequestStatus;
import motorph.infrastructure.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for overtime request records.
 */
public class OvertimeRepository implements Repository<OvertimeRequest, Integer> {

    private static final String BASE_SELECT = """
            SELECT overtime_request_id,
                   employee_id,
                   attendance_id,
                   overtime_type_id,
                   overtime_date,
                   requested_hours,
                   reason,
                   overtime_status,
                   approved_by,
                   approved_at,
                   approval_remarks,
                   created_at
            FROM overtime_request
            """;

    // finds all overtime requests
    @Override
    public List<OvertimeRequest> findAll() {
        String sql = BASE_SELECT + " ORDER BY created_at DESC";
        return executeListQuery(sql, null);
    }

    // finds overtime request by id
    @Override
    public OvertimeRequest findById(Integer overtimeRequestId) {
        validatePositive(overtimeRequestId, "Overtime request ID must be positive.");

        String sql = BASE_SELECT + " WHERE overtime_request_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, overtimeRequestId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find overtime request by id.", e);
        }

        return null;
    }

    // finds overtime requests by employee
    public List<OvertimeRequest> findByEmployeeId(int employeeId) {
        validatePositive(employeeId, "Employee ID must be positive.");

        String sql = BASE_SELECT + """
                WHERE employee_id = ?
                ORDER BY created_at DESC
                """;

        return executeListQuery(sql, stmt -> stmt.setInt(1, employeeId));
    }

    // finds overtime requests by status
    public List<OvertimeRequest> findByStatus(RequestStatus status) {
        validateStatus(status);

        String sql = BASE_SELECT + """
                WHERE overtime_status = ?
                ORDER BY created_at DESC
                """;

        return executeListQuery(sql, stmt -> stmt.setString(1, status.name()));
    }

    // finds pending overtime requests
    public List<OvertimeRequest> findPending() {
        return findByStatus(RequestStatus.PENDING);
    }

    // finds overtime requests within date range
    public List<OvertimeRequest> findByDateRange(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);

        String sql = BASE_SELECT + """
                WHERE overtime_date BETWEEN ? AND ?
                ORDER BY overtime_date DESC
                """;

        return executeListQuery(sql, stmt -> {
            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));
        });
    }

    // finds overtime requests by employee within date range
    public List<OvertimeRequest> findByEmployeeAndDateRange(int employeeId, LocalDate startDate, LocalDate endDate) {
        validatePositive(employeeId, "Employee ID must be positive.");
        validateDateRange(startDate, endDate);

        String sql = BASE_SELECT + """
                WHERE employee_id = ?
                  AND overtime_date BETWEEN ? AND ?
                ORDER BY overtime_date DESC
                """;

        return executeListQuery(sql, stmt -> {
            stmt.setInt(1, employeeId);
            stmt.setDate(2, Date.valueOf(startDate));
            stmt.setDate(3, Date.valueOf(endDate));
        });
    }

    // checks if attendance already has an active overtime request
    public boolean existsByAttendanceId(int attendanceId) {
        validatePositive(attendanceId, "Attendance ID must be positive.");

        String sql = """
                SELECT COUNT(*)
                FROM overtime_request
                WHERE attendance_id = ?
                  AND overtime_status <> 'CANCELLED'
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, attendanceId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to check overtime request existence.", e);
        }
    }

    // saves an overtime request
    @Override
    public void save(OvertimeRequest request) {
        validateRequest(request);

        String sql = """
                INSERT INTO overtime_request (
                    employee_id,
                    attendance_id,
                    overtime_type_id,
                    overtime_date,
                    requested_hours,
                    reason,
                    overtime_status,
                    approved_by,
                    approved_at,
                    approval_remarks
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, request.getEmployeeId());
            stmt.setInt(2, request.getAttendanceId());
            stmt.setInt(3, request.getOvertimeTypeId());
            stmt.setDate(4, Date.valueOf(request.getOvertimeDate()));
            stmt.setDouble(5, request.getRequestedHours());
            stmt.setString(6, request.getReason());
            stmt.setString(7, request.getStatus().name());
            setNullableInteger(stmt, 8, request.getApproverAccountId());
            setNullableDateTime(stmt, 9, request.getDateReviewed());
            stmt.setString(10, request.getRemarks());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save overtime request.", e);
        }
    }

    // updates overtime request status
    public void updateStatus(int overtimeRequestId, RequestStatus status, Integer approvedBy, String remarks) {
        validatePositive(overtimeRequestId, "Overtime request ID must be positive.");
        validateStatus(status);

        LocalDateTime reviewedAt = null;

        if (status == RequestStatus.APPROVED || status == RequestStatus.REJECTED) {
            reviewedAt = LocalDateTime.now();
        }

        String sql = """
                UPDATE overtime_request
                SET overtime_status = ?,
                    approved_by = ?,
                    approved_at = ?,
                    approval_remarks = ?
                WHERE overtime_request_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            setNullableInteger(stmt, 2, approvedBy);
            setNullableDateTime(stmt, 3, reviewedAt);
            stmt.setString(4, clean(remarks));
            stmt.setInt(5, overtimeRequestId);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update overtime request status.", e);
        }
    }

    // gets approved overtime hours of employee within date range
    public double getApprovedHours(int employeeId, LocalDate startDate, LocalDate endDate) {
        validatePositive(employeeId, "Employee ID must be positive.");
        validateDateRange(startDate, endDate);

        String sql = """
                SELECT COALESCE(SUM(requested_hours), 0)
                FROM overtime_request
                WHERE employee_id = ?
                  AND overtime_status = 'APPROVED'
                  AND overtime_date BETWEEN ? AND ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.setDate(2, Date.valueOf(startDate));
            stmt.setDate(3, Date.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to get approved overtime hours.", e);
        }

        return 0.0;
    }

    // finds recent completed attendance of an employee that has no active overtime request
    public List<Attendance> findEligibleAttendanceForOvertime(int employeeId, LocalDate windowStart, LocalDate windowEnd) {
        validatePositive(employeeId, "Employee ID must be positive.");
        validateDateRange(windowStart, windowEnd);

        String sql = """
                SELECT a.attendance_id,
                       a.employee_id,
                       a.work_date,
                       a.time_in,
                       a.time_out,
                       a.attendance_status
                FROM attendance a
                LEFT JOIN overtime_request o
                       ON o.attendance_id = a.attendance_id
                      AND o.overtime_status <> 'CANCELLED'
                WHERE a.employee_id = ?
                  AND a.time_in IS NOT NULL
                  AND a.time_out IS NOT NULL
                  AND a.time_out > a.time_in
                  AND a.work_date BETWEEN ? AND ?
                  AND o.overtime_request_id IS NULL
                ORDER BY a.work_date DESC
                """;

        List<Attendance> eligible = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.setDate(2, Date.valueOf(windowStart));
            stmt.setDate(3, Date.valueOf(windowEnd));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    eligible.add(mapAttendanceRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find eligible attendance for overtime.", e);
        }

        return eligible;
    }

    // maps result set to attendance
    private Attendance mapAttendanceRow(ResultSet rs) throws SQLException {
        Time timeInValue = rs.getTime("time_in");
        Time timeOutValue = rs.getTime("time_out");

        return new Attendance(
                rs.getInt("attendance_id"),
                rs.getInt("employee_id"),
                rs.getDate("work_date").toLocalDate(),
                timeInValue == null ? null : timeInValue.toLocalTime(),
                timeOutValue == null ? null : timeOutValue.toLocalTime(),
                AttendanceStatus.fromValue(rs.getString("attendance_status"))
        );
    }

    // maps result set to overtime request
    private OvertimeRequest mapRow(ResultSet rs) throws SQLException {
        Timestamp approvedAt = rs.getTimestamp("approved_at");

        int approvedByValue = rs.getInt("approved_by");
        Integer approvedBy = rs.wasNull() ? null : approvedByValue;

        return new OvertimeRequest(
                rs.getInt("overtime_request_id"),
                rs.getInt("employee_id"),
                rs.getInt("attendance_id"),
                rs.getInt("overtime_type_id"),
                rs.getDate("overtime_date").toLocalDate(),
                rs.getDouble("requested_hours"),
                rs.getString("reason"),
                RequestStatus.fromValue(rs.getString("overtime_status")),
                rs.getTimestamp("created_at").toLocalDateTime(),
                approvedBy,
                rs.getString("approval_remarks"),
                approvedAt == null ? null : approvedAt.toLocalDateTime()
        );
    }

    // executes a list query and maps all rows
    private List<OvertimeRequest> executeListQuery(String sql, StatementBinder binder) {
        List<OvertimeRequest> requests = new ArrayList<>();

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
            throw new RuntimeException("Failed to query overtime requests.", e);
        }

        return requests;
    }

    // validates overtime request input
    private void validateRequest(OvertimeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Overtime request is required.");
        }

        validatePositive(request.getEmployeeId(), "Employee ID must be positive.");
        validatePositive(request.getAttendanceId(), "Attendance ID must be positive.");
        validatePositive(request.getOvertimeTypeId(), "Overtime type ID must be positive.");

        if (request.getOvertimeDate() == null) {
            throw new IllegalArgumentException("Overtime date is required.");
        }

        if (request.getRequestedHours() <= 0) {
            throw new IllegalArgumentException("Requested overtime hours must be greater than zero.");
        }

        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Reason is required.");
        }

        validateStatus(request.getStatus());
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

    // validates positive number input
    private void validatePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    // sets nullable integer parameter
    private void setNullableInteger(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.INTEGER);
        } else {
            stmt.setInt(index, value);
        }
    }

    // sets nullable date and time parameter
    private void setNullableDateTime(PreparedStatement stmt, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.TIMESTAMP);
        } else {
            stmt.setTimestamp(index, Timestamp.valueOf(value));
        }
    }

    // cleans optional text
    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    // binds prepared statement parameters
    private interface StatementBinder {
        void bind(PreparedStatement stmt) throws SQLException;
    }
}