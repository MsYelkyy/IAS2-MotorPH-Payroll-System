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
 * Issues and resolves opaque OTP login challenges. The client is handed only
 * the challengeId; the backend alone maps it back to a userAccountId, so a
 * client can never assert "verify OTP for user X" directly.
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

    // resolves and consumes a challenge, returning the user account id it was issued for
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
