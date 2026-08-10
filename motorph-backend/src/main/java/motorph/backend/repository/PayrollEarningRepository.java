package motorph.backend.repository;

import motorph.backend.exception.DataAccessException;
import motorph.backend.domain.PayrollEarning;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for payroll earning records.
 */
@Repository
public class PayrollEarningRepository {

    private final DataSource dataSource;

    public PayrollEarningRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<PayrollEarning> findByPayrollResultId(int payrollResultId) {
        List<PayrollEarning> earnings = new ArrayList<>();

        String sql = """
                SELECT payroll_earning_id,
                       payroll_result_id,
                       earning_type_id,
                       amount
                FROM payroll_earning
                WHERE payroll_result_id = ?
                ORDER BY earning_type_id
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payrollResultId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    earnings.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find payroll earnings by payroll result id.", e);
        }

        return earnings;
    }

    public void save(PayrollEarning payrollEarning) {
        String sql = """
                INSERT INTO payroll_earning (
                    payroll_result_id,
                    earning_type_id,
                    amount
                )
                VALUES (?, ?, ?)
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payrollEarning.getPayrollResultId());
            stmt.setInt(2, payrollEarning.getEarningTypeId());
            stmt.setDouble(3, payrollEarning.getAmount());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to save payroll earning.", e);
        }
    }

    private PayrollEarning mapRow(ResultSet rs) throws SQLException {
        return new PayrollEarning(
                rs.getInt("payroll_earning_id"),
                rs.getInt("payroll_result_id"),
                rs.getInt("earning_type_id"),
                rs.getDouble("amount")
        );
    }
}
