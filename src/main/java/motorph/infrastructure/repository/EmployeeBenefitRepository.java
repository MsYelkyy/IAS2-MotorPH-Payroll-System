package motorph.infrastructure.repository;

import motorph.application.exception.DataAccessException;

import motorph.domain.model.EmployeeBenefit;
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
 * Handles database operations for employee benefit records.
 */
public class EmployeeBenefitRepository {

    // finds employee benefit by id
    public EmployeeBenefit findById(int employeeBenefitId) {
        String sql = """
                SELECT employee_benefit_id,
                       employee_id,
                       benefit_type_id,
                       amount,
                       effective_start_date,
                       effective_end_date
                FROM employee_benefit
                WHERE employee_benefit_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeBenefitId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find employee benefit by id.", e);
        }

        return null;
    }

    // finds employee benefits by employee id
    public List<EmployeeBenefit> findByEmployeeId(int employeeId) {
        List<EmployeeBenefit> benefits = new ArrayList<>();

        String sql = """
                SELECT employee_benefit_id,
                       employee_id,
                       benefit_type_id,
                       amount,
                       effective_start_date,
                       effective_end_date
                FROM employee_benefit
                WHERE employee_id = ?
                ORDER BY effective_start_date DESC
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    benefits.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find employee benefits by employee id.", e);
        }

        return benefits;
    }

    // finds active employee benefits by employee id
    public List<EmployeeBenefit> findActiveByEmployeeId(int employeeId, LocalDate effectiveDate) {
        List<EmployeeBenefit> benefits = new ArrayList<>();

        String sql = """
                SELECT employee_benefit_id,
                       employee_id,
                       benefit_type_id,
                       amount,
                       effective_start_date,
                       effective_end_date
                FROM employee_benefit
                WHERE employee_id = ?
                  AND effective_start_date <= ?
                  AND (effective_end_date IS NULL OR effective_end_date >= ?)
                ORDER BY effective_start_date DESC
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.setDate(2, Date.valueOf(effectiveDate));
            stmt.setDate(3, Date.valueOf(effectiveDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    benefits.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find active employee benefits.", e);
        }

        return benefits;
    }

    // saves employee benefit
    public void save(EmployeeBenefit employeeBenefit) {
        String sql = """
                INSERT INTO employee_benefit (
                    employee_id,
                    benefit_type_id,
                    amount,
                    effective_start_date,
                    effective_end_date
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeBenefit.getEmployeeId());
            stmt.setInt(2, employeeBenefit.getBenefitTypeId());
            stmt.setDouble(3, employeeBenefit.getAmount());
            stmt.setDate(4, Date.valueOf(employeeBenefit.getEffectiveStartDate()));

            if (employeeBenefit.getEffectiveEndDate() == null) {
                stmt.setNull(5, java.sql.Types.DATE);
            } else {
                stmt.setDate(5, Date.valueOf(employeeBenefit.getEffectiveEndDate()));
            }

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to save employee benefit.", e);
        }
    }

    // updates employee benefit
    public void update(EmployeeBenefit employeeBenefit) {
        String sql = """
                UPDATE employee_benefit
                SET employee_id = ?,
                    benefit_type_id = ?,
                    amount = ?,
                    effective_start_date = ?,
                    effective_end_date = ?
                WHERE employee_benefit_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeBenefit.getEmployeeId());
            stmt.setInt(2, employeeBenefit.getBenefitTypeId());
            stmt.setDouble(3, employeeBenefit.getAmount());
            stmt.setDate(4, Date.valueOf(employeeBenefit.getEffectiveStartDate()));

            if (employeeBenefit.getEffectiveEndDate() == null) {
                stmt.setNull(5, java.sql.Types.DATE);
            } else {
                stmt.setDate(5, Date.valueOf(employeeBenefit.getEffectiveEndDate()));
            }

            stmt.setInt(6, employeeBenefit.getEmployeeBenefitId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to update employee benefit.", e);
        }
    }

    // maps result set to employee benefit
    private EmployeeBenefit mapRow(ResultSet rs) throws SQLException {
        Date endDateValue = rs.getDate("effective_end_date");

        return new EmployeeBenefit(
                rs.getInt("employee_benefit_id"),
                rs.getInt("employee_id"),
                rs.getInt("benefit_type_id"),
                rs.getDouble("amount"),
                rs.getDate("effective_start_date").toLocalDate(),
                endDateValue == null ? null : endDateValue.toLocalDate()
        );
    }
}