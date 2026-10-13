package motorph.backend.service;

import motorph.backend.dto.accesslog.AccessLogCountsDTO;
import motorph.backend.exception.ValidationException;
import motorph.backend.domain.UserAccessLog;
import motorph.backend.repository.UserAccessLogRepository;
import motorph.backend.security.RequestClientInfo;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Handles reading and writing audit log entries.
 * The actor ID comes from the authenticated access token, while the IP address
 * and device information are taken from the current request.
 */
@Service
public class UserAccessLogService {

    public static final String ACTION_LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String ACTION_LOGIN_FAILED = "LOGIN_FAILED";
    public static final String ACTION_ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String ACTION_LOGOUT = "LOGOUT";
    public static final String ACTION_ACCOUNT_STATUS_CHANGED = "ACCOUNT_STATUS_CHANGED";
    public static final String ACTION_PASSWORD_RESET = "PASSWORD_RESET";
    public static final String ACTION_DATABASE_BACKUP_CREATED = "DATABASE_BACKUP_CREATED";
    public static final String ACTION_DATABASE_RESTORE = "DATABASE_RESTORE";
    public static final String ACTION_DATABASE_BACKUP_DOWNLOADED = "DATABASE_BACKUP_DOWNLOADED";
    public static final String ACTION_DATABASE_BACKUP_DELETED = "DATABASE_BACKUP_DELETED";
    public static final String ACTION_PASSWORD_CHANGED = "PASSWORD_CHANGED";
    public static final String ACTION_PASSWORD_RESET_CANCELLED = "PASSWORD_RESET_CANCELLED";
    public static final String ACTION_USER_ROLES_UPDATED = "USER_ROLES_UPDATED";
    public static final String ACTION_ROLE_PERMISSIONS_UPDATED = "ROLE_PERMISSIONS_UPDATED";
    public static final String ACTION_EMPLOYEE_CREATED = "EMPLOYEE_CREATED";
    public static final String ACTION_EMPLOYEE_UPDATED = "EMPLOYEE_UPDATED";
    public static final String ACTION_EMPLOYEE_DEACTIVATED = "EMPLOYEE_DEACTIVATED";
    public static final String ACTION_SALARY_RECORD_ADDED = "SALARY_RECORD_ADDED";
    public static final String ACTION_SALARY_RECORD_UPDATED = "SALARY_RECORD_UPDATED";
    public static final String ACTION_BENEFIT_ENDED = "BENEFIT_ENDED";
    public static final String ACTION_LEAVE_APPROVED = "LEAVE_APPROVED";
    public static final String ACTION_LEAVE_REJECTED = "LEAVE_REJECTED";
    public static final String ACTION_OVERTIME_APPROVED = "OVERTIME_APPROVED";
    public static final String ACTION_OVERTIME_REJECTED = "OVERTIME_REJECTED";
    public static final String ACTION_PAYRUN_CREATED = "PAYRUN_CREATED";
    public static final String ACTION_PAYROLL_GENERATED = "PAYROLL_GENERATED";
    public static final String ACTION_PAYSLIP_EXPORTED = "PAYSLIP_EXPORTED";
    public static final String ACTION_REPORT_EXPORTED = "REPORT_EXPORTED";
    public static final String ACTION_SUPPORT_TICKET_SUBMITTED = "SUPPORT_TICKET_SUBMITTED";
    public static final String ACTION_SUPPORT_TICKET_RESOLVED = "SUPPORT_TICKET_RESOLVED";
    public static final String ACTION_SUPPORT_TICKET_CANCELLED = "SUPPORT_TICKET_CANCELLED";
    public static final String ACTION_PROFILE_PHOTO_UPDATED = "PROFILE_PHOTO_UPDATED";
    public static final String ACTION_ACCESS_DENIED = "ACCESS_DENIED";
    public static final String ACTION_OTP_FAILED = "OTP_FAILED";
    public static final String ACTION_TOTP_ENROLLMENT_STARTED = "TOTP_ENROLLMENT_STARTED";
    public static final String ACTION_TOTP_ENROLLMENT_COMPLETED = "TOTP_ENROLLMENT_COMPLETED";
    public static final String ACTION_TOTP_VERIFICATION_SUCCEEDED = "TOTP_VERIFICATION_SUCCEEDED";
    public static final String ACTION_TOTP_VERIFICATION_FAILED = "TOTP_VERIFICATION_FAILED";
    public static final String ACTION_TOTP_RESET_BY_ADMIN = "TOTP_RESET_BY_ADMIN";
    public static final String ACTION_PASSWORD_RESET_COMPLETED = "PASSWORD_RESET_COMPLETED";
    public static final String ACTION_ACCESS_LOG_VIEWED = "ACCESS_LOG_VIEWED";

    public static final String TARGET_EMPLOYEE = "EMPLOYEE";
    public static final String TARGET_USER_ACCOUNT = "USER_ACCOUNT";
    public static final String TARGET_LEAVE_REQUEST = "LEAVE_REQUEST";
    public static final String TARGET_OVERTIME_REQUEST = "OVERTIME_REQUEST";
    public static final String TARGET_PAYRUN = "PAYRUN";
    public static final String TARGET_SALARY_RECORD = "SALARY_RECORD";
    public static final String TARGET_EMPLOYEE_BENEFIT = "EMPLOYEE_BENEFIT";
    public static final String TARGET_ROLE = "ROLE";
    public static final String TARGET_PASSWORD_RESET_REQUEST = "PASSWORD_RESET_REQUEST";
    public static final String TARGET_BACKUP_FILE = "BACKUP_FILE";
    public static final String TARGET_GOVERNMENT_RATE = "GOVERNMENT_RATE";
    public static final String TARGET_TAX_BRACKET = "TAX_BRACKET";
    public static final String TARGET_SUPPORT_TICKET = "SUPPORT_TICKET";
    /** Target ID format: {@code <employeeId>:<payrunId>}. */
    public static final String TARGET_PAYSLIP = "PAYSLIP";
    // Target ID is the report file name.
    public static final String TARGET_REPORT = "REPORT";

