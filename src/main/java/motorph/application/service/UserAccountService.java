package motorph.application.service;

import motorph.application.exception.EntityNotFoundException;
import motorph.application.exception.ValidationException;
import motorph.application.security.PasswordHasher;
import motorph.domain.model.UserAccount;
import motorph.domain.value.Role;
import motorph.infrastructure.repository.UserAccountRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Handles user account business rules and account provisioning.
 */
public class UserAccountService {

    private static final int TEMPORARY_PASSWORD_EXPIRY_HOURS = 24;
    private static final String TEMPORARY_PASSWORD_PREFIX = "MotorPH@";
    private static final String COMPANY_EMAIL_DOMAIN = "@motorph.com";

    private final UserAccountRepository userAccountRepository;
    private final EmailService emailService;

    // constructors
    public UserAccountService(UserAccountRepository userAccountRepository,
                              EmailService emailService) {
        if (userAccountRepository == null) {
            throw new IllegalArgumentException("UserAccountRepository is required.");
        }

        if (emailService == null) {
            throw new IllegalArgumentException("EmailService is required.");
        }

        this.userAccountRepository = userAccountRepository;
        this.emailService = emailService;
    }

    // creates an active employee account with a temporary password
    public UserAccount createTemporaryEmployeeAccount(int employeeId,
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

        emailService.sendEmployeeAccountCredentials(personalEmail, companyEmail, temporaryPassword);

        return savedAccount;
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

    // returns the company email linked to an employee, or blank when no account exists
    public String getCompanyEmailByEmployeeId(int employeeId) {
        validateEmployeeId(employeeId);

        UserAccount account = userAccountRepository.findByEmployeeId(employeeId);
        return account != null ? account.getCompanyEmail() : "";
    }

    // locks a user account
    public void lockAccount(int userAccountId) {
        validateUserAccountId(userAccountId);
        userAccountRepository.updateLockStatus(userAccountId, true);
    }

    // unlocks a user account
    public void unlockAccount(int userAccountId) {
        validateUserAccountId(userAccountId);
        userAccountRepository.updateLockStatus(userAccountId, false);
    }

    // disables a user account
    public void disableAccount(int userAccountId) {
        validateUserAccountId(userAccountId);
        userAccountRepository.updateActiveStatus(userAccountId, false);
    }

    // enables a user account
    public void enableAccount(int userAccountId) {
        validateUserAccountId(userAccountId);
        userAccountRepository.updateActiveStatus(userAccountId, true);
    }

    // updates roles assigned to a user account
    public void updateRoles(int userAccountId, List<Role> roles) {
        validateUserAccountId(userAccountId);
        validateRoles(roles);

        userAccountRepository.updateRoles(userAccountId, roles);
    }

    // changes a user password after verifying the current password; throws AuthenticationException if old password is wrong
    public void changeOwnPassword(int userAccountId, String currentPassword, String newPassword) {
        validateUserAccountId(userAccountId);

        if (currentPassword == null || currentPassword.trim().isEmpty()) {
            throw new ValidationException("Current password is required.");
        }

        validatePassword(newPassword);

        UserAccount account = userAccountRepository.findById(userAccountId);

        if (account == null) {
            throw new EntityNotFoundException("User account not found.");
        }

        if (!PasswordHasher.verify(currentPassword, account.getPasswordSalt(), account.getPasswordHash())) {
            throw new motorph.application.exception.AuthenticationException("Current password is incorrect.");
        }

        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(newPassword.trim(), salt);
        userAccountRepository.updatePassword(userAccountId, hash, salt);
    }

    // changes a user password and clears temporary password status
    public void changePassword(int userAccountId, String rawPassword) {
        validateUserAccountId(userAccountId);
        validatePassword(rawPassword);

        UserAccount account = userAccountRepository.findById(userAccountId);

        if (account == null) {
            throw new EntityNotFoundException("User account not found.");
        }

        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(rawPassword.trim(), salt);

        userAccountRepository.updatePassword(userAccountId, hash, salt);
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

    // generates the default temporary password
    public String generateTemporaryPassword(int employeeId) {
        validateEmployeeId(employeeId);
        return TEMPORARY_PASSWORD_PREFIX + employeeId;
    }

    // builds the base company email format
    private String buildBaseCompanyEmail(String firstName, String lastName) {
        validateName(firstName, "First name is required.");
        validateName(lastName, "Last name is required.");

        return sanitizeEmailName(firstName) + "." + sanitizeEmailName(lastName) + COMPANY_EMAIL_DOMAIN;
    }

    // appends a numeric suffix to a duplicate company email
    private String appendEmailSuffix(String baseEmail, int suffix) {
        int atIndex = baseEmail.indexOf("@");

        if (atIndex < 0) {
            throw new IllegalStateException("Base company email is invalid.");
        }

        return baseEmail.substring(0, atIndex) + suffix + baseEmail.substring(atIndex);
    }

    // sanitizes a name for email generation
    private String sanitizeEmailName(String value) {
        return value.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
    }

    // completes temporary password setup after login
    public void completeTemporaryPasswordChange(
            int userAccountId,
            String newPassword,
            String confirmPassword) {

        validateUserAccountId(userAccountId);

        if (newPassword == null || newPassword.isBlank()) {
            throw new ValidationException("New password is required.");
        }

        if (!newPassword.equals(confirmPassword)) {
            throw new ValidationException("Passwords do not match.");
        }

        UserAccount account = userAccountRepository.findById(userAccountId);

        if (account == null) {
            throw new EntityNotFoundException("User account not found.");
        }

        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(newPassword.trim(), salt);

        userAccountRepository.updatePassword(
                userAccountId,
                hash,
                salt
        );
    }

    // returns a user account by its ID
    public UserAccount getById(int userAccountId) {
        validateUserAccountId(userAccountId);
        UserAccount account = userAccountRepository.findById(userAccountId);
        if (account == null) {
            throw new EntityNotFoundException("User account not found.");
        }
        return account;
    }

    public List<UserAccount> findAll() {
        return userAccountRepository.findAll();
    }

    // returns all active unlocked accounts
    public List<UserAccount> findAllActive() {
        return userAccountRepository.findAllActive();
    }

    // returns all locked accounts
    public List<UserAccount> findAllLocked() {
        return userAccountRepository.findAllLocked();
    }

    // returns all disabled accounts
    public List<UserAccount> findAllDisabled() {
        return userAccountRepository.findAllDisabled();
    }

        // returns active unlocked account count
    public int getActiveAccountCount() {
        return userAccountRepository.findAllActive().size();
    }

    // returns locked account count
    public int getLockedAccountCount() {
        return userAccountRepository.findAllLocked().size();
    }

    // returns disabled account count
    public int getDisabledAccountCount() {
        return userAccountRepository.findAllDisabled().size();
    }

    // validates personal email input
    private void validatePersonalEmail(String personalEmail) {
        if (personalEmail == null || personalEmail.trim().isEmpty()) {
            throw new ValidationException("Personal email is required.");
        }

        if (!personalEmail.contains("@")) {
            throw new ValidationException("Personal email must be a valid email address.");
        }
    }

    // validates employee id input
    private void validateEmployeeId(int employeeId) {
        if (employeeId <= 0) {
            throw new ValidationException("Employee ID is required.");
        }
    }

    // validates user account id input
    private void validateUserAccountId(int userAccountId) {
        if (userAccountId <= 0) {
            throw new ValidationException("User account ID is required.");
        }
    }

    // validates roles input
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

    // validates password input
    private void validatePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new ValidationException("Password is required.");
        }
    }

    // validates name input
    private void validateName(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(message);
        }
    }
}
