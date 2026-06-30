package motorph.infrastructure.repository.report;

import motorph.application.exception.DataAccessException;

import motorph.application.dto.report.attendance.AttendanceReportDTO;
import motorph.application.dto.report.attendance.EmployeeTimecardEntryDTO;
import motorph.application.dto.report.attendance.EmployeeTimecardReportDTO;
import motorph.infrastructure.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Handles database operations for attendance report data.
 */
public class AttendanceReportRepository {

    private static final LocalTime BREAK_OUT = LocalTime.of(12, 0);
    private static final LocalTime BREAK_IN = LocalTime.of(13, 0);

    // finds employee timecard report data
    public EmployeeTimecardReportDTO findEmployeeTimecardReport(int employeeId,
                                                                LocalDate startDate,
                                                                LocalDate endDate) {
        validatePositive(employeeId, "Employee ID is required.");
        validateDateRange(startDate, endDate);

        EmployeeTimecardReportDTO header = findEmployeeTimecardHeader(employeeId, endDate);
        List<EmployeeTimecardEntryDTO> entries = findEmployeeTimecardEntries(employeeId, startDate, endDate);

        header.setEntries(entries);
        return header;
    }


    // finds attendance report rows for HR attendance reporting
    public List<AttendanceReportDTO> findAttendanceReport(Integer departmentId,
                                                          Integer employeeId,
                                                          String attendanceStatus,
                                                          LocalDate startDate,
                                                          LocalDate endDate) {
        validateDateRange(startDate, endDate);

        List<AttendanceReportDTO> results = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
                SELECT e.employee_id,
                       CONCAT(e.first_name, ' ', e.last_name) AS employee_name,
                       p.position_name,
                       d.department_name,
                       a.work_date,
                       a.time_in,
                       a.time_out,
                       a.attendance_status
                FROM attendance a
                JOIN employee e ON a.employee_id = e.employee_id
                JOIN position p ON e.position_id = p.position_id
                JOIN department d ON e.department_id = d.department_id
                WHERE a.work_date BETWEEN ? AND ?
                """);

        List<Object> parameters = new ArrayList<>();
        parameters.add(Date.valueOf(startDate));
        parameters.add(Date.valueOf(endDate));

        if (departmentId != null) {
            sql.append(" AND d.department_id = ?");
            parameters.add(departmentId);
        }

        if (employeeId != null) {
            sql.append(" AND e.employee_id = ?");
            parameters.add(employeeId);
        }

        if (attendanceStatus != null && !attendanceStatus.isBlank()) {
            sql.append(" AND a.attendance_status = ?");
            parameters.add(attendanceStatus.trim());
        }

        sql.append(" ORDER BY d.department_name, e.last_name, e.first_name, a.work_date");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            bindParameters(stmt, parameters);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapAttendanceReportRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find attendance report data.", e);
        }

        return results;
    }


    // finds employee timecard header data
    private EmployeeTimecardReportDTO findEmployeeTimecardHeader(int employeeId, LocalDate periodEndDate) {
        String sql = """
                SELECT e.employee_id,
                       CONCAT(e.first_name, ' ', e.last_name) AS employee_name,
                       p.position_name,
                       d.department_name
                FROM employee e
                JOIN position p ON e.position_id = p.position_id
                JOIN department d ON e.department_id = d.department_id
                WHERE e.employee_id = ?
                  AND e.is_active = 1
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new EmployeeTimecardReportDTO(
                            rs.getInt("employee_id"),
                            rs.getString("employee_name"),
                            rs.getString("position_name"),
                            rs.getString("department_name"),
                            periodEndDate,
                            new ArrayList<>()
                    );
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find employee timecard header data.", e);
        }

        throw new DataAccessException("Employee not found for timecard report.");
    }

    // finds employee timecard detail rows
    private List<EmployeeTimecardEntryDTO> findEmployeeTimecardEntries(int employeeId,
                                                                       LocalDate startDate,
                                                                       LocalDate endDate) {
        List<EmployeeTimecardEntryDTO> entries = new ArrayList<>();

        String sql = """
                SELECT work_date,
                       time_in,
                       time_out,
                       attendance_status
                FROM attendance
                WHERE employee_id = ?
                  AND work_date BETWEEN ? AND ?
                ORDER BY work_date
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.setDate(2, Date.valueOf(startDate));
            stmt.setDate(3, Date.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(mapTimecardEntryRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find employee timecard entries.", e);
        }

        return entries;
    }

    // maps one timecard row
    private EmployeeTimecardEntryDTO mapTimecardEntryRow(ResultSet rs) throws SQLException {
        LocalDate workDate = rs.getDate("work_date").toLocalDate();
        LocalTime timeIn = toLocalTime(rs.getTime("time_in"));
        LocalTime timeOut = toLocalTime(rs.getTime("time_out"));
        String remarks = rs.getString("attendance_status");

        return new EmployeeTimecardEntryDTO(
                workDate,
                getDayName(workDate),
                timeIn,
                BREAK_OUT,
                BREAK_IN,
                timeOut,
                calculateTotalHoursWorked(timeIn, timeOut),
                remarks
        );
    }

    // maps one attendance report row
    private AttendanceReportDTO mapAttendanceReportRow(ResultSet rs) throws SQLException {
        LocalTime timeIn = toLocalTime(rs.getTime("time_in"));
        LocalTime timeOut = toLocalTime(rs.getTime("time_out"));

        return new AttendanceReportDTO(
                rs.getInt("employee_id"),
                rs.getString("employee_name"),
                rs.getString("position_name"),
                rs.getString("department_name"),
                rs.getDate("work_date").toLocalDate(),
                timeIn,
                timeOut,
                calculateTotalHoursWorked(timeIn, timeOut),
                rs.getString("attendance_status")
        );
    }


    // calculates total worked hours excluding unpaid lunch
    private double calculateTotalHoursWorked(LocalTime timeIn, LocalTime timeOut) {
        if (timeIn == null || timeOut == null || timeOut.isBefore(timeIn)) {
            return 0.0;
        }

        long totalMinutes = Duration.between(timeIn, timeOut).toMinutes();
        long breakMinutes = calculateBreakOverlapMinutes(timeIn, timeOut);
        long paidMinutes = Math.max(0, totalMinutes - breakMinutes);

        return Math.round((paidMinutes / 60.0) * 100.0) / 100.0;
    }

    // calculates lunch break overlap with worked time
    private long calculateBreakOverlapMinutes(LocalTime timeIn, LocalTime timeOut) {
        LocalTime overlapStart = timeIn.isAfter(BREAK_OUT) ? timeIn : BREAK_OUT;
        LocalTime overlapEnd = timeOut.isBefore(BREAK_IN) ? timeOut : BREAK_IN;

        if (!overlapEnd.isAfter(overlapStart)) {
            return 0;
        }

        return Duration.between(overlapStart, overlapEnd).toMinutes();
    }

    // returns the day name from a date
    private String getDayName(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    // converts SQL time to local time
    private LocalTime toLocalTime(Time time) {
        return time == null ? null : time.toLocalTime();
    }

    // binds dynamic SQL parameters
    private void bindParameters(PreparedStatement stmt, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            Object value = parameters.get(i);

            if (value instanceof Date date) {
                stmt.setDate(i + 1, date);
            } else if (value instanceof Integer number) {
                stmt.setInt(i + 1, number);
            } else {
                stmt.setString(i + 1, value.toString());
            }
        }
    }

    // validates positive id input
    private void validatePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    // validates date range input
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date are required.");
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date.");
        }
    }
}