package motorph.backend.repository.report;

import motorph.backend.exception.DataAccessException;
import motorph.backend.dto.report.payroll.PayrollDashboardDTO;
import motorph.backend.dto.report.payroll.PayrollDashboardDTO.DeductionBreakdown;
import motorph.backend.dto.report.payroll.PayrollDashboardDTO.DepartmentCost;
import motorph.backend.dto.report.payroll.PayrollDashboardDTO.NetPayPoint;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Reads payroll dashboard data from the {@code vw_payroll_summary_monthly} view. */
@Repository
public class PayrollDashboardRepository {

    private final DataSource dataSource;

    public PayrollDashboardRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public PayrollDashboardDTO findMonthlyTotals(String payrollMonth) {
        String sql = """
                SELECT payroll_month_year,
                       COUNT(DISTINCT employee_id)          AS total_employees,
                       COUNT(*)                             AS row_count,
                       MAX(month_end)                       AS most_recent_end,
                       COALESCE(SUM(monthly_gross_pay), 0)  AS total_gross,
                       COALESCE(SUM(monthly_total_deductions), 0) AS total_deductions,
                       COALESCE(SUM(monthly_net_pay), 0)    AS total_net
                FROM vw_payroll_summary_monthly
                WHERE payroll_month = ?
                GROUP BY payroll_month_year
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, payrollMonth);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new PayrollDashboardDTO(
                            rs.getInt("total_employees"),
                            rs.getDouble("total_gross"),
                            rs.getDouble("total_deductions"),
                            rs.getDouble("total_net"),
                            rs.getString("payroll_month_year"),
                            rs.getDate("most_recent_end") == null ? null : rs.getDate("most_recent_end").toLocalDate(),
                            rs.getInt("row_count"),
                            List.of(),
                            new DeductionBreakdown(0, 0, 0, 0),
                            List.of()
                    );
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find monthly payroll totals.", e);
        }

        return null;
    }

    public List<NetPayPoint> findNetPayByMonth(int monthLimit) {
        String sql = """
                SELECT payroll_month,
                       MAX(payroll_month_year)           AS month_label,
                       COALESCE(SUM(monthly_net_pay), 0) AS net_pay
                FROM vw_payroll_summary_monthly
                GROUP BY payroll_month
                ORDER BY payroll_month DESC
                LIMIT ?
                """;

        List<NetPayPoint> points = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, monthLimit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    points.add(new NetPayPoint(rs.getString("month_label"), rs.getDouble("net_pay")));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find net pay by month.", e);
        }

        Collections.reverse(points);
        return points;
    }

    public DeductionBreakdown findDeductionBreakdown(String payrollMonth) {
        String sql = """
                SELECT COALESCE(SUM(sss_deduction), 0)        AS sss,
                       COALESCE(SUM(philhealth_deduction), 0) AS philhealth,
                       COALESCE(SUM(pagibig_deduction), 0)    AS pagibig,
                       COALESCE(SUM(withholding_tax), 0)      AS withholding_tax
                FROM vw_payroll_summary_monthly
                WHERE payroll_month = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, payrollMonth);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new DeductionBreakdown(
                            rs.getDouble("sss"),
                            rs.getDouble("philhealth"),
                            rs.getDouble("pagibig"),
                            rs.getDouble("withholding_tax")
                    );
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find deduction breakdown.", e);
        }

        return new DeductionBreakdown(0, 0, 0, 0);
    }

    public List<DepartmentCost> findDepartmentCosts(String payrollMonth) {
        String sql = """
                SELECT department_name,
                       COALESCE(SUM(monthly_net_pay), 0) AS net_pay
                FROM vw_payroll_summary_monthly
                WHERE payroll_month = ?
                GROUP BY department_name
                ORDER BY net_pay DESC
                """;

        List<DepartmentCost> costs = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, payrollMonth);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    costs.add(new DepartmentCost(rs.getString("department_name"), rs.getDouble("net_pay")));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find department costs.", e);
        }

        return costs;
    }

    public String findLatestPayrollMonth() {
        String sql = "SELECT MAX(payroll_month) AS latest_month FROM vw_payroll_summary_monthly";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getString("latest_month");
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find latest payroll month.", e);
        }

        return null;
    }
}
