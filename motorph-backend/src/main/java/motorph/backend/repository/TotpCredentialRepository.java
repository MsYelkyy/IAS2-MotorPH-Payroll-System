package motorph.backend.repository;

import motorph.backend.domain.TotpCredential;
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
 * Stores the authenticator linked to a user account.
 * A record is created only after setup is confirmed with a valid code.
 */
@Repository
public class TotpCredentialRepository {

    private final DataSource dataSource;

    public TotpCredentialRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public TotpCredential findByUserAccountId(int userAccountId) {
        String sql = """
                SELECT user_account_id, encrypted_secret, secret_iv, totp_enabled, enrolled_at, last_used_time_step
                FROM user_totp_credential
                WHERE user_account_id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userAccountId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                Timestamp enrolledAt = rs.getTimestamp("enrolled_at");
                long lastUsed = rs.getLong("last_used_time_step");
                boolean lastUsedIsNull = rs.wasNull();

                return new TotpCredential(
                        rs.getInt("user_account_id"),
                        rs.getBytes("encrypted_secret"),
                        rs.getBytes("secret_iv"),
                        rs.getBoolean("totp_enabled"),
                        enrolledAt == null ? null : enrolledAt.toLocalDateTime(),
                        lastUsedIsNull ? null : lastUsed
                );
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to load the authenticator enrollment.", e);
        }
    }

    /**
     * Saves a confirmed authenticator setup and marks it as enabled.
     * Only one authenticator can be saved for each user account.
     */
    public void saveEnabled(int userAccountId,
                            byte[] encryptedSecret,
                            byte[] nonce,
                            LocalDateTime enrolledAt,
                            long acceptedTimeStep) {
        String sql = """
                INSERT INTO user_totp_credential
                    (user_account_id, encrypted_secret, secret_iv, totp_enabled, enrolled_at, last_used_time_step)
                VALUES (?, ?, ?, 1, ?, ?)
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userAccountId);
            stmt.setBytes(2, encryptedSecret);
            stmt.setBytes(3, nonce);
            stmt.setTimestamp(4, Timestamp.valueOf(enrolledAt));
            stmt.setLong(5, acceptedTimeStep);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to save the authenticator enrollment.", e);
        }
    }

    /**
     * Records the accepted time step for the authenticator code.
     * Returns false if the same or a newer step has already been used.
     */
    public boolean advanceLastUsedTimeStep(int userAccountId, long timeStep) {
        String sql = """
                UPDATE user_totp_credential
                SET last_used_time_step = ?
                WHERE user_account_id = ?
                  AND (last_used_time_step IS NULL OR last_used_time_step < ?)
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, timeStep);
            stmt.setInt(2, userAccountId);
            stmt.setLong(3, timeStep);
            return stmt.executeUpdate() == 1;

        } catch (SQLException e) {
            throw new DataAccessException("Failed to record the accepted authenticator code.", e);
        }
    }

    /**
     * Removes the enrolment so the user must enrol again at the next login. Returns whether a row
     * existed.
     */
    public boolean deleteByUserAccountId(int userAccountId) {
        String sql = "DELETE FROM user_totp_credential WHERE user_account_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userAccountId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("Failed to reset the authenticator enrollment.", e);
        }
    }
}
