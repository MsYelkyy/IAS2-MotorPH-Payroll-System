package motorph.backend.service;

import motorph.backend.exception.ValidationException;
import motorph.backend.domain.UserAccessLog;
import motorph.backend.repository.UserAccessLogRepository;

import org.springframework.stereotype.Service;

/**
 * Handles creation of user access logs for audit tracking. Every write path
 * on the backend calls this with an identity resolved from the validated
 * token (CurrentUserProvider) - never from anything the client asserts.
 */
@Service
public class UserAccessLogService {

    public static final String ACTION_LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String ACTION_LOGIN_FAILED = "LOGIN_FAILED";
    public static final String ACTION_LOGOUT = "LOGOUT";

    private final UserAccessLogRepository repository;

    public UserAccessLogService(UserAccessLogRepository repository) {
        this.repository = repository;
    }

    public void logLoginSuccess(Integer userAccountId, String details) {
        validateUserAccountId(userAccountId);

        repository.save(new UserAccessLog(
                userAccountId,
                ACTION_LOGIN_SUCCESS,
                clean(details),
                "",
                ""
        ));
    }

    public void logLoginFailed(String companyEmail, String details) {
        validateRequired(companyEmail, "Company email is required.");

        repository.save(new UserAccessLog(
                null,
                ACTION_LOGIN_FAILED,
                buildFailedLoginDetails(companyEmail, details),
                "",
                ""
        ));
    }

    public void logLogout(int userAccountId) {
        validateUserAccountId(userAccountId);

        repository.save(new UserAccessLog(
                userAccountId,
                ACTION_LOGOUT,
                "User logged out.",
                "",
                ""
        ));
    }

    // logs a custom action - used by SystemConfigService for statutory rate change auditing
    public void logAction(Integer userAccountId, String action, String details, String ipAddress, String deviceInfo) {
        validateRequired(action, "Action is required.");

        repository.save(new UserAccessLog(
                userAccountId,
                action.trim(),
                clean(details),
                clean(ipAddress),
                clean(deviceInfo)
        ));
    }

    private void validateUserAccountId(Integer userAccountId) {
        if (userAccountId == null || userAccountId <= 0) {
            throw new ValidationException("User account ID is required.");
        }
    }

    private void validateRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(message);
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String buildFailedLoginDetails(String email, String details) {
        String base = "Failed login for " + email.trim();
        String extra = clean(details);

        return extra.isEmpty() ? base : base + " | " + extra;
    }
}
