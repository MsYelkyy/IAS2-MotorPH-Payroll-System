package motorph.backend.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a server-issued OTP challenge created after password verification.
 * The opaque challengeId is handed to the client instead of the user account id,
 * so the backend - not the client - resolves who is completing the OTP step.
 */
public class LoginChallenge {

    private final String challengeId;
    private final int userAccountId;
    private final LocalDateTime expiresAt;
    private final boolean consumed;
    private final LocalDateTime createdAt;

    public LoginChallenge(String challengeId,
                          int userAccountId,
                          LocalDateTime expiresAt,
                          boolean consumed,
                          LocalDateTime createdAt) {

        this.challengeId = requireNonBlank(challengeId, "Challenge ID is required.");
        this.userAccountId = requirePositive(userAccountId, "User account ID must be positive.");
        this.expiresAt = Objects.requireNonNull(expiresAt, "Expiration is required.");
        this.consumed = consumed;
        this.createdAt = Objects.requireNonNull(createdAt, "Created date and time is required.");
    }

    public String getChallengeId() {
        return challengeId;
    }

    public int getUserAccountId() {
        return userAccountId;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isUsable() {
        return !consumed && !isExpired();
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
