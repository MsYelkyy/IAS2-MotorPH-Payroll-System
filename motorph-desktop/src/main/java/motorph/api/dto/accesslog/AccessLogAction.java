package motorph.api.dto.accesslog;

/**
 * Audit action names the backend records, kept here so IT screens can filter
 * and colour-code log rows without depending on a desktop-side audit service.
 * Mirrors the constants on the backend's UserAccessLogService.
 */
public final class AccessLogAction {

    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILED = "LOGIN_FAILED";
    public static final String LOGOUT = "LOGOUT";
    public static final String ACCOUNT_STATUS_CHANGED = "ACCOUNT_STATUS_CHANGED";
    public static final String PASSWORD_RESET = "PASSWORD_RESET";
    public static final String DATABASE_BACKUP_CREATED = "DATABASE_BACKUP_CREATED";
    public static final String DATABASE_RESTORE = "DATABASE_RESTORE";
    public static final String DATABASE_BACKUP_DOWNLOADED = "DATABASE_BACKUP_DOWNLOADED";
    public static final String DATABASE_BACKUP_DELETED = "DATABASE_BACKUP_DELETED";
    public static final String PASSWORD_CHANGED = "PASSWORD_CHANGED";
    public static final String PASSWORD_RESET_CANCELLED = "PASSWORD_RESET_CANCELLED";
    public static final String USER_ROLES_UPDATED = "USER_ROLES_UPDATED";
    public static final String ROLE_PERMISSIONS_UPDATED = "ROLE_PERMISSIONS_UPDATED";
    public static final String EMPLOYEE_CREATED = "EMPLOYEE_CREATED";
    public static final String EMPLOYEE_UPDATED = "EMPLOYEE_UPDATED";
    public static final String EMPLOYEE_DEACTIVATED = "EMPLOYEE_DEACTIVATED";
    public static final String SALARY_RECORD_ADDED = "SALARY_RECORD_ADDED";
    public static final String SALARY_RECORD_UPDATED = "SALARY_RECORD_UPDATED";
    public static final String BENEFIT_ENDED = "BENEFIT_ENDED";
    public static final String LEAVE_APPROVED = "LEAVE_APPROVED";
    public static final String LEAVE_REJECTED = "LEAVE_REJECTED";
    public static final String OVERTIME_APPROVED = "OVERTIME_APPROVED";
    public static final String OVERTIME_REJECTED = "OVERTIME_REJECTED";
    public static final String PAYRUN_CREATED = "PAYRUN_CREATED";
    public static final String PAYROLL_GENERATED = "PAYROLL_GENERATED";
    public static final String PAYSLIP_EXPORTED = "PAYSLIP_EXPORTED";
    public static final String REPORT_EXPORTED = "REPORT_EXPORTED";
    public static final String SUPPORT_TICKET_SUBMITTED = "SUPPORT_TICKET_SUBMITTED";
    public static final String SUPPORT_TICKET_RESOLVED = "SUPPORT_TICKET_RESOLVED";
    public static final String SUPPORT_TICKET_CANCELLED = "SUPPORT_TICKET_CANCELLED";
    public static final String PROFILE_PHOTO_UPDATED = "PROFILE_PHOTO_UPDATED";
    public static final String ACCESS_DENIED = "ACCESS_DENIED";
    public static final String OTP_FAILED = "OTP_FAILED";
    public static final String ACCESS_LOG_VIEWED = "ACCESS_LOG_VIEWED";

    private AccessLogAction() {
    }
}
