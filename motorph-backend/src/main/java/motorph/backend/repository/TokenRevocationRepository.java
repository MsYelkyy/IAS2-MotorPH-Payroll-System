package motorph.backend.repository;

import motorph.backend.exception.DataAccessException;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Stores revoked token IDs so they can no longer be used.
 * Revoked tokens are checked on every authenticated request.
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
     * Signs the user out everywhere: every token issued at or before {@code revokedAt} stops
     * working. Used after a password reset, when the individual token IDs are not known.
     */
    public void revokeAllSessions(int userAccountId, LocalDateTime revokedAt) {
        String sql = "UPDATE user_account SET sessions_revoked_at = ? WHERE user_account_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(revokedAt));
            stmt.setInt(2, userAccountId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to revoke the user's sessions.", e);
        }
    }

    /**
     * Whether a token issued at {@code issuedAt} predates the user's last "sign out everywhere".
     * Token times have one-second precision, so a token issued in the same second counts as
     * revoked.
     */
    public boolean isIssuedBeforeSessionRevocation(int userAccountId, Instant issuedAt) {
        String sql = "SELECT sessions_revoked_at FROM user_account WHERE user_account_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userAccountId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }

                Timestamp revokedAt = rs.getTimestamp("sessions_revoked_at");

                return revokedAt != null && predatesRevocation(issuedAt, revokedAt.toInstant());
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to check the user's session revocation.", e);
        }
    }

    /**
     * The revocation rule: a token issued at or before the revocation moment no longer works. Token
     * times have one-second precision, so one issued in the same second counts as revoked.
     */
    public static boolean predatesRevocation(Instant issuedAt, Instant revokedAt) {
        return !issuedAt.isAfter(revokedAt);
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
