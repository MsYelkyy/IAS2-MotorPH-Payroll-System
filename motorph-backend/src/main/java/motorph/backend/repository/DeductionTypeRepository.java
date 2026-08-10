package motorph.backend.repository;

import motorph.backend.exception.DataAccessException;
import motorph.backend.domain.DeductionType;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Looks up deduction types by stable code. */
@Repository
public class DeductionTypeRepository {

    private final DataSource dataSource;

    public DeductionTypeRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DeductionType findByCode(String code) {
        String sql = """
                SELECT deduction_type_id, code, is_taxable
                FROM deduction_type
                WHERE code = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, code);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new DeductionType(
                            rs.getInt("deduction_type_id"),
                            rs.getString("code"),
                            rs.getBoolean("is_taxable")
                    );
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find deduction type by code.", e);
        }

        return null;
    }
}
