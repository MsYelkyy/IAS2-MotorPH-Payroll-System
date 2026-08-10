package motorph.backend.repository;

import motorph.backend.exception.DataAccessException;
import motorph.backend.domain.PayrollDeduction;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for payroll deduction records.
 */
@Repository
public class PayrollDeductionRepository {

    private final DataSource dataSource;

    public PayrollDeductionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<PayrollDeduction> findByPayrollResultId(int payrollResultId) {
        List<PayrollDeduction> deductions = new ArrayList<>();

        String sql = """
                SELECT payroll_deduction_id,
                       payroll_result_id,
                       deduction_type_id,
                       amount,
                       is_taxable
                FROM payroll_deduction
                WHERE payroll_result_id = ?
                ORDER BY deduction_type_id
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payrollResultId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    deductions.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find payroll deductions by payroll result id.", e);
        }

        return deductions;
    }

    public void save(PayrollDeduction payrollDeduction) {
        String sql = """
                INSERT INTO payroll_deduction (
                    payroll_result_id,
                    deduction_type_id,
                    amount,
                    is_taxable
                )
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payrollDeduction.getPayrollResultId());
            stmt.setInt(2, payrollDeduction.getDeductionTypeId());
            stmt.setDouble(3, payrollDeduction.getAmount());
            stmt.setBoolean(4, payrollDeduction.isTaxable());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to save payroll deduction.", e);
        }
    }

    private PayrollDeduction mapRow(ResultSet rs) throws SQLException {
        return new PayrollDeduction(
                rs.getInt("payroll_deduction_id"),
                rs.getInt("payroll_result_id"),
                rs.getInt("deduction_type_id"),
                rs.getDouble("amount"),
                rs.getBoolean("is_taxable")
        );
    }
}
