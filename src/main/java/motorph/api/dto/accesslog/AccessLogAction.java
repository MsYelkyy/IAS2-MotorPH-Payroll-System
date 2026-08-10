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

    private AccessLogAction() {
    }
}
