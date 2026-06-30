package motorph.ui.admin;

import motorph.application.service.EmployeeService;
import motorph.application.service.LeaveService;
import motorph.application.service.PayrollService;
import motorph.application.service.UserAccessLogService;
import motorph.application.service.UserAccountService;
import motorph.domain.model.Payrun;
import motorph.domain.model.PayrollResult;
import motorph.domain.model.UserAccessLog;
import motorph.ui.factory.EmployeeUIServiceFactory;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Populates the Admin dashboard with system-wide metrics and quick-access tiles. */
public class AdminDashboardController {

    private static final Logger logger = Logger.getLogger(AdminDashboardController.class.getName());

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");
    private static final NumberFormat CURRENCY =
            NumberFormat.getCurrencyInstance(Locale.of("en", "PH"));

    private EmployeeService      employeeService;
    private LeaveService         leaveService;
    private PayrollService       payrollService;
    private UserAccountService   userAccountService;
    private UserAccessLogService userAccessLogService;

    // Parent shell for dashboard navigation
    private AdminMainController mainController;

    // HR area
    @FXML private Label totalActiveEmployeesLabel;
    @FXML private Label totalAllEmployeesLabel;
    @FXML private Label onLeaveTodayLabel;
    @FXML private Label pendingLeaveLabel;

    // Finance / Payroll area
    @FXML private Label latestPayrunPeriodLabel;
    @FXML private Label latestPayrunNetPayLabel;
    @FXML private Label latestPayrunGrossPayLabel;
    @FXML private Label latestPayrunHeadcountLabel;

    // IT / Security area
    @FXML private Label activeAccountsLabel;
    @FXML private Label lockedAccountsLabel;
    @FXML private Label disabledAccountsLabel;
    @FXML private Label successLoginsLabel;
    @FXML private Label failedLoginsLabel;

    // Recent activity table
    @FXML private VBox activityRows;

    @FXML
    private void initialize() {
        employeeService      = EmployeeUIServiceFactory.getInstance().getEmployeeService();
        leaveService         = EmployeeUIServiceFactory.getInstance().getLeaveService();
        payrollService       = EmployeeUIServiceFactory.getInstance().getPayrollService();
        userAccountService   = EmployeeUIServiceFactory.getInstance().getUserAccountService();
        userAccessLogService = EmployeeUIServiceFactory.getInstance().getUserAccessLogService();
    }

    /** Called by AdminMainController whenever the dashboard tab is shown. */
    public void refresh() {
        refreshEmployeeMetrics();
        refreshPayrollSummary();
        refreshAccountMetrics();
        refreshLoginActivity();
        refreshActivityFeed();
    }

    /** Wired by AdminMainController so quick-access tiles can navigate. */
    public void setMainController(AdminMainController mainController) {
        this.mainController = mainController;
    }

    // Quick-access tile handlers
    @FXML private void onTileEmployees()      { if (mainController != null) mainController.navigateToEmployees(); }
    @FXML private void onTilePayroll()        { if (mainController != null) mainController.navigateToPayroll(); }
    @FXML private void onTileReports()        { if (mainController != null) mainController.navigateToReports(); }
    @FXML private void onTileLeaveOvertime()  { if (mainController != null) mainController.navigateToLeaveOvertime(); }
    @FXML private void onTileUserMgmt()       { if (mainController != null) mainController.navigateToUserManagement(); }
    @FXML private void onTileRoleMgmt()       { if (mainController != null) mainController.navigateToRoleManagement(); }

    // Metric refresh
    private void refreshEmployeeMetrics() {
        try {
            int active = employeeService.getAllActiveEmployees().size();
            int all    = employeeService.getAllEmployees().size();
            setText(totalActiveEmployeesLabel, String.valueOf(active));
            setText(totalAllEmployeesLabel,    String.valueOf(all));
        } catch (Exception e) {
            setText(totalActiveEmployeesLabel, "—");
            setText(totalAllEmployeesLabel,    "—");
        }

        try {
            setText(onLeaveTodayLabel, String.valueOf(leaveService.countApprovedLeavesToday()));
        } catch (Exception e) {
            setText(onLeaveTodayLabel, "—");
        }

        try {
            setText(pendingLeaveLabel, String.valueOf(leaveService.countPendingRequests()));
        } catch (Exception e) {
            setText(pendingLeaveLabel, "—");
        }
    }

