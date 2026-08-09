package motorph.backend.repository;

import motorph.backend.exception.DataAccessException;
import motorph.backend.domain.PayrollResult;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for payroll result records. Read-only for the
 * payslip-viewing slice - payroll computation/persistence moves in Phase 4.
 */
@Repository
public class PayrollResultRepository {

    private final DataSource dataSource;

    public PayrollResultRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

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

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find payroll results by employee id.", e);
        }

        return results;
    }

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
            throw new DataAccessException("Failed to find payroll result by employee and payrun.", e);
        }

        return null;
    }

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
}
