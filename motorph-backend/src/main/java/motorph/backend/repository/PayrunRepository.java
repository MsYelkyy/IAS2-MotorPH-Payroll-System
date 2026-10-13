package motorph.backend.repository;

import motorph.backend.exception.DataAccessException;
import motorph.backend.domain.Payrun;
import motorph.backend.domain.value.PayrollType;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for payroll run records.
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

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                payruns.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find payruns.", e);
        }

        return payruns;
    }

    public List<Payrun> findAllWithPayrollResults() {
        List<Payrun> payruns = new ArrayList<>();

        String sql = """
                SELECT DISTINCT p.pay_run_id,
                       p.start_date,
                       p.end_date,
                       p.pay_date,
                       p.payroll_type
                FROM payrun p
                INNER JOIN payroll_result pr
                  ON pr.pay_run_id = p.pay_run_id
                ORDER BY p.start_date DESC
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                payruns.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find payruns with payroll results.", e);
        }

        return payruns;
    }

    public boolean existsByPeriod(LocalDate startDate, LocalDate endDate) {
        String sql = """
                SELECT COUNT(*)
                FROM payrun
                WHERE start_date = ? AND end_date = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to check payrun existence.", e);
        }

        return false;
    }

    public boolean existsOverlapping(LocalDate startDate, LocalDate endDate) {
        String sql = """
                SELECT COUNT(*)
                FROM payrun
                WHERE start_date <= ? AND end_date >= ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(endDate));
            stmt.setDate(2, Date.valueOf(startDate));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to check overlapping payruns.", e);
        }

        return false;
    }

    public int saveAndReturnId(Payrun payrun) {
        String sql = """
                INSERT INTO payrun (
                    start_date,
                    end_date,
                    pay_date,
                    payroll_type
                )
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setDate(1, Date.valueOf(payrun.getStartDate()));
            stmt.setDate(2, Date.valueOf(payrun.getEndDate()));
            stmt.setDate(3, Date.valueOf(payrun.getPayDate()));
            stmt.setString(4, payrun.getPayrollType().name());

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }

            throw new DataAccessException("No ID generated for payrun.");

        } catch (SQLException e) {
            throw new DataAccessException("Failed to save payrun.", e);
        }
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
