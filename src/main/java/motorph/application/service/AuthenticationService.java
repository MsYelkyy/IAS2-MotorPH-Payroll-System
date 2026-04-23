package motorph.application.service;

import motorph.application.dto.login.LoginRequestDTO;
import motorph.application.dto.login.LoginResultDTO;
import motorph.application.exception.AuthenticationException;
import motorph.application.security.PasswordHasher;
import motorph.domain.model.UserAccount;
import motorph.infrastructure.repository.UserAccountRepository;

/**
 * Handles user authentication using company email and password.
 */
public class AuthenticationService {

    private final UserAccountRepository userAccountRepository;
    private final UserAccessLogService userAccessLogService;

    // constructors
    public AuthenticationService(UserAccountRepository userAccountRepository,
                                 UserAccessLogService userAccessLogService) {
        if (userAccountRepository == null) {
            throw new IllegalArgumentException("UserAccountRepository is required.");
        }

        this.userAccountRepository = userAccountRepository;
        this.userAccessLogService = userAccessLogService;
    }

    // Authenticates a user using company email and password.
    public LoginResultDTO authenticate(LoginRequestDTO request) {
        validateRequest(request);

        UserAccount account = userAccountRepository.findByCompanyEmail(request.getCompanyEmail());

        if (account == null) {
            logFailed(request.getCompanyEmail(), "Login failed because email was not found.");
            throw new AuthenticationException("Invalid email or password.");
        }

        if (!account.isActive()) {
            logFailed(account.getCompanyEmail(), "Login failed because account is inactive.");
            throw new AuthenticationException("Account is inactive.");
        }

        if (account.isLocked()) {
            logFailed(account.getCompanyEmail(), "Login failed because account is locked.");
            throw new AuthenticationException("Account is locked.");
        }

        validateStoredCredentials(account);

        boolean matched = PasswordHasher.verify(
                request.getPassword(),
                account.getPasswordSalt(),
                account.getPasswordHash()
        );

        if (!matched) {
            logFailed(account.getCompanyEmail(), "Login failed because password did not match.");
            throw new AuthenticationException("Invalid email or password.");
        }

        userAccountRepository.updateLastLogin(account.getUserAccountId());
        logSuccess(account);

        return buildResult(account);
    }

    // Builds the login result from the authenticated account.
    private LoginResultDTO buildResult(UserAccount account) {
        return new LoginResultDTO(
                account.getUserAccountId(),
                account.getEmployeeId(),
                account.getCompanyEmail(),
                account.getRole(),
                true,
                "Login successful."
        );
    }

    // Validates the login request input.
    private void validateRequest(LoginRequestDTO request) {
        if (request == null) {
            throw new AuthenticationException("Login request is required.");
        }

        if (request.getCompanyEmail() == null || request.getCompanyEmail().isBlank()) {
            throw new AuthenticationException("Company email is required.");
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new AuthenticationException("Password is required.");
        }
    }

    // Validates that the stored account credentials are complete.
    private void validateStoredCredentials(UserAccount account) {
        if (account.getPasswordHash() == null || account.getPasswordHash().isBlank()) {
            throw new IllegalStateException("Stored password hash is missing.");
        }

        if (account.getPasswordSalt() == null || account.getPasswordSalt().isBlank()) {
            throw new IllegalStateException("Stored password salt is missing.");
        }
    }

    // Logs a successful login attempt.
    private void logSuccess(UserAccount account) {
        if (userAccessLogService == null) {
            return;
        }

        userAccessLogService.logLoginSuccess(
                account.getEmployeeId(),
                account.getRole(),
                "Login successful."
        );
    }

    // Logs a failed login attempt.
    private void logFailed(String companyEmail, String details) {
        if (userAccessLogService == null) {
            return;
        }

        userAccessLogService.logLoginFailed(companyEmail, details);
    }
}