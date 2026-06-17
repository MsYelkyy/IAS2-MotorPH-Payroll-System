package motorph.ui.it;

import motorph.application.service.EmployeeService;
import motorph.domain.value.Role;
import motorph.ui.admin.reports.AdminReportsController;
import motorph.ui.admin.roles.AdminRolesController;
import motorph.ui.admin.systemconfig.AdminSystemConfigController;
import motorph.ui.common.GlobalControlsHelper;
import motorph.ui.common.NavigationHelper;
import motorph.ui.common.RoleSwitchHelper;
import motorph.ui.common.SidebarHelper;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.it.accesslogs.ITAccessLogsController;
import motorph.ui.it.account.ITUserAccountController;
import motorph.ui.it.dashboard.ITDashboardController;
import motorph.ui.it.reset.PasswordResetRequestController;
import motorph.ui.it.ticket.ITSupportTicketController;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.util.NavigationUtil;
import motorph.ui.util.ThemeManager;
import motorph.ui.util.UiMessageUtil;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Manages the IT app shell: sidebar navigation, identity card, and global controls. */
public class ITMainController {

    private EmployeeService employeeService;

    // ── Root ──────────────────────────────────────────────────────────────────
    @FXML private StackPane rootPane;

    // ── Sidebar identity card ─────────────────────────────────────────────────
    @FXML private Label employeeNameLabel;
    @FXML private Label employeeRoleLabel;

    // ── Navigation buttons ────────────────────────────────────────────────────
    @FXML private Button dashboardNavButton;
    @FXML private Button reportsNavButton;
    @FXML private Button userManagementNavButton;
    @FXML private Button roleManagementNavButton;
    @FXML private Button accessLogsNavButton;
    @FXML private Button passwordResetNavButton;
    @FXML private Button supportTicketsNavButton;
    @FXML private Button settingsNavButton;
    @FXML private Button switchRoleButton;

    // ── Topbar ────────────────────────────────────────────────────────────────
    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;
    @FXML private HBox  globalControlsHost;

    // ── Dashboard sub-view ────────────────────────────────────────────────────
    @FXML private VBox                  dashboardView;
    @FXML private ITDashboardController dashboardViewController;

    // ── Reports sub-view (reuses AdminReportsController) ─────────────────────
    @FXML private VBox                   reportsView;
    @FXML private AdminReportsController reportsViewController;

    // ── User Management sub-view ──────────────────────────────────────────────
    @FXML private VBox                    userManagementView;
    @FXML private ITUserAccountController userManagementViewController;

    // ── Role Management sub-view (reuses AdminRolesController) ───────────────
    @FXML private VBox                 roleManagementView;
    @FXML private AdminRolesController roleManagementViewController;

    // ── User Access Logs sub-view ─────────────────────────────────────────────
    @FXML private VBox                   accessLogsView;
    @FXML private ITAccessLogsController accessLogsViewController;

    // ── Password Reset Requests sub-view ──────────────────────────────────────
    @FXML private VBox                          passwordResetRequestsView;
    @FXML private PasswordResetRequestController passwordResetRequestsViewController;

    // ── Support Tickets sub-view ──────────────────────────────────────────────
    @FXML private VBox                    supportTicketsView;
    @FXML private ITSupportTicketController supportTicketsViewController;

    // ── System Settings sub-view (reuses AdminSystemConfigController) ─────────
    @FXML private VBox                        systemSettingsView;
    @FXML private AdminSystemConfigController systemSettingsViewController;

    // ── Footer ────────────────────────────────────────────────────────────────
    @FXML private Label appMessageLabel;

    @FXML
    private void initialize() {
        employeeService = EmployeeUIServiceFactory.getInstance().getEmployeeService();

        buildGlobalControls();
        ThemeManager.getInstance().applyCurrentTo(rootPane);
        wireDashboard();
        showDashboard();
        loadSidebarData();
        configureSwitchRoleButton();

        UiMessageUtil.setSuccess(appMessageLabel, "IT Management System is ready.");
    }

    // ── Navigation handlers ───────────────────────────────────────────────────

