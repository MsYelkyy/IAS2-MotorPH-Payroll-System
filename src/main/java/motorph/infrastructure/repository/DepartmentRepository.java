package motorph.infrastructure.repository;

import motorph.domain.model.Department;
import motorph.infrastructure.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Handles database operations for department reference data. */
public class DepartmentRepository {

    public List<Department> findAllActive() {
        String sql = """
                SELECT department_id, department_code, department_name, is_active
                FROM department
                WHERE is_active = 1
                ORDER BY department_name
                """;
        return executeListQuery(sql);
    }

    private List<Department> executeListQuery(String sql) {
        List<Department> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
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
            throw new RuntimeException("Failed to query departments.", e);
        }

        return list;
    }
}
