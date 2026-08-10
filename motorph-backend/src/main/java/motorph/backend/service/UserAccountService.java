package motorph.backend.service;

import motorph.backend.dto.auth.CredentialsPreview;
import motorph.backend.dto.user.AccountCountsDTO;
import motorph.backend.dto.user.AccountSummaryDTO;
import motorph.backend.dto.user.UserAccountRowDTO;
import motorph.backend.domain.Employee;
import motorph.backend.domain.UserAccount;
import motorph.backend.domain.value.Role;
import motorph.backend.exception.AuthenticationException;
import motorph.backend.exception.BusinessRuleException;
import motorph.backend.exception.EntityNotFoundException;
import motorph.backend.exception.ValidationException;
import motorph.backend.repository.EmployeeRepository;
import motorph.backend.repository.UserAccountRepository;
import motorph.backend.security.PasswordHasher;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

/**
 * Handles account provisioning for newly created/deactivated employees plus
 * the IT account-management operations (list/lock/unlock/enable/disable/
 * roles) ported from the desktop's UserAccountService + ITUserAccountService
 * in Phase 5a.
 *
 * The employee-name join reads EmployeeRepository directly rather than
 * EmployeeService: EmployeeService already depends on this service for
 * account provisioning, so going back through it would be a constructor
 * injection cycle.
 */
@Service
public class UserAccountService {

    private static final int TEMPORARY_PASSWORD_EXPIRY_HOURS = 24;
    private static final String TEMPORARY_PASSWORD_PREFIX = "MotorPH@";
    private static final String COMPANY_EMAIL_DOMAIN = "@motorph.com";

    /** Carries the newly created account together with its delivery preview (dev-only, may be null). */
    public record AccountProvisioningResult(UserAccount account, CredentialsPreview credentialsPreview) {
    }

    private final UserAccountRepository userAccountRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeCredentialsDeliveryService credentialsDeliveryService;
    private final UserAccessLogService userAccessLogService;

    public UserAccountService(UserAccountRepository userAccountRepository,
                              EmployeeRepository employeeRepository,
                              EmployeeCredentialsDeliveryService credentialsDeliveryService,
                              UserAccessLogService userAccessLogService) {
        this.userAccountRepository = userAccountRepository;
        this.employeeRepository = employeeRepository;
        this.credentialsDeliveryService = credentialsDeliveryService;
        this.userAccessLogService = userAccessLogService;
    }

    // creates an active employee account with a temporary password
    public AccountProvisioningResult createTemporaryEmployeeAccount(int employeeId,
                                                                     String firstName,
                                                                     String lastName,
                                                                     String personalEmail) {
        validateEmployeeId(employeeId);
        validatePersonalEmail(personalEmail);

        String companyEmail = generateUniqueCompanyEmail(firstName, lastName);
        String temporaryPassword = generateTemporaryPassword(employeeId);
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(temporaryPassword, salt);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(TEMPORARY_PASSWORD_EXPIRY_HOURS);

        UserAccount account = new UserAccount(
                null,
                employeeId,
                companyEmail,
                companyEmail,
                hash,
                salt,
                List.of(Role.EMPLOYEE),
                Role.EMPLOYEE,
                false,
                true,
                null,
                true,
                expiresAt
        );

        userAccountRepository.save(account);

        UserAccount savedAccount = userAccountRepository.findByCompanyEmail(companyEmail);

        if (savedAccount == null) {
            throw new IllegalStateException("User account was not created successfully.");
        }

        CredentialsPreview preview = credentialsDeliveryService.sendEmployeeAccountCredentials(
                personalEmail, companyEmail, temporaryPassword);

        return new AccountProvisioningResult(savedAccount, preview);
    }

    // disables and locks the account linked to an employee
    public void deactivateAccountByEmployeeId(int employeeId) {
        validateEmployeeId(employeeId);

        UserAccount account = userAccountRepository.findByEmployeeId(employeeId);

        if (account == null) {
            return;
        }

        userAccountRepository.updateLockStatus(account.getUserAccountId(), true);
        userAccountRepository.updateActiveStatus(account.getUserAccountId(), false);
    }

