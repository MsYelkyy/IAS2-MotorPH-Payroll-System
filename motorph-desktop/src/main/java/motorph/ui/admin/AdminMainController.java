package motorph.ui.admin;

import motorph.api.ApiClientFactory;
import motorph.api.EmployeeApiClient;
import motorph.ui.finance.PayrollManagementController;
import motorph.ui.hr.HrEmployeeController;
import motorph.ui.hr.HrLeaveController;
import motorph.ui.it.ITAccessLogsController;
import motorph.ui.it.ITUserAccountController;
import motorph.ui.it.ITRolesController;
import motorph.ui.reports.ReportsController;
import motorph.ui.common.NavigationHelper;
import motorph.ui.common.RoleSwitchHelper;
import motorph.ui.common.SidebarHelper;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.support.SupportTicketController;
import motorph.ui.util.NavigationUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Manages the admin app shell: sidebar navigation, identity card, and global controls. */
public class AdminMainController {

    private EmployeeApiClient employeeApiClient;

    // Root
    @FXML private StackPane rootPane;

    // Sidebar identity card
    @FXML private Label employeeNameLabel;
    @FXML private Label employeeRoleLabel;

    // Navigation buttons
    @FXML private Button dashboardNavButton;
    @FXML private Button employeesNavButton;
    @FXML private Button payrollNavButton;
    @FXML private Button salaryNavButton;
    @FXML private Button reportsNavButton;
    @FXML private Button leaveOvertimeNavButton;
    @FXML private Button userManagementNavButton;
    @FXML private Button roleManagementNavButton;
    @FXML private Button accessLogsNavButton;
    @FXML private Button supportTicketsNavButton;
    @FXML private Button databaseToolsNavButton;
    @FXML private Button settingsNavButton;
    @FXML private Button switchRoleButton;

    // Topbar
    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;

    // Dashboard sub-view
    @FXML private VBox                     dashboardView;
    @FXML private AdminDashboardController dashboardViewController;

    // Employee sub-view (HR-owned screen)
    @FXML private VBox                   employeeView;
    @FXML private HrEmployeeController   employeeViewController;

    // Payroll sub-view (Finance-owned screen)
    @FXML private VBox                        payrollView;
    @FXML private PayrollManagementController  payrollViewController;

    // Salary & Benefits sub-view
    @FXML private VBox                    salaryView;
    @FXML private AdminSalaryController   salaryViewController;

    // User Account Management sub-view
    @FXML private VBox                        userAccountView;
    @FXML private ITUserAccountController     userAccountViewController;

    // Roles & Permissions sub-view
    @FXML private VBox                   rolesView;
    @FXML private ITRolesController      rolesViewController;

    // Access Logs sub-view
    @FXML private VBox                        accessLogsView;
    @FXML private ITAccessLogsController      accessLogsViewController;

    // Reports sub-view
    @FXML private VBox               reportsView;
    @FXML private ReportsController  reportsViewController;

    // Leave & Overtime sub-view (HR-owned screen)
    @FXML private VBox              leaveView;
    @FXML private HrLeaveController leaveViewController;

    // System Config sub-view
    @FXML private VBox                          systemConfigView;
    @FXML private AdminSystemConfigController   systemConfigViewController;

    // Database Tools sub-view
    @FXML private VBox                          databaseToolsView;
    @FXML private AdminDatabaseToolsController  databaseToolsViewController;

    // Support Tickets sub-view
    @FXML private VBox                          supportTicketsView;
    @FXML private SupportTicketController  supportTicketsViewController;

    // Module placeholder (shown for unbuilt modules)
    @FXML private VBox modulePlaceholder;

    @FXML
    private void initialize() {
        employeeApiClient = ApiClientFactory.getInstance().getEmployeeApiClient();

        loadSidebarData();
        showDashboard();
    }

    // Navigation handlers
    @FXML
    private void showDashboard() {
        setNavSelected(dashboardNavButton);
        setHeader("Dashboard");
        showView(ActiveView.DASHBOARD);
        if (dashboardViewController != null) dashboardViewController.refresh();
    }

    @FXML
    private void showEmployees() {
        setNavSelected(employeesNavButton);
        setHeader("Employee Management");
        showView(ActiveView.EMPLOYEES);
        if (employeeViewController != null) employeeViewController.refresh();
    }

