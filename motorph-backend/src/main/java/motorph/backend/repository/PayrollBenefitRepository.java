package motorph.backend.repository;

import motorph.backend.exception.DataAccessException;
import motorph.backend.domain.PayrollBenefit;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for payroll benefit records.
 */
@Repository
public class PayrollBenefitRepository {

    private final DataSource dataSource;

    public PayrollBenefitRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<PayrollBenefit> findByPayrollResultId(int payrollResultId) {
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

        try (Connection conn = dataSource.getConnection();
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

    public void save(PayrollBenefit payrollBenefit) {
        String sql = """
                INSERT INTO payroll_benefit (
                    payroll_result_id,
                    benefit_type_id,
                    amount
                )
                VALUES (?, ?, ?)
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payrollBenefit.getPayrollResultId());
            stmt.setInt(2, payrollBenefit.getBenefitTypeId());
            stmt.setDouble(3, payrollBenefit.getAmount());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to save payroll benefit.", e);
        }
    }

    private PayrollBenefit mapRow(ResultSet rs) throws SQLException {
        return new PayrollBenefit(
                rs.getInt("payroll_benefit_id"),
                rs.getInt("payroll_result_id"),
                rs.getInt("benefit_type_id"),
                rs.getDouble("amount")
        );
    }
}
