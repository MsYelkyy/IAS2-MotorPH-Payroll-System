package motorph.ui.admin;

import motorph.application.service.EmployeeService;
import motorph.domain.value.Role;
import motorph.ui.admin.databasetools.AdminDatabaseToolsController;
import motorph.ui.admin.dashboard.AdminDashboardController;
import motorph.ui.admin.leave.AdminLeaveController;
import motorph.ui.admin.employee.AdminEmployeeController;
import motorph.ui.admin.payroll.AdminPayrollController;
import motorph.ui.admin.reports.AdminReportsController;
import motorph.ui.admin.salary.AdminSalaryController;
import motorph.ui.admin.systemconfig.AdminSystemConfigController;
import motorph.ui.common.GlobalControlsHelper;
import motorph.ui.it.accesslogs.ITAccessLogsController;
import motorph.ui.it.account.ITUserAccountController;
import motorph.ui.it.role.ITRolesController;
import motorph.ui.common.NavigationHelper;
import motorph.ui.common.RoleSwitchHelper;
import motorph.ui.common.SidebarHelper;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.util.NavigationUtil;
import motorph.ui.util.ThemeManager;
import motorph.ui.util.UiMessageUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Manages the admin app shell: sidebar navigation, identity card, and global controls. */
public class AdminMainController {

    private EmployeeService employeeService;

    // ── Root ──────────────────────────────────────────────────────────────────
    @FXML private StackPane rootPane;

    // ── Sidebar identity card ─────────────────────────────────────────────────
    @FXML private Label employeeNameLabel;
    @FXML private Label employeeRoleLabel;

    // ── Navigation buttons ────────────────────────────────────────────────────
    @FXML private Button dashboardNavButton;
    @FXML private Button employeesNavButton;
    @FXML private Button payrollNavButton;
    @FXML private Button salaryNavButton;
    @FXML private Button reportsNavButton;
    @FXML private Button leaveOvertimeNavButton;
    @FXML private Button userManagementNavButton;
    @FXML private Button roleManagementNavButton;
    @FXML private Button accessLogsNavButton;
    @FXML private Button databaseToolsNavButton;
    @FXML private Button settingsNavButton;
    @FXML private Button switchRoleButton;

    // ── Topbar ────────────────────────────────────────────────────────────────
    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;
    @FXML private HBox  globalControlsHost;

    // ── Dashboard sub-view ────────────────────────────────────────────────────
    @FXML private VBox                     dashboardView;
    @FXML private AdminDashboardController dashboardViewController;

    // ── Employee sub-view ─────────────────────────────────────────────────────
    @FXML private VBox                      employeeView;
    @FXML private AdminEmployeeController   employeeViewController;

    // ── Payroll sub-view ──────────────────────────────────────────────────────
    @FXML private VBox                    payrollView;
    @FXML private AdminPayrollController  payrollViewController;

    // ── Salary & Benefits sub-view ────────────────────────────────────────────
    @FXML private VBox                    salaryView;
    @FXML private AdminSalaryController   salaryViewController;

    // ── User Account Management sub-view ──────────────────────────────────────
    @FXML private VBox                        userAccountView;
    @FXML private ITUserAccountController     userAccountViewController;

    // ── Roles & Permissions sub-view ─────────────────────────────────────────
    @FXML private VBox                   rolesView;
    @FXML private ITRolesController      rolesViewController;

    // ── Access Logs sub-view ──────────────────────────────────────────────────
    @FXML private VBox                        accessLogsView;
    @FXML private ITAccessLogsController      accessLogsViewController;

    // ── Reports sub-view ─────────────────────────────────────────────────────
    @FXML private VBox                   reportsView;
    @FXML private AdminReportsController reportsViewController;

    // ── Leave & Overtime sub-view ─────────────────────────────────────────────
    @FXML private VBox                 leaveView;
    @FXML private AdminLeaveController leaveViewController;

