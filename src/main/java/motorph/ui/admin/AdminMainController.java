package motorph.ui.admin;

import motorph.application.service.EmployeeService;
import motorph.domain.model.Employee;
import motorph.domain.value.Role;
import motorph.ui.admin.accesslogs.AdminAccessLogsController;
import motorph.ui.admin.databasetools.AdminDatabaseToolsController;
import motorph.ui.admin.dashboard.AdminDashboardController;
import motorph.ui.admin.leave.AdminLeaveController;
import motorph.ui.admin.employee.AdminEmployeeController;
import motorph.ui.admin.payroll.AdminPayrollController;
import motorph.ui.admin.reports.AdminReportsController;
import motorph.ui.admin.roles.AdminRolesController;
import motorph.ui.admin.salary.AdminSalaryController;
import motorph.ui.admin.systemconfig.AdminSystemConfigController;
import motorph.ui.admin.useraccount.AdminUserAccountController;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.util.NavigationUtil;
import motorph.ui.util.ThemeManager;
import motorph.ui.util.UiMessageUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Manages the admin app shell: sidebar navigation, identity card, and global controls. */
public class AdminMainController {

    private EmployeeService employeeService;

    private ComboBox<String> languageCombo;
    private ComboBox<String> themeCombo;

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
    @FXML private AdminUserAccountController  userAccountViewController;

    // ── Roles & Permissions sub-view ─────────────────────────────────────────
    @FXML private HBox                   rolesView;
    @FXML private AdminRolesController   rolesViewController;

    // ── Access Logs sub-view ──────────────────────────────────────────────────
    @FXML private VBox                        accessLogsView;
    @FXML private AdminAccessLogsController   accessLogsViewController;

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
        if (!CurrentUserSession.hasRole(Role.EMPLOYEE)) return;

        CurrentUserSession.switchRole(Role.EMPLOYEE);
        NavigationUtil.goToEmployeeApp(rootPane.getScene());
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
        Button[] buttons = {
            dashboardNavButton, employeesNavButton, payrollNavButton, salaryNavButton, reportsNavButton,
            leaveOvertimeNavButton, userManagementNavButton, roleManagementNavButton,
            accessLogsNavButton, databaseToolsNavButton, settingsNavButton
        };
        for (Button btn : buttons) {
            if (btn != null) btn.getStyleClass().remove("selected-nav");
        }
        if (selected != null && !selected.getStyleClass().contains("selected-nav")) {
            selected.getStyleClass().add("selected-nav");
        }
    }

    private void setHeader(String title, String subtitle) {
        if (pageTitleLabel    != null) pageTitleLabel.setText(title);
        if (pageSubtitleLabel != null) pageSubtitleLabel.setText(subtitle);
    }

    private void loadSidebarData() {
        setSidebarFallback();
        int employeeId = CurrentUserSession.getEmployeeId();

        if (employeeId <= 0) {
            updateRoleSwitchButton();
            return;
        }

        try {
            Employee employee = employeeService.getEmployeeById(employeeId);
            if (employeeNameLabel != null) employeeNameLabel.setText(safe(employee.getFullName(), "Admin User"));
            if (employeeRoleLabel != null) employeeRoleLabel.setText(
                    CurrentUserSession.getActiveRole() != null
                            ? CurrentUserSession.getActiveRole().name()
                            : "ADMIN"
            );
        } catch (Exception e) {
            setSidebarFallback();
        }

        updateRoleSwitchButton();
    }

    private void setSidebarFallback() {
        if (employeeNameLabel != null) employeeNameLabel.setText("Admin User");
        if (employeeRoleLabel != null) employeeRoleLabel.setText("ADMIN");
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

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void buildGlobalControls() {
        if (globalControlsHost == null) return;

        languageCombo = new ComboBox<>();
        languageCombo.getStyleClass().add("global-combo");
        languageCombo.setPrefWidth(140);
        languageCombo.getItems().setAll("English", "Filipino");
        languageCombo.setValue(ThemeManager.getInstance().getCurrentLanguage());
        languageCombo.setOnAction(e -> ThemeManager.getInstance().setLanguage(languageCombo.getValue(), rootPane));

        themeCombo = new ComboBox<>();
        themeCombo.getStyleClass().add("global-combo");
        themeCombo.setPrefWidth(120);
        themeCombo.getItems().setAll("System", "Light", "Dark");
        themeCombo.setValue(ThemeManager.getInstance().getCurrentTheme());
        themeCombo.setOnAction(e -> ThemeManager.getInstance().setTheme(themeCombo.getValue(), rootPane));

        globalControlsHost.setPickOnBounds(false);
        globalControlsHost.getChildren().setAll(languageCombo, themeCombo);
    }
}
