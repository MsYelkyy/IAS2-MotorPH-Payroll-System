package motorph.ui.it;

import motorph.api.ApiClientFactory;
import motorph.api.EmployeeApiClient;
import motorph.ui.common.NavigationHelper;
import motorph.ui.common.RoleSwitchHelper;
import motorph.ui.common.SidebarHelper;
import motorph.ui.reports.ReportsController;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.support.SupportTicketController;
import motorph.ui.util.NavigationUtil;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Manages the IT app shell: sidebar navigation, identity card, and global controls. */
public class ITMainController {

    private EmployeeApiClient employeeApiClient;

    // Root
    @FXML private StackPane rootPane;

    // Sidebar identity card
    @FXML private Label employeeNameLabel;
    @FXML private Label employeeRoleLabel;

    // Navigation buttons
    @FXML private Button dashboardNavButton;
    @FXML private Button reportsNavButton;
    @FXML private Button userManagementNavButton;
    @FXML private Button roleManagementNavButton;
    @FXML private Button accessLogsNavButton;
    @FXML private Button passwordResetNavButton;
    @FXML private Button supportTicketsNavButton;
    @FXML private Button switchRoleButton;

    // Topbar
    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;

    // Dashboard sub-view
    @FXML private VBox                  dashboardView;
    @FXML private ITDashboardController dashboardViewController;

    // Reports sub-view (shared ReportsController)
    @FXML private VBox              reportsView;
    @FXML private ReportsController reportsViewController;

    // User Management sub-view
    @FXML private VBox                    userManagementView;
    @FXML private ITUserAccountController userManagementViewController;

    // Role Management sub-view (IT-owned ITRolesController)
    @FXML private VBox              roleManagementView;
    @FXML private ITRolesController roleManagementViewController;

    // User Access Logs sub-view
    @FXML private VBox                   accessLogsView;
    @FXML private ITAccessLogsController accessLogsViewController;

    // Password Reset Requests sub-view
    @FXML private VBox                          passwordResetRequestsView;
    @FXML private PasswordResetRequestController passwordResetRequestsViewController;

    // Support Tickets sub-view
    @FXML private VBox                    supportTicketsView;
    @FXML private SupportTicketController supportTicketsViewController;

    @FXML
    private void initialize() {
        employeeApiClient = ApiClientFactory.getInstance().getEmployeeApiClient();

        wireDashboard();
        showDashboard();
        loadSidebarData();
        configureSwitchRoleButton();
    }

    // Navigation handlers
    @FXML
    private void showDashboard() {
        setNavSelected(dashboardNavButton);
        setHeader("Dashboard");
        showView(dashboardView);
        if (dashboardViewController != null) dashboardViewController.refresh();
    }

    @FXML
    private void showReports() {
        setNavSelected(reportsNavButton);
        setHeader("Reports");
        showView(reportsView);
        if (reportsViewController != null) reportsViewController.refresh();
    }

    @FXML
    private void showUserManagement() {
        setNavSelected(userManagementNavButton);
        setHeader("User Management");
        showView(userManagementView);
        if (userManagementViewController != null) userManagementViewController.refresh();
    }

    @FXML
    private void showRoleManagement() {
        setNavSelected(roleManagementNavButton);
        setHeader("Role Management");
        showView(roleManagementView);
        if (roleManagementViewController != null) roleManagementViewController.refresh();
    }

    @FXML
    private void showAccessLogs() {
        setNavSelected(accessLogsNavButton);
        setHeader("User Access Logs");
        showView(accessLogsView);
        if (accessLogsViewController != null) accessLogsViewController.refresh();
    }

    @FXML
    private void showPasswordReset() {
        setNavSelected(passwordResetNavButton);
        setHeader("Password Reset Requests");
        showView(passwordResetRequestsView);
        if (passwordResetRequestsViewController != null) passwordResetRequestsViewController.refresh();
    }

    @FXML
    private void showSupportTickets() {
        setNavSelected(supportTicketsNavButton);
        setHeader("Support Tickets");
        showView(supportTicketsView);
        if (supportTicketsViewController != null) supportTicketsViewController.refresh();
    }

    // Public navigation entry points (called by sub-controllers)
    /** Called by ITDashboardController's "View All" button. */
    public void navigateToSupportTickets() {
        showSupportTickets();
    }

    /** Called by ITDashboardController's "View All" button for resets. */
    public void navigateToPasswordReset() {
        showPasswordReset();
    }

    // Auth handlers
    @FXML
    private void handleSwitchRole() {
        RoleSwitchHelper.switchToNext(rootPane.getScene(), pageTitleLabel);
    }

    @FXML
    private void handleLogout() {
        CurrentUserSession.clear();
        NavigationUtil.goToLogin(rootPane.getScene());
    }

    // Helpers
    private void setNavSelected(Button selected) {
        NavigationHelper.setNavSelected(selected,
                dashboardNavButton, reportsNavButton, userManagementNavButton,
                roleManagementNavButton, accessLogsNavButton,
                passwordResetNavButton, supportTicketsNavButton);
    }

    private void setHeader(String title) {
        NavigationHelper.setHeader(pageTitleLabel, pageSubtitleLabel, title, "IT WORKSPACE");
    }

    private void showView(Node view) {
        Node[] views = {
            dashboardView, reportsView, userManagementView,
            roleManagementView, accessLogsView,
            passwordResetRequestsView, supportTicketsView
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
                "IT User", "IT", "", employeeApiClient);
    }

    private void configureSwitchRoleButton() {
        boolean show = CurrentUserSession.hasMultipleRoles();
        if (show) switchRoleButton.setText("Switch to " + RoleSwitchHelper.nextRoleLabel());
        switchRoleButton.setVisible(show);
        switchRoleButton.setManaged(show);
    }

}
