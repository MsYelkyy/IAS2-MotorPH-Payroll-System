package motorph.backend.repository;

import motorph.backend.exception.DataAccessException;
import motorph.backend.domain.Payrun;
import motorph.backend.domain.value.PayrollType;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Handles database operations for payroll run records. Read-only for the
 * payslip-viewing slice - payrun creation/generation moves in Phase 4.
 */
@Repository
public class PayrunRepository {

    private final DataSource dataSource;

    public PayrunRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Payrun findById(int payrunId) {
        String sql = """
                SELECT pay_run_id,
                       start_date,
                       end_date,
                       pay_date,
                       payroll_type
                FROM payrun
                WHERE pay_run_id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payrunId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find payrun by id.", e);
        }

        return null;
    }

    private Payrun mapRow(ResultSet rs) throws SQLException {
        return new Payrun(
                rs.getInt("pay_run_id"),
                rs.getDate("start_date").toLocalDate(),
                rs.getDate("end_date").toLocalDate(),
                rs.getDate("pay_date").toLocalDate(),
                PayrollType.fromValue(rs.getString("payroll_type"))
        );
    }
}
