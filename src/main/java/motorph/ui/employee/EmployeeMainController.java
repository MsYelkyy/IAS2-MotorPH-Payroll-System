package motorph.ui.employee;

import motorph.application.service.EmployeeService;
import motorph.domain.model.Employee;
import motorph.domain.value.Role;
import motorph.ui.employee.attendance.EmployeeAttendanceController;
import motorph.ui.employee.dashboard.EmployeeDashboardController;
import motorph.ui.employee.leave.EmployeeLeaveController;
import motorph.ui.employee.payslip.EmployeePayslipController;
import motorph.ui.employee.profile.EmployeeProfileController;
import motorph.ui.employee.settings.EmployeeSettingsController;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.util.AnimationUtil;
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

import java.util.List;

/** Manages the employee app shell: sidebar navigation, view switching, and global controls. */
public class EmployeeMainController {

    private EmployeeService employeeService;

    // programmatic global controls inserted into the topbar host
    private ComboBox<String> languageCombo;
    private ComboBox<String> themeCombo;

    @FXML
    private StackPane rootPane;

    // ── Sidebar ──────────────────────────────────────────────────────────────

    @FXML
    private Label dashboardEmployeeNameLabel;

    @FXML
    private Label dashboardEmployeeRoleLabel;

    @FXML
    private Label dashboardEmailLabel;

    // ── Navigation buttons ───────────────────────────────────────────────────

    @FXML
    private Button dashboardNavButton;

    @FXML
    private Button profileNavButton;

    @FXML
    private Button attendanceNavButton;

    @FXML
    private Button leaveNavButton;

    @FXML
    private Button payslipNavButton;

    @FXML
    private Button settingsNavButton;

    @FXML
    private Button switchRoleButton;

    // ── Topbar ───────────────────────────────────────────────────────────────

    @FXML
    private Label pageTitleLabel;

    @FXML
    private Label pageSubtitleLabel;

    @FXML
    private HBox globalControlsHostApp;

    // ── Content views (roots from fx:include) ────────────────────────────────

    @FXML
    private VBox dashboardView;

    @FXML
    private VBox profileView;

    @FXML
    private VBox attendanceView;

    @FXML
    private VBox leaveView;

    @FXML
    private VBox payslipView;

    @FXML
    private VBox settingsView;

    // ── Sub-controllers (injected by fx:include) ─────────────────────────────

    @FXML
    private EmployeeDashboardController dashboardViewController;

    @FXML
    private EmployeeProfileController profileViewController;

    @FXML
    private EmployeeAttendanceController attendanceViewController;

    @FXML
    private EmployeeLeaveController leaveViewController;

    @FXML
    private EmployeePayslipController payslipViewController;

    @FXML
    private EmployeeSettingsController settingsViewController;

    // ── Footer ───────────────────────────────────────────────────────────────

    @FXML
    private Label appMessageLabel;

    // ── Side panel ───────────────────────────────────────────────────────────

    @FXML
    private VBox sidePanel;

    @FXML
    private void initialize() {
        employeeService = EmployeeUIServiceFactory.getInstance().getEmployeeService();

        buildGlobalControls();
        wireSubControllers();
        initSidePanel();

        ThemeManager.getInstance().applyCurrentTo(rootPane);

        loadSidebarData();
        showDashboard();

        UiMessageUtil.setSuccess(appMessageLabel, "Employee workspace is ready.");
    }

    @FXML
    private void showDashboard() {
        showView(dashboardView, dashboardNavButton, "Dashboard", "EMPLOYEE WORKSPACE");
        if (dashboardViewController != null) dashboardViewController.refresh();
    }

    @FXML
    private void showProfile() {
        showView(profileView, profileNavButton, "My Profile", "View and update your contact information");
        if (profileViewController != null) profileViewController.refresh();
    }

    @FXML
    private void showAttendance() {
        showView(attendanceView, attendanceNavButton, "Attendance", "Daily time-in, time-out, and attendance logs");
        if (attendanceViewController != null) attendanceViewController.refresh();
    }

    @FXML
    private void showLeave() {
        showView(leaveView, leaveNavButton, "Leave Request", "Submit leave requests and monitor credits");

        if (leaveViewController != null) {
            leaveViewController.resetForm();
            leaveViewController.refresh();
        }
    }

    @FXML
    private void showPayslip() {
        showView(payslipView, payslipNavButton, "Payslip / Payroll", "View persisted payroll results and payslips");
        if (payslipViewController != null) payslipViewController.refresh();
    }

    @FXML
    private void showSettings() {
        showView(settingsView, settingsNavButton, "Account Settings", "Manage password and recovery-account details");

        if (settingsViewController != null) {
            settingsViewController.resetForm();
            settingsViewController.refresh();
        }
    }

    @FXML
    private void handleLogout() {
        CurrentUserSession.clear();
        NavigationUtil.goToLogin(rootPane.getScene());
    }

    @FXML
    private void handleSwitchRole() {
        Role targetRole = getPreferredWorkRole();
        if (targetRole == null) return;

        CurrentUserSession.switchRole(targetRole);
        navigateToRole(targetRole);
    }

