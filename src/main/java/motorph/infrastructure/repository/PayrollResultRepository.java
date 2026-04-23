package motorph.infrastructure.repository;

import motorph.domain.model.PayrollResult;
import motorph.infrastructure.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for payroll result records.
 */
public class PayrollResultRepository {

    // finds payroll result by id
    public PayrollResult findById(int payrollResultId) {
        String sql = """
                SELECT payroll_result_id,
                       employee_id,
                       pay_run_id,
                       total_hours_worked,
                       gross_pay,
                       total_deductions,
                       net_pay,
                       generated_at
                FROM payroll_result
                WHERE payroll_result_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payrollResultId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find payroll result by id.", e);
        }

        return null;
    }

    // finds payroll results by employee id
    public List<PayrollResult> findByEmployeeId(int employeeId) {
        List<PayrollResult> results = new ArrayList<>();

        String sql = """
                SELECT payroll_result_id,
                       employee_id,
                       pay_run_id,
                       total_hours_worked,
                       gross_pay,
                       total_deductions,
                       net_pay,
                       generated_at
                FROM payroll_result
                WHERE employee_id = ?
                ORDER BY generated_at DESC
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find payroll results by employee id.", e);
        }

        return results;
    }

    // finds payroll results by payrun id
    public List<PayrollResult> findByPayrunId(int payrunId) {
        List<PayrollResult> results = new ArrayList<>();

        String sql = """
                SELECT payroll_result_id,
                       employee_id,
                       pay_run_id,
                       total_hours_worked,
                       gross_pay,
                       total_deductions,
                       net_pay,
                       generated_at
                FROM payroll_result
                WHERE pay_run_id = ?
                ORDER BY employee_id
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payrunId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find payroll results by payrun id.", e);
        }

        return results;
    }

    // finds payroll result by employee and payrun
    public PayrollResult findByEmployeeAndPayrunId(int employeeId, int payrunId) {
        String sql = """
                SELECT payroll_result_id,
                       employee_id,
                       pay_run_id,
                       total_hours_worked,
                       gross_pay,
                       total_deductions,
                       net_pay,
                       generated_at
                FROM payroll_result
                WHERE employee_id = ? AND pay_run_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.setInt(2, payrunId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find payroll result by employee and payrun.", e);
        }

        return null;
    }

    // checks if payroll result already exists
    public boolean existsByEmployeeAndPayrunId(int employeeId, int payrunId) {
        String sql = """
                SELECT COUNT(*)
                FROM payroll_result
                WHERE employee_id = ? AND pay_run_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.setInt(2, payrunId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to check payroll result existence.", e);
        }

        return false;
    }

    // saves a payroll result
    public void save(PayrollResult payrollResult) {
        String sql = """
                INSERT INTO payroll_result (
                    employee_id,
                    pay_run_id,
                    total_hours_worked,
                    gross_pay,
                    total_deductions,
                    net_pay,
                    generated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payrollResult.getEmployeeId());
            stmt.setInt(2, payrollResult.getPayrunId());
            stmt.setDouble(3, payrollResult.getTotalHoursWorked());
            stmt.setDouble(4, payrollResult.getGrossPay());
            stmt.setDouble(5, payrollResult.getTotalDeductions());
            stmt.setDouble(6, payrollResult.getNetPay());
            setNullableTimestamp(stmt, 7, payrollResult.getGeneratedAt());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save payroll result.", e);
        }
    }

    // maps result set to payroll result
    private PayrollResult mapRow(ResultSet rs) throws SQLException {
        Timestamp generatedAtValue = rs.getTimestamp("generated_at");
        LocalDateTime generatedAt = generatedAtValue == null ? null : generatedAtValue.toLocalDateTime();

        return new PayrollResult(
                rs.getInt("payroll_result_id"),
                rs.getInt("employee_id"),
                rs.getInt("pay_run_id"),
                rs.getDouble("total_hours_worked"),
                rs.getDouble("gross_pay"),
                rs.getDouble("total_deductions"),
                rs.getDouble("net_pay"),
                generatedAt
        );
    }

    // sets a nullable timestamp
    private void setNullableTimestamp(PreparedStatement stmt, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.TIMESTAMP);
            return;
        }

        stmt.setTimestamp(index, Timestamp.valueOf(value));
    }
}