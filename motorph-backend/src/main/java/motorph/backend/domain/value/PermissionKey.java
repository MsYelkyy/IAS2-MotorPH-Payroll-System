package motorph.backend.domain.value;

/**
 * Represents system permission keys used for role access control.
 */
public enum PermissionKey {

    CHANGE_OWN_PASSWORD,

    VIEW_DASHBOARD,

    VIEW_EMPLOYEE,
    MANAGE_EMPLOYEE,

    VIEW_ATTENDANCE,
    MANAGE_ATTENDANCE,

    VIEW_LEAVE,
    SUBMIT_LEAVE,
    APPROVE_LEAVE,

    VIEW_OVERTIME,
    SUBMIT_OVERTIME,
    APPROVE_OVERTIME,

    VIEW_PAYROLL,
    PROCESS_PAYROLL,
    VIEW_PAYSLIP,

    VIEW_REPORTS,
    GENERATE_REPORTS,

    VIEW_ROLES,
    MANAGE_ROLES,

    VIEW_USERS,
    MANAGE_USERS,

    VIEW_ACCESS_LOGS,
    MANAGE_2FA,
    MANAGE_PASSWORD_RESET,

    VIEW_DATABASE_TOOLS,
    BACKUP_DATABASE,
    
    // --- Control 2 (RBAC) Support Ticket Permissions ---
    VIEW_TICKETS,
    RESOLVE_TICKETS,
    CANCEL_TICKETS;

    public static PermissionKey fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Permission key is required.");
        }

        try {
            return PermissionKey.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid permission key: " + value);
        }
    }
}
