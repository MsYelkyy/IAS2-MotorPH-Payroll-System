package motorph.backend.repository.report;

import motorph.backend.exception.DataAccessException;
import motorph.backend.dto.payroll.PayslipReportDTO;
import motorph.backend.dto.report.payroll.OrganizationPayrollReportDTO;
import motorph.backend.dto.report.payroll.PayrollSummaryReportDTO;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the pre-joined, pre-pivoted payslip view for individual payslips, plus
 * two aggregate payroll reports (organization-wide monthly, and per-department
 * summary). All join/aggregation logic and official figures live in the
 * underlying views/tables; these queries only select and filter.
 */
@Repository
public class PayrollReportRepository {

    private final DataSource dataSource;

    public PayrollReportRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<OrganizationPayrollReportDTO> findOrganizationPayrollReport(Integer departmentId,
                                                                            String departmentName,
                                                                            Integer employeeId,
                                                                            Integer payrunId,
                                                                            LocalDate startDate,
                                                                            LocalDate endDate) {
        // payrunId is deliberately unused: this report merges both semi-monthly
        // cutoffs into one monthly row, so a single payrun/cutoff has no meaning here
        StringBuilder sql = new StringBuilder("""
                SELECT employee_id, employee_name, position_name, department_name,
                       payroll_month, month_start, month_end, monthly_gross_pay,
                       sss_number, philhealth_number, pagibig_number, tin_number,
                       sss_deduction, philhealth_deduction, pagibig_deduction, withholding_tax,
                       monthly_total_deductions, monthly_net_pay
                FROM vw_payroll_summary_monthly
                WHERE 1 = 1
                """);

        List<Object> params = new ArrayList<>();

        if (departmentId != null && departmentName != null && !departmentName.isBlank()) {
            sql.append(" AND department_name = ?");
            params.add(departmentName);
        }

        if (employeeId != null) {
            sql.append(" AND employee_id = ?");
            params.add(employeeId);
        }

        if (startDate != null && endDate != null) {
            sql.append(" AND month_end >= ? AND month_start <= ?");
            params.add(Date.valueOf(startDate));
            params.add(Date.valueOf(endDate));
        }

        sql.append(" ORDER BY department_name, employee_name, payroll_month");

        List<OrganizationPayrollReportDTO> rows = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            setParameters(stmt, params);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapOrganizationRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find organization payroll report.", e);
        }

