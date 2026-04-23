package motorph.application.service;

import motorph.application.exception.ValidationException;
import motorph.domain.model.UserAccessLog;
import motorph.domain.value.Role;
import motorph.infrastructure.repository.UserAccessLogRepository;

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
    public static final String ACTION_ACCOUNT_ROLE_CHANGED = "ACCOUNT_ROLE_CHANGED";
    public static final String ACTION_DATABASE_BACKUP_DOWNLOADED = "DATABASE_BACKUP_DOWNLOADED";

    private final UserAccessLogRepository repository;

    // constructors
    public UserAccessLogService(UserAccessLogRepository repository) {
        if (repository == null) {
            throw new IllegalArgumentException("UserAccessLogRepository is required.");
        }
        this.repository = repository;
    }

    // Logs a successful login.
    public void logLoginSuccess(Integer employeeId, Role role, String details) {
        validateRole(role);

        UserAccessLog log = new UserAccessLog(
                LocalDateTime.now(),
                employeeId,
                role,
                ACTION_LOGIN_SUCCESS,
                clean(details),
                "",
                ""
        );

        repository.save(log);
    }

    // Logs a failed login attempt.
    public void logLoginFailed(String companyEmail, String details) {
        validateRequired(companyEmail, "Company email is required.");

        UserAccessLog log = new UserAccessLog(
                LocalDateTime.now(),
                null,
                null,
                ACTION_LOGIN_FAILED,
                buildFailedLoginDetails(companyEmail, details),
                "",
                ""
        );

        repository.save(log);
    }

    // Logs opening a view.
    public void logOpenView(Integer employeeId, Role role, String viewName) {
        validateRole(role);
        validateRequired(viewName, "View name is required.");

        UserAccessLog log = new UserAccessLog(
                LocalDateTime.now(),
                employeeId,
                role,
                ACTION_OPEN_VIEW,
                "Opened view: " + viewName.trim(),
                "",
                ""
        );

        repository.save(log);
    }

    // Logs password reset action.
    public void logPasswordReset(Integer employeeId, Role role, int targetEmployeeId) {
        validateRole(role);

        if (targetEmployeeId <= 0) {
            throw new ValidationException("Target employee ID is required.");
        }

        UserAccessLog log = new UserAccessLog(
                LocalDateTime.now(),
                employeeId,
                role,
                ACTION_PASSWORD_RESET,
                "Reset password for employee ID " + targetEmployeeId,
                "",
                ""
        );

        repository.save(log);
    }

    // Logs role change action.
    public void logAccountRoleChanged(Integer employeeId,
                                      Role role,
                                      int targetEmployeeId,
                                      Role oldRole,
                                      Role newRole) {

        validateRole(role);

        String details = "Changed role of employee ID " + targetEmployeeId +
                " from " + (oldRole != null ? oldRole.name() : "UNKNOWN") +
                " to " + (newRole != null ? newRole.name() : "UNKNOWN");

        UserAccessLog log = new UserAccessLog(
                LocalDateTime.now(),
                employeeId,
                role,
                ACTION_ACCOUNT_ROLE_CHANGED,
                details,
                "",
                ""
        );

        repository.save(log);
    }

    // Logs database backup download.
    public void logDatabaseBackup(Integer employeeId, Role role) {
        validateRole(role);

        UserAccessLog log = new UserAccessLog(
                LocalDateTime.now(),
                employeeId,
                role,
                ACTION_DATABASE_BACKUP_DOWNLOADED,
                "Downloaded database backup.",
                "",
                ""
        );

        repository.save(log);
    }

    // Logs a custom action.
    public void logAction(Integer employeeId,
                          Role role,
                          String action,
                          String details,
                          String ipAddress,
                          String deviceInfo) {

        validateRole(role);
        validateRequired(action, "Action is required.");

        UserAccessLog log = new UserAccessLog(
                LocalDateTime.now(),
                employeeId,
                role,
                action.trim(),
                clean(details),
                clean(ipAddress),
                clean(deviceInfo)
        );

        repository.save(log);
    }

    // Returns all logs.
    public List<UserAccessLog> getAllLogs() {
        return repository.findAll();
    }

    // Returns logs by employee.
    public List<UserAccessLog> getLogsByEmployeeId(int employeeId) {
        if (employeeId <= 0) {
            throw new ValidationException("Employee ID is required.");
        }
        return repository.findByEmployeeId(employeeId);
    }

    // Returns logs by action.
    public List<UserAccessLog> getLogsByAction(String action) {
        validateRequired(action, "Action is required.");
        return repository.findByAction(action.trim());
    }

    // Searches logs.
    public List<UserAccessLog> searchLogs(String keyword) {
        validateRequired(keyword, "Keyword is required.");
        return repository.search(keyword.trim());
    }

    // Returns logs within date range.
    public List<UserAccessLog> getLogsByDateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new ValidationException("Start and end date are required.");
        }

        if (end.isBefore(start)) {
            throw new ValidationException("End date cannot be before start date.");
        }

        return repository.findByDateRange(
                java.sql.Timestamp.valueOf(start),
                java.sql.Timestamp.valueOf(end)
        );
    }

    // Validates required string.
    private void validateRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(message);
        }
    }

    // Validates role.
    private void validateRole(Role role) {
        if (role == null) {
            throw new ValidationException("Role is required.");
        }
    }

    // Cleans optional strings.
    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    // Builds failed login message.
    private String buildFailedLoginDetails(String email, String details) {
        String base = "Failed login for " + email.trim();
        String extra = clean(details);

        return extra.isEmpty() ? base : base + " | " + extra;
    }
}