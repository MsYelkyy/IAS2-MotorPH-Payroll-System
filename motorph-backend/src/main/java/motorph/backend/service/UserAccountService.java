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
import motorph.backend.repository.TokenRevocationRepository;
import motorph.backend.repository.TotpCredentialRepository;
import motorph.backend.repository.UserAccountRepository;
import motorph.backend.security.PasswordHasher;
import motorph.backend.security.PasswordPolicy;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

/**
 * Creates and manages user accounts, including account creation, status changes,
 * role assignment, password changes, and authenticator resets.
 *
 * <p>Employee names are read through {@link EmployeeRepository} rather than
 * {@code EmployeeService}, because {@code EmployeeService} already depends on this class.
 */
@Service
public class UserAccountService {

    private static final int TEMPORARY_PASSWORD_EXPIRY_HOURS = 24;
    private static final int TEMPORARY_PASSWORD_LENGTH = 12;

    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMBERS = "0123456789";
    private static final String SPECIAL_CHARACTERS = "@#$%";
    private static final String PASSWORD_CHARACTERS =
            UPPERCASE + LOWERCASE + NUMBERS + SPECIAL_CHARACTERS;

    private static final String COMPANY_EMAIL_DOMAIN = "@motorph.com";

    /**
     * Contains the newly created account and its credentials preview.
     * The preview is only available when preview mode is enabled.
     */
    public record AccountProvisioningResult(
            UserAccount account,
            CredentialsPreview credentialsPreview) {
    }

    private final UserAccountRepository userAccountRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeCredentialsDeliveryService credentialsDeliveryService;
    private final UserAccessLogService userAccessLogService;
    private final TotpCredentialRepository totpCredentialRepository;
    private final TokenRevocationRepository tokenRevocationRepository;
    private final Clock clock;

    private final SecureRandom secureRandom = new SecureRandom();

    public UserAccountService(
            UserAccountRepository userAccountRepository,
            EmployeeRepository employeeRepository,
            EmployeeCredentialsDeliveryService credentialsDeliveryService,
            UserAccessLogService userAccessLogService,
            TotpCredentialRepository totpCredentialRepository,
            TokenRevocationRepository tokenRevocationRepository,
            Clock clock) {

        this.userAccountRepository = userAccountRepository;
        this.employeeRepository = employeeRepository;
        this.credentialsDeliveryService = credentialsDeliveryService;
        this.userAccessLogService = userAccessLogService;
        this.totpCredentialRepository = totpCredentialRepository;
        this.tokenRevocationRepository = tokenRevocationRepository;
        this.clock = clock;
    }

    /**
     * Creates an active account for the employee with a temporary password.
     */
    public AccountProvisioningResult createTemporaryEmployeeAccount(
            int employeeId,
            String firstName,
            String lastName,
            String personalEmail) {

        validateEmployeeId(employeeId);
        validatePersonalEmail(personalEmail);

        String companyEmail = generateUniqueCompanyEmail(firstName, lastName);
        String temporaryPassword = generateTemporaryPassword();

        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(temporaryPassword, salt);

        LocalDateTime expiresAt =
                LocalDateTime.now().plusHours(TEMPORARY_PASSWORD_EXPIRY_HOURS);

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

        UserAccount savedAccount =
                userAccountRepository.findByCompanyEmail(companyEmail);

        if (savedAccount == null) {
            throw new IllegalStateException(
                    "User account was not created successfully.");
        }

        CredentialsPreview preview =
                credentialsDeliveryService.sendEmployeeAccountCredentials(
                        personalEmail,
                        companyEmail,
                        temporaryPassword);

        return new AccountProvisioningResult(savedAccount, preview);
    }

    /**
     * Disables and locks the account linked to the employee.
     */
    public void deactivateAccountByEmployeeId(int employeeId) {

        validateEmployeeId(employeeId);

        UserAccount account =
                userAccountRepository.findByEmployeeId(employeeId);

        if (account == null) {
            return;
        }

        userAccountRepository.updateLockStatus(
                account.getUserAccountId(), true);

        userAccountRepository.updateActiveStatus(
                account.getUserAccountId(), false);
    }