        return rows;
    }

    public List<PayrollSummaryReportDTO> findPayrollSummaryReport(Integer departmentId, Integer payrunId,
                                                                   LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder("""
                SELECT d.department_name,
                       COUNT(DISTINCT e.employee_id) AS employee_count,
                       COALESCE(SUM(pr.gross_pay), 0) AS gross_pay,
                       COALESCE(SUM(pb.total_benefits), 0) AS total_benefits,
                       COALESCE(SUM(po.total_overtime), 0) AS total_overtime,
                       COALESCE(SUM(pr.total_deductions), 0) AS total_deductions,
                       COALESCE(SUM(pr.net_pay), 0) AS net_pay
                FROM payroll_result pr
                JOIN employee e ON pr.employee_id = e.employee_id
                JOIN department d ON e.department_id = d.department_id
                JOIN payrun p ON pr.pay_run_id = p.pay_run_id
                LEFT JOIN (
                    SELECT payroll_result_id, SUM(amount) AS total_benefits
                    FROM payroll_benefit GROUP BY payroll_result_id
                ) pb ON pr.payroll_result_id = pb.payroll_result_id
                LEFT JOIN (
                    SELECT payroll_result_id, SUM(amount) AS total_overtime
                    FROM payroll_overtime GROUP BY payroll_result_id
                ) po ON pr.payroll_result_id = po.payroll_result_id
                WHERE EXISTS (
                    SELECT 1 FROM attendance a
                    WHERE a.employee_id = pr.employee_id
                      AND a.work_date BETWEEN p.start_date AND p.end_date
                )
                """);

        List<Object> params = new ArrayList<>();

        if (departmentId != null) {
            sql.append(" AND d.department_id = ?");
            params.add(departmentId);
        }

        if (payrunId != null) {
            sql.append(" AND p.pay_run_id = ?");
            params.add(payrunId);
        }

        if (startDate != null && endDate != null) {
            sql.append(" AND p.start_date >= ? AND p.end_date <= ?");
            params.add(Date.valueOf(startDate));
            params.add(Date.valueOf(endDate));
        }

        sql.append(" GROUP BY d.department_name ORDER BY d.department_name");

        List<PayrollSummaryReportDTO> rows = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            setParameters(stmt, params);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapSummaryRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find payroll summary report.", e);
        }

        return rows;
    }

    private OrganizationPayrollReportDTO mapOrganizationRow(ResultSet rs) throws SQLException {
        return new OrganizationPayrollReportDTO(
                rs.getInt("employee_id"),
                rs.getString("employee_name"),
                rs.getString("position_name"),
                rs.getString("department_name"),
                rs.getString("payroll_month"),
                rs.getDate("month_start").toLocalDate(),
                rs.getDate("month_end").toLocalDate(),
                rs.getDouble("monthly_gross_pay"),
                rs.getString("sss_number"),
                rs.getString("philhealth_number"),
                rs.getString("pagibig_number"),
                rs.getString("tin_number"),
                rs.getDouble("sss_deduction"),
                rs.getDouble("philhealth_deduction"),
                rs.getDouble("pagibig_deduction"),
                rs.getDouble("withholding_tax"),
                rs.getDouble("monthly_total_deductions"),
                rs.getDouble("monthly_net_pay")
        );
    }

    private PayrollSummaryReportDTO mapSummaryRow(ResultSet rs) throws SQLException {
        return new PayrollSummaryReportDTO(
                rs.getString("department_name"),
                rs.getInt("employee_count"),
                rs.getDouble("gross_pay"),
                rs.getDouble("total_benefits"),
                rs.getDouble("total_overtime"),
                rs.getDouble("total_deductions"),
                rs.getDouble("net_pay")
        );
    }

    private void setParameters(PreparedStatement stmt, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object value = params.get(i);

            if (value instanceof Integer intValue) {
                stmt.setInt(i + 1, intValue);
            } else if (value instanceof Date dateValue) {
                stmt.setDate(i + 1, dateValue);
            } else {
                stmt.setObject(i + 1, value);
            }
        }
    }

    public PayslipReportDTO findPayslipReport(int employeeId, int payrunId) {
        String sql = """
                SELECT payroll_result_id,
                       employee_id,
                       pay_run_id,
                       employee_name,
                       position_name,
                       department_name,
                       period_start,
                       period_end,
                       pay_date,
                       monthly_salary,
                       daily_rate,
                       days_worked,
                       basic_pay,
                       overtime_pay,
                       rice_subsidy,
                       phone_allowance,
                       clothing_allowance,
                       total_benefits,
                       sss_deduction,
                       philhealth_deduction,
                       pagibig_deduction,
                       withholding_tax,
                       other_deductions,
                       gross_pay,
                       total_deductions,
                       net_pay
                FROM vw_employee_payslip_report
                WHERE employee_id = ?
                  AND pay_run_id = ?
                LIMIT 1
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.setInt(2, payrunId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find payslip report data.", e);
        }

        return null;
    }

    private PayslipReportDTO mapRow(ResultSet rs) throws SQLException {
        return new PayslipReportDTO(
                rs.getInt("payroll_result_id"),
                rs.getInt("employee_id"),
                rs.getInt("pay_run_id"),
                rs.getString("employee_name"),
                rs.getString("position_name"),
                rs.getString("department_name"),
                rs.getDate("period_start").toLocalDate(),
                rs.getDate("period_end").toLocalDate(),
                rs.getDate("pay_date").toLocalDate(),
                rs.getDouble("monthly_salary"),
                rs.getDouble("daily_rate"),
                rs.getDouble("days_worked"),
                rs.getDouble("basic_pay"),
                rs.getDouble("overtime_pay"),
                rs.getDouble("rice_subsidy"),
                rs.getDouble("phone_allowance"),
                rs.getDouble("clothing_allowance"),
                rs.getDouble("total_benefits"),
                rs.getDouble("sss_deduction"),
                rs.getDouble("philhealth_deduction"),
                rs.getDouble("pagibig_deduction"),
                rs.getDouble("withholding_tax"),
                rs.getDouble("other_deductions"),
                rs.getDouble("gross_pay"),
                rs.getDouble("total_deductions"),
                rs.getDouble("net_pay")
        );
    }
}
