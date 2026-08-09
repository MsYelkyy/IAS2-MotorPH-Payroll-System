package motorph.backend.repository;

import motorph.backend.exception.DataAccessException;
import motorph.backend.domain.Position;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Handles database operations for position reference data. */
@Repository
public class PositionRepository {

    private final DataSource dataSource;

    public PositionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Position> findAll() {
        String sql = """
                SELECT position_id, position_name, description, department_id
                FROM position
                ORDER BY position_name
                """;
        return executeListQuery(sql, null);
    }

    public List<Position> findByDepartmentId(int departmentId) {
        String sql = """
                SELECT position_id, position_name, description, department_id
                FROM position
                WHERE department_id = ?
                ORDER BY position_name
                """;
        return executeListQuery(sql, departmentId);
    }

    private List<Position> executeListQuery(String sql, Integer departmentIdFilter) {
        List<Position> list = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (departmentIdFilter != null) {
                stmt.setInt(1, departmentIdFilter);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new Position(
                            rs.getInt("position_id"),
                            rs.getString("position_name"),
                            rs.getString("description"),
                            rs.getInt("department_id")
                    ));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to query positions.", e);
        }

        return list;
    }
}
