package motorph.backend.service;

import motorph.backend.domain.LoginChallenge;
import motorph.backend.domain.TotpCredential;
import motorph.backend.domain.UserAccount;
import motorph.backend.domain.value.ChallengePurpose;
import motorph.backend.exception.AuthenticationException;
import motorph.backend.exception.OtpVerificationException;
import motorph.backend.exception.ValidationException;
import motorph.backend.repository.TotpCredentialRepository;
import motorph.backend.security.TotpSecretCipher;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.OptionalLong;

/**
 * Handles TOTP authenticator setup and verification.
 * The authenticator is only enabled after the user enters a valid code.
 * Used codes cannot be accepted again, and the challenge is blocked after five failed attempts.
 * Sensitive values such as the secret, code, and encryption key are not written to logs.
 */
@Service
public class TotpService {

    // Shown for every recovery failure so the response never reveals whether the account exists.
    public static final String RECOVERY_FAILURE_MESSAGE = "That code is incorrect, or the request has expired.";

    // Everything the client needs to show the enrolment screen. The secret is not stored by the client.
    public record EnrollmentDetails(byte[] qrCodePng, String manualEntryKey, String accountLabel) {
    }

    private final LoginChallengeService challengeService;
    private final AuthenticationService authenticationService;
    private final TotpCredentialRepository credentialRepository;
    private final TotpSecretCipher cipher;
    private final TotpCodeService codeService;
    private final TotpQrCodeGenerator qrCodeGenerator;
    private final UserAccessLogService userAccessLogService;
    private final Clock clock;

    public TotpService(LoginChallengeService challengeService,
                       AuthenticationService authenticationService,
                       TotpCredentialRepository credentialRepository,
                       TotpSecretCipher cipher,
                       TotpCodeService codeService,
                       TotpQrCodeGenerator qrCodeGenerator,
                       UserAccessLogService userAccessLogService,
                       Clock clock) {
        this.challengeService = challengeService;
        this.authenticationService = authenticationService;
        this.credentialRepository = credentialRepository;
        this.cipher = cipher;
        this.codeService = codeService;
        this.qrCodeGenerator = qrCodeGenerator;
        this.userAccessLogService = userAccessLogService;
        this.clock = clock;
    }

    public boolean isEnrolled(int userAccountId) {
        TotpCredential credential = credentialRepository.findByUserAccountId(userAccountId);
        return credential != null && credential.isEnabled();
    }

    /**
     * Generates a new secret for the enrolment challenge and returns the QR code and manual-entry
     * key. Each call replaces the previous secret, so only the most recent QR code can be confirmed.
     * Nothing is enabled until {@link #confirmEnrollment} succeeds.
     */
    public EnrollmentDetails startEnrollment(String challengeId) {
        LoginChallenge challenge = challengeService.resolve(challengeId, ChallengePurpose.TOTP_SETUP);
        UserAccount account = authenticationService.requireLoginEligible(challenge.getUserAccountId());

        if (isEnrolled(account.getUserAccountId())) {
            throw new AuthenticationException("Login challenge is invalid or expired.");
        }

        byte[] secret = codeService.generateSecret();
        TotpSecretCipher.EncryptedSecret encrypted = cipher.encrypt(secret, pendingContext(challenge.getChallengeId()));
        challengeService.savePendingSecret(challenge.getChallengeId(), encrypted.ciphertext(), encrypted.nonce());

        logEvent(account.getUserAccountId(), UserAccessLogService.ACTION_TOTP_ENROLLMENT_STARTED,
                "Started authenticator enrollment.");

        String uri = codeService.buildOtpAuthUri(account.getCompanyEmail(), secret);

        return new EnrollmentDetails(
                qrCodePng(uri),
                groupForReading(codeService.toBase32(secret)),
                account.getCompanyEmail()
        );
    }

    /**
     * Enables TOTP for the account if the code matches the secret generated for this challenge, and
     * returns the account so the caller can complete the login.
     */
    public UserAccount confirmEnrollment(String challengeId, String submittedCode) {
        requireCodeFormat(submittedCode);

        LoginChallenge challenge = challengeService.resolve(challengeId, ChallengePurpose.TOTP_SETUP);
        UserAccount account = authenticationService.requireLoginEligible(challenge.getUserAccountId());

        if (challenge.getPendingSecretEncrypted() == null) {
            throw new ValidationException("Scan the QR code with your authenticator app first.");
        }

        if (isEnrolled(account.getUserAccountId())) {
            throw new AuthenticationException("Login challenge is invalid or expired.");
        }

        byte[] secret = cipher.decrypt(challenge.getPendingSecretEncrypted(), challenge.getPendingSecretNonce(),
                pendingContext(challenge.getChallengeId()));
        OptionalLong step = codeService.findMatchingTimeStep(secret, submittedCode, null);

        if (step.isEmpty()) {
            throw rejectLoginCode(challenge, account.getUserAccountId());
        }

        challengeService.consume(challenge.getChallengeId());

        TotpSecretCipher.EncryptedSecret encrypted = cipher.encrypt(secret, credentialContext(account.getUserAccountId()));
        credentialRepository.saveEnabled(account.getUserAccountId(), encrypted.ciphertext(), encrypted.nonce(),
                LocalDateTime.now(clock), step.getAsLong());

        logEvent(account.getUserAccountId(), UserAccessLogService.ACTION_TOTP_ENROLLMENT_COMPLETED,
                "Completed authenticator enrollment.");

        return account;
    }

