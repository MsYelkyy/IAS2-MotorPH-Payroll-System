package motorph.backend.repository;

import motorph.backend.exception.DataAccessException;

import motorph.backend.domain.Attendance;
import motorph.backend.domain.value.AttendanceStatus;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for attendance records. updateStatus/delete
 * are not ported - grepping the desktop UI confirmed neither is called from
 * any controller (dead code), same as LeaveReportService in Phase 2.
 */
@Repository
public class AttendanceRepository {

    private final DataSource dataSource;

    public AttendanceRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Attendance> findByEmployeeId(int employeeId) {
        String sql = """
                SELECT attendance_id, employee_id, work_date, time_in, time_out, attendance_status
                FROM attendance
                WHERE employee_id = ?
                ORDER BY work_date DESC
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);

            try (ResultSet rs = stmt.executeQuery()) {
                return mapAll(rs);
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find attendance by employee id.", e);
        }
    }

    public List<Attendance> findAll() {
        String sql = """
                SELECT attendance_id, employee_id, work_date, time_in, time_out, attendance_status
                FROM attendance
                ORDER BY work_date DESC, employee_id
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            return mapAll(rs);

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find all attendance records.", e);
        }
    }

    public List<Attendance> findByDateRange(LocalDate startDate, LocalDate endDate) {
        String sql = """
                SELECT attendance_id, employee_id, work_date, time_in, time_out, attendance_status
                FROM attendance
                WHERE work_date BETWEEN ? AND ?
                ORDER BY work_date DESC, employee_id
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                return mapAll(rs);
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find attendance by date range.", e);
        }
    }

    public List<Attendance> findByEmployeeAndDateRange(int employeeId, LocalDate startDate, LocalDate endDate) {
        String sql = """
                SELECT attendance_id, employee_id, work_date, time_in, time_out, attendance_status
                FROM attendance
                WHERE employee_id = ? AND work_date BETWEEN ? AND ?
                ORDER BY work_date DESC
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.setDate(2, Date.valueOf(startDate));
            stmt.setDate(3, Date.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                return mapAll(rs);
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find attendance by employee and date range.", e);
        }
    }

    public Attendance findToday(int employeeId, LocalDate workDate) {
        String sql = """
                SELECT attendance_id, employee_id, work_date, time_in, time_out, attendance_status
                FROM attendance
                WHERE employee_id = ? AND work_date = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.setDate(2, Date.valueOf(workDate));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find today's attendance.", e);
        }

        return null;
    }

    public boolean existsByEmployeeAndDate(int employeeId, LocalDate workDate) {
        String sql = "SELECT COUNT(*) FROM attendance WHERE employee_id = ? AND work_date = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.setDate(2, Date.valueOf(workDate));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to check attendance existence.", e);
        }

        return false;
    }

    public void timeIn(int employeeId, LocalDate workDate, LocalTime timeIn, AttendanceStatus attendanceStatus) {
        String sql = """
                INSERT INTO attendance (
                    employee_id,
                    work_date,
                    time_in,
                    time_out,
                    attendance_status
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.setDate(2, Date.valueOf(workDate));
            stmt.setTime(3, Time.valueOf(timeIn));
            stmt.setNull(4, Types.TIME);
            stmt.setString(5, attendanceStatus.name());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert time in.", e);
        }
    }

    public void timeOut(int employeeId, LocalDate workDate, LocalTime timeOut) {
        String sql = """
                UPDATE attendance
                SET time_out = ?
                WHERE employee_id = ? AND work_date = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTime(1, Time.valueOf(timeOut));
            stmt.setInt(2, employeeId);
            stmt.setDate(3, Date.valueOf(workDate));

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to update time out.", e);
        }
    }

    private List<Attendance> mapAll(ResultSet rs) throws SQLException {
        List<Attendance> attendances = new ArrayList<>();
        while (rs.next()) {
            attendances.add(mapRow(rs));
        }
        return attendances;
    }

    private Attendance mapRow(ResultSet rs) throws SQLException {
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
}
