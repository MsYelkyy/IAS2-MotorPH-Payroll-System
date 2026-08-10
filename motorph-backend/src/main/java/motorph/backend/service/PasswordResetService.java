package motorph.backend.service;

import motorph.backend.domain.Employee;
import motorph.backend.domain.PasswordResetRequest;
import motorph.backend.domain.UserAccount;
import motorph.backend.domain.value.PasswordResetStatus;
import motorph.backend.dto.auth.CredentialsPreview;
import motorph.backend.dto.user.PasswordResetCountsDTO;
import motorph.backend.dto.user.PasswordResetRequestRowDTO;
import motorph.backend.exception.BusinessRuleException;
import motorph.backend.exception.EntityNotFoundException;
import motorph.backend.exception.ValidationException;
import motorph.backend.repository.EmployeeRepository;
import motorph.backend.repository.PasswordResetRequestRepository;
import motorph.backend.repository.UserAccountRepository;
import motorph.backend.security.PasswordHasher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles password reset requests and IT resolution. Ported from the desktop
 * service with two deliberate changes:
 *
 * - requestPasswordReset never tells its caller whether the account exists,
 *   is active, is unlocked, has a personal email on file, or already has a
 *   pending request. Those all log server-side and return silently, because
 *   this path is reachable unauthenticated (see the forgot-password endpoint).
 * - The desktop's self-service "verify OTP then set your own new password"
 *   methods are not ported: no UI ever called them, resolution has always
 *   been IT-driven through the temporary-password path.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private static final int TEMPORARY_PASSWORD_EXPIRY_MINUTES = 60;
    private static final int TEMPORARY_PASSWORD_LENGTH = 10;
    private static final String TEMPORARY_PASSWORD_PREFIX = "MPH-";
    private static final String TEMPORARY_PASSWORD_CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final PasswordResetRequestRepository passwordResetRequestRepository;
    private final UserAccountRepository userAccountRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordResetDeliveryService deliveryService;
    private final UserAccessLogService userAccessLogService;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(PasswordResetRequestRepository passwordResetRequestRepository,
                                UserAccountRepository userAccountRepository,
                                EmployeeRepository employeeRepository,
                                PasswordResetDeliveryService deliveryService,
                                UserAccessLogService userAccessLogService) {
        this.passwordResetRequestRepository = passwordResetRequestRepository;
        this.userAccountRepository = userAccountRepository;
        this.employeeRepository = employeeRepository;
        this.deliveryService = deliveryService;
        this.userAccessLogService = userAccessLogService;
    }

    // records a pending reset request when the account qualifies; stays silent
    // either way so an unauthenticated caller learns nothing about the account
    public void requestPasswordReset(String companyEmail) {
        if (companyEmail == null || companyEmail.trim().isEmpty()) {
            throw new ValidationException("Company email is required.");
        }

        String email = companyEmail.trim();

        UserAccount account = userAccountRepository.findByCompanyEmail(email);

        if (account == null || !account.isActive() || account.isLocked()) {
            log.info("Password reset request ignored for {}: no eligible account.", email);
            return;
        }

        String personalEmail = employeeRepository.findPersonalEmailByEmployeeId(account.getEmployeeId());

        if (personalEmail == null || personalEmail.isBlank()) {
            log.info("Password reset request ignored for {}: no personal email on file.", email);
            return;
        }

        if (passwordResetRequestRepository.findLatestPendingByUserAccountId(account.getUserAccountId()) != null) {
            log.info("Password reset request ignored for {}: a pending request already exists.", email);
            return;
        }

        passwordResetRequestRepository.saveAndReturnId(new PasswordResetRequest(account.getUserAccountId()));
    }

    // returns reset requests as display rows, optionally narrowed to one status
    public List<PasswordResetRequestRowDTO> getRequestRows(PasswordResetStatus status) {
        List<PasswordResetRequest> requests = status == PasswordResetStatus.PENDING
                ? passwordResetRequestRepository.findPendingRequests()
                : passwordResetRequestRepository.findAll();

        if (status != null && status != PasswordResetStatus.PENDING) {
            requests = requests.stream()
                    .filter(request -> request.getRequestStatus() == status)
                    .toList();
        }

        Map<Integer, UserAccount> accountsById = userAccountRepository.findAll()
                .stream()
                .collect(Collectors.toMap(UserAccount::getUserAccountId, account -> account, (a, b) -> a));

        Map<Integer, String> namesByEmployeeId = employeeRepository.findAllWithInactive()
                .stream()
                .collect(Collectors.toMap(Employee::getEmployeeId, Employee::getFullName, (a, b) -> a));

        return requests.stream()
                .map(request -> toRow(request, accountsById, namesByEmployeeId))
                .toList();
    }

    // returns the summary card tallies
    public PasswordResetCountsDTO getCounts() {
        return new PasswordResetCountsDTO(
                passwordResetRequestRepository.countByStatus(PasswordResetStatus.PENDING),
                passwordResetRequestRepository.countByStatus(PasswordResetStatus.COMPLETED),
                passwordResetRequestRepository.countByStatus(PasswordResetStatus.CANCELLED),
                passwordResetRequestRepository.countAll()
        );
    }

    // returns the pending request count on its own - the dashboard metric
    public int getPendingRequestCount() {
        return passwordResetRequestRepository.countByStatus(PasswordResetStatus.PENDING);
    }

    // issues a temporary password for a pending request and closes it out
    public CredentialsPreview processPasswordResetRequest(int resetRequestId,
                                                          int resolverUserAccountId,
                                                          int resolverEmployeeId) {
        validateResetRequestId(resetRequestId);

        PasswordResetRequest request = getPendingRequestOrThrow(resetRequestId);
        UserAccount account = getResettableAccountOrThrow(request.getUserAccountId());

        String personalEmail = employeeRepository.findPersonalEmailByEmployeeId(account.getEmployeeId());

        if (personalEmail == null || personalEmail.isBlank()) {
            throw new ValidationException("Registered personal email is required for password reset.");
        }

        String temporaryPassword = generateTemporaryPassword();
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(temporaryPassword, salt);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(TEMPORARY_PASSWORD_EXPIRY_MINUTES);

        userAccountRepository.updateTemporaryPassword(account.getUserAccountId(), hash, salt, expiresAt);
        passwordResetRequestRepository.markManuallyResolved(resetRequestId, resolverEmployeeId);

        CredentialsPreview preview = deliveryService.sendTemporaryPassword(
                personalEmail, account.getCompanyEmail(), temporaryPassword);

        logPasswordReset(resolverUserAccountId, account.getEmployeeId());

        return preview;
    }

    // cancels a pending request without issuing a password
    public void cancelPasswordResetRequest(int resetRequestId, int cancellerEmployeeId) {
        validateResetRequestId(resetRequestId);

        PasswordResetRequest request = getPendingRequestOrThrow(resetRequestId);
        passwordResetRequestRepository.markCancelled(request.getResetRequestId(), cancellerEmployeeId);
    }

    private PasswordResetRequestRowDTO toRow(PasswordResetRequest request,
                                             Map<Integer, UserAccount> accountsById,
                                             Map<Integer, String> namesByEmployeeId) {
        UserAccount account = accountsById.get(request.getUserAccountId());

        String employeeName = "Employee";
        String companyEmail = "-";

        if (account != null) {
            companyEmail = account.getCompanyEmail();
            employeeName = namesByEmployeeId.getOrDefault(
                    account.getEmployeeId(),
                    "Employee #" + account.getEmployeeId());
        }

        return new PasswordResetRequestRowDTO(
                request.getResetRequestId(),
                request.getUserAccountId(),
                employeeName,
                companyEmail,
                request.getRequestStatus(),
                request.getRequestedAt(),
                request.getResolvedByEmployeeId() == null
                        ? "-"
                        : "Employee #" + request.getResolvedByEmployeeId(),
                request.getCompletedAt(),
                request.getCancelledAt()
        );
    }

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

    private UserAccount getResettableAccountOrThrow(int userAccountId) {
        UserAccount account = userAccountRepository.findById(userAccountId);

        if (account == null || !account.isActive()) {
            throw new EntityNotFoundException("Active user account not found.");
        }

        if (account.isLocked()) {
            throw new BusinessRuleException("Account is locked.");
        }

        return account;
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(TEMPORARY_PASSWORD_PREFIX);

        for (int i = 0; i < TEMPORARY_PASSWORD_LENGTH; i++) {
            password.append(TEMPORARY_PASSWORD_CHARACTERS.charAt(
                    secureRandom.nextInt(TEMPORARY_PASSWORD_CHARACTERS.length())));
        }

        return password.toString();
    }

    // a failed audit write must not undo an already-applied password reset
    private void logPasswordReset(int resolverUserAccountId, int targetEmployeeId) {
        try {
            userAccessLogService.logPasswordReset(resolverUserAccountId, targetEmployeeId);
        } catch (RuntimeException e) {
            log.warn("Failed to write password reset audit log.", e);
        }
    }

    private void validateResetRequestId(int resetRequestId) {
        if (resetRequestId <= 0) {
            throw new ValidationException("Reset request ID is required.");
        }
    }
}
