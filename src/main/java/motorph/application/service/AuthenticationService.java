package motorph.application.service;

import motorph.application.dto.login.LoginRequestDTO;
import motorph.application.dto.login.LoginResultDTO;
import motorph.application.exception.AuthenticationException;
import motorph.application.exception.ValidationException;
import motorph.application.security.PasswordHasher;
import motorph.domain.model.UserAccount;
import motorph.domain.value.OtpPurpose;
import motorph.infrastructure.repository.EmployeeRepository;
import motorph.infrastructure.repository.UserAccountRepository;

/**
 * Handles user authentication with password validation and OTP verification.
 */
public class AuthenticationService {

    private final UserAccountRepository userAccountRepository;
    private final EmployeeRepository employeeRepository;
    private final TwoFactorService twoFactorService;
    private final EmailService emailService;
    private final UserAccessLogService userAccessLogService;

    // constructors
    public AuthenticationService(UserAccountRepository userAccountRepository,
                                 EmployeeRepository employeeRepository,
                                 TwoFactorService twoFactorService,
                                 EmailService emailService,
                                 UserAccessLogService userAccessLogService) {

        if (userAccountRepository == null) {
            throw new IllegalArgumentException("UserAccountRepository is required.");
        }

        if (employeeRepository == null) {
            throw new IllegalArgumentException("EmployeeRepository is required.");
        }

        if (twoFactorService == null) {
            throw new IllegalArgumentException("TwoFactorService is required.");
        }

        if (emailService == null) {
            throw new IllegalArgumentException("EmailService is required.");
        }

        this.userAccountRepository = userAccountRepository;
        this.employeeRepository = employeeRepository;
        this.twoFactorService = twoFactorService;
        this.emailService = emailService;
        this.userAccessLogService = userAccessLogService;
    }

    // Authenticates email and password then sends OTP.
    public LoginResultDTO authenticate(LoginRequestDTO request) {
        validateRequest(request);

        UserAccount account = userAccountRepository.findByCompanyEmail(request.getCompanyEmail());

        if (account == null) {
            logFailed(request.getCompanyEmail(), "Login failed because email was not found.");
            throw new AuthenticationException("Invalid email or password.");
        }

        validateAccountStatus(account);
        validateStoredCredentials(account);
        validatePassword(request.getPassword(), account);

        String personalEmail = employeeRepository.findPersonalEmailByEmployeeId(account.getEmployeeId());

        if (personalEmail == null || personalEmail.isBlank()) {
            throw new ValidationException("Registered personal email is required for OTP verification.");
        }

        String rawOtp = twoFactorService.createOtp(
                account.getUserAccountId(),
                null,
                OtpPurpose.LOGIN_2FA
        );

        emailService.sendOtp(personalEmail, rawOtp, "MotorPH Login Verification");

        return buildOtpRequiredResult(account);
    }

    // Verifies login OTP and completes login.
    public LoginResultDTO verifyLoginOtp(int userAccountId, String rawOtp) {
        validateUserAccountId(userAccountId);

        UserAccount account = userAccountRepository.findById(userAccountId);

        if (account == null) {
            throw new AuthenticationException("User account not found.");
        }

        validateAccountStatus(account);

        twoFactorService.verifyLoginOtp(userAccountId, rawOtp);

        userAccountRepository.updateLastLogin(userAccountId);
        logSuccess(account);

        return buildSuccessResult(account);
    }

    // Builds the OTP required login result.
    private LoginResultDTO buildOtpRequiredResult(UserAccount account) {
        return new LoginResultDTO(
                account.getUserAccountId(),
                account.getEmployeeId(),
                account.getCompanyEmail(),
                account.getRole(),
                false,
                true,
                "OTP verification required."
        );
    }

    // Builds the successful login result.
    private LoginResultDTO buildSuccessResult(UserAccount account) {
        return new LoginResultDTO(
                account.getUserAccountId(),
                account.getEmployeeId(),
                account.getCompanyEmail(),
                account.getRole(),
                true,
                false,
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

    // Validates account status before login.
    private void validateAccountStatus(UserAccount account) {
        if (!account.isActive()) {
            logFailed(account.getCompanyEmail(), "Login failed because account is inactive.");
            throw new AuthenticationException("Account is inactive.");
        }

        if (account.isLocked()) {
            logFailed(account.getCompanyEmail(), "Login failed because account is locked.");
            throw new AuthenticationException("Account is locked.");
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

    // Validates the entered password.
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

    // Validates user account id input.
    private void validateUserAccountId(int userAccountId) {
        if (userAccountId <= 0) {
            throw new ValidationException("User account ID is required.");
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