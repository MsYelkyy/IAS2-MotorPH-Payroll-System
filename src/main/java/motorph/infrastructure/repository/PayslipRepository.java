package motorph.infrastructure.repository;

import motorph.domain.model.Payslip;
import motorph.infrastructure.config.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for payslip records.
 */
public class PayslipRepository {

    // save payslip
    public void save(Payslip p) {
        String sql = "INSERT INTO payslip " +
                "(pay_run_id, employee_id, basic_salary, overtime_pay, allowances_total, deductions_total, gross_pay, net_pay, generated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, p.getPayRunId());
            stmt.setInt(2, p.getEmployeeId());
            stmt.setDouble(3, p.getBasicPay());
            stmt.setDouble(4, p.getOvertimePay());
            stmt.setDouble(5, p.getAllowancesTotal());
            stmt.setDouble(6, p.getDeductionsTotal());
            stmt.setDouble(7, p.getGrossPay());
            stmt.setDouble(8, p.getNetPay());
            stmt.setTimestamp(9, Timestamp.valueOf(p.getGeneratedAt()));

            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // find payslips by employee
    public List<Payslip> findByEmployeeId(int employeeId) {
        List<Payslip> list = new ArrayList<>();
        String sql = "SELECT * FROM payslip WHERE employee_id = ? ORDER BY generated_at DESC";

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

    // find by pay run
    public List<Payslip> findByPayRunId(int payRunId) {
        List<Payslip> list = new ArrayList<>();
        String sql = "SELECT * FROM payslip WHERE pay_run_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payRunId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // find by employee and date range
    public List<Payslip> findByEmployeeAndDate(int employeeId, Timestamp start, Timestamp end) {
        List<Payslip> list = new ArrayList<>();
        String sql = "SELECT * FROM payslip WHERE employee_id = ? AND generated_at BETWEEN ? AND ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.setTimestamp(2, start);
            stmt.setTimestamp(3, end);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // mapper
    private Payslip mapRow(ResultSet rs) throws SQLException {
        return new Payslip(
                rs.getInt("payslip_id"),
                rs.getInt("pay_run_id"),
                rs.getInt("employee_id"),
                rs.getDouble("basic_salary"),
                rs.getDouble("overtime_pay"),
                rs.getDouble("allowances_total"),
                rs.getDouble("deductions_total"),
                rs.getDouble("gross_pay"),
                rs.getDouble("net_pay"),
                rs.getTimestamp("generated_at").toLocalDateTime()
        );
    }
}