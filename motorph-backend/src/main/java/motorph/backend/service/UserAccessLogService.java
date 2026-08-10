package motorph.backend.service;

import motorph.backend.dto.accesslog.AccessLogCountsDTO;
import motorph.backend.exception.ValidationException;
import motorph.backend.domain.UserAccessLog;
import motorph.backend.repository.UserAccessLogRepository;

import org.springframework.stereotype.Service;

import java.util.List;

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
    public static final String ACTION_ACCOUNT_STATUS_CHANGED = "ACCOUNT_STATUS_CHANGED";

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

    // logs an account lock/unlock/enable/disable performed by IT
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

    // returns every log, latest first
    public List<UserAccessLog> getAllLogs() {
        return repository.findAll();
    }

    // returns only the newest logs - dashboard activity feed
    public List<UserAccessLog> getRecentLogs(int limit) {
        if (limit <= 0) {
            throw new ValidationException("Limit must be positive.");
        }

        return repository.findRecent(limit);
    }

    // returns login success/failure tallies for the dashboards
    public AccessLogCountsDTO getLoginActivityCounts() {
        return new AccessLogCountsDTO(
                repository.countByAction(ACTION_LOGIN_SUCCESS),
                repository.countByAction(ACTION_LOGIN_FAILED)
        );
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
