package motorph.backend.repository;

import motorph.backend.exception.DataAccessException;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Tracks revoked token IDs ({@code jti}). Checked on every authenticated request so a revoked token
 * stops working immediately rather than at its natural expiry.
 */
@Repository
public class TokenRevocationRepository {

    private final DataSource dataSource;

    public TokenRevocationRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void revoke(String jti, int userAccountId, LocalDateTime tokenExpiresAt) {
        String sql = """
                INSERT INTO token_revocation (jti, user_account_id, expires_at, revoked_at)
                VALUES (?, ?, ?, NOW())
                ON DUPLICATE KEY UPDATE revoked_at = VALUES(revoked_at)
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, jti);
            stmt.setInt(2, userAccountId);
            stmt.setTimestamp(3, Timestamp.valueOf(tokenExpiresAt));
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to revoke token.", e);
        }
    }

    public boolean isRevoked(String jti) {
        String sql = "SELECT 1 FROM token_revocation WHERE jti = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, jti);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to check token revocation status.", e);
        }
    }

    /**
     * Removes entries for tokens that have already expired, since those tokens are rejected anyway.
     */
    public void deleteExpiredBefore(LocalDateTime cutoff) {
        String sql = "DELETE FROM token_revocation WHERE expires_at < ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(cutoff));
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete expired token revocation rows.", e);
        }
    }
}