    @FXML
    private void toggleSidePanel() {
        if (sidePanel == null) return;
        if (sidePanel.isVisible()) {
            AnimationUtil.slideOutToRight(sidePanel, 280, null);
        } else {
            AnimationUtil.slideInFromRight(sidePanel, 280);
        }
    }

    // makes only the target view visible and marks the matching nav button as selected
    private void showView(VBox target, Button selectedBtn, String title, String subtitle) {
        for (VBox view : List.of(dashboardView, profileView, attendanceView, leaveView, payslipView, settingsView)) {
            boolean active = view == target;
            view.setVisible(active);
            view.setManaged(active);
        }
        for (Button btn : List.of(dashboardNavButton, profileNavButton, attendanceNavButton, leaveNavButton, payslipNavButton, settingsNavButton)) {
            btn.getStyleClass().remove("selected-nav");
        }
        if (!selectedBtn.getStyleClass().contains("selected-nav")) {
            selectedBtn.getStyleClass().add("selected-nav");
        }
        if (pageTitleLabel    != null) pageTitleLabel.setText(title);
        if (pageSubtitleLabel != null) pageSubtitleLabel.setText(subtitle);
    }

    // loads the current employee's name, role, and email into the sidebar
    private void loadSidebarData() {
        setSidebarFallback();
        int employeeId = CurrentUserSession.getEmployeeId();

        if (employeeId <= 0) {
            updateRoleSwitchButton();
            return;
        }

        try {
            Employee employee = employeeService.getEmployeeById(employeeId);

            if (dashboardEmployeeNameLabel != null) {
                dashboardEmployeeNameLabel.setText(safe(employee.getFullName(), "Employee"));
            }
            if (dashboardEmployeeRoleLabel != null) {
                dashboardEmployeeRoleLabel.setText(activeRoleName("EMPLOYEE"));
            }
            if (dashboardEmailLabel != null) {
                dashboardEmailLabel.setText(safe(CurrentUserSession.getCompanyEmail(), "No email on file"));
            }

        } catch (Exception e) {
            setSidebarFallback();
        }

        updateRoleSwitchButton();
    }

    // shows default sidebar text when employee data is unavailable
    private void setSidebarFallback() {
        if (dashboardEmployeeNameLabel != null) {
            dashboardEmployeeNameLabel.setText("Employee");
        }

        if (dashboardEmployeeRoleLabel != null) {
            dashboardEmployeeRoleLabel.setText("EMPLOYEE");
        }

        if (dashboardEmailLabel != null) {
            dashboardEmailLabel.setText("No email on file");
        }
    }

    private void updateRoleSwitchButton() {
        Role targetRole = getPreferredWorkRole();
        boolean show = targetRole != null;

        if (switchRoleButton != null) {
            switchRoleButton.setVisible(show);
            switchRoleButton.setManaged(show);
            if (show) {
                switchRoleButton.setText("Switch to " + displayRole(targetRole));
            }
        }
    }

    private Role getPreferredWorkRole() {
        if (!CurrentUserSession.hasMultipleRoles()) return null;

        for (Role role : List.of(Role.ADMIN, Role.IT, Role.HR, Role.FINANCE)) {
            if (CurrentUserSession.hasRole(role) && CurrentUserSession.getActiveRole() != role) {
                return role;
            }
        }

        return null;
    }

    private void navigateToRole(Role role) {
        if (role == Role.ADMIN || role == Role.HR || role == Role.FINANCE) {
            NavigationUtil.goToAdminApp(rootPane.getScene());
        } else if (role == Role.IT) {
            NavigationUtil.goToITApp(rootPane.getScene());
        } else {
            NavigationUtil.goToEmployeeApp(rootPane.getScene());
        }
    }

    private String activeRoleName(String fallback) {
        Role role = CurrentUserSession.getActiveRole();
        return role != null ? role.name() : fallback;
    }

    private String displayRole(Role role) {
        return role == null ? "Role" : role.name();
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    // builds and inserts language/theme combos into the topbar global controls host
    private void buildGlobalControls() {
        if (globalControlsHostApp == null) return;

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

        Button moreBtn = new Button("⋮");
        moreBtn.getStyleClass().add("more-button");
        moreBtn.setOnAction(e -> toggleSidePanel());

        globalControlsHostApp.setPickOnBounds(false);
        globalControlsHostApp.getChildren().setAll(languageCombo, themeCombo, moreBtn);
    }

    // wires cross-controller callbacks between sub-controllers and this shell
    private void wireSubControllers() {
        if (dashboardViewController != null) {
            dashboardViewController.setOnNavigateToPayslip(this::showPayslip);
        }
        if (leaveViewController != null) {
            leaveViewController.setOnLeaveCreditsUpdated(credits -> {
                if (dashboardViewController != null) dashboardViewController.setLeaveCreditsDisplay(credits);
            });
        }
    }

    // hides the side panel on startup
    private void initSidePanel() {
        if (sidePanel != null) {
            sidePanel.setVisible(false);
            sidePanel.setManaged(false);
        }
    }
}