    // ── System Config sub-view ────────────────────────────────────────────────
    @FXML private VBox                          systemConfigView;
    @FXML private AdminSystemConfigController   systemConfigViewController;

    // ── Database Tools sub-view ───────────────────────────────────────────────
    @FXML private VBox                          databaseToolsView;
    @FXML private AdminDatabaseToolsController  databaseToolsViewController;

    // ── Module placeholder (shown for unbuilt modules) ────────────────────────
    @FXML private VBox modulePlaceholder;

    // ── Footer ────────────────────────────────────────────────────────────────
    @FXML private Label appMessageLabel;

    @FXML
    private void initialize() {
        employeeService = EmployeeUIServiceFactory.getInstance().getEmployeeService();

        buildGlobalControls();
        ThemeManager.getInstance().applyCurrentTo(rootPane);
        loadSidebarData();
        wireDashboard();
        showDashboard();

        UiMessageUtil.setSuccess(appMessageLabel, "Admin Control Panel is ready.");
    }

    // ── Navigation handlers ───────────────────────────────────────────────────

    @FXML
    private void showDashboard() {
        setNavSelected(dashboardNavButton);
        setHeader("Dashboard", "ADMIN CONTROL PANEL");
        showView(ActiveView.DASHBOARD);
        if (dashboardViewController != null) dashboardViewController.refresh();
    }

    @FXML
    private void showEmployees() {
        setNavSelected(employeesNavButton);
        setHeader("Employee Management", "View and manage all employees");
        showView(ActiveView.EMPLOYEES);
        if (employeeViewController != null) employeeViewController.refresh();
    }

    @FXML
    private void showPayroll() {
        setNavSelected(payrollNavButton);
        setHeader("Payroll", "Process and review payroll runs");
        showView(ActiveView.PAYROLL);
        if (payrollViewController != null) payrollViewController.refresh();
    }

    @FXML
    private void showSalary() {
        setNavSelected(salaryNavButton);
        setHeader("Salary & Benefits", "Manage salary history and benefit allocations");
        showView(ActiveView.SALARY);
        if (salaryViewController != null) salaryViewController.refresh();
    }

    @FXML
    private void showReports() {
        setNavSelected(reportsNavButton);
        setHeader("Reports", "Generate and export organization reports");
        showView(ActiveView.REPORTS);
        if (reportsViewController != null) reportsViewController.refresh();
    }

    @FXML
    private void showLeaveOvertime() {
        setNavSelected(leaveOvertimeNavButton);
        setHeader("Leave & Overtime", "Review and approve leave requests");
        showView(ActiveView.LEAVE);
        if (leaveViewController != null) leaveViewController.refresh();
    }

    @FXML
    private void showUserManagement() {
        setNavSelected(userManagementNavButton);
        setHeader("User Management", "Manage user accounts and access");
        showView(ActiveView.USER_ACCOUNTS);
        if (userAccountViewController != null) userAccountViewController.refresh();
    }

    @FXML
    private void showRoleManagement() {
        setNavSelected(roleManagementNavButton);
        setHeader("Role Management", "Configure role permissions");
        showView(ActiveView.ROLES);
        if (rolesViewController != null) rolesViewController.refresh();
    }

    @FXML
    private void showAccessLogs() {
        setNavSelected(accessLogsNavButton);
        setHeader("User Access Logs", "Audit system activity and logins");
        showView(ActiveView.ACCESS_LOGS);
        if (accessLogsViewController != null) accessLogsViewController.refresh();
    }

    @FXML
    private void showDatabaseTools() {
        setNavSelected(databaseToolsNavButton);
        setHeader("Database Backup & Recovery", "Create and restore database backups");
        showView(ActiveView.DATABASE_TOOLS);
        if (databaseToolsViewController != null) databaseToolsViewController.refresh();
    }

