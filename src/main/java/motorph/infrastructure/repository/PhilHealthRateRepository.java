package motorph.infrastructure.repository;

import motorph.application.dto.systemconfig.PhilHealthRateDTO;
import motorph.infrastructure.config.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PhilHealthRateRepository {

    private static final String SELECT_COLS = """
            SELECT philhealth_rate_id, deduction_type_id, premium_rate,
                   salary_floor, salary_ceiling,
                   employee_share_ratio, employer_share_ratio,
                   effective_start_date, effective_end_date
            FROM philhealth_contribution_rate
            """;

    public List<PhilHealthRateDTO> findAll() {
        String sql = SELECT_COLS + "ORDER BY effective_start_date DESC";
        List<PhilHealthRateDTO> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql);
             ResultSet rs = s.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load PhilHealth rates.", e);
        }
        return list;
    }

    public void save(PhilHealthRateDTO dto) {
        String sql = """
                INSERT INTO philhealth_contribution_rate
                    (deduction_type_id, premium_rate, salary_floor, salary_ceiling,
                     employee_share_ratio, employer_share_ratio,
                     effective_start_date, effective_end_date)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, 2);
            s.setDouble(2, dto.getPremiumRate());
            s.setDouble(3, dto.getSalaryFloor());
            s.setDouble(4, dto.getSalaryCeiling());
            s.setDouble(5, dto.getEmployeeShareRatio());
            s.setDouble(6, dto.getEmployerShareRatio());
            s.setDate(7, Date.valueOf(dto.getEffectiveStartDate()));
            setNullableDate(s, 8, dto.getEffectiveEndDate());
            s.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save PhilHealth rate.", e);
        }
    }

    public void update(PhilHealthRateDTO dto) {
        String sql = """
                UPDATE philhealth_contribution_rate
                SET premium_rate = ?, salary_floor = ?, salary_ceiling = ?,
                    employee_share_ratio = ?, employer_share_ratio = ?,
                    effective_start_date = ?, effective_end_date = ?
                WHERE philhealth_rate_id = ?
                """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setDouble(1, dto.getPremiumRate());
            s.setDouble(2, dto.getSalaryFloor());
            s.setDouble(3, dto.getSalaryCeiling());
            s.setDouble(4, dto.getEmployeeShareRatio());
            s.setDouble(5, dto.getEmployerShareRatio());
            s.setDate(6, Date.valueOf(dto.getEffectiveStartDate()));
            setNullableDate(s, 7, dto.getEffectiveEndDate());
            s.setInt(8, dto.getPhilhealthRateId());
            s.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update PhilHealth rate.", e);
        }
    }

    public void closeAllOpenEnded(LocalDate endDate) {
        String sql = """
                UPDATE philhealth_contribution_rate
                SET effective_end_date = ?
                WHERE effective_end_date IS NULL
                """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setDate(1, Date.valueOf(endDate));
            s.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to close open-ended PhilHealth rates.", e);
        }
    }

    private PhilHealthRateDTO mapRow(ResultSet rs) throws SQLException {
        LocalDate end = rs.getDate("effective_end_date") != null
                ? rs.getDate("effective_end_date").toLocalDate() : null;
        return new PhilHealthRateDTO(
                rs.getInt("philhealth_rate_id"),
                rs.getInt("deduction_type_id"),
                rs.getDouble("premium_rate"),
                rs.getDouble("salary_floor"),
                rs.getDouble("salary_ceiling"),
                rs.getDouble("employee_share_ratio"),
                rs.getDouble("employer_share_ratio"),
                rs.getDate("effective_start_date").toLocalDate(),
                end);
    }

    private void setNullableDate(PreparedStatement s, int idx, LocalDate d) throws SQLException {
        if (d != null) s.setDate(idx, Date.valueOf(d));
        else           s.setNull(idx, Types.DATE);
    }
}
