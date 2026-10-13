package motorph.backend.domain;

import java.time.LocalDateTime;

/**
 * A user's enrolled authenticator: the encrypted TOTP secret, whether TOTP is enabled, and the last
 * time step that was accepted, which blocks a code from being used twice.
 */
public class TotpCredential {

    private final int userAccountId;
    private final byte[] encryptedSecret;
    private final byte[] secretNonce;
    private final boolean enabled;
    private final LocalDateTime enrolledAt;
    private final Long lastUsedTimeStep;

    public TotpCredential(int userAccountId,
                          byte[] encryptedSecret,
                          byte[] secretNonce,
                          boolean enabled,
                          LocalDateTime enrolledAt,
                          Long lastUsedTimeStep) {
        this.userAccountId = userAccountId;
        this.encryptedSecret = encryptedSecret;
        this.secretNonce = secretNonce;
        this.enabled = enabled;
        this.enrolledAt = enrolledAt;
        this.lastUsedTimeStep = lastUsedTimeStep;
    }

    public int getUserAccountId() {
        return userAccountId;
    }

    public byte[] getEncryptedSecret() {
        return encryptedSecret;
    }

    public byte[] getSecretNonce() {
        return secretNonce;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }

    /** Null until the first code is accepted. */
    public Long getLastUsedTimeStep() {
        return lastUsedTimeStep;
    }
}