    @FXML
    private void showDashboard() {
        setNavSelected(dashboardNavButton);
        setHeader("Dashboard", "IT MANAGEMENT SYSTEM");
        showView(dashboardView);
        if (dashboardViewController != null) dashboardViewController.refresh();
    }

    @FXML
    private void showReports() {
        setNavSelected(reportsNavButton);
        setHeader("Reports", "Generate and preview organization reports");
        showView(reportsView);
        if (reportsViewController != null) reportsViewController.refresh();
    }

    @FXML
    private void showUserManagement() {
        setNavSelected(userManagementNavButton);
        setHeader("User Management", "Manage system access and account status");
        showView(userManagementView);
        if (userManagementViewController != null) userManagementViewController.refresh();
    }

    @FXML
    private void showRoleManagement() {
        setNavSelected(roleManagementNavButton);
        setHeader("Role Management", "Configure role permissions");
        showView(roleManagementView);
        if (roleManagementViewController != null) roleManagementViewController.refresh();
    }

    @FXML
    private void showAccessLogs() {
        setNavSelected(accessLogsNavButton);
        setHeader("User Access Logs", "Monitor login and access activity");
        showView(accessLogsView);
        if (accessLogsViewController != null) accessLogsViewController.refresh();
    }

    @FXML
    private void showPasswordReset() {
        setNavSelected(passwordResetNavButton);
        setHeader("Password Reset Requests", "Review and process employee password reset requests");
        showView(passwordResetRequestsView);
        if (passwordResetRequestsViewController != null) passwordResetRequestsViewController.refresh();
    }

    @FXML
    private void showSupportTickets() {
        setNavSelected(supportTicketsNavButton);
        setHeader("Support Tickets", "Manage and respond to employee support requests");
        showView(supportTicketsView);
        if (supportTicketsViewController != null) supportTicketsViewController.refresh();
    }

    @FXML
    private void showSettings() {
        setNavSelected(settingsNavButton);
        setHeader("System Settings", "Configure system preferences");
        showView(systemSettingsView);
        if (systemSettingsViewController != null) systemSettingsViewController.refresh();
    }

    // ── Public navigation entry points (called by sub-controllers) ───────────

    /** Called by ITDashboardController's "View All" button. */
    public void navigateToSupportTickets() {
        showSupportTickets();
    }

    /** Called by ITDashboardController's "View All" button for resets. */
    public void navigateToPasswordReset() {
        showPasswordReset();
    }

    // ── Auth handlers ─────────────────────────────────────────────────────────

    @FXML
    private void handleSwitchRole() {
        RoleSwitchHelper.switchTo(Role.EMPLOYEE, rootPane.getScene(), appMessageLabel, "No employee role available.");
    }

    @FXML
    private void handleLogout() {
        CurrentUserSession.clear();
        NavigationUtil.goToLogin(rootPane.getScene());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setNavSelected(Button selected) {
        NavigationHelper.setNavSelected(selected,
                dashboardNavButton, reportsNavButton, userManagementNavButton,
                roleManagementNavButton, accessLogsNavButton,
                passwordResetNavButton, supportTicketsNavButton, settingsNavButton);
    }

    private void setHeader(String title, String subtitle) {
        NavigationHelper.setHeader(pageTitleLabel, pageSubtitleLabel, title, subtitle);
    }

    private void showView(Node view) {
        Node[] views = {
            dashboardView, reportsView, userManagementView,
            roleManagementView, accessLogsView,
            passwordResetRequestsView, supportTicketsView, systemSettingsView
        };
        for (Node current : views) {
            if (current == null) continue;
            boolean selected = current == view;
            current.setVisible(selected);
            current.setManaged(selected);
        }
    }

    private void wireDashboard() {
        if (dashboardViewController != null) {
            dashboardViewController.setMainController(this);
        }
    }

    private void loadSidebarData() {
        SidebarHelper.load(employeeNameLabel, employeeRoleLabel, null,
                "IT User", "IT", "", employeeService);
    }

    private void configureSwitchRoleButton() {
        boolean show = CurrentUserSession.hasMultipleRoles();
        switchRoleButton.setVisible(show);
        switchRoleButton.setManaged(show);
    }

    private void buildGlobalControls() {
        GlobalControlsHelper.build(globalControlsHost, rootPane, null);
    }
}
