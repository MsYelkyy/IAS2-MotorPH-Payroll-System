package motorph.backend.repository;

import motorph.backend.domain.LoginChallenge;
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
 * Persists opaque OTP login challenges. The client only ever sees the
 * challengeId (a random token); the mapping to a real userAccountId lives
 * here, server-side, so the client can never assert who it is mid-login.
 */
@Repository
public class LoginChallengeRepository {

    private final DataSource dataSource;

    public LoginChallengeRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void save(LoginChallenge challenge) {
        String sql = """
                INSERT INTO login_challenge (challenge_id, user_account_id, expires_at, consumed, created_at)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, challenge.getChallengeId());
            stmt.setInt(2, challenge.getUserAccountId());
            stmt.setTimestamp(3, Timestamp.valueOf(challenge.getExpiresAt()));
            stmt.setBoolean(4, challenge.isConsumed());
            stmt.setTimestamp(5, Timestamp.valueOf(challenge.getCreatedAt()));

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to save login challenge.", e);
        }
    }

    public LoginChallenge findById(String challengeId) {
        String sql = """
                SELECT challenge_id, user_account_id, expires_at, consumed, created_at
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
                            rs.getTimestamp("expires_at").toLocalDateTime(),
                            rs.getBoolean("consumed"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    );
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find login challenge.", e);
        }

        return null;
    }

    public void markConsumed(String challengeId) {
        String sql = "UPDATE login_challenge SET consumed = 1 WHERE challenge_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, challengeId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to mark login challenge as consumed.", e);
        }
    }

    // deletes challenges past their expiry, called opportunistically to keep the table small
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
