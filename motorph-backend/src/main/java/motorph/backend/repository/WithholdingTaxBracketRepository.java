package motorph.backend.repository;

import motorph.backend.exception.DataAccessException;

import motorph.backend.dto.systemconfig.WithholdingTaxBracketDTO;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages withholding tax bracket records. findEffectiveOn is not ported -
 * it's only used by PayrollCalculator, out of scope until Phase 4.
 */
@Repository
public class WithholdingTaxBracketRepository {

    private static final String SELECT_COLS = """
            SELECT tax_bracket_id, deduction_type_id,
                   min_salary, max_salary, base_tax, tax_rate, excess_over,
                   effective_start_date, effective_end_date
            FROM withholding_tax_bracket
            """;

    private final DataSource dataSource;

    public WithholdingTaxBracketRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<WithholdingTaxBracketDTO> findAll() {
        String sql = SELECT_COLS + "ORDER BY effective_start_date DESC, min_salary ASC";
        List<WithholdingTaxBracketDTO> list = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement s = c.prepareStatement(sql);
             ResultSet rs = s.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load tax brackets.", e);
        }
        return list;
    }

    public List<WithholdingTaxBracketDTO> findEffectiveOn(LocalDate date) {
        String sql = SELECT_COLS + """
                WHERE effective_start_date <= ?
                  AND (effective_end_date IS NULL OR effective_end_date >= ?)
                ORDER BY min_salary ASC
                """;
        List<WithholdingTaxBracketDTO> list = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setDate(1, Date.valueOf(date));
            s.setDate(2, Date.valueOf(date));
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load effective tax brackets.", e);
        }
        return list;
    }

    public void save(WithholdingTaxBracketDTO dto) {
        String sql = """
                INSERT INTO withholding_tax_bracket
                    (deduction_type_id, min_salary, max_salary,
                     base_tax, tax_rate, excess_over,
                     effective_start_date, effective_end_date)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = dataSource.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, 4);
            s.setDouble(2, dto.getMinSalary());
            s.setDouble(3, dto.getMaxSalary());
            s.setDouble(4, dto.getBaseTax());
            s.setDouble(5, dto.getTaxRate());
            s.setDouble(6, dto.getExcessOver());
            s.setDate(7, Date.valueOf(dto.getEffectiveStartDate()));
            setNullableDate(s, 8, dto.getEffectiveEndDate());
            s.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save tax bracket.", e);
        }
    }

    public void update(WithholdingTaxBracketDTO dto) {
        String sql = """
                UPDATE withholding_tax_bracket
                SET min_salary = ?, max_salary = ?,
                    base_tax = ?, tax_rate = ?, excess_over = ?,
                    effective_start_date = ?, effective_end_date = ?
                WHERE tax_bracket_id = ?
                """;
        try (Connection c = dataSource.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setDouble(1, dto.getMinSalary());
            s.setDouble(2, dto.getMaxSalary());
            s.setDouble(3, dto.getBaseTax());
            s.setDouble(4, dto.getTaxRate());
            s.setDouble(5, dto.getExcessOver());
            s.setDate(6, Date.valueOf(dto.getEffectiveStartDate()));
            setNullableDate(s, 7, dto.getEffectiveEndDate());
            s.setInt(8, dto.getTaxBracketId());
            s.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update tax bracket.", e);
        }
    }

    public void closeAllOpenEnded(LocalDate endDate) {
        String sql = "UPDATE withholding_tax_bracket SET effective_end_date = ? WHERE effective_end_date IS NULL";
        try (Connection c = dataSource.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setDate(1, Date.valueOf(endDate));
            s.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to close open-ended tax brackets.", e);
        }
    }

    private WithholdingTaxBracketDTO mapRow(ResultSet rs) throws SQLException {
        LocalDate end = rs.getDate("effective_end_date") != null
                ? rs.getDate("effective_end_date").toLocalDate() : null;
        return new WithholdingTaxBracketDTO(
                rs.getInt("tax_bracket_id"),
                rs.getInt("deduction_type_id"),
                rs.getDouble("min_salary"),
                rs.getDouble("max_salary"),
                rs.getDouble("base_tax"),
                rs.getDouble("tax_rate"),
                rs.getDouble("excess_over"),
                rs.getDate("effective_start_date").toLocalDate(),
                end);
    }

    private void setNullableDate(PreparedStatement s, int idx, LocalDate d) throws SQLException {
        if (d != null) s.setDate(idx, Date.valueOf(d));
        else           s.setNull(idx, Types.DATE);
    }
}
