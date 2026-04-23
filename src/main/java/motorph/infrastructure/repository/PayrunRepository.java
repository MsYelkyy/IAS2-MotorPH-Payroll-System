package motorph.infrastructure.repository;

import motorph.domain.model.Payrun;
import motorph.domain.value.PayrollType;
import motorph.infrastructure.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for payroll run records.
 */
public class PayrunRepository {

    // finds payrun by id
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

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payrunId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find payrun by id.", e);
        }

        return null;
    }

    // finds all payruns
    public List<Payrun> findAll() {
        List<Payrun> payruns = new ArrayList<>();

        String sql = """
                SELECT pay_run_id,
                       start_date,
                       end_date,
                       pay_date,
                       payroll_type
                FROM payrun
                ORDER BY start_date DESC
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                payruns.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find payruns.", e);
        }

        return payruns;
    }

    // finds payruns by pay date
    public List<Payrun> findByPayDate(LocalDate payDate) {
        List<Payrun> payruns = new ArrayList<>();

        String sql = """
                SELECT pay_run_id,
                       start_date,
                       end_date,
                       pay_date,
                       payroll_type
                FROM payrun
                WHERE pay_date = ?
                ORDER BY start_date DESC
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(payDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    payruns.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find payruns by pay date.", e);
        }

        return payruns;
    }

    // checks if payrun already exists for the period
    public boolean existsByPeriod(LocalDate startDate, LocalDate endDate) {
        String sql = """
                SELECT COUNT(*)
                FROM payrun
                WHERE start_date = ? AND end_date = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to check payrun existence.", e);
        }

        return false;
    }

    // saves a payrun
    public void save(Payrun payrun) {
        String sql = """
                INSERT INTO payrun (
                    start_date,
                    end_date,
                    pay_date,
                    payroll_type
                )
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(payrun.getStartDate()));
            stmt.setDate(2, Date.valueOf(payrun.getEndDate()));
            stmt.setDate(3, Date.valueOf(payrun.getPayDate()));
            stmt.setString(4, payrun.getPayrollType().name());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save payrun.", e);
        }
    }

    // maps result set to payrun
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