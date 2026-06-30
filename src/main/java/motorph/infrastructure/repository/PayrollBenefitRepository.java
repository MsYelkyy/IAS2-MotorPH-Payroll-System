package motorph.infrastructure.repository;

import motorph.application.exception.DataAccessException;

import motorph.domain.model.PayrollBenefit;
import motorph.infrastructure.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for payroll benefit records.
 */
public class PayrollBenefitRepository {

    // finds payroll benefit by id
    public PayrollBenefit findById(int payrollBenefitId) {
        validatePositive(payrollBenefitId, "Payroll benefit ID must be positive.");

        String sql = """
                SELECT payroll_benefit_id,
                       payroll_result_id,
                       benefit_type_id,
                       amount
                FROM payroll_benefit
                WHERE payroll_benefit_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payrollBenefitId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find payroll benefit by id.", e);
        }

        return null;
    }

    // finds payroll benefits by payroll result id
    public List<PayrollBenefit> findByPayrollResultId(int payrollResultId) {
        validatePositive(payrollResultId, "Payroll result ID must be positive.");

        List<PayrollBenefit> benefits = new ArrayList<>();

        String sql = """
                SELECT payroll_benefit_id,
                       payroll_result_id,
                       benefit_type_id,
                       amount
                FROM payroll_benefit
                WHERE payroll_result_id = ?
                ORDER BY benefit_type_id
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payrollResultId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    benefits.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find payroll benefits by payroll result id.", e);
        }

        return benefits;
    }

    // finds payroll benefits by benefit type id
    public List<PayrollBenefit> findByBenefitTypeId(int benefitTypeId) {
        validatePositive(benefitTypeId, "Benefit type ID must be positive.");

        List<PayrollBenefit> benefits = new ArrayList<>();

        String sql = """
                SELECT payroll_benefit_id,
                       payroll_result_id,
                       benefit_type_id,
                       amount
                FROM payroll_benefit
                WHERE benefit_type_id = ?
                ORDER BY payroll_result_id
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, benefitTypeId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    benefits.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find payroll benefits by benefit type id.", e);
        }

        return benefits;
    }

    // finds payroll benefit by payroll result and benefit type
    public PayrollBenefit findByPayrollResultAndBenefitType(int payrollResultId, int benefitTypeId) {
        validatePositive(payrollResultId, "Payroll result ID must be positive.");
        validatePositive(benefitTypeId, "Benefit type ID must be positive.");

        String sql = """
                SELECT payroll_benefit_id,
                       payroll_result_id,
                       benefit_type_id,
                       amount
                FROM payroll_benefit
                WHERE payroll_result_id = ? AND benefit_type_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payrollResultId);
            stmt.setInt(2, benefitTypeId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find payroll benefit by payroll result and benefit type.", e);
        }

        return null;
    }

    // checks if payroll benefit already exists
    public boolean existsByPayrollResultAndBenefitType(int payrollResultId, int benefitTypeId) {
        validatePositive(payrollResultId, "Payroll result ID must be positive.");
        validatePositive(benefitTypeId, "Benefit type ID must be positive.");

        String sql = """
                SELECT COUNT(*)
                FROM payroll_benefit
                WHERE payroll_result_id = ? AND benefit_type_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payrollResultId);
            stmt.setInt(2, benefitTypeId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to check payroll benefit existence.", e);
        }
    }

    // saves a payroll benefit
    public void save(PayrollBenefit payrollBenefit) {
        validatePayrollBenefit(payrollBenefit);

        String sql = """
                INSERT INTO payroll_benefit (
                    payroll_result_id,
                    benefit_type_id,
                    amount
                )
                VALUES (?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payrollBenefit.getPayrollResultId());
            stmt.setInt(2, payrollBenefit.getBenefitTypeId());
            stmt.setDouble(3, payrollBenefit.getAmount());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to save payroll benefit.", e);
        }
    }

    // maps result set to payroll benefit
    private PayrollBenefit mapRow(ResultSet rs) throws SQLException {
        return new PayrollBenefit(
                rs.getInt("payroll_benefit_id"),
                rs.getInt("payroll_result_id"),
                rs.getInt("benefit_type_id"),
                rs.getDouble("amount")
        );
    }

    // validates payroll benefit input
    private void validatePayrollBenefit(PayrollBenefit payrollBenefit) {
        if (payrollBenefit == null) {
            throw new IllegalArgumentException("Payroll benefit is required.");
        }
    }

    // validates required positive id
    private void validatePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }
}