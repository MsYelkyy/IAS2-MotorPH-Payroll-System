package motorph.backend.service;

import motorph.backend.domain.LoginChallenge;
import motorph.backend.exception.AuthenticationException;
import motorph.backend.repository.LoginChallengeRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Issues and resolves OTP login challenges. The client only receives an opaque challenge ID and the
 * backend maps it back to the user account, so a client cannot choose which account it is
 * verifying.
 */
@Service
public class LoginChallengeService {

    private final LoginChallengeRepository repository;
    private final int expiryMinutes;
    private final SecureRandom secureRandom = new SecureRandom();

    public LoginChallengeService(
            LoginChallengeRepository repository,
            @Value("${motorph.auth.otp-challenge-expiry-minutes:10}") int expiryMinutes) {
        this.repository = repository;
        this.expiryMinutes = expiryMinutes;
    }

    public String issue(int userAccountId) {
        String challengeId = generateOpaqueId();

        LoginChallenge challenge = new LoginChallenge(
                challengeId,
                userAccountId,
                LocalDateTime.now().plusMinutes(expiryMinutes),
                false,
                LocalDateTime.now()
        );

        repository.save(challenge);
        return challengeId;
    }

    /**
     * Resolves a challenge to its user account ID and marks it as used, so each challenge works
     * only once.
     */
    public int resolveAndConsume(String challengeId) {
        if (challengeId == null || challengeId.isBlank()) {
            throw new AuthenticationException("Login challenge is required.");
        }

        LoginChallenge challenge = repository.findById(challengeId.trim());

        if (challenge == null || !challenge.isUsable()) {
            throw new AuthenticationException("Login challenge is invalid or expired.");
        }

        repository.markConsumed(challenge.getChallengeId());
        return challenge.getUserAccountId();
    }

    private String generateOpaqueId() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
