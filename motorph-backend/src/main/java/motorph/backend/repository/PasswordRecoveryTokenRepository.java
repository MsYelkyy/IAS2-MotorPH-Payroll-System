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
import java.util.OptionalInt;

/**
 * Stores password reset tokens as SHA-256 hashes.
 * The original token is only kept by the client that received it.
 */
@Repository
public class PasswordRecoveryTokenRepository {

    private final DataSource dataSource;

    public PasswordRecoveryTokenRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void save(String tokenHash, int userAccountId, LocalDateTime expiresAt, LocalDateTime createdAt) {
        String sql = """
                INSERT INTO password_recovery_token (token_hash, user_account_id, expires_at, consumed, created_at)
                VALUES (?, ?, ?, 0, ?)
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, tokenHash);
            stmt.setInt(2, userAccountId);
            stmt.setTimestamp(3, Timestamp.valueOf(expiresAt));
            stmt.setTimestamp(4, Timestamp.valueOf(createdAt));
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to save the password reset token.", e);
        }
    }

    /**
     * Uses the token and returns its user account, or empty when it is unknown, expired, or already
     * used. The check and the update are one statement, so a token cannot be used twice even by two
     * simultaneous requests.
     */
    public OptionalInt consume(String tokenHash, LocalDateTime now) {
        String update = """
                UPDATE password_recovery_token
                SET consumed = 1
                WHERE token_hash = ? AND consumed = 0 AND expires_at > ?
                """;
        String select = "SELECT user_account_id FROM password_recovery_token WHERE token_hash = ?";

        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(update)) {
                stmt.setString(1, tokenHash);
                stmt.setTimestamp(2, Timestamp.valueOf(now));

                if (stmt.executeUpdate() != 1) {
                    return OptionalInt.empty();
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(select)) {
                stmt.setString(1, tokenHash);

                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() ? OptionalInt.of(rs.getInt("user_account_id")) : OptionalInt.empty();
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to use the password reset token.", e);
        }
    }

    /**
     * Cancels every unused token of the user, so a new request or a completed reset leaves none
     * behind.
     */
    public void invalidateAllForUser(int userAccountId) {
        String sql = "UPDATE password_recovery_token SET consumed = 1 WHERE user_account_id = ? AND consumed = 0";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userAccountId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to cancel the password reset tokens.", e);
        }
    }
}
