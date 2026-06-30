package motorph.infrastructure.repository.report;

import motorph.application.exception.DataAccessException;

import motorph.application.dto.report.leave.LeaveReportDTO;
import motorph.infrastructure.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for leave report data.
 */
public class LeaveReportRepository {

    // finds leave report rows using optional filters
    public List<LeaveReportDTO> findLeaveReport(Integer departmentId,
                                                Integer employeeId,
                                                String leaveType,
                                                String leaveStatus,
                                                Date startDate,
                                                Date endDate) {
        List<LeaveReportDTO> results = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
                SELECT lr.leave_request_id,
                       e.employee_id,
                       CONCAT(e.first_name, ' ', e.last_name) AS employee_name,
                       d.department_name,
                       lt.leave_name AS leave_type_name,
                       lr.start_date,
                       lr.end_date,
                       lr.leave_hours,
                       ls.leave_status_name AS status
                FROM leave_request lr
                JOIN employee e ON lr.employee_id = e.employee_id
                JOIN department d ON e.department_id = d.department_id
                JOIN leave_type lt ON lr.leave_type_id = lt.leave_type_id
                JOIN leave_status ls ON lr.leave_status_id = ls.leave_status_id
                WHERE lr.start_date >= ? AND lr.end_date <= ?
                """);

        List<Object> parameters = new ArrayList<>();
        parameters.add(endDate);
        parameters.add(startDate);

        if (departmentId != null) {
            sql.append(" AND d.department_id = ?");
            parameters.add(departmentId);
        }

        if (employeeId != null) {
            sql.append(" AND e.employee_id = ?");
            parameters.add(employeeId);
        }

        if (leaveType != null && !leaveType.isBlank()) {
            sql.append(" AND lt.leave_type_name = ?");
            parameters.add(leaveType.trim());
        }

        if (leaveStatus != null && !leaveStatus.isBlank()) {
            sql.append(" AND ls.leave_status_name = ?");
            parameters.add(leaveStatus.trim());
        }

        sql.append(" ORDER BY d.department_name, e.last_name, e.first_name, lr.start_date");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            bindParameters(stmt, parameters);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find leave report data.", e);
        }

        return results;
    }

    // maps result set to leave report dto
    private LeaveReportDTO mapRow(ResultSet rs) throws SQLException {
        return new LeaveReportDTO(
                rs.getInt("leave_request_id"),
                rs.getInt("employee_id"),
                rs.getString("employee_name"),
                rs.getString("department_name"),
                rs.getString("leave_type_name"),
                rs.getDate("start_date").toLocalDate(),
                rs.getDate("end_date").toLocalDate(),
                rs.getDouble("leave_hours"),
                rs.getString("status")
        );
    }

    // binds dynamic query parameters
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
}