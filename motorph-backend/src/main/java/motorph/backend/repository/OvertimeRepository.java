package motorph.backend.repository;

import motorph.backend.exception.DataAccessException;

import motorph.backend.domain.Attendance;
import motorph.backend.domain.OvertimeRequest;
import motorph.backend.domain.value.AttendanceStatus;
import motorph.backend.domain.value.RequestStatus;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
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
 * Handles database operations for overtime request records. Methods with no
 * UI caller (findByStatus/findPending/findByDateRange/findByEmployeeAndDateRange/
 * getApprovedHours) are not ported, matching the dead-code-skip precedent
 * used elsewhere this session.
 */
@Repository
public class OvertimeRepository {

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

    private final DataSource dataSource;

    public OvertimeRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<OvertimeRequest> findAll() {
        String sql = BASE_SELECT + " ORDER BY created_at DESC";
        return executeListQuery(sql, null);
    }

    public OvertimeRequest findById(int overtimeRequestId) {
        String sql = BASE_SELECT + " WHERE overtime_request_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, overtimeRequestId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find overtime request by id.", e);
        }

        return null;
    }

    public List<OvertimeRequest> findByEmployeeId(int employeeId) {
        String sql = BASE_SELECT + """
                WHERE employee_id = ?
                ORDER BY created_at DESC
                """;

        return executeListQuery(sql, stmt -> stmt.setInt(1, employeeId));
    }

    public boolean existsByAttendanceId(int attendanceId) {
        String sql = """
                SELECT COUNT(*)
                FROM overtime_request
                WHERE attendance_id = ?
                  AND overtime_status <> 'CANCELLED'
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, attendanceId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to check overtime request existence.", e);
        }
    }

    public void save(OvertimeRequest request) {
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

        try (Connection conn = dataSource.getConnection();
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
            throw new DataAccessException("Failed to save overtime request.", e);
        }
    }

    public void updateStatus(int overtimeRequestId, RequestStatus status, Integer approvedBy, String remarks) {
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

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            setNullableInteger(stmt, 2, approvedBy);
            setNullableDateTime(stmt, 3, reviewedAt);
            stmt.setString(4, clean(remarks));
            stmt.setInt(5, overtimeRequestId);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to update overtime request status.", e);
        }
    }

    // finds recent completed attendance of an employee that has no active overtime request
    public List<Attendance> findEligibleAttendanceForOvertime(int employeeId, LocalDate windowStart, LocalDate windowEnd) {
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

        try (Connection conn = dataSource.getConnection();
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
            throw new DataAccessException("Failed to find eligible attendance for overtime.", e);
        }

        return eligible;
    }

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

    private List<OvertimeRequest> executeListQuery(String sql, StatementBinder binder) {
        List<OvertimeRequest> requests = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
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
            throw new DataAccessException("Failed to query overtime requests.", e);
        }

        return requests;
    }

    private void setNullableInteger(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.INTEGER);
        } else {
            stmt.setInt(index, value);
        }
    }

    private void setNullableDateTime(PreparedStatement stmt, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.TIMESTAMP);
        } else {
            stmt.setTimestamp(index, Timestamp.valueOf(value));
        }
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private interface StatementBinder {
        void bind(PreparedStatement stmt) throws SQLException;
    }
}
