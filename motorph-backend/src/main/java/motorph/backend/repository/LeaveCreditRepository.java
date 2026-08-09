package motorph.backend.repository;

import motorph.backend.exception.DataAccessException;

import motorph.backend.domain.LeaveCredit;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for employee leave credit records.
 */
@Repository
public class LeaveCreditRepository {

    private final DataSource dataSource;

    public LeaveCreditRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean hasEnoughCredits(int employeeId, int leaveTypeId, int year, double leaveHours) {
        String sql = """
                SELECT total_credits, used_credits
                FROM leave_credit
                WHERE employee_id = ?
                  AND leave_type_id = ?
                  AND year = ?
                """;

        double requestedCredits = convertHoursToCredits(leaveHours);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.setInt(2, leaveTypeId);
            stmt.setInt(3, year);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double totalCredits = rs.getDouble("total_credits");
                    double usedCredits = rs.getDouble("used_credits");
                    double remainingCredits = totalCredits - usedCredits;

                    return remainingCredits >= requestedCredits;
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to check leave credits.", e);
        }

        return false;
    }

    public double getRemainingCredits(int employeeId, int leaveTypeId, int year) {
        String sql = """
                SELECT total_credits, used_credits
                FROM leave_credit
                WHERE employee_id = ?
                  AND leave_type_id = ?
                  AND year = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.setInt(2, leaveTypeId);
            stmt.setInt(3, year);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double totalCredits = rs.getDouble("total_credits");
                    double usedCredits = rs.getDouble("used_credits");

                    return totalCredits - usedCredits;
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to get remaining leave credits.", e);
        }

        return 0.0;
    }

    public void deductCredits(int employeeId, int leaveTypeId, int year, double leaveHours) {
        String sql = """
                UPDATE leave_credit
                SET used_credits = used_credits + ?
                WHERE employee_id = ?
                  AND leave_type_id = ?
                  AND year = ?
                  AND (total_credits - used_credits) >= ?
                """;

        double requestedCredits = convertHoursToCredits(leaveHours);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, requestedCredits);
            stmt.setInt(2, employeeId);
            stmt.setInt(3, leaveTypeId);
            stmt.setInt(4, year);
            stmt.setDouble(5, requestedCredits);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new IllegalStateException("Insufficient leave credits.");
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to deduct leave credits.", e);
        }
    }

    public List<LeaveCredit> findByEmployeeId(int employeeId, int year) {
        String sql = """
                SELECT leave_credit_id, employee_id, leave_type_id,
                    total_credits, used_credits, year
                FROM leave_credit
                WHERE employee_id = ?
                AND year = ?
                ORDER BY leave_type_id
                """;

        List<LeaveCredit> results = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.setInt(2, year);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new LeaveCredit(
                            rs.getInt("leave_credit_id"),
                            rs.getInt("employee_id"),
                            rs.getInt("leave_type_id"),
                            rs.getDouble("total_credits"),
                            rs.getDouble("used_credits"),
                            rs.getInt("year")
                    ));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to load leave credits for employee.", e);
        }

        return results;
    }

    private double convertHoursToCredits(double leaveHours) {
        return leaveHours / 8.0;
    }
}
