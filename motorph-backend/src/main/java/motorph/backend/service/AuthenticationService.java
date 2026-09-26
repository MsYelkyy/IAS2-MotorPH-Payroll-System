package motorph.backend.service;

import motorph.backend.exception.AuthenticationException;
import motorph.backend.exception.ValidationException;
import motorph.backend.security.PasswordHasher;
import motorph.backend.domain.UserAccount;
import motorph.backend.repository.EmployeeRepository;
import motorph.backend.repository.UserAccountRepository;

import org.springframework.stereotype.Service;

/**
 * Verifies login credentials and OTPs. Roles and permissions are not handled here; they are
 * resolved per request by {@link PermissionService}.
 */
@Service
public class AuthenticationService {

    private final UserAccountRepository userAccountRepository;
    private final EmployeeRepository employeeRepository;
    private final TwoFactorService twoFactorService;
    private final OtpDeliveryService otpDeliveryService;
    private final UserAccessLogService userAccessLogService;

    public AuthenticationService(UserAccountRepository userAccountRepository,
                                 EmployeeRepository employeeRepository,
                                 TwoFactorService twoFactorService,
                                 OtpDeliveryService otpDeliveryService,
                                 UserAccessLogService userAccessLogService) {

        this.userAccountRepository = userAccountRepository;
        this.employeeRepository = employeeRepository;
        this.twoFactorService = twoFactorService;
        this.otpDeliveryService = otpDeliveryService;
        this.userAccessLogService = userAccessLogService;
    }

    /**
     * Verifies the email and password. If the account has a temporary password, the outcome asks
     * for a password change; otherwise a login OTP is sent.
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

        getPersonalEmailOrThrow(account);

        String rawOtp = twoFactorService.createLoginOtp(account.getUserAccountId());
        var otpPreview = otpDeliveryService.sendLoginOtp(account.getCompanyEmail(), rawOtp);

        return new LoginAttemptOutcome(
                LoginAttemptOutcome.Type.OTP_SENT,
                account.getUserAccountId(),
                account.getCompanyEmail(),
                "OTP verification required.",
                otpPreview
        );
    }

    /**
     * Verifies the login OTP, records the successful login, and returns the authenticated account.
     */
    public UserAccount verifyLoginOtp(int userAccountId, String rawOtp) {
        validateUserAccountId(userAccountId);

        UserAccount account = userAccountRepository.findById(userAccountId);

        if (account == null) {
            throw new AuthenticationException("User account not found.");
        }

        validateAccountForLogin(account);
        twoFactorService.verifyLoginOtp(userAccountId, rawOtp);

        userAccountRepository.updateLastLogin(userAccountId);
        logSuccess(account);

        return account;
    }

    private void validateAccountForLogin(UserAccount account) {
        if (!account.isActive()) {
            logFailed(account.getCompanyEmail(), "Login failed because account is inactive.");
            throw new AuthenticationException("Account is inactive.");
        }

        if (account.isLocked()) {
            logFailed(account.getCompanyEmail(), "Login failed because account is locked.");
            throw new AuthenticationException("Account is locked.");
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

    private String getPersonalEmailOrThrow(UserAccount account) {
        String personalEmail = employeeRepository.findPersonalEmailByEmployeeId(account.getEmployeeId());

        if (personalEmail == null || personalEmail.isBlank()) {
            throw new ValidationException("Registered personal email is required for OTP verification.");
        }

        return personalEmail.trim();
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
            logFailed(account.getCompanyEmail(), "Login failed because password did not match.");
            throw new AuthenticationException("Invalid email or password.");
        }
    }

    private void validateUserAccountId(int userAccountId) {
        if (userAccountId <= 0) {
            throw new ValidationException("User account ID is required.");
        }
    }

    private void logSuccess(UserAccount account) {
        userAccessLogService.logLoginSuccess(account.getUserAccountId(), "Login successful.");
    }

    private void logFailed(String companyEmail, String details) {
        userAccessLogService.logLoginFailed(companyEmail, details);
    }
}
