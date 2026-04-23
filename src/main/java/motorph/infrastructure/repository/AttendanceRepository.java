package motorph.infrastructure.repository;

import motorph.domain.model.Attendance;
import motorph.domain.value.AttendanceStatus;
import motorph.infrastructure.config.DatabaseConnection;

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
 * Handles database operations for attendance records.
 */
public class AttendanceRepository {

    // finds attendance by employee
    public List<Attendance> findByEmployeeId(int employeeId) {
        List<Attendance> attendances = new ArrayList<>();

        String sql = """
                SELECT attendance_id, employee_id, work_date, time_in, time_out, overtime_hours, attendance_status
                FROM attendance
                WHERE employee_id = ?
                ORDER BY work_date DESC
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    attendances.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find attendance by employee id.", e);
        }

        return attendances;
    }

    // finds attendance within date range
    public List<Attendance> findByDateRange(LocalDate startDate, LocalDate endDate) {
        List<Attendance> attendances = new ArrayList<>();

        String sql = """
                SELECT attendance_id, employee_id, work_date, time_in, time_out, overtime_hours, attendance_status
                FROM attendance
                WHERE work_date BETWEEN ? AND ?
                ORDER BY work_date DESC, employee_id
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    attendances.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find attendance by date range.", e);
        }

        return attendances;
    }

    // finds attendance by employee within date range
    public List<Attendance> findByEmployeeAndDateRange(int employeeId, LocalDate startDate, LocalDate endDate) {
        List<Attendance> attendances = new ArrayList<>();

        String sql = """
                SELECT attendance_id, employee_id, work_date, time_in, time_out, overtime_hours, attendance_status
                FROM attendance
                WHERE employee_id = ? AND work_date BETWEEN ? AND ?
                ORDER BY work_date DESC
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.setDate(2, Date.valueOf(startDate));
            stmt.setDate(3, Date.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    attendances.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find attendance by employee and date range.", e);
        }

        return attendances;
    }

    // finds today's attendance of employee
    public Attendance findToday(int employeeId, LocalDate workDate) {
        String sql = """
                SELECT attendance_id, employee_id, work_date, time_in, time_out, overtime_hours, attendance_status
                FROM attendance
                WHERE employee_id = ? AND work_date = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.setDate(2, Date.valueOf(workDate));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find today's attendance.", e);
        }

        return null;
    }

    // checks if attendance exists for employee on date
    public boolean existsByEmployeeAndDate(int employeeId, LocalDate workDate) {
        String sql = "SELECT COUNT(*) FROM attendance WHERE employee_id = ? AND work_date = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.setDate(2, Date.valueOf(workDate));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to check attendance existence.", e);
        }

        return false;
    }

    // inserts time in
    public void timeIn(int employeeId, LocalDate workDate, LocalTime timeIn, AttendanceStatus attendanceStatus) {
        String sql = """
                INSERT INTO attendance (
                    employee_id,
                    work_date,
                    time_in,
                    time_out,
                    overtime_hours,
                    attendance_status
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.setDate(2, Date.valueOf(workDate));
            stmt.setTime(3, Time.valueOf(timeIn));
            stmt.setNull(4, Types.TIME);
            stmt.setDouble(5, 0.0);
            stmt.setString(6, attendanceStatus.name());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert time in.", e);
        }
    }

    // updates time out
    public void timeOut(int employeeId, LocalDate workDate, LocalTime timeOut) {
        String sql = """
                UPDATE attendance
                SET time_out = ?
                WHERE employee_id = ? AND work_date = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTime(1, Time.valueOf(timeOut));
            stmt.setInt(2, employeeId);
            stmt.setDate(3, Date.valueOf(workDate));

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update time out.", e);
        }
    }

    // updates overtime hours and attendance status
    public void updateOvertimeAndStatus(int attendanceId, double overtimeHours, AttendanceStatus attendanceStatus) {
        String sql = """
                UPDATE attendance
                SET overtime_hours = ?, attendance_status = ?
                WHERE attendance_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, overtimeHours);
            stmt.setString(2, attendanceStatus.name());
            stmt.setInt(3, attendanceId);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update overtime hours and attendance status.", e);
        }
    }

    // deletes attendance record
    public void delete(int attendanceId) {
        String sql = "DELETE FROM attendance WHERE attendance_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, attendanceId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete attendance record.", e);
        }
    }

    // maps database row to attendance
    private Attendance mapRow(ResultSet rs) throws SQLException {
        Time timeInValue = rs.getTime("time_in");
        Time timeOutValue = rs.getTime("time_out");

        return new Attendance(
                rs.getInt("attendance_id"),
                rs.getInt("employee_id"),
                rs.getDate("work_date").toLocalDate(),
                timeInValue == null ? null : timeInValue.toLocalTime(),
                timeOutValue == null ? null : timeOutValue.toLocalTime(),
                rs.getDouble("overtime_hours"),
                AttendanceStatus.fromValue(rs.getString("attendance_status"))
        );
    }
}