    /**
     * Returns all user accounts together with the employee name.
     */
    public List<UserAccountRowDTO> getUserAccountRows() {

        Map<Integer, String> namesByEmployeeId =
                employeeRepository.findAllWithInactive()
                        .stream()
                        .collect(Collectors.toMap(
                                Employee::getEmployeeId,
                                Employee::getFullName,
                                (first, second) -> first));

        return userAccountRepository.findAll()
                .stream()
                .map(account ->
                        toRow(account, namesByEmployeeId))
                .toList();
    }

    /**
     * Returns the account details for the given user account.
     */
    public AccountSummaryDTO getAccountSummary(int userAccountId) {

        validateUserAccountId(userAccountId);

        return toSummary(requireAccount(userAccountId));
    }

    /**
     * Returns the account details linked to an employee.
     */
    public AccountSummaryDTO getAccountSummaryByEmployeeId(
            int employeeId) {

        validateEmployeeId(employeeId);

        UserAccount account =
                userAccountRepository.findByEmployeeId(employeeId);

        if (account == null) {
            throw new EntityNotFoundException(
                    "This employee has no user account.");
        }

        return toSummary(account);
    }

    /**
     * Returns the number of active, locked, and disabled accounts.
     */
    public AccountCountsDTO getAccountCounts() {

        return new AccountCountsDTO(
                userAccountRepository.countActive(),
                userAccountRepository.countLocked(),
                userAccountRepository.countDisabled()
        );
    }

    /**
     * Locks an account and signs the user out everywhere. Sessions stay revoked after an unlock.
     */
    public void lockAccount(
            int actorUserAccountId,
            int userAccountId) {

        changeStatus(
                actorUserAccountId,
                userAccountId,
                id -> {
                    userAccountRepository.updateLockStatus(id, true);
                    revokeSessions(id);
                });
    }

    /**
     * Unlocks a locked user account.
     */
    public void unlockAccount(
            int actorUserAccountId,
            int userAccountId) {

        changeStatus(
                actorUserAccountId,
                userAccountId,
                id -> userAccountRepository.updateLockStatus(id, false));
    }

    /**
     * Enables a user account.
     */
    public void enableAccount(
            int actorUserAccountId,
            int userAccountId) {

        changeStatus(
                actorUserAccountId,
                userAccountId,
                id -> userAccountRepository.updateActiveStatus(id, true));
    }

    /**
     * Disables a user account and signs the user out everywhere. Sessions stay revoked after the
     * account is enabled again.
     */
    public void disableAccount(
            int actorUserAccountId,
            int userAccountId) {

        changeStatus(
                actorUserAccountId,
                userAccountId,
                id -> {
                    userAccountRepository.updateActiveStatus(id, false);
                    revokeSessions(id);
                });
    }

    /**
     * Replaces all roles assigned to the account.
     */
    public void updateRoles(
            int actorUserAccountId,
            int userAccountId,
            List<Role> roles) {

        validateUserAccountId(userAccountId);
        validateRoles(roles);

        UserAccount account = requireAccount(userAccountId);

        userAccountRepository.updateRoles(
                userAccountId,
                roles);

        userAccessLogService.logEvent(
                actorUserAccountId,
                UserAccessLogService.ACTION_USER_ROLES_UPDATED,
                "Changed roles of employee ID "
                        + account.getEmployeeId()
                        + " from "
                        + account.getRoles()
                        + " to "
                        + roles,
                UserAccessLogService.TARGET_USER_ACCOUNT,
                userAccountId);
    }

    /**
     * Changes the user's password after verifying the current password.
     */
    public void changeOwnPassword(
            int userAccountId,
            String currentPassword,
            String newPassword) {

        validateUserAccountId(userAccountId);

        if (currentPassword == null
                || currentPassword.trim().isEmpty()) {

            throw new ValidationException(
                    "Current password is required.");
        }

        validateNewPassword(newPassword);

        UserAccount account =
                requireAccount(userAccountId);

        if (!PasswordHasher.verify(
                currentPassword,
                account.getPasswordSalt(),
                account.getPasswordHash())) {

            throw new AuthenticationException(
                    "Current password is incorrect.");
        }

        writePassword(
                userAccountId,
                newPassword);

        userAccessLogService.logEvent(
                userAccountId,
                UserAccessLogService.ACTION_PASSWORD_CHANGED,
                "Changed own password.",
                UserAccessLogService.TARGET_USER_ACCOUNT,
                userAccountId);
    }

