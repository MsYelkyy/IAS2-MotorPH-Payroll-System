package motorph.backend.service;

import motorph.backend.dto.accesslog.AccessLogCountsDTO;
import motorph.backend.exception.ValidationException;
import motorph.backend.domain.UserAccessLog;
import motorph.backend.repository.UserAccessLogRepository;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Writes and reads audit log entries. Callers always pass the actor ID taken from the access token,
 * never a value supplied by the client.
 */
@Service
public class UserAccessLogService {

    public static final String ACTION_LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String ACTION_LOGIN_FAILED = "LOGIN_FAILED";
    public static final String ACTION_LOGOUT = "LOGOUT";
    public static final String ACTION_ACCOUNT_STATUS_CHANGED = "ACCOUNT_STATUS_CHANGED";
    public static final String ACTION_PASSWORD_RESET = "PASSWORD_RESET";
    public static final String ACTION_DATABASE_BACKUP_CREATED = "DATABASE_BACKUP_CREATED";
    public static final String ACTION_DATABASE_RESTORE = "DATABASE_RESTORE";

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

    /**
     * Logs a password reset under the IT user who performed it.
     */
    public void logPasswordReset(int actorUserAccountId, int targetEmployeeId) {
        validateUserAccountId(actorUserAccountId);

        if (targetEmployeeId <= 0) {
            throw new ValidationException("Target employee ID is required.");
        }

        repository.save(new UserAccessLog(
                actorUserAccountId,
                ACTION_PASSWORD_RESET,
                "Reset password for employee ID " + targetEmployeeId,
                "",
                ""
        ));
    }

    /**
     * Logs an account lock, unlock, enable, or disable performed by IT.
     */
    public void logAccountStatusChanged(int actorUserAccountId,
                                        int targetEmployeeId,
                                        String oldStatus,
                                        String newStatus) {
        validateUserAccountId(actorUserAccountId);

        String details = "Changed account status of employee ID "
                + targetEmployeeId
                + " from "
                + clean(oldStatus)
                + " to "
                + clean(newStatus);

        repository.save(new UserAccessLog(
                actorUserAccountId,
                ACTION_ACCOUNT_STATUS_CHANGED,
                details,
                "",
                ""
        ));
    }

    public void logDatabaseBackupCreated(int actorUserAccountId, String filename) {
        validateUserAccountId(actorUserAccountId);

        repository.save(new UserAccessLog(
                actorUserAccountId,
                ACTION_DATABASE_BACKUP_CREATED,
                "Created backup: " + clean(filename),
                "",
                ""
        ));
    }

    public void logDatabaseRestore(int actorUserAccountId, String filename) {
        validateUserAccountId(actorUserAccountId);

        repository.save(new UserAccessLog(
                actorUserAccountId,
                ACTION_DATABASE_RESTORE,
                "Restored from backup: " + clean(filename),
                "",
                ""
        ));
    }

    /**
     * Returns all log entries, newest first.
     */
    public List<UserAccessLog> getAllLogs() {
        return repository.findAll();
    }

    /**
     * Returns the newest log entries for the dashboard activity feed.
     */
    public List<UserAccessLog> getRecentLogs(int limit) {
        if (limit <= 0) {
            throw new ValidationException("Limit must be positive.");
        }

        return repository.findRecent(limit);
    }

    public AccessLogCountsDTO getLoginActivityCounts() {
        return new AccessLogCountsDTO(
                repository.countByAction(ACTION_LOGIN_SUCCESS),
                repository.countByAction(ACTION_LOGIN_FAILED)
        );
    }

    /**
     * Logs a custom action. Used for auditing changes to government rates.
     */
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
