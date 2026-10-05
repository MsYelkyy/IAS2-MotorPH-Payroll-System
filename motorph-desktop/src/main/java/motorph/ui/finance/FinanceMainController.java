package motorph.ui.finance;

import motorph.api.ApiClientFactory;
import motorph.api.EmployeeApiClient;
import motorph.ui.common.NavigationHelper;
import motorph.ui.common.RoleSwitchHelper;
import motorph.ui.common.SidebarHelper;
import motorph.ui.reports.ReportsController;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.support.SupportTicketController;
import motorph.ui.util.NavigationUtil;
import motorph.ui.util.UiMessageUtil;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Manages the Finance app shell: sidebar navigation, identity card, and global controls. */
public class FinanceMainController {

    private EmployeeApiClient employeeApiClient;

    // Root
    @FXML private StackPane rootPane;

    // Sidebar identity card
    @FXML private Label employeeNameLabel;
    @FXML private Label employeeRoleLabel;

    // Navigation buttons
    @FXML private Button dashboardNavButton;
    @FXML private Button payrollNavButton;
    @FXML private Button viewPayslipNavButton;
    @FXML private Button reportsNavButton;
    @FXML private Button supportTicketsNavButton;
    @FXML private Button switchRoleButton;

    // Topbar
    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;

    // Dashboard sub-view
    @FXML private VBox                       dashboardView;
    @FXML private FinanceDashboardController  dashboardViewController;

    // Payroll Management sub-view (shared Finance-owned screen)
    @FXML private VBox                        payrollView;
    @FXML private PayrollManagementController payrollViewController;

    // View Payslip sub-view (Finance-owned screen)
    @FXML private StackPane                   viewPayslipView;
    @FXML private FinanceViewPayslipController viewPayslipViewController;

    // Reports sub-view (shared ReportsController)
    @FXML private VBox             reportsView;
    @FXML private ReportsController reportsViewController;

    // Support Tickets sub-view (shared support ticket pattern)
    @FXML private VBox                         supportTicketsView;
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
        setHeader("Payroll Reporting Dashboard");
        showView(dashboardView);
        if (dashboardViewController != null) dashboardViewController.refresh();
    }

    @FXML
    private void showPayroll() {
        setNavSelected(payrollNavButton);
        setHeader("Payroll Management");
        showView(payrollView);
        if (payrollViewController != null) payrollViewController.refresh();
    }

    @FXML
    private void showViewPayslip() {
        setNavSelected(viewPayslipNavButton);
        setHeader("View Payslip");
        showView(viewPayslipView);
        if (viewPayslipViewController != null) viewPayslipViewController.refresh();
    }

    @FXML
    private void showReports() {
        setNavSelected(reportsNavButton);
        setHeader("Reports");
        showView(reportsView);
        if (reportsViewController != null) reportsViewController.refresh();
    }

    @FXML
    private void showSupportTickets() {
        setNavSelected(supportTicketsNavButton);
        setHeader("Support Tickets");
        showView(supportTicketsView);
        if (supportTicketsViewController != null) supportTicketsViewController.refresh();
    }

    // Public navigation entry points (called by FinanceDashboardController buttons)
    /** Called by the dashboard's "Open Payroll Management" button. */
    public void navigateToPayroll() {
        showPayroll();
    }

    /** Called by the dashboard's "Open Reports" button. */
    public void navigateToReports() {
        showReports();
    }

    // Auth handlers
    @FXML
    private void handleSwitchRole() {
        try {
            RoleSwitchHelper.switchToNext(rootPane.getScene(), pageTitleLabel);
        } catch (Exception e) {
            UiMessageUtil.setError(pageTitleLabel, "Could not switch role: " + UiMessageUtil.toUserMessage(e));
        }
    }

    @FXML
    private void handleLogout() {
        CurrentUserSession.clear();
        NavigationUtil.goToLogin(rootPane.getScene());
    }

    // Helpers
    private void setNavSelected(Button selected) {
        NavigationHelper.setNavSelected(selected,
                dashboardNavButton, payrollNavButton, viewPayslipNavButton, reportsNavButton, supportTicketsNavButton);
    }

    private void setHeader(String title) {
        NavigationHelper.setHeader(pageTitleLabel, pageSubtitleLabel, title, "FINANCE WORKSPACE");
    }

    private void showView(Node view) {
        Node[] views = { dashboardView, payrollView, viewPayslipView, reportsView, supportTicketsView };
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
                "Finance User", "FINANCE", "", employeeApiClient);
    }

    private void configureSwitchRoleButton() {
        boolean show = CurrentUserSession.hasMultipleRoles();
        if (show) switchRoleButton.setText("Switch to " + RoleSwitchHelper.nextRoleLabel());
        switchRoleButton.setVisible(show);
        switchRoleButton.setManaged(show);
    }

}
