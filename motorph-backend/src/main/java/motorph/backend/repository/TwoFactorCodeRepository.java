package motorph.backend.repository;

import motorph.backend.exception.DataAccessException;

import motorph.backend.domain.TwoFactorCode;
import motorph.backend.domain.value.OtpPurpose;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;

/**
 * Handles database operations for two-factor authentication code records.
 */
@Repository
public class TwoFactorCodeRepository {

    private final DataSource dataSource;

    public TwoFactorCodeRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void save(TwoFactorCode code) {
        validateCode(code);

        String sql = """
                INSERT INTO two_factor_code (
                    user_account_id,
                    reset_request_id,
                    otp_code_hash,
                    purpose,
                    expires_at,
                    is_used,
                    used_at,
                    attempt_count,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, code.getUserAccountId());
            setNullableInteger(stmt, 2, code.getResetRequestId());
            stmt.setString(3, normalizeRequired(code.getOtpCodeHash(), "OTP code hash is required."));
            stmt.setString(4, code.getPurpose().name());
            stmt.setTimestamp(5, Timestamp.valueOf(code.getExpiresAt()));
            stmt.setBoolean(6, code.isUsed());
            setNullableTimestamp(stmt, 7, code.getUsedAt());
            stmt.setInt(8, code.getAttemptCount());
            stmt.setTimestamp(9, Timestamp.valueOf(code.getCreatedAt()));

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to save two-factor code.", e);
        }
    }

    /**
     * Finds the most recent valid code for the account and purpose.
     */
    public TwoFactorCode findLatestValidCode(int userAccountId, OtpPurpose purpose) {
        validatePositive(userAccountId, "User account ID must be positive.");
        validatePurpose(purpose);

        String sql = """
                SELECT two_factor_code_id,
                       user_account_id,
                       reset_request_id,
                       otp_code_hash,
                       purpose,
                       expires_at,
                       is_used,
                       used_at,
                       attempt_count,
                       created_at
                FROM two_factor_code
                WHERE user_account_id = ?
                  AND purpose = ?
                  AND is_used = 0
                  AND expires_at > NOW()
                ORDER BY created_at DESC
                LIMIT 1
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userAccountId);
            stmt.setString(2, purpose.name());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find latest valid two-factor code.", e);
        }

        return null;
    }

    public void incrementAttemptCount(int twoFactorCodeId) {
        validatePositive(twoFactorCodeId, "Two-factor code ID must be positive.");

        String sql = """
                UPDATE two_factor_code
                SET attempt_count = attempt_count + 1
                WHERE two_factor_code_id = ?
                """;

        executeUpdateById(sql, twoFactorCodeId, "Failed to increment OTP attempt count.");
    }

    public void markAsUsed(int twoFactorCodeId) {
        validatePositive(twoFactorCodeId, "Two-factor code ID must be positive.");

        String sql = """
                UPDATE two_factor_code
                SET is_used = 1,
                    used_at = NOW()
                WHERE two_factor_code_id = ?
                """;

        executeUpdateById(sql, twoFactorCodeId, "Failed to mark OTP as used.");
    }

    /**
     * Expires all valid codes for the account and purpose, so only a newly issued code can be used.
     */
    public void expireOldCodes(int userAccountId, OtpPurpose purpose) {
        validatePositive(userAccountId, "User account ID must be positive.");
        validatePurpose(purpose);

        String sql = """
                UPDATE two_factor_code
                SET expires_at = NOW()
                WHERE user_account_id = ?
                  AND purpose = ?
                  AND is_used = 0
                  AND expires_at > NOW()
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userAccountId);
            stmt.setString(2, purpose.name());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to expire old OTP codes.", e);
        }
    }

    private void executeUpdateById(String sql, int twoFactorCodeId, String errorMessage) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, twoFactorCodeId);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new IllegalArgumentException("Two-factor code not found.");
            }

        } catch (SQLException e) {
            throw new DataAccessException(errorMessage, e);
        }
    }

    private TwoFactorCode mapRow(ResultSet rs) throws SQLException {
        return new TwoFactorCode(
                rs.getInt("two_factor_code_id"),
                rs.getInt("user_account_id"),
                getNullableInteger(rs, "reset_request_id"),
                rs.getString("otp_code_hash"),
                OtpPurpose.fromValue(rs.getString("purpose")),
                getLocalDateTime(rs, "expires_at"),
                rs.getBoolean("is_used"),
                getLocalDateTime(rs, "used_at"),
                rs.getInt("attempt_count"),
                getLocalDateTime(rs, "created_at")
        );
    }

    private void validateCode(TwoFactorCode code) {
        if (code == null) {
            throw new IllegalArgumentException("Two-factor code is required.");
        }

        validatePositive(code.getUserAccountId(), "User account ID must be positive.");
        validatePurpose(code.getPurpose());
        normalizeRequired(code.getOtpCodeHash(), "OTP code hash is required.");

        if (code.getExpiresAt() == null) {
            throw new IllegalArgumentException("Expiration date and time is required.");
        }

        if (code.getCreatedAt() == null) {
            throw new IllegalArgumentException("Created date and time is required.");
        }

        if (code.getAttemptCount() < 0) {
            throw new IllegalArgumentException("Attempt count cannot be negative.");
        }
    }

    private void validatePurpose(OtpPurpose purpose) {
        if (purpose == null) {
            throw new IllegalArgumentException("OTP purpose is required.");
        }
    }

    private void validatePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private void setNullableInteger(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.INTEGER);
            return;
        }

        stmt.setInt(index, value);
    }

    private void setNullableTimestamp(PreparedStatement stmt, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.TIMESTAMP);
            return;
        }

        stmt.setTimestamp(index, Timestamp.valueOf(value));
    }

    private LocalDateTime getLocalDateTime(ResultSet rs, String columnName) throws SQLException {
        Timestamp value = rs.getTimestamp(columnName);
        return value == null ? null : value.toLocalDateTime();
    }

    private Integer getNullableInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }
}
