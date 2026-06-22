package motorph.infrastructure.repository;

import motorph.domain.model.PayrollDeduction;
import motorph.infrastructure.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for payroll deduction records.
 */
public class PayrollDeductionRepository {

    // finds payroll deduction by id
    public PayrollDeduction findById(int payrollDeductionId) {
        validatePositive(payrollDeductionId, "Payroll deduction ID must be positive.");

        String sql = """
                SELECT payroll_deduction_id,
                       payroll_result_id,
                       deduction_type_id,
                       amount,
                       is_taxable
                FROM payroll_deduction
                WHERE payroll_deduction_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payrollDeductionId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find payroll deduction by id.", e);
        }

        return null;
    }

    // finds payroll deductions by payroll result id
    public List<PayrollDeduction> findByPayrollResultId(int payrollResultId) {
        validatePositive(payrollResultId, "Payroll result ID must be positive.");

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

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payrollResultId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    deductions.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find payroll deductions by payroll result id.", e);
        }

        return deductions;
    }

    // finds payroll deductions by deduction type id
    public List<PayrollDeduction> findByDeductionTypeId(int deductionTypeId) {
        validatePositive(deductionTypeId, "Deduction type ID must be positive.");

        List<PayrollDeduction> deductions = new ArrayList<>();

        String sql = """
                SELECT payroll_deduction_id,
                       payroll_result_id,
                       deduction_type_id,
                       amount,
                       is_taxable
                FROM payroll_deduction
                WHERE deduction_type_id = ?
                ORDER BY payroll_result_id
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, deductionTypeId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    deductions.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find payroll deductions by deduction type id.", e);
        }

        return deductions;
    }

    // finds payroll deduction by payroll result and deduction type
    public PayrollDeduction findByPayrollResultAndDeductionType(int payrollResultId, int deductionTypeId) {
        validatePositive(payrollResultId, "Payroll result ID must be positive.");
        validatePositive(deductionTypeId, "Deduction type ID must be positive.");

        String sql = """
                SELECT payroll_deduction_id,
                       payroll_result_id,
                       deduction_type_id,
                       amount,
                       is_taxable
                FROM payroll_deduction
                WHERE payroll_result_id = ? AND deduction_type_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payrollResultId);
            stmt.setInt(2, deductionTypeId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find payroll deduction by payroll result and deduction type.", e);
        }

        return null;
    }

    // checks if payroll deduction already exists
    public boolean existsByPayrollResultAndDeductionType(int payrollResultId, int deductionTypeId) {
        validatePositive(payrollResultId, "Payroll result ID must be positive.");
        validatePositive(deductionTypeId, "Deduction type ID must be positive.");

        String sql = """
                SELECT COUNT(*)
                FROM payroll_deduction
                WHERE payroll_result_id = ? AND deduction_type_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payrollResultId);
            stmt.setInt(2, deductionTypeId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to check payroll deduction existence.", e);
        }
    }

    // saves a payroll deduction
    public void save(PayrollDeduction payrollDeduction) {
        validatePayrollDeduction(payrollDeduction);

        String sql = """
                INSERT INTO payroll_deduction (
                    payroll_result_id,
                    deduction_type_id,
                    amount,
                    is_taxable
                )
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payrollDeduction.getPayrollResultId());
            stmt.setInt(2, payrollDeduction.getDeductionTypeId());
            stmt.setDouble(3, payrollDeduction.getAmount());
            stmt.setBoolean(4, payrollDeduction.isTaxable());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save payroll deduction.", e);
        }
    }

    // maps result set to payroll deduction
    private PayrollDeduction mapRow(ResultSet rs) throws SQLException {
        return new PayrollDeduction(
                rs.getInt("payroll_deduction_id"),
                rs.getInt("payroll_result_id"),
                rs.getInt("deduction_type_id"),
                rs.getDouble("amount"),
                rs.getBoolean("is_taxable")
        );
    }

    // validates payroll deduction input
    private void validatePayrollDeduction(PayrollDeduction payrollDeduction) {
        if (payrollDeduction == null) {
            throw new IllegalArgumentException("Payroll deduction is required.");
        }
    }

    // validates required positive id
    private void validatePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }
}