    /**
     * Checks the authenticator code for a login and returns the account so the caller can issue
     * tokens.
     */
    public UserAccount verifyLogin(String challengeId, String submittedCode) {
        requireCodeFormat(submittedCode);

        LoginChallenge challenge = challengeService.resolve(challengeId, ChallengePurpose.TOTP_LOGIN);
        UserAccount account = authenticationService.requireLoginEligible(challenge.getUserAccountId());

        if (!codeAccepted(account.getUserAccountId(), submittedCode)) {
            throw rejectLoginCode(challenge, account.getUserAccountId());
        }

        challengeService.consume(challenge.getChallengeId());

        logEvent(account.getUserAccountId(), UserAccessLogService.ACTION_TOTP_VERIFICATION_SUCCEEDED,
                "Authenticator code accepted at login.");

        return account;
    }

    /**
     * Checks the authenticator code used for password recovery.
     * If the code is valid, the related user account ID is returned.
     * All failed attempts return the same message so account information is not revealed.
     */
    public int verifyRecovery(String challengeId, String submittedCode) {
        requireCodeFormat(submittedCode);

        LoginChallenge challenge;
        UserAccount account;

        try {
            challenge = challengeService.resolve(challengeId, ChallengePurpose.PASSWORD_RECOVERY);
            account = authenticationService.requireLoginEligible(challenge.getUserAccountId());
        } catch (AuthenticationException e) {
            throw new OtpVerificationException(OtpVerificationException.Reason.EXPIRED_OR_UNAVAILABLE,
                    RECOVERY_FAILURE_MESSAGE);
        }

        if (!codeAccepted(account.getUserAccountId(), submittedCode)) {
            challengeService.recordFailedAttempt(challenge.getChallengeId());

            logEvent(account.getUserAccountId(), UserAccessLogService.ACTION_TOTP_VERIFICATION_FAILED,
                    "Authenticator code rejected during password recovery.");

            throw new OtpVerificationException(OtpVerificationException.Reason.INVALID_CODE,
                    RECOVERY_FAILURE_MESSAGE);
        }

        challengeService.consume(challenge.getChallengeId());

        logEvent(account.getUserAccountId(), UserAccessLogService.ACTION_TOTP_VERIFICATION_SUCCEEDED,
                "Authenticator code accepted for password recovery.");

        return account.getUserAccountId();
    }

    /**
     * Checks the code against the stored secret and, if it is valid and unused, records its time
     * step so it cannot be accepted again.
     */
    private boolean codeAccepted(int userAccountId, String submittedCode) {
        TotpCredential credential = credentialRepository.findByUserAccountId(userAccountId);

        if (credential == null || !credential.isEnabled()) {
            return false;
        }

        byte[] secret = cipher.decrypt(credential.getEncryptedSecret(), credential.getSecretNonce(),
                credentialContext(userAccountId));
        OptionalLong step = codeService.findMatchingTimeStep(secret, submittedCode, credential.getLastUsedTimeStep());

        return step.isPresent() && credentialRepository.advanceLastUsedTimeStep(userAccountId, step.getAsLong());
    }

    /**
     * Counts a wrong code against the challenge, audits it without the code, and builds the error
     * that tells the user how many attempts are left.
     */
    private OtpVerificationException rejectLoginCode(LoginChallenge challenge, int userAccountId) {
        int remaining = challengeService.recordFailedAttempt(challenge.getChallengeId());

        logEvent(userAccountId, UserAccessLogService.ACTION_TOTP_VERIFICATION_FAILED,
                remaining == 0
                        ? "Authenticator code rejected; attempt limit reached."
                        : "Authenticator code rejected; " + remaining + " attempts remaining.");

        if (remaining == 0) {
            return new OtpVerificationException(OtpVerificationException.Reason.ATTEMPT_LIMIT_REACHED,
                    "Too many incorrect codes. Please sign in again.");
        }

        return new OtpVerificationException(OtpVerificationException.Reason.INVALID_CODE,
                "Incorrect code. " + remaining + (remaining == 1 ? " attempt" : " attempts") + " remaining.");
    }

    private void requireCodeFormat(String submittedCode) {
        if (submittedCode == null || !submittedCode.matches("\\d{" + TotpCodeService.DIGITS + "}")) {
            throw new ValidationException("Enter the " + TotpCodeService.DIGITS + "-digit code from your authenticator app.");
        }
    }

    private byte[] qrCodePng(String uri) {
        return qrCodeGenerator.toPng(uri);
    }

    // Formats the Base32 secret into groups of four characters to make manual entry easier.
    private String groupForReading(String base32) {
        return base32.replaceAll("(.{4})(?=.)", "$1 ");
    }

    private String pendingContext(String challengeId) {
        return "motorph-totp-setup:" + challengeId;
    }

    private String credentialContext(int userAccountId) {
        return "motorph-totp-credential:" + userAccountId;
    }

    private void logEvent(int userAccountId, String action, String details) {
        userAccessLogService.logEvent(userAccountId, action, details,
                UserAccessLogService.TARGET_USER_ACCOUNT, userAccountId);
    }
}
