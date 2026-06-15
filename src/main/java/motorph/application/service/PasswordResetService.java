package motorph.application.service;

import motorph.application.exception.BusinessRuleException;
import motorph.application.exception.EntityNotFoundException;
import motorph.application.exception.ValidationException;
import motorph.application.security.PasswordHasher;
import motorph.domain.model.PasswordResetRequest;
import motorph.domain.model.UserAccount;
import motorph.domain.value.OtpPurpose;
import motorph.infrastructure.repository.EmployeeRepository;
import motorph.infrastructure.repository.PasswordResetRequestRepository;
import motorph.infrastructure.repository.UserAccountRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Handles forgot password requests and IT/Admin temporary password processing.
 */
public class PasswordResetService {

    private static final int TEMPORARY_PASSWORD_EXPIRY_MINUTES = 5;
    private static final int TEMPORARY_PASSWORD_LENGTH = 10;
    private static final String TEMPORARY_PASSWORD_PREFIX = "MPH-";
    private static final String TEMPORARY_PASSWORD_CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final PasswordResetRequestRepository passwordResetRequestRepository;
    private final UserAccountRepository userAccountRepository;
    private final EmployeeRepository employeeRepository;
    private final TwoFactorService twoFactorService;
    private final EmailService emailService;
    private final UserAccessLogService userAccessLogService;
    private final SecureRandom secureRandom = new SecureRandom();

    // constructors
    public PasswordResetService(PasswordResetRequestRepository passwordResetRequestRepository,
                                UserAccountRepository userAccountRepository,
                                EmployeeRepository employeeRepository,
                                TwoFactorService twoFactorService,
                                EmailService emailService,
                                UserAccessLogService userAccessLogService) {

        this.passwordResetRequestRepository = require(
                passwordResetRequestRepository,
                "PasswordResetRequestRepository is required."
        );
        this.userAccountRepository = require(userAccountRepository, "UserAccountRepository is required.");
        this.employeeRepository = require(employeeRepository, "EmployeeRepository is required.");
        this.twoFactorService = require(twoFactorService, "TwoFactorService is required.");
        this.emailService = require(emailService, "EmailService is required.");
        this.userAccessLogService = require(userAccessLogService, "UserAccessLogService is required.");
    }

    // creates a pending forgot password request
    public int requestPasswordReset(String companyEmail) {
        validateCompanyEmail(companyEmail);

        UserAccount account = userAccountRepository.findByCompanyEmail(companyEmail.trim());

        if (account == null || !account.isActive()) {
            throw new EntityNotFoundException("Active user account not found.");
        }

        if (account.isLocked()) {
            throw new BusinessRuleException("Account is locked.");
        }

        String personalEmail = employeeRepository.findPersonalEmailByEmployeeId(account.getEmployeeId());

        if (personalEmail == null || personalEmail.isBlank()) {
            throw new ValidationException("Registered personal email is required for password reset.");
        }

        PasswordResetRequest existingRequest =
                passwordResetRequestRepository.findLatestPendingByUserAccountId(account.getUserAccountId());

        if (existingRequest != null) {
            throw new BusinessRuleException("You already have a pending password reset request.");
        }

        PasswordResetRequest request = new PasswordResetRequest(account.getUserAccountId());
        int requestId = passwordResetRequestRepository.saveAndReturnId(request);

        String rawOtp = twoFactorService.createOtp(account.getUserAccountId(), requestId, OtpPurpose.PASSWORD_RESET);
        emailService.sendOtp(personalEmail, rawOtp, "MotorPH Password Reset");

        return requestId;
    }

    // verifies the OTP for a self-service password reset
    public void verifyResetOtp(int resetRequestId, String rawOtp) {
        validateResetRequestId(resetRequestId);
        twoFactorService.verifyPasswordResetOtp(resetRequestId, rawOtp);
    }

    // completes a self-service password reset after OTP is verified
    public void resetPassword(int resetRequestId, String newPassword, String confirmPassword) {
        validateResetRequestId(resetRequestId);

        if (newPassword == null || newPassword.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters.");
        }

        if (!newPassword.equals(confirmPassword)) {
            throw new ValidationException("Passwords do not match.");
        }

        PasswordResetRequest request = passwordResetRequestRepository.findById(resetRequestId);

        if (request == null || !request.isPending()) {
            throw new BusinessRuleException("Password reset request is invalid or already completed.");
        }

        UserAccount account = getActiveAccountOrThrow(request.getUserAccountId());
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(newPassword, salt);
        userAccountRepository.updatePassword(account.getUserAccountId(), hash, salt);
        passwordResetRequestRepository.markCompleted(resetRequestId);
    }

