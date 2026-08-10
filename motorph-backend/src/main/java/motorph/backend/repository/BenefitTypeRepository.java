package motorph.backend.repository;

import motorph.backend.exception.DataAccessException;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Looks up benefit type identifiers by stable code. */
@Repository
public class BenefitTypeRepository {

    private final DataSource dataSource;

    public BenefitTypeRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Integer findIdByCode(String code) {
        String sql = """
                SELECT benefit_type_id
                FROM benefit_type
                WHERE code = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, code);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("benefit_type_id") : null;
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find benefit type by code.", e);
        }
    }
}
