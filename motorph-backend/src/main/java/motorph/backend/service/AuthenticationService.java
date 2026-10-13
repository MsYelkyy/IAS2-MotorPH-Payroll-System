package motorph.backend.service;

import motorph.backend.exception.AuthenticationException;
import motorph.backend.exception.ValidationException;
import motorph.backend.security.PasswordHasher;
import motorph.backend.domain.TotpCredential;
import motorph.backend.domain.UserAccount;
import motorph.backend.repository.TokenRevocationRepository;
import motorph.backend.repository.TotpCredentialRepository;
import motorph.backend.repository.UserAccountRepository;
import motorph.backend.repository.UserAccountRepository.FailedPasswordAttemptResult;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Verifies the user's login credentials and handles the next login step.
 * A correct password alone does not complete the login. The user must still finish
 * authenticator setup or enter a valid authenticator code before tokens are issued.
 * Roles and permissions are handled separately by {@link PermissionService}.
 */
@Service
public class AuthenticationService {

    private final UserAccountRepository userAccountRepository;
    private final TotpCredentialRepository totpCredentialRepository;
    private final UserAccessLogService userAccessLogService;
    private final TokenRevocationRepository tokenRevocationRepository;
    private final Clock clock;
    @Value("${motorph.auth.lockout.max-failed-password-attempts:5}")
    private int maxFailedPasswordAttempts = 5;

    public AuthenticationService(UserAccountRepository userAccountRepository,
                                 TotpCredentialRepository totpCredentialRepository,
                                 UserAccessLogService userAccessLogService,
                                 TokenRevocationRepository tokenRevocationRepository,
                                 Clock clock) {

        this.userAccountRepository = userAccountRepository;
        this.totpCredentialRepository = totpCredentialRepository;
        this.userAccessLogService = userAccessLogService;
        this.tokenRevocationRepository = tokenRevocationRepository;
        this.clock = clock;
    }

    /**
     * Verifies the user's email and password.
     * If the account uses a temporary password, the user must change it first.
     * Otherwise, the next step is either authenticator setup or authenticator code verification.
     */
    public LoginAttemptOutcome authenticate(String companyEmailInput, String passwordInput) {
        validateCredentialsInput(companyEmailInput, passwordInput);

        String companyEmail = companyEmailInput.trim();
        UserAccount account = userAccountRepository.findByCompanyEmail(companyEmail);

        if (account == null) {
            logFailed(companyEmail, "Login failed because email was not found.");
            throw new AuthenticationException("Invalid email or password.");
        }

        validateAccountForLogin(account);
        validatePassword(passwordInput, account);

        if (account.mustChangePassword()) {
            return new LoginAttemptOutcome(
                    LoginAttemptOutcome.Type.TEMPORARY_PASSWORD_CHANGE_REQUIRED,
                    account.getUserAccountId(),
                    account.getCompanyEmail(),
                    "Temporary password verified. Password change required."
            );
        }

        TotpCredential credential = totpCredentialRepository.findByUserAccountId(account.getUserAccountId());
        boolean enrolled = credential != null && credential.isEnabled();

        return new LoginAttemptOutcome(
                enrolled ? LoginAttemptOutcome.Type.TOTP_REQUIRED : LoginAttemptOutcome.Type.TOTP_SETUP_REQUIRED,
                account.getUserAccountId(),
                account.getCompanyEmail(),
                enrolled
                        ? "Enter the code from your authenticator app."
                        : "Set up your authenticator app to continue."
        );
    }

    /**
     * Loads the account linked to the challenge and checks if the user can still sign in.
     * The account is checked again before the login is completed in case it was locked or deactivated.
     */
    public UserAccount requireLoginEligible(int userAccountId) {
        if (userAccountId <= 0) {
            throw new ValidationException("User account ID is required.");
        }

        UserAccount account = userAccountRepository.findById(userAccountId);

        if (account == null) {
            throw new AuthenticationException("User account not found.");
        }

        validateAccountForLogin(account);
        return account;
    }

    /**
     * Records a successful login once the authenticator code has been accepted.
     */
    public void completeLogin(UserAccount account) {
        userAccountRepository.updateLastLogin(account.getUserAccountId());
        userAccessLogService.logLoginSuccess(account.getUserAccountId(), "Login successful.");
    }

    private void validateAccountForLogin(UserAccount account) {
        if (!account.isActive()) {
            logFailed(account.getCompanyEmail(), "Login failed because account is inactive.");
            throw new AuthenticationException("Account is inactive.");
        }

        if (account.isLocked()) {
            logFailed(account.getCompanyEmail(), "Login failed because account is locked.");
            throw new AuthenticationException(
                    "Invalid email or password.", AuthenticationException.Reason.ACCOUNT_LOCKED);
        }

        if (account.getRoles() == null || account.getRoles().isEmpty()) {
            logFailed(account.getCompanyEmail(), "Login failed because account has no assigned role.");
            throw new AuthenticationException("Account has no assigned role.");
        }

        if (!account.hasPassword()) {
            throw new IllegalStateException("Stored password credentials are missing.");
        }

        if (account.isTemporaryPasswordExpired()) {
            logFailed(account.getCompanyEmail(), "Login failed because temporary password expired.");
            throw new AuthenticationException("Temporary password has expired. Please request another password reset.");
        }
    }

    private void validateCredentialsInput(String companyEmail, String password) {
        if (companyEmail == null || companyEmail.isBlank()) {
            throw new AuthenticationException("Company email is required.");
        }

        if (password == null || password.isBlank()) {
            throw new AuthenticationException("Password is required.");
        }
    }

    private void validatePassword(String rawPassword, UserAccount account) {
        boolean matched = PasswordHasher.verify(
                rawPassword,
                account.getPasswordSalt(),
                account.getPasswordHash()
        );

        if (!matched) {
            FailedPasswordAttemptResult result = userAccountRepository.recordFailedPasswordAttempt(
                    account.getUserAccountId(), maxFailedPasswordAttempts);
            logFailed(account.getCompanyEmail(), "Login failed because password did not match.");

            if (result == FailedPasswordAttemptResult.LOCK_APPLIED) {
                tokenRevocationRepository.revokeAllSessions(account.getUserAccountId(), LocalDateTime.now(clock));
                userAccessLogService.logAutomaticAccountLock(account.getUserAccountId());
                throw new AuthenticationException(
                        "Invalid email or password.", AuthenticationException.Reason.ACCOUNT_LOCKED);
            }

            throw new AuthenticationException("Invalid email or password.");
        }

        if (!userAccountRepository.resetFailedPasswordAttempts(account.getUserAccountId())) {
            logFailed(account.getCompanyEmail(), "Login failed because account is locked.");
            throw new AuthenticationException("Invalid email or password.");
        }

        upgradePasswordHashIfNeeded(rawPassword, account);
    }

    private void upgradePasswordHashIfNeeded(String rawPassword, UserAccount account) {
        if (!PasswordHasher.needsRehash(account.getPasswordHash())) {
            return;
        }

        String salt = PasswordHasher.generateSalt();

        userAccountRepository.updatePasswordHash(
                account.getUserAccountId(), PasswordHasher.hash(rawPassword, salt), salt);
    }

    private void logFailed(String companyEmail, String details) {
        userAccessLogService.logLoginFailed(companyEmail, details);
    }
}
