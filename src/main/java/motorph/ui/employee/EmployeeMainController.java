package motorph.ui.employee;

import motorph.application.service.EmployeeService;
import motorph.ui.employee.attendance.EmployeeAttendanceController;
import motorph.ui.employee.dashboard.EmployeeDashboardController;
import motorph.ui.employee.leave.EmployeeLeaveController;
import motorph.ui.employee.payslip.EmployeePayslipController;
import motorph.ui.employee.profile.EmployeeProfileController;
import motorph.ui.employee.settings.EmployeeSettingsController;
import motorph.ui.employee.support.EmployeeSupportTicketController;
import motorph.ui.common.GlobalControlsHelper;
import motorph.ui.common.NavigationHelper;
import motorph.ui.common.RoleSwitchHelper;
import motorph.ui.common.SidebarHelper;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.util.AnimationUtil;
import motorph.ui.util.NavigationUtil;
import motorph.ui.util.ThemeManager;
import motorph.ui.util.UiMessageUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

/** Manages the Employee app shell. */
public class EmployeeMainController {

    private EmployeeService employeeService;

    @FXML
    private StackPane rootPane;

    // ── Sidebar ──────────────────────────────────────────────────────────────

    @FXML
    private Label dashboardEmployeeNameLabel;

    @FXML
    private Label dashboardEmployeeRoleLabel;

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
    private Button supportTicketsNavButton;

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
    private VBox supportTicketsView;

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
    private EmployeeSupportTicketController supportTicketsViewController;

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
        configureSwitchRoleButton();
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
    private void showSupportTickets() {
        showView(supportTicketsView, supportTicketsNavButton, "Support Tickets",
                "Submit a support ticket and track your requests");
        if (supportTicketsViewController != null) supportTicketsViewController.refresh();
    }

    @FXML
    private void showSettings() {
        showView(settingsView, settingsNavButton, "Account Settings", "Manage password and recovery-account details");

        if (settingsViewController != null) {
            settingsViewController.resetForm();
            settingsViewController.refresh();
        }
    }

    // switches to the first non-active secondary role (IT before ADMIN)
    @FXML
    private void handleSwitchRole() {
        RoleSwitchHelper.switchToNextAvailable(rootPane.getScene(), appMessageLabel);
    }

    @FXML
    private void handleLogout() {
        CurrentUserSession.clear();
        NavigationUtil.goToLogin(rootPane.getScene());
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
        boolean leavingLeaveView = leaveView != null
                && leaveView.isVisible()
                && target != leaveView;
        if (leavingLeaveView && leaveViewController != null) {
            leaveViewController.resetForm();
        }

        boolean leavingProfileView = profileView != null
                && profileView.isVisible()
                && target != profileView;
        if (leavingProfileView && profileViewController != null) {
            profileViewController.cancelEditIfActive();
        }

        boolean leavingSupportTicketsView = supportTicketsView != null
                && supportTicketsView.isVisible()
                && target != supportTicketsView;
        if (leavingSupportTicketsView && supportTicketsViewController != null) {
            supportTicketsViewController.resetForm();
        }

        for (VBox view : List.of(dashboardView, profileView, attendanceView, leaveView,
                payslipView, supportTicketsView, settingsView)) {
            boolean active = view == target;
            view.setVisible(active);
            view.setManaged(active);
        }
        NavigationHelper.setNavSelected(selectedBtn,
                dashboardNavButton, profileNavButton, attendanceNavButton,
                leaveNavButton, payslipNavButton, supportTicketsNavButton, settingsNavButton);
        NavigationHelper.setHeader(pageTitleLabel, pageSubtitleLabel, title, subtitle);
    }

    // loads the current employee's name, role, and email into the sidebar
    private void loadSidebarData() {
        SidebarHelper.load(dashboardEmployeeNameLabel, dashboardEmployeeRoleLabel, null,
                "Employee", "EMPLOYEE", "", employeeService);
    }

    // configures switch role button visibility
    private void configureSwitchRoleButton() {
        boolean show = CurrentUserSession.hasMultipleRoles();
        switchRoleButton.setVisible(show);
        switchRoleButton.setManaged(show);
    }

    // builds and inserts language/theme combos and the overflow button into the topbar host
    private void buildGlobalControls() {
        GlobalControlsHelper.build(globalControlsHostApp, rootPane, this::toggleSidePanel);
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
