package motorph.infrastructure.repository;

import motorph.application.exception.DataAccessException;

import motorph.application.dto.systemconfig.SssRateDTO;
import motorph.infrastructure.config.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Manages SSS contribution rate records in the database. */
public class SssRateRepository {

    private static final String SELECT_COLS = """
            SELECT sss_rate_id, deduction_type_id,
                   min_salary, max_salary, employee_share, employer_share,
                   effective_start_date, effective_end_date
            FROM sss_contribution_rate
            """;

    // returns all SSS rates ordered by most recent effective date
    public List<SssRateDTO> findAll() {
        String sql = SELECT_COLS + "ORDER BY effective_start_date DESC";
        List<SssRateDTO> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql);
             ResultSet rs = s.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load SSS rates.", e);
        }
        return list;
    }

    // returns the single SSS rate effective on the given date
    public SssRateDTO findEffectiveOn(LocalDate date) {
        String sql = SELECT_COLS + """
                WHERE effective_start_date <= ?
                  AND (effective_end_date IS NULL OR effective_end_date >= ?)
                ORDER BY effective_start_date DESC
                LIMIT 1
                """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setDate(1, Date.valueOf(date));
            s.setDate(2, Date.valueOf(date));
            try (ResultSet rs = s.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load effective SSS rate.", e);
        }
    }

    // inserts a new SSS rate record
    public void save(SssRateDTO dto) {
        String sql = """
                INSERT INTO sss_contribution_rate
                    (deduction_type_id, min_salary, max_salary,
                     employee_share, employer_share,
                     effective_start_date, effective_end_date)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, 1);
            s.setDouble(2, dto.getMinSalary());
            s.setDouble(3, dto.getMaxSalary());
            s.setDouble(4, dto.getEmployeeShare());
            s.setDouble(5, dto.getEmployerShare());
            s.setDate(6, Date.valueOf(dto.getEffectiveStartDate()));
            setNullableDate(s, 7, dto.getEffectiveEndDate());
            s.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save SSS rate.", e);
        }
    }

    // updates an existing SSS rate record
    public void update(SssRateDTO dto) {
        String sql = """
                UPDATE sss_contribution_rate
                SET min_salary = ?, max_salary = ?,
                    employee_share = ?, employer_share = ?,
                    effective_start_date = ?, effective_end_date = ?
                WHERE sss_rate_id = ?
                """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setDouble(1, dto.getMinSalary());
            s.setDouble(2, dto.getMaxSalary());
            s.setDouble(3, dto.getEmployeeShare());
            s.setDouble(4, dto.getEmployerShare());
            s.setDate(5, Date.valueOf(dto.getEffectiveStartDate()));
            setNullableDate(s, 6, dto.getEffectiveEndDate());
            s.setInt(7, dto.getSssRateId());
            s.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update SSS rate.", e);
        }
    }

    // closes all open-ended SSS rates by setting their end date
    public void closeAllOpenEnded(LocalDate endDate) {
        String sql = "UPDATE sss_contribution_rate SET effective_end_date = ? WHERE effective_end_date IS NULL";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setDate(1, Date.valueOf(endDate));
            s.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to close open-ended SSS rates.", e);
        }
    }

    private SssRateDTO mapRow(ResultSet rs) throws SQLException {
        LocalDate end = rs.getDate("effective_end_date") != null
                ? rs.getDate("effective_end_date").toLocalDate() : null;
        return new SssRateDTO(
                rs.getInt("sss_rate_id"),
                rs.getInt("deduction_type_id"),
                rs.getDouble("min_salary"),
                rs.getDouble("max_salary"),
                rs.getDouble("employee_share"),
                rs.getDouble("employer_share"),
                rs.getDate("effective_start_date").toLocalDate(),
                end);
    }

    private void setNullableDate(PreparedStatement s, int idx, LocalDate d) throws SQLException {
        if (d != null) s.setDate(idx, Date.valueOf(d));
        else           s.setNull(idx, Types.DATE);
    }
}
