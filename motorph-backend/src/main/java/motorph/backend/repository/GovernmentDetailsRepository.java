package motorph.backend.repository;

import motorph.backend.exception.DataAccessException;
import motorph.backend.domain.GovernmentDetails;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Handles database operations for government details records.
 * Read-only for Phase 2 - write methods move with employee CRUD (Phase 3).
 */
@Repository
public class GovernmentDetailsRepository {

    private final DataSource dataSource;

    public GovernmentDetailsRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public GovernmentDetails findByEmployeeId(int employeeId) {
        String sql = """
                SELECT government_details_id,
                       employee_id,
                       sss_number,
                       tin_number,
                       pagibig_number,
                       philhealth_number
                FROM government_details
                WHERE employee_id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find government details by employee id.", e);
        }

        return null;
    }

    // saves government details
    public void save(GovernmentDetails governmentDetails) {
        String sql = """
                INSERT INTO government_details (
                    employee_id,
                    sss_number,
                    tin_number,
                    pagibig_number,
                    philhealth_number
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, governmentDetails.getEmployeeId());
            stmt.setString(2, governmentDetails.getSssNumber());
            stmt.setString(3, governmentDetails.getTinNumber());
            stmt.setString(4, governmentDetails.getPagibigNumber());
            stmt.setString(5, governmentDetails.getPhilhealthNumber());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to save government details.", e);
        }
    }

    // updates government details
    public void update(GovernmentDetails governmentDetails) {
        String sql = """
                UPDATE government_details
                SET employee_id = ?,
                    sss_number = ?,
                    tin_number = ?,
                    pagibig_number = ?,
                    philhealth_number = ?
                WHERE government_details_id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, governmentDetails.getEmployeeId());
            stmt.setString(2, governmentDetails.getSssNumber());
            stmt.setString(3, governmentDetails.getTinNumber());
            stmt.setString(4, governmentDetails.getPagibigNumber());
            stmt.setString(5, governmentDetails.getPhilhealthNumber());
            stmt.setInt(6, governmentDetails.getGovernmentDetailsId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to update government details.", e);
        }
    }

    private GovernmentDetails mapRow(ResultSet rs) throws SQLException {
        return new GovernmentDetails(
                rs.getInt("government_details_id"),
                rs.getInt("employee_id"),
                rs.getString("sss_number"),
                rs.getString("tin_number"),
                rs.getString("pagibig_number"),
                rs.getString("philhealth_number")
        );
    }
}
