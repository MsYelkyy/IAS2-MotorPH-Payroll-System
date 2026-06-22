package motorph.ui.hr;

import motorph.application.service.EmployeeService;
import motorph.ui.common.GlobalControlsHelper;
import motorph.ui.common.NavigationHelper;
import motorph.ui.common.RoleSwitchHelper;
import motorph.ui.common.SidebarHelper;
import motorph.ui.factory.HrUIServiceFactory;
import motorph.ui.reports.ReportsController;
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
    @FXML private Button employeesNavButton;
    @FXML private Button attendanceNavButton;
    @FXML private Button leaveOvertimeNavButton;
    @FXML private Button reportsNavButton;
    @FXML private Button supportTicketsNavButton;
    @FXML private Button switchRoleButton;

    // Topbar
    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;
    @FXML private HBox  globalControlsHost;

    // Dashboard sub-view
    @FXML private VBox                 dashboardView;
    @FXML private HrDashboardController dashboardViewController;

    // Employee Management sub-view
    @FXML private VBox                employeeView;
    @FXML private HrEmployeeController employeeViewController;

    // Attendance sub-view
    @FXML private VBox                  attendanceView;
    @FXML private HrAttendanceController attendanceViewController;

    // Leave & Overtime sub-view
    @FXML private VBox             leaveView;
    @FXML private HrLeaveController leaveViewController;

    // Reports sub-view
    @FXML private VBox               reportsView;
    @FXML private ReportsController  reportsViewController;

    // Support Tickets sub-view
    @FXML private VBox                    supportTicketsView;
    @FXML private HrSupportTicketController supportTicketsViewController;

    // Footer
    @FXML private Label appMessageLabel;

    // sets up services, controls, and the default dashboard view
    @FXML
    private void initialize() {
        employeeService = HrUIServiceFactory.getInstance().getEmployeeService();

        buildGlobalControls();
        ThemeManager.getInstance().applyCurrentTo(rootPane);
        wireDashboard();
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
        showView(ActiveView.DASHBOARD);
        if (dashboardViewController != null) dashboardViewController.refresh();
    }

    // shows the employee management view
    @FXML
    private void showEmployees() {
        setNavSelected(employeesNavButton);
        setHeader("Employee Management", "View and manage all employees");
        showView(ActiveView.EMPLOYEES);
        if (employeeViewController != null) employeeViewController.refresh();
    }

    // shows the attendance view
    @FXML
    private void showAttendance() {
        setNavSelected(attendanceNavButton);
        setHeader("Attendance", "Monitor employee attendance logs");
        showView(ActiveView.ATTENDANCE);
        if (attendanceViewController != null) attendanceViewController.refresh();
    }

    // shows the leave and overtime view
    @FXML
    private void showLeaveOvertime() {
        setNavSelected(leaveOvertimeNavButton);
        setHeader("Leave & Overtime", "Review and approve leave requests");
        showView(ActiveView.LEAVE);
        if (leaveViewController != null) leaveViewController.refresh();
    }

    // shows the reports view
    @FXML
    private void showReports() {
        setNavSelected(reportsNavButton);
        setHeader("Reports", "Generate and preview organization reports");
        showView(ActiveView.REPORTS);
        if (reportsViewController != null) reportsViewController.refresh();
    }

    // shows the support tickets view
    @FXML
    private void showSupportTickets() {
        setNavSelected(supportTicketsNavButton);
        setHeader("Support Tickets", "Review and respond to employee support requests");
        showView(ActiveView.SUPPORT_TICKETS);
        if (supportTicketsViewController != null) supportTicketsViewController.refresh();
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

    // identifies which sub-view is currently shown
    private enum ActiveView { DASHBOARD, EMPLOYEES, ATTENDANCE, LEAVE, REPORTS, SUPPORT_TICKETS }

    // shows the active view and hides the others
    private void showView(ActiveView active) {
        setPane(dashboardView,      active == ActiveView.DASHBOARD);
        setPane(employeeView,       active == ActiveView.EMPLOYEES);
        setPane(attendanceView,     active == ActiveView.ATTENDANCE);
        setPane(leaveView,          active == ActiveView.LEAVE);
        setPane(reportsView,        active == ActiveView.REPORTS);
        setPane(supportTicketsView, active == ActiveView.SUPPORT_TICKETS);
    }

    // toggles a single sub-view's visibility
    private void setPane(javafx.scene.Node node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    // links the dashboard view back to this shell
    private void wireDashboard() {
        if (dashboardViewController != null) {
            dashboardViewController.setMainController(this);
        }
    }

    // highlights the active navigation button
    private void setNavSelected(Button selected) {
        NavigationHelper.setNavSelected(selected,
                dashboardNavButton, employeesNavButton, attendanceNavButton, leaveOvertimeNavButton,
                reportsNavButton, supportTicketsNavButton);
    }

    // updates the topbar title and subtitle
    private void setHeader(String title, String subtitle) {
        NavigationHelper.setHeader(pageTitleLabel, pageSubtitleLabel, title, subtitle);
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
