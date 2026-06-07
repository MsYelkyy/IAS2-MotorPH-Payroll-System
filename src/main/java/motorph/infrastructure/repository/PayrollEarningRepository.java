package motorph.infrastructure.repository;

import motorph.domain.model.PayrollEarning;
import motorph.infrastructure.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for payroll earning records.
 */
public class PayrollEarningRepository {

    // finds payroll earning by id
    public PayrollEarning findById(int payrollEarningId) {
        validatePositive(payrollEarningId, "Payroll earning ID must be positive.");

        String sql = """
                SELECT payroll_earning_id,
                       payroll_result_id,
                       earning_type_id,
                       amount
                FROM payroll_earning
                WHERE payroll_earning_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payrollEarningId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find payroll earning by id.", e);
        }

        return null;
    }

    // finds payroll earnings by payroll result id
    public List<PayrollEarning> findByPayrollResultId(int payrollResultId) {
        validatePositive(payrollResultId, "Payroll result ID must be positive.");

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

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payrollResultId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    earnings.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find payroll earnings by payroll result id.", e);
        }

        return earnings;
    }

    // finds payroll earnings by earning type id
    public List<PayrollEarning> findByEarningTypeId(int earningTypeId) {
        validatePositive(earningTypeId, "Earning type ID must be positive.");

        List<PayrollEarning> earnings = new ArrayList<>();

        String sql = """
                SELECT payroll_earning_id,
                       payroll_result_id,
                       earning_type_id,
                       amount
                FROM payroll_earning
                WHERE earning_type_id = ?
                ORDER BY payroll_result_id
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, earningTypeId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    earnings.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find payroll earnings by earning type id.", e);
        }

        return earnings;
    }

    // finds payroll earning by payroll result and earning type
    public PayrollEarning findByPayrollResultAndEarningType(int payrollResultId, int earningTypeId) {
        validatePositive(payrollResultId, "Payroll result ID must be positive.");
        validatePositive(earningTypeId, "Earning type ID must be positive.");

        String sql = """
                SELECT payroll_earning_id,
                       payroll_result_id,
                       earning_type_id,
                       amount
                FROM payroll_earning
                WHERE payroll_result_id = ? AND earning_type_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payrollResultId);
            stmt.setInt(2, earningTypeId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find payroll earning by payroll result and earning type.", e);
        }

        return null;
    }

    // checks if payroll earning already exists
    public boolean existsByPayrollResultAndEarningType(int payrollResultId, int earningTypeId) {
        validatePositive(payrollResultId, "Payroll result ID must be positive.");
        validatePositive(earningTypeId, "Earning type ID must be positive.");

        String sql = """
                SELECT COUNT(*)
                FROM payroll_earning
                WHERE payroll_result_id = ? AND earning_type_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payrollResultId);
            stmt.setInt(2, earningTypeId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to check payroll earning existence.", e);
        }
    }

    // saves a payroll earning
    public void save(PayrollEarning payrollEarning) {
        validatePayrollEarning(payrollEarning);

        String sql = """
                INSERT INTO payroll_earning (
                    payroll_result_id,
                    earning_type_id,
                    amount
                )
                VALUES (?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payrollEarning.getPayrollResultId());
            stmt.setInt(2, payrollEarning.getEarningTypeId());
            stmt.setDouble(3, payrollEarning.getAmount());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save payroll earning.", e);
        }
    }

    // maps result set to payroll earning
    private PayrollEarning mapRow(ResultSet rs) throws SQLException {
        return new PayrollEarning(
                rs.getInt("payroll_earning_id"),
                rs.getInt("payroll_result_id"),
                rs.getInt("earning_type_id"),
                rs.getDouble("amount")
        );
    }

    // validates payroll earning input
    private void validatePayrollEarning(PayrollEarning payrollEarning) {
        if (payrollEarning == null) {
            throw new IllegalArgumentException("Payroll earning is required.");
        }
    }

    // validates required positive id
    private void validatePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }
}