    // returns every account as a display row, joined with the employee's name
    public List<UserAccountRowDTO> getUserAccountRows() {
        Map<Integer, String> namesByEmployeeId = employeeRepository.findAllWithInactive()
                .stream()
                .collect(Collectors.toMap(Employee::getEmployeeId, Employee::getFullName, (first, second) -> first));

        return userAccountRepository.findAll()
                .stream()
                .map(account -> toRow(account, namesByEmployeeId))
                .toList();
    }

    // returns the display-only summary of one account
    public AccountSummaryDTO getAccountSummary(int userAccountId) {
        validateUserAccountId(userAccountId);
        return toSummary(requireAccount(userAccountId));
    }

    // returns the display-only summary of the account belonging to an employee
    public AccountSummaryDTO getAccountSummaryByEmployeeId(int employeeId) {
        validateEmployeeId(employeeId);

        UserAccount account = userAccountRepository.findByEmployeeId(employeeId);

        if (account == null) {
            throw new EntityNotFoundException("This employee has no user account.");
        }

        return toSummary(account);
    }

    // returns active/locked/disabled tallies for the dashboards
    public AccountCountsDTO getAccountCounts() {
        return new AccountCountsDTO(
                userAccountRepository.countActive(),
                userAccountRepository.countLocked(),
                userAccountRepository.countDisabled()
        );
    }

    // locks an account, blocking login until IT unlocks it
    public void lockAccount(int actorUserAccountId, int userAccountId) {
        changeStatus(actorUserAccountId, userAccountId,
                id -> userAccountRepository.updateLockStatus(id, true));
    }

    // unlocks a locked account
    public void unlockAccount(int actorUserAccountId, int userAccountId) {
        changeStatus(actorUserAccountId, userAccountId,
                id -> userAccountRepository.updateLockStatus(id, false));
    }

    // enables a disabled account
    public void enableAccount(int actorUserAccountId, int userAccountId) {
        changeStatus(actorUserAccountId, userAccountId,
                id -> userAccountRepository.updateActiveStatus(id, true));
    }

    // disables an account
    public void disableAccount(int actorUserAccountId, int userAccountId) {
        changeStatus(actorUserAccountId, userAccountId,
                id -> userAccountRepository.updateActiveStatus(id, false));
    }

    // replaces the roles assigned to an account
    public void updateRoles(int userAccountId, List<Role> roles) {
        validateUserAccountId(userAccountId);
        validateRoles(roles);
        requireAccount(userAccountId);

        userAccountRepository.updateRoles(userAccountId, roles);
    }

    // replaces the caller's own password after re-verifying the current one
    public void changeOwnPassword(int userAccountId, String currentPassword, String newPassword) {
        validateUserAccountId(userAccountId);

        if (currentPassword == null || currentPassword.trim().isEmpty()) {
            throw new ValidationException("Current password is required.");
        }

        validateNewPassword(newPassword);

        UserAccount account = requireAccount(userAccountId);

        if (!PasswordHasher.verify(currentPassword, account.getPasswordSalt(), account.getPasswordHash())) {
            throw new AuthenticationException("Current password is incorrect.");
        }

        writePassword(userAccountId, newPassword);
    }

    // replaces an IT-issued temporary password with one the user chose
    public void completeTemporaryPasswordChange(int userAccountId, String newPassword, String confirmPassword) {
        validateUserAccountId(userAccountId);
        validateNewPassword(newPassword);

        if (!newPassword.equals(confirmPassword)) {
            throw new ValidationException("Passwords do not match.");
        }

        UserAccount account = requireAccount(userAccountId);

        // guards against this path being used to overwrite a normal account's
        // password - it only ever applies to an account still on a temp password
        if (!account.mustChangePassword()) {
            throw new BusinessRuleException("This account is not awaiting a temporary password change.");
        }

        writePassword(userAccountId, newPassword);
    }

    private void writePassword(int userAccountId, String newPassword) {
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(newPassword.trim(), salt);

        userAccountRepository.updatePassword(userAccountId, hash, salt);
    }

