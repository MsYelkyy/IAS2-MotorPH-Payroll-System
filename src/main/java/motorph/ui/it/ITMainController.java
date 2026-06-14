package motorph.ui.it;

import motorph.application.service.EmployeeService;
import motorph.domain.model.Employee;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.it.account.ITUserAccountController;
import motorph.ui.it.dashboard.ITDashboardController;
import motorph.ui.it.reset.PasswordResetRequestController;
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

/** Manages the IT app shell: sidebar navigation, employee card, and global controls. */
public class ITMainController {

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
    @FXML private Button userAccountsNavButton;
    @FXML private Button passwordResetNavButton;
    @FXML private Button accessLogsNavButton;
    @FXML private Button supportTicketsNavButton;
    @FXML private Button settingsNavButton;

    // ── Topbar ────────────────────────────────────────────────────────────────
    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;
    @FXML private HBox  globalControlsHost;

    // ── Dashboard sub-view ────────────────────────────────────────────────────
    @FXML private VBox                dashboardView;
    @FXML private ITDashboardController dashboardViewController;
    @FXML private VBox                userAccountsView;
    @FXML private ITUserAccountController userAccountsViewController;
    @FXML private VBox                passwordResetRequestsView;
    @FXML private PasswordResetRequestController passwordResetRequestsViewController;

    // ── Footer ────────────────────────────────────────────────────────────────
    @FXML private Label appMessageLabel;

    @FXML
    private void initialize() {
        System.out.println("ITMainController initialize called");
        employeeService = EmployeeUIServiceFactory.getInstance().getEmployeeService();

        buildGlobalControls();
        ThemeManager.getInstance().applyCurrentTo(rootPane);
        showDashboard();
        loadSidebarData();

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
    private void showUserAccounts() {
        setNavSelected(userAccountsNavButton);
        setHeader("User Accounts", "Manage system access and account status");
        showView(userAccountsView);
        if (userAccountsViewController != null) userAccountsViewController.refresh();
    }

    @FXML
    private void showPasswordReset() {
        setNavSelected(passwordResetNavButton);
        setHeader("Password Reset Requests", "Review and process employee password reset requests");
        showView(passwordResetRequestsView);
        if (passwordResetRequestsViewController != null) passwordResetRequestsViewController.refresh();
    }

    @FXML
    private void showAccessLogs() {
        setNavSelected(accessLogsNavButton);
        setHeader("User Access Logs", "Monitor login and access activity");
    }

    @FXML
    private void showSupportTickets() {
        setNavSelected(supportTicketsNavButton);
        setHeader("Support Tickets", "View and manage support requests");
    }

    @FXML
    private void showSettings() {
        setNavSelected(settingsNavButton);
        setHeader("System Settings", "Configure system preferences");
    }

    // ── Auth handlers ─────────────────────────────────────────────────────────

    @FXML
    private void handleSwitchRole() {
        CurrentUserSession.clear();
        NavigationUtil.goToLogin(rootPane.getScene());
    }

    @FXML
    private void handleLogout() {
        CurrentUserSession.clear();
        NavigationUtil.goToLogin(rootPane.getScene());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setNavSelected(Button selected) {
        Button[] buttons = {
            dashboardNavButton, userAccountsNavButton, passwordResetNavButton,
            accessLogsNavButton, supportTicketsNavButton, settingsNavButton
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

    private void showView(VBox view) {
        VBox[] views = { dashboardView, userAccountsView, passwordResetRequestsView };
        for (VBox current : views) {
            if (current == null) continue;
            boolean selected = current == view;
            current.setVisible(selected);
            current.setManaged(selected);
        }
    }

    // loads the logged-in employee name and active role into the sidebar
    private void loadSidebarData() {
        int employeeId = CurrentUserSession.getEmployeeId();

        System.out.println("Sidebar Employee ID: " + employeeId);
        System.out.println("Sidebar Role: " + CurrentUserSession.getActiveRole());
        System.out.println("Sidebar Email: " + CurrentUserSession.getCompanyEmail());

        if (employeeId <= 0) {
            System.out.println("Employee ID is invalid.");
            setSidebarFallback();
            return;
        }

        try {
            Employee employee = employeeService.getEmployeeById(employeeId);

            System.out.println("Employee found: " + employee.getFullName());

            employeeNameLabel.setText(employee.getFullName());
            employeeRoleLabel.setText(
                    "Current Role: " + (CurrentUserSession.getActiveRole() != null
                            ? CurrentUserSession.getActiveRole().name()
                            : "IT")
            );

        } catch (Exception e) {
            e.printStackTrace();
            setSidebarFallback();
        }
    }

    private void setSidebarFallback() {
        if (employeeNameLabel != null) employeeNameLabel.setText("IT User");
        if (employeeRoleLabel != null) employeeRoleLabel.setText("Current Role: IT");
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
