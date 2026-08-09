package motorph.backend.repository;

import motorph.backend.exception.DataAccessException;
import motorph.backend.domain.Department;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Handles database operations for department reference data. */
@Repository
public class DepartmentRepository {

    private final DataSource dataSource;

    public DepartmentRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Department> findAllActive() {
        String sql = """
                SELECT department_id, department_code, department_name, is_active
                FROM department
                WHERE is_active = 1
                ORDER BY department_name
                """;

        List<Department> list = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(new Department(
                        rs.getInt("department_id"),
                        rs.getString("department_code"),
                        rs.getString("department_name"),
                        rs.getBoolean("is_active")
                ));
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to query departments.", e);
        }

        return list;
    }
}
