package motorph.infrastructure.repository;

import motorph.application.dto.report.OrganizationPayrollReportDTO;
import motorph.infrastructure.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database retrieval for organization payroll report data.
 */
public class OrganizationPayrollReportRepository {

    // finds organization payroll report data using optional filters
    public List<OrganizationPayrollReportDTO> findOrganizationPayrollReport(
            Integer departmentId,
            Integer employeeId,
            Integer payrunId,
            LocalDate startDate,
            LocalDate endDate) {

        List<OrganizationPayrollReportDTO> reportRows = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
                SELECT pr.payroll_result_id,
                       e.employee_id,
                       CONCAT(e.first_name, ' ', e.last_name) AS employee_name,
                       d.department_name,
                       p.pay_run_id,
                       p.start_date,
                       p.end_date,
                       p.pay_date,
                       pr.total_hours_worked,
                       pr.gross_pay,
                       pr.total_deductions,
                       pr.net_pay
                FROM payroll_result pr
                JOIN employee e
                  ON pr.employee_id = e.employee_id
                JOIN department d
                  ON e.department_id = d.department_id
                JOIN payrun p
                  ON pr.pay_run_id = p.pay_run_id
                WHERE 1 = 1
                """);

        List<Object> parameters = new ArrayList<>();

        if (departmentId != null) {
            sql.append(" AND d.department_id = ?");
            parameters.add(departmentId);
        }

        if (employeeId != null) {
            sql.append(" AND e.employee_id = ?");
            parameters.add(employeeId);
        }

        if (payrunId != null) {
            sql.append(" AND p.pay_run_id = ?");
            parameters.add(payrunId);
        }

        if (startDate != null && endDate != null) {
            sql.append(" AND p.start_date >= ? AND p.end_date <= ?");
            parameters.add(Date.valueOf(startDate));
            parameters.add(Date.valueOf(endDate));
        }

        sql.append(" ORDER BY d.department_name, e.last_name, e.first_name, p.start_date");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            setParameters(stmt, parameters);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reportRows.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find organization payroll report data.", e);
        }

        return reportRows;
    }

    // maps result set to organization payroll report dto
    private OrganizationPayrollReportDTO mapRow(ResultSet rs) throws SQLException {
        return new OrganizationPayrollReportDTO(
                rs.getInt("payroll_result_id"),
                rs.getInt("employee_id"),
                rs.getString("employee_name"),
                rs.getString("department_name"),
                rs.getInt("pay_run_id"),
                rs.getDate("start_date").toLocalDate(),
                rs.getDate("end_date").toLocalDate(),
                rs.getDate("pay_date").toLocalDate(),
                rs.getDouble("total_hours_worked"),
                rs.getDouble("gross_pay"),
                rs.getDouble("total_deductions"),
                rs.getDouble("net_pay")
        );
    }

    // sets prepared statement parameters
    private void setParameters(PreparedStatement stmt, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            Object value = parameters.get(i);

            if (value instanceof Integer intValue) {
                stmt.setInt(i + 1, intValue);
            } else if (value instanceof Date dateValue) {
                stmt.setDate(i + 1, dateValue);
            } else {
                stmt.setObject(i + 1, value);
            }
        }
    }
}