    @FXML
    private void showSettings() {
        setNavSelected(settingsNavButton);
        setHeader("Settings", "System configuration and preferences");
        showView(ActiveView.SYSTEM_CONFIG);
        if (systemConfigViewController != null) systemConfigViewController.refresh();
    }

    // ── Auth handlers ─────────────────────────────────────────────────────────

    @FXML
    private void handleLogout() {
        CurrentUserSession.clear();
        NavigationUtil.goToLogin(rootPane.getScene());
    }

    @FXML
    private void handleSwitchRole() {
        RoleSwitchHelper.switchTo(Role.EMPLOYEE, rootPane.getScene(), null, null);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Exposes the navigate-to-employees action so the dashboard quick-access tiles can trigger it. */
    public void navigateToEmployees()      { showEmployees(); }
    public void navigateToPayroll()        { showPayroll(); }
    public void navigateToSalary()         { showSalary(); }
    public void navigateToReports()        { showReports(); }
    public void navigateToLeaveOvertime()  { showLeaveOvertime(); }
    public void navigateToUserManagement() { showUserManagement(); }
    public void navigateToRoleManagement() { showRoleManagement(); }
    public void navigateToAccessLogs()     { showAccessLogs(); }

    private enum ActiveView { DASHBOARD, EMPLOYEES, PAYROLL, SALARY, REPORTS, USER_ACCOUNTS, ROLES, ACCESS_LOGS, LEAVE, SYSTEM_CONFIG, DATABASE_TOOLS, PLACEHOLDER }

    private void showView(ActiveView active) {
        setPane(dashboardView,     active == ActiveView.DASHBOARD);
        setPane(employeeView,      active == ActiveView.EMPLOYEES);
        setPane(payrollView,       active == ActiveView.PAYROLL);
        setPane(salaryView,        active == ActiveView.SALARY);
        setPane(reportsView,       active == ActiveView.REPORTS);
        setPane(userAccountView,   active == ActiveView.USER_ACCOUNTS);
        setPane(rolesView,         active == ActiveView.ROLES);
        setPane(accessLogsView,    active == ActiveView.ACCESS_LOGS);
        setPane(leaveView,         active == ActiveView.LEAVE);
        setPane(systemConfigView,    active == ActiveView.SYSTEM_CONFIG);
        setPane(databaseToolsView,   active == ActiveView.DATABASE_TOOLS);
        setPane(modulePlaceholder,   active == ActiveView.PLACEHOLDER);
    }

    private void setPane(javafx.scene.Node node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    private void wireDashboard() {
        if (dashboardViewController != null) {
            dashboardViewController.setMainController(this);
        }
    }

    private void setNavSelected(Button selected) {
        NavigationHelper.setNavSelected(selected,
                dashboardNavButton, employeesNavButton, payrollNavButton, salaryNavButton, reportsNavButton,
                leaveOvertimeNavButton, userManagementNavButton, roleManagementNavButton,
                accessLogsNavButton, databaseToolsNavButton, settingsNavButton);
    }

    private void setHeader(String title, String subtitle) {
        NavigationHelper.setHeader(pageTitleLabel, pageSubtitleLabel, title, subtitle);
    }

    private void loadSidebarData() {
        SidebarHelper.load(employeeNameLabel, employeeRoleLabel, null,
                "Admin User", "ADMIN", "", employeeService);
        updateRoleSwitchButton();
    }

    private void updateRoleSwitchButton() {
        boolean show = CurrentUserSession.hasMultipleRoles()
                && CurrentUserSession.hasRole(Role.EMPLOYEE)
                && CurrentUserSession.getActiveRole() != Role.EMPLOYEE;

        if (switchRoleButton != null) {
            switchRoleButton.setVisible(show);
            switchRoleButton.setManaged(show);
        }
    }

    // builds and inserts language/theme combos into the topbar host
    private void buildGlobalControls() {
        GlobalControlsHelper.build(globalControlsHost, rootPane, null);
    }
}
