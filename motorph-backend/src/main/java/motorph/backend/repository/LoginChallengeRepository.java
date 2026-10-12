package motorph.backend.repository;

import motorph.backend.domain.LoginChallenge;
import motorph.backend.domain.value.ChallengePurpose;
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
 * Stores login challenges on the server.
 * The client only receives the challenge ID, while the related user account stays hidden.
 */
@Repository
public class LoginChallengeRepository {

    private final DataSource dataSource;

    public LoginChallengeRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void save(LoginChallenge challenge) {
        String sql = """
                INSERT INTO login_challenge
                    (challenge_id, user_account_id, purpose, expires_at, consumed, attempt_count, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, challenge.getChallengeId());
            stmt.setInt(2, challenge.getUserAccountId());
            stmt.setString(3, challenge.getPurpose().name());
            stmt.setTimestamp(4, Timestamp.valueOf(challenge.getExpiresAt()));
            stmt.setBoolean(5, challenge.isConsumed());
            stmt.setInt(6, challenge.getAttemptCount());
            stmt.setTimestamp(7, Timestamp.valueOf(challenge.getCreatedAt()));

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to save login challenge.", e);
        }
    }

    public LoginChallenge findById(String challengeId) {
        String sql = """
                SELECT challenge_id, user_account_id, purpose, expires_at, consumed, attempt_count,
                       pending_secret_encrypted, pending_secret_iv, created_at
                FROM login_challenge
                WHERE challenge_id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, challengeId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new LoginChallenge(
                            rs.getString("challenge_id"),
                            rs.getInt("user_account_id"),
                            ChallengePurpose.valueOf(rs.getString("purpose")),
                            rs.getTimestamp("expires_at").toLocalDateTime(),
                            rs.getBoolean("consumed"),
                            rs.getInt("attempt_count"),
                            rs.getBytes("pending_secret_encrypted"),
                            rs.getBytes("pending_secret_iv"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    );
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find login challenge.", e);
        }

        return null;
    }

    /**
     * Marks the challenge as used.
     * Returns false if the challenge has already been used.
     */
    public boolean markConsumed(String challengeId) {
        String sql = """
                UPDATE login_challenge
                SET consumed = 1
                WHERE challenge_id = ?
                  AND consumed = 0
                  AND expires_at > NOW()
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, challengeId);
            return stmt.executeUpdate() == 1;

        } catch (SQLException e) {
            throw new DataAccessException("Failed to mark login challenge as consumed.", e);
        }
    }

    /**
     * Counts one wrong code against the challenge and returns the new total.
     */
    public int incrementAttemptCount(String challengeId) {
        String update = "UPDATE login_challenge SET attempt_count = attempt_count + 1 WHERE challenge_id = ?";
        String select = "SELECT attempt_count FROM login_challenge WHERE challenge_id = ?";

        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(update)) {
                stmt.setString(1, challengeId);
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(select)) {
                stmt.setString(1, challengeId);

                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() ? rs.getInt("attempt_count") : Integer.MAX_VALUE;
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to count the failed attempt.", e);
        }
    }

    /**
     * Stores the encrypted secret created during initial authenticator setup.
     * Any previous setup secret is replaced.
     */
    public void savePendingSecret(String challengeId, byte[] encryptedSecret, byte[] nonce) {
        String sql = """
                UPDATE login_challenge
                SET pending_secret_encrypted = ?, pending_secret_iv = ?
                WHERE challenge_id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBytes(1, encryptedSecret);
            stmt.setBytes(2, nonce);
            stmt.setString(3, challengeId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to save the pending authenticator secret.", e);
        }
    }

    /**
     * Deletes challenges that expired before the cutoff.
     */
    public void deleteExpiredBefore(LocalDateTime cutoff) {
        String sql = "DELETE FROM login_challenge WHERE expires_at < ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(cutoff));
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete expired login challenges.", e);
        }
    }
}
