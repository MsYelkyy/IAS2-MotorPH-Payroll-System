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

/**
 * Handles forgot password and password reset business logic.
 */
public class PasswordResetService {

    private final PasswordResetRequestRepository passwordResetRequestRepository;
    private final UserAccountRepository userAccountRepository;
    private final EmployeeRepository employeeRepository;
    private final TwoFactorService twoFactorService;
    private final EmailService emailService;
    private final UserAccessLogService userAccessLogService;

    // constructors
    public PasswordResetService(PasswordResetRequestRepository passwordResetRequestRepository,
                                UserAccountRepository userAccountRepository,
                                EmployeeRepository employeeRepository,
                                TwoFactorService twoFactorService,
                                EmailService emailService,
                                UserAccessLogService userAccessLogService) {

        if (passwordResetRequestRepository == null) {
            throw new IllegalArgumentException("PasswordResetRequestRepository is required.");
        }

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

        if (userAccessLogService == null) {
            throw new IllegalArgumentException("UserAccessLogService is required.");
        }

        this.passwordResetRequestRepository = passwordResetRequestRepository;
        this.userAccountRepository = userAccountRepository;
        this.employeeRepository = employeeRepository;
        this.twoFactorService = twoFactorService;
        this.emailService = emailService;
        this.userAccessLogService = userAccessLogService;
    }

    // Starts a password reset request and sends OTP to registered personal email.
    public int requestPasswordReset(String companyEmail) {
        validateCompanyEmail(companyEmail);

        UserAccount account = userAccountRepository.findByCompanyEmail(companyEmail);

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

        // Only create the reset request here; actual OTP/email will be sent after IT approval.
        PasswordResetRequest request = new PasswordResetRequest(account.getUserAccountId());
        return passwordResetRequestRepository.saveAndReturnId(request);
    }

    // Returns all pending password reset requests for IT/Admin to review.
    public java.util.List<PasswordResetRequest> listPendingRequests() {
        return passwordResetRequestRepository.findPendingRequests();
    }

    // Called by IT/Admin to approve a pending reset request; this sends the password reset OTP.
    public void approvePasswordReset(int resetRequestId, int approverEmployeeId) {
        validateResetRequestId(resetRequestId);

        PasswordResetRequest request = getPendingRequestOrThrow(resetRequestId);

        UserAccount account = userAccountRepository.findById(request.getUserAccountId());

        if (account == null) {
            throw new EntityNotFoundException("User account not found for reset request.");
        }

        String personalEmail = employeeRepository.findPersonalEmailByEmployeeId(account.getEmployeeId());

        if (personalEmail == null || personalEmail.isBlank()) {
            throw new ValidationException("Registered personal email is required for password reset.");
        }

        String rawOtp = twoFactorService.createOtp(
                account.getUserAccountId(),
                resetRequestId,
                OtpPurpose.PASSWORD_RESET
        );

        emailService.sendOtp(personalEmail, rawOtp, "MotorPH Password Reset Verification");

        // Log the approval action
        try {
            userAccessLogService.logPasswordReset(approverEmployeeId, account.getEmployeeId());
        } catch (Exception ignored) {
            // Logging failure should not block approval
        }
    }

    // Verifies the reset OTP.
    public void verifyResetOtp(int resetRequestId, String rawOtp) {
        validateResetRequestId(resetRequestId);

        PasswordResetRequest request = getPendingRequestOrThrow(resetRequestId);
        twoFactorService.verifyPasswordResetOtp(request.getResetRequestId(), rawOtp);
    }

    // Completes password reset after OTP verification.
    public void resetPassword(int resetRequestId, String newPassword, String confirmPassword) {
        validateResetRequestId(resetRequestId);
        validatePassword(newPassword, confirmPassword);

        PasswordResetRequest request = getPendingRequestOrThrow(resetRequestId);

        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(newPassword, salt);

        userAccountRepository.updatePassword(request.getUserAccountId(), hash, salt);
        passwordResetRequestRepository.markCompleted(resetRequestId);
    }

    // Gets a pending reset request or throws an error.
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

    // Validates company email input.
    private void validateCompanyEmail(String companyEmail) {
        if (companyEmail == null || companyEmail.isBlank()) {
            throw new ValidationException("Company email is required.");
        }
    }

    // Validates reset request id input.
    private void validateResetRequestId(int resetRequestId) {
        if (resetRequestId <= 0) {
            throw new ValidationException("Reset request ID is required.");
        }
    }

    // Validates password input.
    private void validatePassword(String password, String confirmPassword) {
        if (password == null || password.isBlank()) {
            throw new ValidationException("New password is required.");
        }

        if (confirmPassword == null || confirmPassword.isBlank()) {
            throw new ValidationException("Confirm password is required.");
        }

        if (password.length() < 8) {
            throw new ValidationException("New password must be at least 8 characters.");
        }

        if (!password.equals(confirmPassword)) {
            throw new ValidationException("New password and confirm password do not match.");
        }
    }
}