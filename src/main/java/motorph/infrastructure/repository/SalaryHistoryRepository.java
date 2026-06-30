package motorph.infrastructure.repository;

import motorph.application.exception.DataAccessException;

import motorph.domain.model.SalaryHistory;
import motorph.domain.value.SalaryType;
import motorph.infrastructure.config.DatabaseConnection;

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
 * Handles database operations for salary history records.
 */
public class SalaryHistoryRepository {

    // finds salary history by id
    public SalaryHistory findById(int salaryHistoryId) {
        String sql = """
                SELECT salary_history_id,
                       employee_id,
                       salary_type,
                       basic_salary,
                       hourly_rate,
                       start_date,
                       end_date
                FROM salary_history
                WHERE salary_history_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, salaryHistoryId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find salary history by id.", e);
        }

        return null;
    }

    // finds salary history by employee id
    public List<SalaryHistory> findByEmployeeId(int employeeId) {
        List<SalaryHistory> histories = new ArrayList<>();

        String sql = """
                SELECT salary_history_id,
                       employee_id,
                       salary_type,
                       basic_salary,
                       hourly_rate,
                       start_date,
                       end_date
                FROM salary_history
                WHERE employee_id = ?
                ORDER BY start_date DESC
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    histories.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find salary history by employee id.", e);
        }

        return histories;
    }

    // finds current salary by employee id
    public SalaryHistory findCurrentByEmployeeId(int employeeId, LocalDate effectiveDate) {
        String sql = """
                SELECT salary_history_id,
                       employee_id,
                       salary_type,
                       basic_salary,
                       hourly_rate,
                       start_date,
                       end_date
                FROM salary_history
                WHERE employee_id = ?
                  AND start_date <= ?
                  AND (end_date IS NULL OR end_date >= ?)
                ORDER BY start_date DESC
                LIMIT 1
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.setDate(2, Date.valueOf(effectiveDate));
            stmt.setDate(3, Date.valueOf(effectiveDate));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find current salary history.", e);
        }

        return null;
    }

    // saves salary history
    public void save(SalaryHistory salaryHistory) {
        String sql = """
                INSERT INTO salary_history (
                    employee_id,
                    salary_type,
                    basic_salary,
                    hourly_rate,
                    start_date,
                    end_date
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, salaryHistory.getEmployeeId());
            stmt.setString(2, salaryHistory.getSalaryType().name());
            setNullableDouble(stmt, 3, salaryHistory.getBasicSalary());
            setNullableDouble(stmt, 4, salaryHistory.getHourlyRate());
            stmt.setDate(5, Date.valueOf(salaryHistory.getStartDate()));
            setNullableDate(stmt, 6, salaryHistory.getEndDate());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to save salary history.", e);
        }
    }

    // updates salary history
    public void update(SalaryHistory salaryHistory) {
        String sql = """
                UPDATE salary_history
                SET employee_id = ?,
                    salary_type = ?,
                    basic_salary = ?,
                    hourly_rate = ?,
                    start_date = ?,
                    end_date = ?
                WHERE salary_history_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, salaryHistory.getEmployeeId());
            stmt.setString(2, salaryHistory.getSalaryType().name());
            setNullableDouble(stmt, 3, salaryHistory.getBasicSalary());
            setNullableDouble(stmt, 4, salaryHistory.getHourlyRate());
            stmt.setDate(5, Date.valueOf(salaryHistory.getStartDate()));
            setNullableDate(stmt, 6, salaryHistory.getEndDate());
            stmt.setInt(7, salaryHistory.getSalaryHistoryId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to update salary history.", e);
        }
    }

    // maps result set to salary history
    private SalaryHistory mapRow(ResultSet rs) throws SQLException {
        Date endDateValue = rs.getDate("end_date");

        return new SalaryHistory(
                rs.getInt("salary_history_id"),
                rs.getInt("employee_id"),
                SalaryType.fromValue(rs.getString("salary_type")),
                getNullableDouble(rs, "basic_salary"),
                getNullableDouble(rs, "hourly_rate"),
                rs.getDate("start_date").toLocalDate(),
                endDateValue == null ? null : endDateValue.toLocalDate()
        );
    }

    // sets a nullable double value
    private void setNullableDouble(PreparedStatement stmt, int index, Double value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.DECIMAL);
        } else {
            stmt.setDouble(index, value);
        }
    }

    // gets a nullable double value
    private Double getNullableDouble(ResultSet rs, String columnName) throws SQLException {
        double value = rs.getDouble(columnName);
        return rs.wasNull() ? null : value;
    }

    // sets a nullable date value
    private void setNullableDate(PreparedStatement stmt, int index, LocalDate value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.DATE);
        } else {
            stmt.setDate(index, Date.valueOf(value));
        }
    }
}