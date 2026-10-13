package motorph.backend.service;

import motorph.backend.domain.UserAccount;
import motorph.backend.domain.value.ChallengePurpose;
import motorph.backend.exception.AuthenticationException;
import motorph.backend.exception.ValidationException;
import motorph.backend.repository.PasswordRecoveryTokenRepository;
import motorph.backend.repository.TokenRevocationRepository;
import motorph.backend.repository.UserAccountRepository;
import motorph.backend.security.PasswordPolicy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.OptionalInt;

/**
 * Handles the Forgot Password process using an authenticator code.
 * After the code is verified, the user receives a temporary reset token that can be used to set a new password.
 * Users without an authenticator setup must have their enrolment reset by an administrator.
 *
 * <p>{@link #start} returns the same response for every email address to avoid revealing account information.
 */
@Service
public class PasswordRecoveryService {

    private static final String INVALID_TOKEN_MESSAGE =
            "This password reset request is invalid or has expired. Please start again.";

    /** The challenge to put the code against and how long it stays valid. */
    public record RecoveryStart(String challengeId, long expiresInSeconds) {
    }

    /** The single-use token that authorises one password change. */
    public record RecoveryToken(String resetToken, long expiresInSeconds) {
    }

    private final UserAccountRepository userAccountRepository;
    private final LoginChallengeService challengeService;
    private final TotpService totpService;
    private final PasswordRecoveryTokenRepository tokenRepository;
    private final TokenRevocationRepository tokenRevocationRepository;
    private final UserAccountService userAccountService;
    private final UserAccessLogService userAccessLogService;
    private final Clock clock;
    private final int tokenExpiryMinutes;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordRecoveryService(UserAccountRepository userAccountRepository,
                                   LoginChallengeService challengeService,
                                   TotpService totpService,
                                   PasswordRecoveryTokenRepository tokenRepository,
                                   TokenRevocationRepository tokenRevocationRepository,
                                   UserAccountService userAccountService,
                                   UserAccessLogService userAccessLogService,
                                   Clock clock,
                                   @Value("${motorph.auth.password-reset-token-expiry-minutes:10}") int tokenExpiryMinutes) {
        this.userAccountRepository = userAccountRepository;
        this.challengeService = challengeService;
        this.totpService = totpService;
        this.tokenRepository = tokenRepository;
        this.tokenRevocationRepository = tokenRevocationRepository;
        this.userAccountService = userAccountService;
        this.userAccessLogService = userAccessLogService;
        this.clock = clock;
        this.tokenExpiryMinutes = tokenExpiryMinutes;
    }

    /**
     * Starts a recovery. An account that exists, may sign in, and has an authenticator gets a real
     * challenge; every other email gets a challenge ID that matches nothing, so the code screen looks
     * the same and every code is simply rejected.
     */
    public RecoveryStart start(String companyEmail) {
        if (companyEmail == null || companyEmail.trim().isEmpty()) {
            throw new ValidationException("Company email is required.");
        }

        UserAccount account = userAccountRepository.findByCompanyEmail(companyEmail.trim());
        long expiresInSeconds = challengeService.lifetimeOf(ChallengePurpose.PASSWORD_RECOVERY).toSeconds();

        if (isEligible(account)) {
            String challengeId = challengeService.issue(account.getUserAccountId(), ChallengePurpose.PASSWORD_RECOVERY);
            return new RecoveryStart(challengeId, expiresInSeconds);
        }

        return new RecoveryStart(randomOpaqueValue(), expiresInSeconds);
    }

    /**
     * Verifies the authenticator code and issues the reset token. Earlier unused tokens of the user
     * are cancelled, so only the newest one works.
     */
    public RecoveryToken verifyCode(String challengeId, String submittedCode) {
        int userAccountId = totpService.verifyRecovery(challengeId, submittedCode);

        tokenRepository.invalidateAllForUser(userAccountId);

        String token = randomOpaqueValue();
        LocalDateTime now = LocalDateTime.now(clock);
        Duration lifetime = Duration.ofMinutes(tokenExpiryMinutes);

        tokenRepository.save(sha256(token), userAccountId, now.plus(lifetime), now);

        return new RecoveryToken(token, lifetime.toSeconds());
    }

    /**
     * Sets the new password, uses up the token, and signs the user out everywhere. The password
     * rules are checked before the token is used, so a typo does not cost the user their token.
     */
    @Transactional
    public void resetPassword(String resetToken, String newPassword, String confirmPassword) {
        if (resetToken == null || resetToken.isBlank()) {
            throw new AuthenticationException(INVALID_TOKEN_MESSAGE);
        }

        PasswordPolicy.validate(newPassword);

        if (!newPassword.equals(confirmPassword)) {
            throw new ValidationException("Passwords do not match.");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        OptionalInt userAccountId = tokenRepository.consume(sha256(resetToken.trim()), now);

        if (userAccountId.isEmpty()) {
            throw new AuthenticationException(INVALID_TOKEN_MESSAGE);
        }

        UserAccount account = userAccountRepository.findById(userAccountId.getAsInt());

        if (!isEligible(account, false)) {
            throw new AuthenticationException(INVALID_TOKEN_MESSAGE);
        }

        userAccountService.resetPasswordAfterRecovery(account.getUserAccountId(), newPassword, confirmPassword);
        tokenRevocationRepository.revokeAllSessions(account.getUserAccountId(), now);
        tokenRepository.invalidateAllForUser(account.getUserAccountId());

        userAccessLogService.logEvent(account.getUserAccountId(), UserAccessLogService.ACTION_PASSWORD_RESET_COMPLETED,
                "Password reset completed with authenticator verification. Existing sessions were signed out.",
                UserAccessLogService.TARGET_USER_ACCOUNT, account.getUserAccountId());
    }

    private boolean isEligible(UserAccount account) {
        return isEligible(account, true);
    }

    private boolean isEligible(UserAccount account, boolean requireEnrollment) {
        return account != null
                && account.isActive()
                && !account.isLocked()
                && account.getRoles() != null
                && !account.getRoles().isEmpty()
                && (!requireEnrollment || totpService.isEnrolled(account.getUserAccountId()));
    }

    private String randomOpaqueValue() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }
}
