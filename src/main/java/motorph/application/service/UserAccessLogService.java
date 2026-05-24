package motorph.application.service;

import motorph.application.exception.ValidationException;
import motorph.domain.model.UserAccessLog;
import motorph.infrastructure.repository.UserAccessLogRepository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Handles creation and retrieval of user access logs for audit tracking.
 */
public class UserAccessLogService {

    public static final String ACTION_LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String ACTION_LOGIN_FAILED = "LOGIN_FAILED";
    public static final String ACTION_OPEN_VIEW = "OPEN_VIEW";
    public static final String ACTION_PASSWORD_RESET = "PASSWORD_RESET";
    public static final String ACTION_ACCOUNT_STATUS_CHANGED = "ACCOUNT_STATUS_CHANGED";
    public static final String ACTION_DATABASE_BACKUP_DOWNLOADED = "DATABASE_BACKUP_DOWNLOADED";
    public static final String ACTION_GENERATED_REPORT = "GENERATED_REPORT";

    private final UserAccessLogRepository repository;

    // constructors
    public UserAccessLogService(UserAccessLogRepository repository) {
        if (repository == null) {
            throw new IllegalArgumentException("UserAccessLogRepository is required.");
        }

        this.repository = repository;
    }

    // logs successful login
    public void logLoginSuccess(Integer userAccountId, String details) {
        validateUserAccountId(userAccountId);

        UserAccessLog log = new UserAccessLog(
                LocalDateTime.now(),
                userAccountId,
                ACTION_LOGIN_SUCCESS,
                clean(details),
                "",
                ""
        );

        repository.save(log);
    }

    // logs failed login attempt
    public void logLoginFailed(String companyEmail, String details) {
        validateRequired(companyEmail, "Company email is required.");

        UserAccessLog log = new UserAccessLog(
                LocalDateTime.now(),
                null,
                ACTION_LOGIN_FAILED,
                buildFailedLoginDetails(companyEmail, details),
                "",
                ""
        );

        repository.save(log);
    }

    // logs opened view action
    public void logOpenView(Integer userAccountId, String viewName) {
        validateUserAccountId(userAccountId);
        validateRequired(viewName, "View name is required.");

        UserAccessLog log = new UserAccessLog(
                LocalDateTime.now(),
                userAccountId,
                ACTION_OPEN_VIEW,
                "Opened view: " + viewName.trim(),
                "",
                ""
        );

        repository.save(log);
    }

    // logs password reset action
    public void logPasswordReset(Integer userAccountId, int targetEmployeeId) {
        validateUserAccountId(userAccountId);

        if (targetEmployeeId <= 0) {
            throw new ValidationException("Target employee ID is required.");
        }

        UserAccessLog log = new UserAccessLog(
                LocalDateTime.now(),
                userAccountId,
                ACTION_PASSWORD_RESET,
                "Reset password for employee ID " + targetEmployeeId,
                "",
                ""
        );

        repository.save(log);
    }

    // logs account status change action
    public void logAccountStatusChanged(Integer userAccountId,
                                        int targetEmployeeId,
                                        String oldStatus,
                                        String newStatus) {

        validateUserAccountId(userAccountId);

        String details = "Changed account status of employee ID "
                + targetEmployeeId
                + " from "
                + clean(oldStatus)
                + " to "
                + clean(newStatus);

        UserAccessLog log = new UserAccessLog(
                LocalDateTime.now(),
                userAccountId,
                ACTION_ACCOUNT_STATUS_CHANGED,
                details,
                "",
                ""
        );

        repository.save(log);
    }

    // logs database backup download
    public void logDatabaseBackup(Integer userAccountId) {
        validateUserAccountId(userAccountId);

        UserAccessLog log = new UserAccessLog(
                LocalDateTime.now(),
                userAccountId,
                ACTION_DATABASE_BACKUP_DOWNLOADED,
                "Downloaded database backup.",
                "",
                ""
        );

        repository.save(log);
    }

    // logs generated report action
    public void logGeneratedReport(Integer userAccountId, String reportName) {
        validateUserAccountId(userAccountId);
        validateRequired(reportName, "Report name is required.");

        UserAccessLog log = new UserAccessLog(
                LocalDateTime.now(),
                userAccountId,
                ACTION_GENERATED_REPORT,
                "Generated report: " + reportName.trim(),
                "",
                ""
        );

        repository.save(log);
    }

    // logs custom action
    public void logAction(Integer userAccountId,
                          String action,
                          String details,
                          String ipAddress,
                          String deviceInfo) {

        validateRequired(action, "Action is required.");

        UserAccessLog log = new UserAccessLog(
                LocalDateTime.now(),
                userAccountId,
                action.trim(),
                clean(details),
                clean(ipAddress),
                clean(deviceInfo)
        );

        repository.save(log);
    }

    // returns all logs
    public List<UserAccessLog> getAllLogs() {
        return repository.findAll();
    }

    // returns logs by user account
    public List<UserAccessLog> getLogsByUserAccountId(int userAccountId) {
        validateUserAccountId(userAccountId);
        return repository.findByUserAccountId(userAccountId);
    }

    // returns logs by action
    public List<UserAccessLog> getLogsByAction(String action) {
        validateRequired(action, "Action is required.");
        return repository.findByAction(action.trim());
    }

    // searches logs
    public List<UserAccessLog> searchLogs(String keyword) {
        validateRequired(keyword, "Keyword is required.");
        return repository.search(keyword.trim());
    }

    // returns logs within date range
    public List<UserAccessLog> getLogsByDateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new ValidationException("Start and end date are required.");
        }

        if (end.isBefore(start)) {
            throw new ValidationException("End date cannot be before start date.");
        }

        return repository.findByDateRange(
                Timestamp.valueOf(start),
                Timestamp.valueOf(end)
        );
    }

    // validates user account id input
    private void validateUserAccountId(Integer userAccountId) {
        if (userAccountId == null || userAccountId <= 0) {
            throw new ValidationException("User account ID is required.");
        }
    }

    // validates required string input
    private void validateRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(message);
        }
    }

    // cleans optional string values
    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    // builds failed login details
    private String buildFailedLoginDetails(String email, String details) {
        String base = "Failed login for " + email.trim();
        String extra = clean(details);

        return extra.isEmpty() ? base : base + " | " + extra;
    }
}