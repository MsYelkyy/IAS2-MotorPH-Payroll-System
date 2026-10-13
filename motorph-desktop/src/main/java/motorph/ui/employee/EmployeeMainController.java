package motorph.ui.employee;

import motorph.api.ApiClientFactory;
import motorph.api.EmployeeApiClient;
import motorph.ui.common.NavigationHelper;
import motorph.ui.common.RoleSwitchHelper;
import motorph.ui.common.SidebarHelper;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.util.NavigationUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

/** Manages the Employee app shell. */
public class EmployeeMainController {

    private EmployeeApiClient employeeApiClient;

    @FXML
    private StackPane rootPane;

    // Sidebar
    @FXML
    private Label dashboardEmployeeNameLabel;

    @FXML
    private Label dashboardEmployeeRoleLabel;

    // Navigation buttons
    @FXML
    private Button dashboardNavButton;

    @FXML
    private Button profileNavButton;

    @FXML
    private Button attendanceNavButton;

    @FXML
    private Button leaveNavButton;

    @FXML
    private Button overtimeNavButton;

    @FXML
    private Button payslipNavButton;

    @FXML
    private Button supportTicketsNavButton;

    @FXML
    private Button settingsNavButton;

    @FXML
    private Button switchRoleButton;

    // Topbar
    @FXML
    private Label pageTitleLabel;

    @FXML
    private Label pageSubtitleLabel;

    // Content views (roots from fx:include)
    @FXML
    private VBox dashboardView;

    @FXML
    private StackPane profileView;

    @FXML
    private VBox attendanceView;

    @FXML
    private StackPane leaveView;

    @FXML
    private StackPane overtimeView;

    @FXML
    private VBox payslipView;

    @FXML
    private StackPane supportTicketsView;

    @FXML
    private VBox settingsView;

    // Sub-controllers (injected by fx:include)
    @FXML
    private EmployeeDashboardController dashboardViewController;

    @FXML
    private EmployeeProfileController profileViewController;

    @FXML
    private EmployeeAttendanceController attendanceViewController;

    @FXML
    private EmployeeLeaveController leaveViewController;

    @FXML
    private EmployeeOvertimeController overtimeViewController;

    @FXML
    private EmployeePayslipController payslipViewController;

    @FXML
    private EmployeeSupportTicketController supportTicketsViewController;

    @FXML
    private EmployeeSettingsController settingsViewController;

    @FXML
    private void initialize() {
        employeeApiClient = ApiClientFactory.getInstance().getEmployeeApiClient();

        wireSubControllers();

        loadSidebarData();
        configureSwitchRoleButton();
        showDashboard();
    }

    @FXML
    private void showDashboard() {
        showView(dashboardView, dashboardNavButton, "Dashboard");
        if (dashboardViewController != null) dashboardViewController.refresh();
    }

    @FXML
    private void showProfile() {
        showView(profileView, profileNavButton, "My Profile");
        if (profileViewController != null) profileViewController.refresh();
    }

    @FXML
    private void showAttendance() {
        showView(attendanceView, attendanceNavButton, "Attendance");
        if (attendanceViewController != null) attendanceViewController.refresh();
    }

    @FXML
    private void showLeave() {
        showView(leaveView, leaveNavButton, "Leave Request");

        if (leaveViewController != null) {
            leaveViewController.resetForm();
            leaveViewController.refresh();
        }
    }

    @FXML
    private void showOvertime() {
        showView(overtimeView, overtimeNavButton, "Overtime Request");

        if (overtimeViewController != null) {
            overtimeViewController.resetForm();
            overtimeViewController.refresh();
        }
    }

    @FXML
    private void showPayslip() {
        showView(payslipView, payslipNavButton, "Payslip / Payroll");
        if (payslipViewController != null) payslipViewController.refresh();
    }

    @FXML
    private void showSupportTickets() {
        showView(supportTicketsView, supportTicketsNavButton, "Support Tickets");
        if (supportTicketsViewController != null) supportTicketsViewController.refresh();
    }

    @FXML
    private void showSettings() {
        showView(settingsView, settingsNavButton, "Account Settings");

        if (settingsViewController != null) {
            settingsViewController.resetForm();
            settingsViewController.refresh();
        }
    }

    @FXML
    private void handleSwitchRole() {
        RoleSwitchHelper.switchToNext(rootPane.getScene(), pageTitleLabel);
    }

    @FXML
    private void handleLogout() {
        CurrentUserSession.clear();
        NavigationUtil.goToLogin(rootPane.getScene());
    }

    // makes only the target view visible and marks the matching nav button as selected
    private void showView(Region target, Button selectedBtn, String title) {
        boolean leavingLeaveView = leaveView != null
                && leaveView.isVisible()
                && target != leaveView;
        if (leavingLeaveView && leaveViewController != null) {
            leaveViewController.resetForm();
        }

        boolean leavingOvertimeView = overtimeView != null
                && overtimeView.isVisible()
                && target != overtimeView;
        if (leavingOvertimeView && overtimeViewController != null) {
            overtimeViewController.resetForm();
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

        for (Region view : List.of(dashboardView, profileView, attendanceView, leaveView,
                overtimeView, payslipView, supportTicketsView, settingsView)) {
            boolean active = view == target;
            view.setVisible(active);
            view.setManaged(active);
        }
        NavigationHelper.setNavSelected(selectedBtn,
                dashboardNavButton, profileNavButton, attendanceNavButton,
                leaveNavButton, overtimeNavButton, payslipNavButton, supportTicketsNavButton, settingsNavButton);
        NavigationHelper.setHeader(pageTitleLabel, pageSubtitleLabel, title, "EMPLOYEE WORKSPACE");
    }

    // loads the current employee's name, role, and email into the sidebar
    private void loadSidebarData() {
        SidebarHelper.load(dashboardEmployeeNameLabel, dashboardEmployeeRoleLabel, null,
                "Employee", "EMPLOYEE", "", employeeApiClient);
    }

    // configures switch role button visibility
    private void configureSwitchRoleButton() {
        boolean show = CurrentUserSession.hasMultipleRoles();
        if (show) switchRoleButton.setText("Switch to " + RoleSwitchHelper.nextRoleLabel());
        switchRoleButton.setVisible(show);
        switchRoleButton.setManaged(show);
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

}