    @FXML
    private void showPayroll() {
        setNavSelected(payrollNavButton);
        setHeader("Payroll");
        showView(ActiveView.PAYROLL);
        if (payrollViewController != null) payrollViewController.refresh();
    }

    @FXML
    private void showSalary() {
        setNavSelected(salaryNavButton);
        setHeader("Salary & Benefits");
        showView(ActiveView.SALARY);
        if (salaryViewController != null) salaryViewController.refresh();
    }

    @FXML
    private void showReports() {
        setNavSelected(reportsNavButton);
        setHeader("Reports");
        showView(ActiveView.REPORTS);
        if (reportsViewController != null) reportsViewController.refresh();
    }

    @FXML
    private void showLeaveOvertime() {
        setNavSelected(leaveOvertimeNavButton);
        setHeader("Leave & Overtime");
        showView(ActiveView.LEAVE);
        if (leaveViewController != null) leaveViewController.refresh();
    }

    @FXML
    private void showUserManagement() {
        setNavSelected(userManagementNavButton);
        setHeader("User Management");
        showView(ActiveView.USER_ACCOUNTS);
        if (userAccountViewController != null) userAccountViewController.refresh();
    }

    @FXML
    private void showRoleManagement() {
        setNavSelected(roleManagementNavButton);
        setHeader("Role Management");
        showView(ActiveView.ROLES);
        if (rolesViewController != null) rolesViewController.refresh();
    }

    @FXML
    private void showAccessLogs() {
        setNavSelected(accessLogsNavButton);
        setHeader("User Access Logs");
        showView(ActiveView.ACCESS_LOGS);
        if (accessLogsViewController != null) accessLogsViewController.refresh();
    }

    @FXML
    private void showDatabaseTools() {
        setNavSelected(databaseToolsNavButton);
        setHeader("Database Backup & Recovery");
        showView(ActiveView.DATABASE_TOOLS);
        if (databaseToolsViewController != null) databaseToolsViewController.refresh();
    }

    @FXML
    private void showSettings() {
        setNavSelected(settingsNavButton);
        setHeader("Settings");
        showView(ActiveView.SYSTEM_CONFIG);
        if (systemConfigViewController != null) systemConfigViewController.refresh();
    }

    @FXML
    private void showSupportTickets() {
        setNavSelected(supportTicketsNavButton);
        setHeader("Support Tickets");
        showView(ActiveView.SUPPORT_TICKETS);
        if (supportTicketsViewController != null) supportTicketsViewController.refresh();
    }

    // Auth handlers
    @FXML
    private void handleLogout() {
        CurrentUserSession.clear();
        NavigationUtil.goToLogin(rootPane.getScene());
    }

    @FXML
    private void handleSwitchRole() {
        RoleSwitchHelper.switchToNext(rootPane.getScene(), null);
    }

    // Helpers
    private enum ActiveView { DASHBOARD, EMPLOYEES, PAYROLL, SALARY, REPORTS, USER_ACCOUNTS, ROLES, ACCESS_LOGS, LEAVE, SYSTEM_CONFIG, DATABASE_TOOLS, SUPPORT_TICKETS, PLACEHOLDER }

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
        setPane(supportTicketsView,  active == ActiveView.SUPPORT_TICKETS);
        setPane(modulePlaceholder,   active == ActiveView.PLACEHOLDER);
    }

    private void setPane(javafx.scene.Node node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    private void setNavSelected(Button selected) {
        NavigationHelper.setNavSelected(selected,
                dashboardNavButton, employeesNavButton, payrollNavButton, salaryNavButton, reportsNavButton,
                leaveOvertimeNavButton, userManagementNavButton, roleManagementNavButton,
                accessLogsNavButton, supportTicketsNavButton, databaseToolsNavButton, settingsNavButton);
    }

    private void setHeader(String title) {
        NavigationHelper.setHeader(pageTitleLabel, pageSubtitleLabel, title, "ADMIN CONTROL PANEL");
    }

    private void loadSidebarData() {
        SidebarHelper.load(employeeNameLabel, employeeRoleLabel, null,
                "Admin User", "ADMIN", "", employeeApiClient);
        updateRoleSwitchButton();
    }

    private void updateRoleSwitchButton() {
        boolean show = CurrentUserSession.hasMultipleRoles();

        if (switchRoleButton != null) {
            if (show) switchRoleButton.setText("Switch to " + RoleSwitchHelper.nextRoleLabel());
            switchRoleButton.setVisible(show);
            switchRoleButton.setManaged(show);
        }
    }

}
