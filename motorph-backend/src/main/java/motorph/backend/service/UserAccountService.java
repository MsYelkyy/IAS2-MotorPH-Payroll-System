package motorph.backend.service;

import motorph.backend.dto.auth.CredentialsPreview;
import motorph.backend.domain.UserAccount;
import motorph.backend.domain.value.Role;
import motorph.backend.exception.ValidationException;
import motorph.backend.repository.UserAccountRepository;
import motorph.backend.security.PasswordHasher;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Handles account provisioning for newly created/deactivated employees.
 * Ported from the desktop's UserAccountService, trimmed to only the methods
 * EmployeeService's write paths need (create/deactivate) - password
 * change/lock/unlock/role management belong to a separate IT-account-
 * management feature, not employee CRUD, and stay off the backend for now.
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
    private final EmployeeCredentialsDeliveryService credentialsDeliveryService;

    public UserAccountService(UserAccountRepository userAccountRepository,
                              EmployeeCredentialsDeliveryService credentialsDeliveryService) {
        this.userAccountRepository = userAccountRepository;
        this.credentialsDeliveryService = credentialsDeliveryService;
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

    private void validateName(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(message);
        }
    }
}