    // returns pending password reset requests for IT/Admin review
    public List<PasswordResetRequest> listPendingRequests() {
        return passwordResetRequestRepository.findPendingRequests();
    }

    // returns pending password reset request count
    public int getPendingRequestCount() {
        return listPendingRequests().size();
    }

    // processes a pending request and emails a temporary password
    public void processPasswordResetRequest(int resetRequestId, int resolverEmployeeId) {
        validateResetRequestId(resetRequestId);
        validateEmployeeId(resolverEmployeeId);

        PasswordResetRequest request = getPendingRequestOrThrow(resetRequestId);
        UserAccount account = getActiveAccountOrThrow(request.getUserAccountId());

        String personalEmail = employeeRepository.findPersonalEmailByEmployeeId(account.getEmployeeId());

        if (personalEmail == null || personalEmail.isBlank()) {
            throw new ValidationException("Registered personal email is required for password reset.");
        }

        String temporaryPassword = generateTemporaryPassword();
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(temporaryPassword, salt);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(TEMPORARY_PASSWORD_EXPIRY_MINUTES);

        userAccountRepository.updateTemporaryPassword(
                account.getUserAccountId(),
                hash,
                salt,
                expiresAt
        );

        passwordResetRequestRepository.markManuallyResolved(resetRequestId, resolverEmployeeId);

        emailService.sendTemporaryPassword(personalEmail, account.getCompanyEmail(), temporaryPassword);

        logPasswordReset(resolverEmployeeId, account.getEmployeeId());
    }

    // keeps old method name as wrapper for IT/Admin approval
    public void approvePasswordReset(int resetRequestId, int approverEmployeeId) {
        processPasswordResetRequest(resetRequestId, approverEmployeeId);
    }

    // cancels a pending password reset request
    public void cancelPasswordResetRequest(int resetRequestId, int cancellerEmployeeId) {
        validateResetRequestId(resetRequestId);
        validateEmployeeId(cancellerEmployeeId);

        PasswordResetRequest request = getPendingRequestOrThrow(resetRequestId);
        passwordResetRequestRepository.markCancelled(request.getResetRequestId(), cancellerEmployeeId);
    }

    // gets a pending reset request or throws an error
    private PasswordResetRequest getPendingRequestOrThrow(int resetRequestId) {
        PasswordResetRequest request = passwordResetRequestRepository.findById(resetRequestId);

        if (request == null) {
            throw new EntityNotFoundException("Password reset request not found.");
        }

        if (!request.isPending()) {
            throw new BusinessRuleException("Password reset request is no longer pending.");
        }

        return request;
    }

    // gets an active user account or throws an error
    private UserAccount getActiveAccountOrThrow(int userAccountId) {
        UserAccount account = userAccountRepository.findById(userAccountId);

        if (account == null || !account.isActive()) {
            throw new EntityNotFoundException("Active user account not found.");
        }

        if (account.isLocked()) {
            throw new BusinessRuleException("Account is locked.");
        }

        return account;
    }

    // generates a temporary password
    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(TEMPORARY_PASSWORD_PREFIX);

        for (int i = 0; i < TEMPORARY_PASSWORD_LENGTH; i++) {
            int index = secureRandom.nextInt(TEMPORARY_PASSWORD_CHARACTERS.length());
            password.append(TEMPORARY_PASSWORD_CHARACTERS.charAt(index));
        }

        return password.toString();
    }

    // logs password reset action without blocking the reset flow
    private void logPasswordReset(int resolverEmployeeId, int targetEmployeeId) {
        try {
            userAccessLogService.logPasswordReset(resolverEmployeeId, targetEmployeeId);
        } catch (RuntimeException ignored) {
            // Logging failure should not block password reset processing.
        }
    }

    // validates company email input
    private void validateCompanyEmail(String companyEmail) {
        if (companyEmail == null || companyEmail.trim().isEmpty()) {
            throw new ValidationException("Company email is required.");
        }
    }

    // validates reset request id input
    private void validateResetRequestId(int resetRequestId) {
        if (resetRequestId <= 0) {
            throw new ValidationException("Reset request ID is required.");
        }
    }

    // validates employee id input
    private void validateEmployeeId(int employeeId) {
        if (employeeId <= 0) {
            throw new ValidationException("Employee ID is required.");
        }
    }

    // validates required dependency
    private <T> T require(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }
}