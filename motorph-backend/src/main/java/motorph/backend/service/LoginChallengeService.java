package motorph.backend.service;

import motorph.backend.domain.LoginChallenge;
import motorph.backend.domain.value.ChallengePurpose;
import motorph.backend.exception.AuthenticationException;
import motorph.backend.repository.LoginChallengeRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Creates and checks login challenges.
 * The client only receives the challenge ID, while the backend keeps track of the related user account.
 * Each challenge has one purpose, expires after a short time, and is blocked after five failed code attempts.
 */
@Service
public class LoginChallengeService {

    public static final int MAX_ATTEMPTS = 5;

    private static final String INVALID_MESSAGE = "Login challenge is invalid or expired.";

    private final LoginChallengeRepository repository;
    private final Clock clock;
    private final int temporaryPasswordExpiryMinutes;
    private final int totpSetupExpiryMinutes;
    private final int totpCodeExpiryMinutes;
    private final SecureRandom secureRandom = new SecureRandom();

    public LoginChallengeService(
            LoginChallengeRepository repository,
            Clock clock,
            @Value("${motorph.auth.otp-challenge-expiry-minutes:10}") int temporaryPasswordExpiryMinutes,
            @Value("${motorph.auth.totp-setup-expiry-minutes:10}") int totpSetupExpiryMinutes,
            @Value("${motorph.auth.totp-code-expiry-minutes:5}") int totpCodeExpiryMinutes) {
        this.repository = repository;
        this.clock = clock;
        this.temporaryPasswordExpiryMinutes = temporaryPasswordExpiryMinutes;
        this.totpSetupExpiryMinutes = totpSetupExpiryMinutes;
        this.totpCodeExpiryMinutes = totpCodeExpiryMinutes;
    }

    public String issue(int userAccountId, ChallengePurpose purpose) {
        String challengeId = generateOpaqueId();
        LocalDateTime now = now();

        repository.save(new LoginChallenge(
                challengeId,
                userAccountId,
                purpose,
                now.plus(lifetimeOf(purpose)),
                false,
                0,
                null,
                null,
                now
        ));

        return challengeId;
    }

    // How long a challenge of this purpose stays valid.
    public Duration lifetimeOf(ChallengePurpose purpose) {
        return Duration.ofMinutes(switch (purpose) {
            case TEMPORARY_PASSWORD -> temporaryPasswordExpiryMinutes;
            case TOTP_SETUP -> totpSetupExpiryMinutes;
            case TOTP_LOGIN, PASSWORD_RECOVERY -> totpCodeExpiryMinutes;
        });
    }

    /**
     * Returns the challenge if it exists, belongs to the expected purpose, is unused, unexpired, and
     * has attempts left. The challenge stays usable, so a wrong code can be retried.
     */
    public LoginChallenge resolve(String challengeId, ChallengePurpose expectedPurpose) {
        if (challengeId == null || challengeId.isBlank()) {
            throw new AuthenticationException("Login challenge is required.");
        }

        LoginChallenge challenge = repository.findById(challengeId.trim());

        if (challenge == null
                || challenge.getPurpose() != expectedPurpose
                || !challenge.isUsable(now())
                || challenge.getAttemptCount() >= MAX_ATTEMPTS) {
            throw new AuthenticationException(INVALID_MESSAGE);
        }

        return challenge;
    }

    /**
     * Marks the challenge used so it cannot be replayed. Fails if another request already used it.
     */
    public void consume(String challengeId) {
        if (!repository.markConsumed(challengeId)) {
            throw new AuthenticationException(INVALID_MESSAGE);
        }
    }

    /**
     * Resolves a challenge to its user account ID and marks it as used, so each challenge works
     * only once.
     */
    public int resolveAndConsume(String challengeId, ChallengePurpose expectedPurpose) {
        LoginChallenge challenge = resolve(challengeId, expectedPurpose);
        consume(challenge.getChallengeId());
        return challenge.getUserAccountId();
    }

    /**
     * Counts a wrong code. At the fifth the challenge is closed, so the user has to start again.
     * Returns how many attempts remain.
     */
    public int recordFailedAttempt(String challengeId) {
        int attempts = repository.incrementAttemptCount(challengeId);

        if (attempts >= MAX_ATTEMPTS) {
            repository.markConsumed(challengeId);
        }

        return Math.max(0, MAX_ATTEMPTS - attempts);
    }

    public void savePendingSecret(String challengeId, byte[] encryptedSecret, byte[] nonce) {
        repository.savePendingSecret(challengeId, encryptedSecret, nonce);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private String generateOpaqueId() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
