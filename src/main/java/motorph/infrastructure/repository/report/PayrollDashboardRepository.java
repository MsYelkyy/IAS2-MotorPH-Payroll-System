package motorph.infrastructure.repository.report;

import motorph.application.dto.report.payroll.PayrollDashboardDTO;
import motorph.application.exception.DataAccessException;
import motorph.infrastructure.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only aggregate queries for the Payroll Reporting Dashboard.
 * Every query reads from vw_payroll_summary_monthly. No writes.
 */
public class PayrollDashboardRepository {

    // returns the KPI totals for one payroll month (null when the month has no rows)
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

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, payrollMonth);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapMonthlyTotals(rs);
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to load payroll dashboard totals.", e);
        }

        return null;
    }

    // returns net pay per month for the trend line (oldest -> newest, capped)
    public List<PayrollDashboardDTO.NetPayPoint> findNetPayByMonth(int monthLimit) {
        // Latest N months first, then reversed below so the chart reads left-to-right.
        String sql = """
                SELECT payroll_month,
                       MAX(payroll_month_year)           AS month_label,
                       COALESCE(SUM(monthly_net_pay), 0) AS net_pay
                FROM vw_payroll_summary_monthly
                GROUP BY payroll_month
                ORDER BY payroll_month DESC
                LIMIT ?
                """;

        List<PayrollDashboardDTO.NetPayPoint> points = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, monthLimit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    points.add(new PayrollDashboardDTO.NetPayPoint(
                            rs.getString("month_label"),
                            rs.getDouble("net_pay")));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to load net pay by month.", e);
        }

        // flip to chronological order for plotting
        List<PayrollDashboardDTO.NetPayPoint> chronological = new ArrayList<>(points);
        java.util.Collections.reverse(chronological);
        return chronological;
    }

    // returns the deduction totals for one payroll month (donut chart)
    public PayrollDashboardDTO.DeductionBreakdown findDeductionBreakdown(String payrollMonth) {
        String sql = """
                SELECT COALESCE(SUM(sss_deduction), 0)        AS sss,
                       COALESCE(SUM(philhealth_deduction), 0) AS philhealth,
                       COALESCE(SUM(pagibig_deduction), 0)    AS pagibig,
                       COALESCE(SUM(withholding_tax), 0)      AS withholding_tax
                FROM vw_payroll_summary_monthly
                WHERE payroll_month = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, payrollMonth);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new PayrollDashboardDTO.DeductionBreakdown(
                            rs.getDouble("sss"),
                            rs.getDouble("philhealth"),
                            rs.getDouble("pagibig"),
                            rs.getDouble("withholding_tax"));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to load deduction breakdown.", e);
        }

        return new PayrollDashboardDTO.DeductionBreakdown(0, 0, 0, 0);
    }

    // returns net pay per department for one payroll month (bar chart)
    public List<PayrollDashboardDTO.DepartmentCost> findDepartmentCosts(String payrollMonth) {
        String sql = """
                SELECT department_name,
                       COALESCE(SUM(monthly_net_pay), 0) AS net_pay
                FROM vw_payroll_summary_monthly
                WHERE payroll_month = ?
                GROUP BY department_name
                ORDER BY net_pay DESC
                """;

        List<PayrollDashboardDTO.DepartmentCost> costs = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, payrollMonth);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    costs.add(new PayrollDashboardDTO.DepartmentCost(
                            rs.getString("department_name"),
                            rs.getDouble("net_pay")));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to load payroll cost by department.", e);
        }

        return costs;
    }

    // returns the most recent payroll month present in the view (e.g. "2024-06")
    public String findLatestPayrollMonth() {
        String sql = "SELECT MAX(payroll_month) AS latest_month FROM vw_payroll_summary_monthly";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getString("latest_month");
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to load latest payroll month.", e);
        }

        return null;
    }

    // maps the KPI totals row (charts are attached later by the service)
    private PayrollDashboardDTO mapMonthlyTotals(ResultSet rs) throws SQLException {
        java.sql.Date recentEnd = rs.getDate("most_recent_end");
        LocalDate mostRecentPayDate = (recentEnd == null) ? null : recentEnd.toLocalDate();

        return new PayrollDashboardDTO(
                rs.getInt("total_employees"),
                rs.getDouble("total_gross"),
                rs.getDouble("total_deductions"),
                rs.getDouble("total_net"),
                rs.getString("payroll_month_year"),
                mostRecentPayDate,
                rs.getInt("row_count"),
                List.of(),
                new PayrollDashboardDTO.DeductionBreakdown(0, 0, 0, 0),
                List.of());
    }
}
