package motorph.infrastructure.repository;

import motorph.application.dto.systemconfig.PagIbigRateDTO;
import motorph.infrastructure.config.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PagIbigRateRepository {

    private static final String SELECT_COLS = """
            SELECT pagibig_contribution_rate_id, deduction_type_id,
                   min_salary, max_salary,
                   employee_share_rate, employer_share_rate,
                   max_employee_share, max_employer_share,
                   effective_start_date, effective_end_date
            FROM pagibig_contribution_rate
            """;

    public List<PagIbigRateDTO> findAll() {
        String sql = SELECT_COLS + "ORDER BY effective_start_date DESC";
        List<PagIbigRateDTO> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql);
             ResultSet rs = s.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load Pag-IBIG rates.", e);
        }
        return list;
    }

    public void save(PagIbigRateDTO dto) {
        String sql = """
                INSERT INTO pagibig_contribution_rate
                    (deduction_type_id, min_salary, max_salary,
                     employee_share_rate, employer_share_rate,
                     max_employee_share, max_employer_share,
                     effective_start_date, effective_end_date)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, 3);
            s.setDouble(2, dto.getMinSalary());
            s.setDouble(3, dto.getMaxSalary());
            s.setDouble(4, dto.getEmployeeShareRate());
            s.setDouble(5, dto.getEmployerShareRate());
            setNullableDouble(s, 6, dto.getMaxEmployeeShare());
            setNullableDouble(s, 7, dto.getMaxEmployerShare());
            s.setDate(8, Date.valueOf(dto.getEffectiveStartDate()));
            setNullableDate(s, 9, dto.getEffectiveEndDate());
            s.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save Pag-IBIG rate.", e);
        }
    }

    public void update(PagIbigRateDTO dto) {
        String sql = """
                UPDATE pagibig_contribution_rate
                SET min_salary = ?, max_salary = ?,
                    employee_share_rate = ?, employer_share_rate = ?,
                    max_employee_share = ?, max_employer_share = ?,
                    effective_start_date = ?, effective_end_date = ?
                WHERE pagibig_contribution_rate_id = ?
                """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setDouble(1, dto.getMinSalary());
            s.setDouble(2, dto.getMaxSalary());
            s.setDouble(3, dto.getEmployeeShareRate());
            s.setDouble(4, dto.getEmployerShareRate());
            setNullableDouble(s, 5, dto.getMaxEmployeeShare());
            setNullableDouble(s, 6, dto.getMaxEmployerShare());
            s.setDate(7, Date.valueOf(dto.getEffectiveStartDate()));
            setNullableDate(s, 8, dto.getEffectiveEndDate());
            s.setInt(9, dto.getPagibigRateId());
            s.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update Pag-IBIG rate.", e);
        }
    }

    public void closeAllOpenEnded(LocalDate endDate) {
        String sql = "UPDATE pagibig_contribution_rate SET effective_end_date = ? WHERE effective_end_date IS NULL";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setDate(1, Date.valueOf(endDate));
            s.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to close open-ended Pag-IBIG rates.", e);
        }
    }

    private PagIbigRateDTO mapRow(ResultSet rs) throws SQLException {
        double maxEmp   = rs.getDouble("max_employee_share");
        Double maxEmpV  = rs.wasNull() ? null : maxEmp;
        double maxEmpl  = rs.getDouble("max_employer_share");
        Double maxEmplV = rs.wasNull() ? null : maxEmpl;
        LocalDate end   = rs.getDate("effective_end_date") != null
                ? rs.getDate("effective_end_date").toLocalDate() : null;
        return new PagIbigRateDTO(
                rs.getInt("pagibig_contribution_rate_id"),
                rs.getInt("deduction_type_id"),
                rs.getDouble("min_salary"),
                rs.getDouble("max_salary"),
                rs.getDouble("employee_share_rate"),
                rs.getDouble("employer_share_rate"),
                maxEmpV, maxEmplV,
                rs.getDate("effective_start_date").toLocalDate(),
                end);
    }

    private void setNullableDate(PreparedStatement s, int idx, LocalDate d) throws SQLException {
        if (d != null) s.setDate(idx, Date.valueOf(d));
        else           s.setNull(idx, Types.DATE);
    }

    private void setNullableDouble(PreparedStatement s, int idx, Double d) throws SQLException {
        if (d != null) s.setDouble(idx, d);
        else           s.setNull(idx, Types.DECIMAL);
    }
}
