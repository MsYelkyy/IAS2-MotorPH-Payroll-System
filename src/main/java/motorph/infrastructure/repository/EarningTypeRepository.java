package motorph.infrastructure.repository;

import motorph.application.exception.DataAccessException;

import motorph.infrastructure.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Looks up earning type identifiers by stable code. */
public class EarningTypeRepository {

    // finds an earning type identifier by code
    public Integer findIdByCode(String code) {
        validateCode(code);

        String sql = """
                SELECT earning_type_id
                FROM earning_type
                WHERE code = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, code);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("earning_type_id") : null;
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find earning type by code.", e);
        }
    }

    // validates a type code
    private void validateCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Earning type code is required.");
        }
    }
}
