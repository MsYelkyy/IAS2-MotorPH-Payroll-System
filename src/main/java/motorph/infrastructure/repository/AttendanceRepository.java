package motorph.infrastructure.repository;

import motorph.domain.model.Attendance;
import motorph.infrastructure.config.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for attendance records.
 */
public class AttendanceRepository {

    // find attendance by employee
    public List<Attendance> findByEmployeeId(int employeeId) {
        List<Attendance> list = new ArrayList<>();

        String sql = "SELECT * FROM attendance WHERE employee_id = ? ORDER BY work_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // find attendance by payroll
    public List<Attendance> findByPayrollId(int payrollId) {
        List<Attendance> list = new ArrayList<>();

        String sql = "SELECT * FROM attendance WHERE payroll_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payrollId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // find attendance within date range
    public List<Attendance> findByDateRange(LocalDate start, LocalDate end) {
        List<Attendance> list = new ArrayList<>();

        String sql = "SELECT * FROM attendance WHERE work_date BETWEEN ? AND ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(start));
            stmt.setDate(2, Date.valueOf(end));

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // find attendance by employee within date range
    public List<Attendance> findByEmployeeAndDateRange(int employeeId, LocalDate start, LocalDate end) {
        List<Attendance> list = new ArrayList<>();

        String sql = "SELECT * FROM attendance WHERE employee_id = ? AND work_date BETWEEN ? AND ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.setDate(2, Date.valueOf(start));
            stmt.setDate(3, Date.valueOf(end));

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // find today's attendance of employee
    public Attendance findToday(int employeeId, LocalDate date) {
        String sql = "SELECT * FROM attendance WHERE employee_id = ? AND work_date = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.setDate(2, Date.valueOf(date));

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapRow(rs);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // check if attendance exists for employee on date
    public boolean existsByEmployeeAndDate(int employeeId, LocalDate date) {
        String sql = "SELECT COUNT(*) FROM attendance WHERE employee_id = ? AND work_date = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.setDate(2, Date.valueOf(date));

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // insert time in
    public void timeIn(int employeeId, LocalDate workDate, LocalTime timeIn) {
        String sql = "INSERT INTO attendance (employee_id, work_date, time_in) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.setDate(2, Date.valueOf(workDate));
            stmt.setTime(3, Time.valueOf(timeIn));

            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // update time out
    public void timeOut(int employeeId, LocalDate workDate, LocalTime timeOut) {
        String sql = "UPDATE attendance SET time_out = ? WHERE employee_id = ? AND work_date = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTime(1, Time.valueOf(timeOut));
            stmt.setInt(2, employeeId);
            stmt.setDate(3, Date.valueOf(workDate));

            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // delete attendance record
    public void delete(int attendanceId) {
        String sql = "DELETE FROM attendance WHERE attendance_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, attendanceId);
            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // maps database row to Attendance object
    private Attendance mapRow(ResultSet rs) throws SQLException {
        return new Attendance(
                rs.getInt("employee_id"),
                rs.getDate("work_date").toLocalDate(),
                rs.getTime("time_in") != null ? rs.getTime("time_in").toLocalTime() : null,
                rs.getTime("time_out") != null ? rs.getTime("time_out").toLocalTime() : null
        );
    }
}