    private void refreshPayrollSummary() {
        try {
            List<Payrun> payruns = payrollService.getAllPayruns();

            if (payruns.isEmpty()) {
                setText(latestPayrunPeriodLabel,    "No payrun yet");
                setText(latestPayrunNetPayLabel,    "—");
                setText(latestPayrunGrossPayLabel,  "—");
                setText(latestPayrunHeadcountLabel, "—");
                return;
            }

            Payrun latest = payruns.get(0);
            String period = DATE_FMT.format(latest.getStartDate())
                    + " – " + DATE_FMT.format(latest.getEndDate());
            setText(latestPayrunPeriodLabel, period);

            List<PayrollResult> results = payrollService.getPayrollResult(latest.getPayrunId());

            double totalGross = results.stream().mapToDouble(PayrollResult::getGrossPay).sum();
            double totalNet   = results.stream().mapToDouble(PayrollResult::getNetPay).sum();

            setText(latestPayrunGrossPayLabel,  CURRENCY.format(totalGross));
            setText(latestPayrunNetPayLabel,    CURRENCY.format(totalNet));
            setText(latestPayrunHeadcountLabel, results.size() + " employees");

        } catch (Exception e) {
            setText(latestPayrunPeriodLabel,    "—");
            setText(latestPayrunNetPayLabel,    "—");
            setText(latestPayrunGrossPayLabel,  "—");
            setText(latestPayrunHeadcountLabel, "—");
        }
    }

    private void refreshAccountMetrics() {
        try {
            setText(activeAccountsLabel,   String.valueOf(userAccountService.getActiveAccountCount()));
            setText(lockedAccountsLabel,   String.valueOf(userAccountService.getLockedAccountCount()));
            setText(disabledAccountsLabel, String.valueOf(userAccountService.getDisabledAccountCount()));
        } catch (Exception e) {
            setText(activeAccountsLabel,   "—");
            setText(lockedAccountsLabel,   "—");
            setText(disabledAccountsLabel, "—");
        }
    }

    private void refreshLoginActivity() {
        try {
            int success = userAccessLogService
                    .getLogsByAction(UserAccessLogService.ACTION_LOGIN_SUCCESS).size();
            int failed  = userAccessLogService
                    .getLogsByAction(UserAccessLogService.ACTION_LOGIN_FAILED).size();
            setText(successLoginsLabel, String.valueOf(success));
            setText(failedLoginsLabel,  String.valueOf(failed));
        } catch (Exception e) {
            setText(successLoginsLabel, "—");
            setText(failedLoginsLabel,  "—");
        }
    }

    private void refreshActivityFeed() {
        if (activityRows == null) return;
        activityRows.getChildren().clear();

        try {
            List<UserAccessLog> logs = userAccessLogService.getAllLogs();
            logs.stream()
                .limit(10)
                .map(this::buildActivityRow)
                .forEach(activityRows.getChildren()::add);
        } catch (Exception e) {
            // leaves the table empty on service error
            logger.log(Level.WARNING, "Failed to load recent activity", e);
        }
    }

    // Row builder
    private HBox buildActivityRow(UserAccessLog log) {
        HBox row = new HBox();
        row.getStyleClass().addAll("table-data-row", "admin-activity-row");

        String actionText = log.getAction() != null ? log.getAction() : "N/A";
        Label action = activityCell(formatActivityAction(actionText), 200);
        action.getStyleClass().add("admin-activity-status");
        if (UserAccessLogService.ACTION_LOGIN_SUCCESS.equals(actionText)) {
            action.getStyleClass().add("admin-activity-success");
        } else if (UserAccessLogService.ACTION_LOGIN_FAILED.equals(actionText)) {
            action.getStyleClass().add("admin-activity-failed");
        }

        Label details = new Label(log.getDetails() != null ? log.getDetails() : "—");
        details.getStyleClass().addAll("table-cell", "data-line");
        details.setMinWidth(0);
        details.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(details, Priority.ALWAYS);

        String dateText = log.getLoggedAt() != null
                ? DT_FMT.format(log.getLoggedAt()) : "—";
        Label date = new Label(dateText);
        date.getStyleClass().addAll("table-cell", "data-line", "admin-activity-time");
        date.setMinWidth(0);
        date.setPrefWidth(160);

        row.getChildren().addAll(action, details, date);
        return row;
    }

    private Label activityCell(String text, double prefWidth) {
        Label label = new Label(text);
        label.getStyleClass().addAll("table-cell", "data-line");
        label.setMinWidth(0);
        if (prefWidth > 0) label.setPrefWidth(prefWidth);
        return label;
    }

    private String formatActivityAction(String action) {
        if (UserAccessLogService.ACTION_LOGIN_SUCCESS.equals(action)) {
            return "SUCCESSFUL";
        }
        if (UserAccessLogService.ACTION_LOGIN_FAILED.equals(action)) {
            return "FAILED";
        }
        return action;
    }

    // Utility
    private void setText(Label label, String text) {
        if (label != null) label.setText(text);
    }
}
