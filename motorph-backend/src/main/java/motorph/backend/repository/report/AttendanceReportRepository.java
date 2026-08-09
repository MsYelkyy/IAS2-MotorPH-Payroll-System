package motorph.backend.repository.report;

import motorph.backend.exception.DataAccessException;
import motorph.backend.dto.report.attendance.AttendanceReportDTO;
import motorph.backend.dto.report.attendance.EmployeeTimecardEntryDTO;
import motorph.backend.dto.report.attendance.EmployeeTimecardReportDTO;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
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

@Repository
public class AttendanceReportRepository {

    private static final LocalTime BREAK_OUT = LocalTime.of(12, 0);
    private static final LocalTime BREAK_IN = LocalTime.of(13, 0);

    private final DataSource dataSource;

    public AttendanceReportRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<AttendanceReportDTO> findAttendanceReport(Integer departmentId, Integer employeeId,
                                                           String attendanceStatus, LocalDate startDate,
                                                           LocalDate endDate) {
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

        List<Object> params = new ArrayList<>();
        params.add(Date.valueOf(startDate));
        params.add(Date.valueOf(endDate));

        if (departmentId != null) {
            sql.append(" AND d.department_id = ?");
            params.add(departmentId);
        }

        if (employeeId != null) {
            sql.append(" AND e.employee_id = ?");
            params.add(employeeId);
        }

        if (attendanceStatus != null && !attendanceStatus.isBlank()) {
            sql.append(" AND a.attendance_status = ?");
            params.add(attendanceStatus.trim());
        }

        sql.append(" ORDER BY d.department_name, e.last_name, e.first_name, a.work_date");

        List<AttendanceReportDTO> rows = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            bindParameters(stmt, params);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapAttendanceReportRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find attendance report.", e);
        }

        return rows;
    }

    public EmployeeTimecardReportDTO findEmployeeTimecardReport(int employeeId, LocalDate startDate, LocalDate endDate) {
        EmployeeTimecardReportDTO header = findEmployeeTimecardHeader(employeeId, endDate);
        header.setEntries(findEmployeeTimecardEntries(employeeId, startDate, endDate));
        return header;
    }

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

        try (Connection conn = dataSource.getConnection();
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
            throw new DataAccessException("Failed to find employee timecard header.", e);
        }

        throw new DataAccessException("Employee not found for timecard report.");
    }

    private List<EmployeeTimecardEntryDTO> findEmployeeTimecardEntries(int employeeId, LocalDate startDate, LocalDate endDate) {
        String sql = """
                SELECT work_date, time_in, time_out, attendance_status
                FROM attendance
                WHERE employee_id = ? AND work_date BETWEEN ? AND ?
                ORDER BY work_date
                """;

        List<EmployeeTimecardEntryDTO> entries = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
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

    private EmployeeTimecardEntryDTO mapTimecardEntryRow(ResultSet rs) throws SQLException {
        LocalDate workDate = rs.getDate("work_date").toLocalDate();
        LocalTime timeIn = toLocalTime(rs.getTime("time_in"));
        LocalTime timeOut = toLocalTime(rs.getTime("time_out"));

        return new EmployeeTimecardEntryDTO(
                workDate,
                getDayName(workDate),
                timeIn,
                BREAK_OUT,
                BREAK_IN,
                timeOut,
                calculateTotalHoursWorked(timeIn, timeOut),
                rs.getString("attendance_status")
        );
    }

    private String getDayName(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    private double calculateTotalHoursWorked(LocalTime timeIn, LocalTime timeOut) {
        if (timeIn == null || timeOut == null || timeOut.isBefore(timeIn)) {
            return 0.0;
        }

        long totalMinutes = Duration.between(timeIn, timeOut).toMinutes();
        long breakMinutes = calculateBreakOverlapMinutes(timeIn, timeOut);
        long paidMinutes = Math.max(0, totalMinutes - breakMinutes);

        return Math.round((paidMinutes / 60.0) * 100.0) / 100.0;
    }

    private long calculateBreakOverlapMinutes(LocalTime timeIn, LocalTime timeOut) {
        LocalTime overlapStart = timeIn.isAfter(BREAK_OUT) ? timeIn : BREAK_OUT;
        LocalTime overlapEnd = timeOut.isBefore(BREAK_IN) ? timeOut : BREAK_IN;

        if (!overlapEnd.isAfter(overlapStart)) {
            return 0;
        }

        return Duration.between(overlapStart, overlapEnd).toMinutes();
    }

    private LocalTime toLocalTime(Time time) {
        return time == null ? null : time.toLocalTime();
    }

    private void bindParameters(PreparedStatement stmt, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object value = params.get(i);

            if (value instanceof Date dateValue) {
                stmt.setDate(i + 1, dateValue);
            } else if (value instanceof Integer intValue) {
                stmt.setInt(i + 1, intValue);
            } else {
                stmt.setString(i + 1, value.toString());
            }
        }
    }
}