    /**
     * Replaces a temporary password with a password chosen by the user.
     */
    public void completeTemporaryPasswordChange(
            int userAccountId,
            String newPassword,
            String confirmPassword) {

        validateUserAccountId(userAccountId);
        validateNewPassword(newPassword);

        if (!newPassword.equals(confirmPassword)) {
            throw new ValidationException(
                    "Passwords do not match.");
        }

        UserAccount account =
                requireAccount(userAccountId);

        // Only accounts using a temporary password can use this method.
        if (!account.mustChangePassword()) {
            throw new BusinessRuleException(
                    "This account is not awaiting a temporary password change.");
        }

        writePassword(
                userAccountId,
                newPassword);

        userAccessLogService.logEvent(
                userAccountId,
                UserAccessLogService.ACTION_PASSWORD_CHANGED,
                "Replaced temporary password.",
                UserAccessLogService.TARGET_USER_ACCOUNT,
                userAccountId);
    }

    /**
     * Sets a new password after password recovery has been verified.
     */
    public void resetPasswordAfterRecovery(
            int userAccountId,
            String newPassword,
            String confirmPassword) {

        validateUserAccountId(userAccountId);
        validateNewPassword(newPassword);

        if (!newPassword.equals(confirmPassword)) {
            throw new ValidationException(
                    "Passwords do not match.");
        }

        requireAccount(userAccountId);

        writePassword(
                userAccountId,
                newPassword);
    }

    /**
     * Removes the user's authenticator setup.
     * The user must set up an authenticator again on their next login.
     */
    public void resetTotp(
            int actorUserAccountId,
            int targetUserAccountId) {

        validateUserAccountId(actorUserAccountId);
        validateUserAccountId(targetUserAccountId);

        UserAccount target =
                requireAccount(targetUserAccountId);

        boolean hadEnrollment =
                totpCredentialRepository
                        .deleteByUserAccountId(targetUserAccountId);

        userAccessLogService.logEvent(
                actorUserAccountId,
                UserAccessLogService.ACTION_TOTP_RESET_BY_ADMIN,
                (hadEnrollment
                        ? "Reset authenticator enrollment of employee ID "
                        : "Reset requested; no authenticator enrollment existed for employee ID ")
                        + target.getEmployeeId(),
                UserAccessLogService.TARGET_USER_ACCOUNT,
                targetUserAccountId);
    }

    /**
     * Hashes and saves a new password for the user.
     */
    private void writePassword(
            int userAccountId,
            String newPassword) {

        String salt =
                PasswordHasher.generateSalt();

        String hash =
                PasswordHasher.hash(
                        newPassword.trim(),
                        salt);

        userAccountRepository.updatePassword(
                userAccountId,
                hash,
                salt);
    }

    /**
     * Checks that the new password follows the password requirements.
     */
    private void validateNewPassword(
            String newPassword) {

        PasswordPolicy.validate(newPassword);
    }

    /**
     * Applies an account status change and records it in the audit log.
     */
    private void changeStatus(
            int actorUserAccountId,
            int userAccountId,
            IntConsumer mutation) {

        validateUserAccountId(actorUserAccountId);
        validateUserAccountId(userAccountId);

        UserAccount before =
                requireAccount(userAccountId);

        mutation.accept(userAccountId);

        UserAccount after =
                requireAccount(userAccountId);

        userAccessLogService.logAccountStatusChanged(
                actorUserAccountId,
                before.getEmployeeId(),
                before.getAccountStatus().name(),
                after.getAccountStatus().name()
        );
    }

    private void revokeSessions(int userAccountId) {
        tokenRevocationRepository.revokeAllSessions(userAccountId, LocalDateTime.now(clock));
    }

    /**
     * Returns a user account or throws an error if it does not exist.
     */
    private UserAccount requireAccount(
            int userAccountId) {

        UserAccount account =
                userAccountRepository.findById(userAccountId);

        if (account == null) {
            throw new EntityNotFoundException(
                    "User account not found.");
        }

        return account;
    }

    /**
     * Converts a user account into an account summary.
     */
    private AccountSummaryDTO toSummary(
            UserAccount account) {

        Employee employee =
                employeeRepository.findAnyById(
                        account.getEmployeeId());

        return new AccountSummaryDTO(
                account.getUserAccountId(),
                account.getEmployeeId(),
                employee == null
                        ? "Employee #" + account.getEmployeeId()
                        : employee.getFullName(),
                account.getCompanyEmail()
        );
    }

