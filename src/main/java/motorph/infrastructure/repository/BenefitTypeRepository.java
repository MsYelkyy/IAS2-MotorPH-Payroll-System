package motorph.infrastructure.repository;

import motorph.infrastructure.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Looks up benefit type identifiers by stable code. */
public class BenefitTypeRepository {

    // finds a benefit type identifier by code
    public Integer findIdByCode(String code) {
        validateCode(code);

        String sql = """
                SELECT benefit_type_id
                FROM benefit_type
                WHERE code = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, code);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("benefit_type_id") : null;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find benefit type by code.", e);
        }
    }

    // validates a type code
    private void validateCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Benefit type code is required.");
        }
    }
}