    private void validateNewPassword(String newPassword) {
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new ValidationException("New password is required.");
        }
    }

    // applies a status mutation and audits the before/after status
    private void changeStatus(int actorUserAccountId, int userAccountId, IntConsumer mutation) {
        validateUserAccountId(actorUserAccountId);
        validateUserAccountId(userAccountId);

        UserAccount before = requireAccount(userAccountId);
        mutation.accept(userAccountId);
        UserAccount after = requireAccount(userAccountId);

        userAccessLogService.logAccountStatusChanged(
                actorUserAccountId,
                before.getEmployeeId(),
                before.getAccountStatus().name(),
                after.getAccountStatus().name()
        );
    }

    private UserAccount requireAccount(int userAccountId) {
        UserAccount account = userAccountRepository.findById(userAccountId);

        if (account == null) {
            throw new EntityNotFoundException("User account not found.");
        }

        return account;
    }

    private AccountSummaryDTO toSummary(UserAccount account) {
        Employee employee = employeeRepository.findAnyById(account.getEmployeeId());

        return new AccountSummaryDTO(
                account.getUserAccountId(),
                account.getEmployeeId(),
                employee == null ? "Employee #" + account.getEmployeeId() : employee.getFullName(),
                account.getCompanyEmail()
        );
    }

    private UserAccountRowDTO toRow(UserAccount account, Map<Integer, String> namesByEmployeeId) {
        String fullName = namesByEmployeeId.getOrDefault(
                account.getEmployeeId(),
                "Employee #" + account.getEmployeeId());

        return new UserAccountRowDTO(
                account.getUserAccountId(),
                account.getEmployeeId(),
                fullName,
                account.getCompanyEmail(),
                account.getRoles(),
                account.getActiveRole(),
                account.getAccountStatus(),
                account.isLocked(),
                account.isActive(),
                account.hasMultipleRoles(),
                account.getLastLoginAt()
        );
    }

    // generates a unique company email for a new account
    private String generateUniqueCompanyEmail(String firstName, String lastName) {
        String baseEmail = buildBaseCompanyEmail(firstName, lastName);
        String candidate = baseEmail;
        int suffix = 2;

        while (userAccountRepository.existsByCompanyEmail(candidate)) {
            candidate = appendEmailSuffix(baseEmail, suffix);
            suffix++;
        }

        return candidate;
    }

    private String generateTemporaryPassword(int employeeId) {
        return TEMPORARY_PASSWORD_PREFIX + employeeId;
    }

    private String buildBaseCompanyEmail(String firstName, String lastName) {
        validateName(firstName, "First name is required.");
        validateName(lastName, "Last name is required.");

        return sanitizeEmailName(firstName) + "." + sanitizeEmailName(lastName) + COMPANY_EMAIL_DOMAIN;
    }

    private String appendEmailSuffix(String baseEmail, int suffix) {
        int atIndex = baseEmail.indexOf("@");

        if (atIndex < 0) {
            throw new IllegalStateException("Base company email is invalid.");
        }

        return baseEmail.substring(0, atIndex) + suffix + baseEmail.substring(atIndex);
    }

    private String sanitizeEmailName(String value) {
        return value.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
    }

    private void validatePersonalEmail(String personalEmail) {
        if (personalEmail == null || personalEmail.trim().isEmpty()) {
            throw new ValidationException("Personal email is required.");
        }

        if (!personalEmail.contains("@")) {
            throw new ValidationException("Personal email must be a valid email address.");
        }
    }

    private void validateEmployeeId(int employeeId) {
        if (employeeId <= 0) {
            throw new ValidationException("Employee ID is required.");
        }
    }

    private void validateUserAccountId(int userAccountId) {
        if (userAccountId <= 0) {
            throw new ValidationException("User account ID is required.");
        }
    }

    private void validateRoles(List<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new ValidationException("At least one role is required.");
        }

        for (Role role : roles) {
            if (role == null) {
                throw new ValidationException("Role cannot be null.");
            }
        }
    }

    private void validateName(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(message);
        }
    }
}
