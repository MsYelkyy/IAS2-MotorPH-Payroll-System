package motorph.ui.hr;

import motorph.application.service.EmployeeService;
import motorph.ui.common.GlobalControlsHelper;
import motorph.ui.common.NavigationHelper;
import motorph.ui.common.RoleSwitchHelper;
import motorph.ui.common.SidebarHelper;
import motorph.ui.factory.HrUIServiceFactory;
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

/** Manages the HR app shell: sidebar navigation, identity card, and global controls. */
public class HrMainController {

    private EmployeeService employeeService;

    // Root
    @FXML private StackPane rootPane;

    // Sidebar identity card
    @FXML private Label employeeNameLabel;
    @FXML private Label employeeRoleLabel;

    // Navigation buttons
    @FXML private Button dashboardNavButton;
    @FXML private Button switchRoleButton;

    // Topbar
    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;
    @FXML private HBox  globalControlsHost;

    // Dashboard sub-view
    @FXML private VBox                dashboardView;
    @FXML private HrDashboardController dashboardViewController;

    // Footer
    @FXML private Label appMessageLabel;

    // sets up services, controls, and the default dashboard view
    @FXML
    private void initialize() {
        employeeService = HrUIServiceFactory.getInstance().getEmployeeService();

        buildGlobalControls();
        ThemeManager.getInstance().applyCurrentTo(rootPane);
        showDashboard();
        loadSidebarData();
        configureSwitchRoleButton();

        UiMessageUtil.setSuccess(appMessageLabel, "HR Management System is ready.");
    }

    // shows the HR dashboard view
    @FXML
    private void showDashboard() {
        setNavSelected(dashboardNavButton);
        setHeader("Dashboard", "HR MANAGEMENT SYSTEM");
        showView(dashboardView);
        if (dashboardViewController != null) dashboardViewController.refresh();
    }

    // switches to the next assigned role
    @FXML
    private void handleSwitchRole() {
        RoleSwitchHelper.switchToNext(rootPane.getScene(), appMessageLabel);
    }

    // clears the session and returns to login
    @FXML
    private void handleLogout() {
        CurrentUserSession.clear();
        NavigationUtil.goToLogin(rootPane.getScene());
    }

    // highlights the active navigation button
    private void setNavSelected(Button selected) {
        NavigationHelper.setNavSelected(selected, dashboardNavButton);
    }

    // updates the topbar title and subtitle
    private void setHeader(String title, String subtitle) {
        NavigationHelper.setHeader(pageTitleLabel, pageSubtitleLabel, title, subtitle);
    }

    // shows the given view and hides the others
    private void showView(Node view) {
        Node[] views = { dashboardView };
        for (Node current : views) {
            if (current == null) continue;
            boolean selected = current == view;
            current.setVisible(selected);
            current.setManaged(selected);
        }
    }

    // loads the logged-in HR user's identity into the sidebar
    private void loadSidebarData() {
        SidebarHelper.load(employeeNameLabel, employeeRoleLabel, null,
                "HR User", "HR", "", employeeService);
    }

    // shows the switch-role button only for multi-role users
    private void configureSwitchRoleButton() {
        boolean show = CurrentUserSession.hasMultipleRoles();
        switchRoleButton.setVisible(show);
        switchRoleButton.setManaged(show);
    }

    // builds the language and theme controls in the topbar
    private void buildGlobalControls() {
        GlobalControlsHelper.build(globalControlsHost, rootPane, null);
    }
}
