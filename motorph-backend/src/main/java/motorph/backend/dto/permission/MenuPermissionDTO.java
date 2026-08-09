package motorph.backend.dto.permission;

/**
 * Stores menu visibility permissions for the active user role.
 */
public class MenuPermissionDTO {

    private final boolean dashboardVisible;
    private final boolean employeeManagementVisible;
    private final boolean attendanceVisible;
    private final boolean leaveVisible;
    private final boolean overtimeVisible;
    private final boolean payrollVisible;
    private final boolean reportsVisible;
    private final boolean roleManagementVisible;
    private final boolean userManagementVisible;
    private final boolean securityVisible;
    private final boolean databaseToolsVisible;
    private final boolean accountSettingsVisible;

    public MenuPermissionDTO(boolean dashboardVisible,
                             boolean employeeManagementVisible,
                             boolean attendanceVisible,
                             boolean leaveVisible,
                             boolean overtimeVisible,
                             boolean payrollVisible,
                             boolean reportsVisible,
                             boolean roleManagementVisible,
                             boolean userManagementVisible,
                             boolean securityVisible,
                             boolean databaseToolsVisible,
                             boolean accountSettingsVisible) {

        this.dashboardVisible = dashboardVisible;
        this.employeeManagementVisible = employeeManagementVisible;
        this.attendanceVisible = attendanceVisible;
        this.leaveVisible = leaveVisible;
        this.overtimeVisible = overtimeVisible;
        this.payrollVisible = payrollVisible;
        this.reportsVisible = reportsVisible;
        this.roleManagementVisible = roleManagementVisible;
        this.userManagementVisible = userManagementVisible;
        this.securityVisible = securityVisible;
        this.databaseToolsVisible = databaseToolsVisible;
        this.accountSettingsVisible = accountSettingsVisible;
    }

    public boolean isDashboardVisible() {
        return dashboardVisible;
    }

    public boolean isEmployeeManagementVisible() {
        return employeeManagementVisible;
    }

    public boolean isAttendanceVisible() {
        return attendanceVisible;
    }

    public boolean isLeaveVisible() {
        return leaveVisible;
    }

    public boolean isOvertimeVisible() {
        return overtimeVisible;
    }

    public boolean isPayrollVisible() {
        return payrollVisible;
    }

    public boolean isReportsVisible() {
        return reportsVisible;
    }

    public boolean isRoleManagementVisible() {
        return roleManagementVisible;
    }

    public boolean isUserManagementVisible() {
        return userManagementVisible;
    }

    public boolean isSecurityVisible() {
        return securityVisible;
    }

    public boolean isDatabaseToolsVisible() {
        return databaseToolsVisible;
    }

    public boolean isAccountSettingsVisible() {
        return accountSettingsVisible;
    }
}
