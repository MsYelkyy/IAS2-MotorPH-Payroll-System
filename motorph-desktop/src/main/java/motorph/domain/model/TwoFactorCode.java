package motorph.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import motorph.domain.value.OtpPurpose;

/**
 * Represents a two-factor authentication code and maps to the two_factor_code table.
 */
public class TwoFactorCode {

    private Integer twoFactorCodeId;
    private int userAccountId;
    private Integer resetRequestId;
    private String otpCodeHash;
    private OtpPurpose purpose;
    private LocalDateTime expiresAt;
    private boolean used;
    private LocalDateTime usedAt;
    private int attemptCount;
    private LocalDateTime createdAt;

    // constructors
    public TwoFactorCode(Integer twoFactorCodeId,
                         int userAccountId,
                         Integer resetRequestId,
                         String otpCodeHash,
                         OtpPurpose purpose,
                         LocalDateTime expiresAt,
                         boolean used,
                         LocalDateTime usedAt,
                         int attemptCount,
                         LocalDateTime createdAt) {

        setTwoFactorCodeId(twoFactorCodeId);
        setUserAccountId(userAccountId);
        setResetRequestId(resetRequestId);
        setOtpCodeHash(otpCodeHash);
        setPurpose(purpose);
        setExpiresAt(expiresAt);
        setUsed(used);
        setUsedAt(usedAt);
        setAttemptCount(attemptCount);
        setCreatedAt(createdAt);
    }

    public TwoFactorCode(int userAccountId,
                         Integer resetRequestId,
                         String otpCodeHash,
                         OtpPurpose purpose,
                         LocalDateTime expiresAt) {

        this(
                null,
                userAccountId,
                resetRequestId,
                otpCodeHash,
                purpose,
                expiresAt,
                false,
                null,
                0,
                LocalDateTime.now()
        );
    }

    // getters
    public Integer getTwoFactorCodeId() {
        return twoFactorCodeId;
    }

    public int getUserAccountId() {
        return userAccountId;
    }

    public Integer getResetRequestId() {
        return resetRequestId;
    }

    public String getOtpCodeHash() {
        return otpCodeHash;
    }

    public OtpPurpose getPurpose() {
        return purpose;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isUsed() {
        return used;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // checks if the OTP is expired
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    // checks if the OTP is still valid
    public boolean isValid() {
        return !used && !isExpired();
    }

    // marks the OTP as used
    public void markUsed() {
        if (used) {
            throw new IllegalStateException("OTP code is already used.");
        }

        if (isExpired()) {
            throw new IllegalStateException("OTP code is already expired.");
        }

        this.used = true;
        this.usedAt = LocalDateTime.now();
    }

    // increases failed OTP attempt count
    public void incrementAttemptCount() {
        this.attemptCount++;
    }

    // setters
    public void setTwoFactorCodeId(Integer twoFactorCodeId) {
        if (twoFactorCodeId != null && twoFactorCodeId <= 0) {
            throw new IllegalArgumentException("Two-factor code ID must be positive.");
        }

        this.twoFactorCodeId = twoFactorCodeId;
    }

    public void setUserAccountId(int userAccountId) {
        if (userAccountId <= 0) {
            throw new IllegalArgumentException("User account ID must be positive.");
        }

        this.userAccountId = userAccountId;
    }

    public void setResetRequestId(Integer resetRequestId) {
        if (resetRequestId != null && resetRequestId <= 0) {
            throw new IllegalArgumentException("Reset request ID must be positive.");
        }

        this.resetRequestId = resetRequestId;
    }

    public void setOtpCodeHash(String otpCodeHash) {
        this.otpCodeHash = requireNonBlank(otpCodeHash, "OTP code hash is required.");
    }

    public void setPurpose(OtpPurpose purpose) {
        this.purpose = Objects.requireNonNull(purpose, "OTP purpose is required.");
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = Objects.requireNonNull(expiresAt, "Expiration date and time is required.");
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }

    public void setAttemptCount(int attemptCount) {
        if (attemptCount < 0) {
            throw new IllegalArgumentException("Attempt count cannot be negative.");
        }

        this.attemptCount = attemptCount;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = Objects.requireNonNull(createdAt, "Created date and time is required.");
    }

    // validates required text
    private String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }

    // returns the two-factor code summary
    @Override
    public String toString() {
        return "TwoFactorCode{"
                + "twoFactorCodeId=" + twoFactorCodeId
                + ", userAccountId=" + userAccountId
                + ", resetRequestId=" + resetRequestId
                + ", purpose=" + purpose
                + ", expiresAt=" + expiresAt
                + ", used=" + used
                + ", usedAt=" + usedAt
                + ", attemptCount=" + attemptCount
                + ", createdAt=" + createdAt
                + '}';
    }
}