    /**
     * Converts a user account into a row used by the account list.
     */
    private UserAccountRowDTO toRow(
            UserAccount account,
            Map<Integer, String> namesByEmployeeId) {

        String fullName =
                namesByEmployeeId.getOrDefault(
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

    /**
     * Creates a unique company email for the employee.
     */
    private String generateUniqueCompanyEmail(
            String firstName,
            String lastName) {

        String baseEmail =
                buildBaseCompanyEmail(
                        firstName,
                        lastName);

        String candidate = baseEmail;
        int suffix = 2;

        while (userAccountRepository
                .existsByCompanyEmail(candidate)) {

            candidate =
                    appendEmailSuffix(
                            baseEmail,
                            suffix);

            suffix++;
        }

        return candidate;
    }

    /**
     * Generates a random temporary password for a new user account.
     */
    private String generateTemporaryPassword() {

        char[] password =
                new char[TEMPORARY_PASSWORD_LENGTH];

        password[0] = randomCharacter(UPPERCASE);
        password[1] = randomCharacter(LOWERCASE);
        password[2] = randomCharacter(NUMBERS);
        password[3] = randomCharacter(SPECIAL_CHARACTERS);

        for (int i = 4; i < password.length; i++) {
            password[i] =
                    randomCharacter(PASSWORD_CHARACTERS);
        }

        for (int i = password.length - 1; i > 0; i--) {

            int index =
                    secureRandom.nextInt(i + 1);

            char temp = password[i];
            password[i] = password[index];
            password[index] = temp;
        }

        return new String(password);
    }

    /**
     * Returns a random character from the given set.
     */
    private char randomCharacter(
            String characters) {

        return characters.charAt(
                secureRandom.nextInt(
                        characters.length()));
    }

    /**
     * Creates the employee's company email address.
     */
    private String buildBaseCompanyEmail(
            String firstName,
            String lastName) {

        validateName(
                firstName,
                "First name is required.");

        validateName(
                lastName,
                "Last name is required.");

        return sanitizeEmailName(firstName)
                + "."
                + sanitizeEmailName(lastName)
                + COMPANY_EMAIL_DOMAIN;
    }

    /**
     * Adds a number to an email when the original email is already in use.
     */
    private String appendEmailSuffix(
            String baseEmail,
            int suffix) {

        int atIndex =
                baseEmail.indexOf("@");

        if (atIndex < 0) {
            throw new IllegalStateException(
                    "Base company email is invalid.");
        }

        return baseEmail.substring(0, atIndex)
                + suffix
                + baseEmail.substring(atIndex);
    }

    /**
     * Removes unsupported characters before using a name in an email address.
     */
    private String sanitizeEmailName(
            String value) {

        return value.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
    }

    /**
     * Checks that a personal email was provided.
     */
    private void validatePersonalEmail(
            String personalEmail) {

        if (personalEmail == null
                || personalEmail.trim().isEmpty()) {

            throw new ValidationException(
                    "Personal email is required.");
        }

        if (!personalEmail.contains("@")) {
            throw new ValidationException(
                    "Personal email must be a valid email address.");
        }
    }

    /**
     * Checks that the employee ID is valid.
     */
    private void validateEmployeeId(
            int employeeId) {

        if (employeeId <= 0) {
            throw new ValidationException(
                    "Employee ID is required.");
        }
    }

    /**
     * Checks that the user account ID is valid.
     */
    private void validateUserAccountId(
            int userAccountId) {

        if (userAccountId <= 0) {
            throw new ValidationException(
                    "User account ID is required.");
        }
    }

    /**
     * Checks that at least one valid role was provided.
     */
    private void validateRoles(
            List<Role> roles) {

        if (roles == null || roles.isEmpty()) {
            throw new ValidationException(
                    "At least one role is required.");
        }

        for (Role role : roles) {

            if (role == null) {
                throw new ValidationException(
                        "Role cannot be null.");
            }
        }
    }

    /**
     * Checks that a required name was provided.
     */
    private void validateName(
            String value,
            String message) {

        if (value == null
                || value.trim().isEmpty()) {

            throw new ValidationException(message);
        }
    }
}