    private static final Set<String> TARGET_TYPES = Set.of(
            TARGET_EMPLOYEE, TARGET_USER_ACCOUNT, TARGET_LEAVE_REQUEST, TARGET_OVERTIME_REQUEST,
            TARGET_PAYRUN, TARGET_SALARY_RECORD, TARGET_EMPLOYEE_BENEFIT, TARGET_ROLE,
            TARGET_PASSWORD_RESET_REQUEST, TARGET_BACKUP_FILE, TARGET_GOVERNMENT_RATE,
            TARGET_TAX_BRACKET, TARGET_SUPPORT_TICKET, TARGET_PAYSLIP, TARGET_REPORT);

    private static final int MAX_DETAILS_LENGTH = 255;
    private static final int MAX_TARGET_ID_LENGTH = 64;

    private final UserAccessLogRepository repository;
    private final RequestClientInfo requestClientInfo;

    public UserAccessLogService(UserAccessLogRepository repository, RequestClientInfo requestClientInfo) {
        this.repository = repository;
        this.requestClientInfo = requestClientInfo;
    }

    public void logLoginSuccess(Integer userAccountId, String details) {
        validateUserAccountId(userAccountId);

        save(userAccountId, ACTION_LOGIN_SUCCESS, clean(details), null, null);
    }

    public void logLoginFailed(String companyEmail, String details) {
        validateRequired(companyEmail, "Company email is required.");

        save(null, ACTION_LOGIN_FAILED, buildFailedLoginDetails(companyEmail, details), null, null);
    }

    /**
     * Records an automatic login lock against its target account without attributing the action to
     * an unauthenticated caller.
     */
    public void logAutomaticAccountLock(int targetUserAccountId) {
        validateUserAccountId(targetUserAccountId);

        save(null, ACTION_ACCOUNT_LOCKED, "Automatic lock applied after repeated failed passwords.",
                TARGET_USER_ACCOUNT, targetUserAccountId);
    }

    /**
     * Logs an action performed by an authenticated user. Details longer than the column allows are
     * truncated rather than failing the write.
     */
    public void logEvent(int actorUserAccountId, String action, String details) {
        logEvent(actorUserAccountId, action, details, null, null);
    }

    /**
     * Logs an action performed by an authenticated user on a specific record. The target type must
     * be one of the TARGET_ constants. The target ID may be null when the record has no ID yet.
     */
    public void logEvent(int actorUserAccountId, String action, String details, String targetType, Object targetId) {
        validateUserAccountId(actorUserAccountId);
        validateRequired(action, "Action is required.");

        save(actorUserAccountId, action.trim(), truncate(clean(details)), targetType, targetId);
    }

    public void logLogout(int userAccountId) {
        validateUserAccountId(userAccountId);

        save(userAccountId, ACTION_LOGOUT, "User logged out.", null, null);
    }

    /**
     * Logs a password reset under the IT user who performed it.
     */
    public void logPasswordReset(int actorUserAccountId, int targetEmployeeId) {
        validateUserAccountId(actorUserAccountId);

        if (targetEmployeeId <= 0) {
            throw new ValidationException("Target employee ID is required.");
        }

        save(actorUserAccountId, ACTION_PASSWORD_RESET, "Reset password for employee ID " + targetEmployeeId,
                TARGET_EMPLOYEE, targetEmployeeId);
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

        save(actorUserAccountId, ACTION_ACCOUNT_STATUS_CHANGED, details, TARGET_EMPLOYEE, targetEmployeeId);
    }

    public void logDatabaseBackupCreated(int actorUserAccountId, String filename) {
        validateUserAccountId(actorUserAccountId);

        save(actorUserAccountId, ACTION_DATABASE_BACKUP_CREATED, "Created backup: " + clean(filename),
                TARGET_BACKUP_FILE, clean(filename));
    }

    public void logDatabaseRestore(int actorUserAccountId, String filename) {
        validateUserAccountId(actorUserAccountId);

        save(actorUserAccountId, ACTION_DATABASE_RESTORE, "Restored from backup: " + clean(filename),
                TARGET_BACKUP_FILE, clean(filename));
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

    private void save(Integer userAccountId, String action, String details, String targetType, Object targetId) {
        validateTarget(targetType, targetId);

        repository.save(new UserAccessLog(
                userAccountId,
                action,
                details,
                requestClientInfo.getIpAddress(),
                requestClientInfo.getDeviceInfo(),
                targetType,
                targetId == null ? null : String.valueOf(targetId)
        ));
    }

    private void validateTarget(String targetType, Object targetId) {
        if (targetType == null) {
            if (targetId != null) {
                throw new ValidationException("Target type is required when a target ID is given.");
            }
            return;
        }

        if (!TARGET_TYPES.contains(targetType)) {
            throw new ValidationException("Unknown audit target type: " + targetType);
        }

        if (targetId != null && String.valueOf(targetId).length() > MAX_TARGET_ID_LENGTH) {
            throw new ValidationException("Audit target ID is too long.");
        }
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

    private String truncate(String value) {
        return value.length() <= MAX_DETAILS_LENGTH ? value : value.substring(0, MAX_DETAILS_LENGTH - 3) + "...";
    }

    private String buildFailedLoginDetails(String email, String details) {
        String base = "Failed login for " + email.trim();
        String extra = clean(details);

        return extra.isEmpty() ? base : base + " | " + extra;
    }
}
