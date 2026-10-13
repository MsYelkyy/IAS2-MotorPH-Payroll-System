package motorph.backend.domain;

import motorph.backend.domain.value.ChallengePurpose;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Stores a temporary challenge created after the password is verified.
 * The client receives the challenge ID instead of the user account ID.
 * The backend uses it to identify the account and required verification step.
 * The challenge expires and is disabled after too many incorrect attempts.
 */
public class LoginChallenge {

    private final String challengeId;
    private final int userAccountId;
    private final ChallengePurpose purpose;
    private final LocalDateTime expiresAt;
    private final boolean consumed;
    private final int attemptCount;
    private final byte[] pendingSecretEncrypted;
    private final byte[] pendingSecretNonce;
    private final LocalDateTime createdAt;

    public LoginChallenge(String challengeId,
                          int userAccountId,
                          ChallengePurpose purpose,
                          LocalDateTime expiresAt,
                          boolean consumed,
                          int attemptCount,
                          byte[] pendingSecretEncrypted,
                          byte[] pendingSecretNonce,
                          LocalDateTime createdAt) {

        this.challengeId = requireNonBlank(challengeId, "Challenge ID is required.");
        this.userAccountId = requirePositive(userAccountId, "User account ID must be positive.");
        this.purpose = Objects.requireNonNull(purpose, "Purpose is required.");
        this.expiresAt = Objects.requireNonNull(expiresAt, "Expiration is required.");
        this.consumed = consumed;
        this.attemptCount = attemptCount;
        this.pendingSecretEncrypted = pendingSecretEncrypted;
        this.pendingSecretNonce = pendingSecretNonce;
        this.createdAt = Objects.requireNonNull(createdAt, "Created date and time is required.");
    }

    public String getChallengeId() {
        return challengeId;
    }

    public int getUserAccountId() {
        return userAccountId;
    }

    public ChallengePurpose getPurpose() {
        return purpose;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    /** The encrypted enrolment secret, or null when none has been generated for this challenge. */
    public byte[] getPendingSecretEncrypted() {
        return pendingSecretEncrypted;
    }

    public byte[] getPendingSecretNonce() {
        return pendingSecretNonce;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(expiresAt);
    }

    public boolean isUsable(LocalDateTime now) {
        return !consumed && !isExpired(now);
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private int requirePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
