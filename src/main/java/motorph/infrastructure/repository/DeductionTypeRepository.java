package motorph.infrastructure.repository;

import motorph.domain.model.DeductionType;
import motorph.infrastructure.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Looks up deduction types by stable code. */
public class DeductionTypeRepository {

    // finds a deduction type by code
    public DeductionType findByCode(String code) {
        validateCode(code);

        String sql = """
                SELECT deduction_type_id, code, is_taxable
                FROM deduction_type
                WHERE code = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
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
            throw new RuntimeException("Failed to find deduction type by code.", e);
        }

        return null;
    }

    // validates a type code
    private void validateCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Deduction type code is required.");
        }
    }
}
