package motorph.ui.admin;

import motorph.api.AccessLogApiClient;
import motorph.api.EmployeeApiClient;
import motorph.api.ApiClientFactory;
import motorph.api.LeaveApiClient;
import motorph.api.UserAccountApiClient;
import motorph.api.dto.accesslog.AccessLogAction;
import motorph.api.dto.accesslog.AccessLogCountsDTO;
import motorph.api.dto.accesslog.UserAccessLogDTO;
import motorph.api.dto.employee.EmployeeSummaryDTO;
import motorph.api.dto.leave.LeaveSummaryDTO;
import motorph.api.dto.user.AccountCountsDTO;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Populates the Admin dashboard with people and account metrics and the recent activity feed. */
public class AdminDashboardController {

    private static final Logger logger = Logger.getLogger(AdminDashboardController.class.getName());

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");

    private EmployeeApiClient    employeeApiClient;
    private LeaveApiClient       leaveApiClient;
    private UserAccountApiClient userAccountApiClient;
    private AccessLogApiClient   accessLogApiClient;

    // HR area
    @FXML private Label totalActiveEmployeesLabel;
    @FXML private Label totalAllEmployeesLabel;
    @FXML private Label onLeaveTodayLabel;
    @FXML private Label pendingLeaveLabel;

    // IT / Security area
    @FXML private Label activeAccountsLabel;
    @FXML private Label lockedAccountsLabel;
    @FXML private Label disabledAccountsLabel;
    @FXML private Label successLoginsLabel;
    @FXML private Label failedLoginsLabel;

    // Recent activity table
    private static final double ACTION_COLUMN_WIDTH = 196;
    private static final double TIME_COLUMN_WIDTH = 180;

    @FXML private VBox activityRows;

    @FXML
    private void initialize() {
        employeeApiClient    = ApiClientFactory.getInstance().getEmployeeApiClient();
        leaveApiClient       = ApiClientFactory.getInstance().getLeaveApiClient();
        userAccountApiClient = ApiClientFactory.getInstance().getUserAccountApiClient();
        accessLogApiClient   = ApiClientFactory.getInstance().getAccessLogApiClient();
    }

    /** Called by AdminMainController whenever the dashboard tab is shown. */
    public void refresh() {
        refreshEmployeeMetrics();
        refreshAccountMetrics();
        refreshLoginActivity();
        refreshActivityFeed();
    }

    // Metric refresh
    private void refreshEmployeeMetrics() {
        try {
            // one call: the endpoint returns everyone, active flag included
            List<EmployeeSummaryDTO> employees = employeeApiClient.getAllEmployeeSummaries();
            long active = employees.stream().filter(EmployeeSummaryDTO::isActive).count();
            int all = employees.size();
            setText(totalActiveEmployeesLabel, String.valueOf(active));
            setText(totalAllEmployeesLabel,    String.valueOf(all));
        } catch (Exception e) {
            setText(totalActiveEmployeesLabel, "—");
            setText(totalAllEmployeesLabel,    "—");
        }

        try {
            LeaveSummaryDTO summary = leaveApiClient.getSummary();
            setText(onLeaveTodayLabel, String.valueOf(summary.approvedTodayCount()));
            setText(pendingLeaveLabel, String.valueOf(summary.pendingCount()));
        } catch (Exception e) {
            setText(onLeaveTodayLabel, "—");
            setText(pendingLeaveLabel, "—");
        }
    }

    private void refreshAccountMetrics() {
        try {
            AccountCountsDTO counts = userAccountApiClient.getCounts();
            setText(activeAccountsLabel,   String.valueOf(counts.activeCount()));
            setText(lockedAccountsLabel,   String.valueOf(counts.lockedCount()));
            setText(disabledAccountsLabel, String.valueOf(counts.disabledCount()));
        } catch (Exception e) {
            setText(activeAccountsLabel,   "—");
            setText(lockedAccountsLabel,   "—");
            setText(disabledAccountsLabel, "—");
        }
    }

    private void refreshLoginActivity() {
        try {
            AccessLogCountsDTO counts = accessLogApiClient.getCounts();
            setText(successLoginsLabel, String.valueOf(counts.loginSuccessCount()));
            setText(failedLoginsLabel,  String.valueOf(counts.loginFailedCount()));
        } catch (Exception e) {
            setText(successLoginsLabel, "—");
            setText(failedLoginsLabel,  "—");
        }
    }

    private void refreshActivityFeed() {
        if (activityRows == null) return;
        activityRows.getChildren().clear();

        try {
            List<UserAccessLogDTO> logs = accessLogApiClient.getRecent(10);
            logs.stream()
                .map(this::buildActivityRow)
                .forEach(activityRows.getChildren()::add);
        } catch (Exception e) {
            // leaves the table empty on service error
            logger.log(Level.WARNING, "Failed to load recent activity", e);
        }
    }

    private HBox buildActivityRow(UserAccessLogDTO log) {
        HBox row = new HBox();
        row.getStyleClass().add("admin-activity-row");

        String actionText = log.getAction() != null ? log.getAction() : "N/A";
        Label action = fixedCell(formatActivityAction(actionText), ACTION_COLUMN_WIDTH, "admin-activity-action");
        if (AccessLogAction.LOGIN_SUCCESS.equals(actionText)) {
            action.getStyleClass().add("admin-activity-success");
        } else if (AccessLogAction.LOGIN_FAILED.equals(actionText)) {
            action.getStyleClass().add("admin-activity-failed");
        }

        Label details = new Label(log.getDetails() != null ? log.getDetails() : "—");
        details.getStyleClass().add("admin-activity-details");
        details.setMinWidth(0);
        details.setMaxWidth(Double.MAX_VALUE);
        details.setTooltip(new Tooltip(details.getText()));
        HBox.setHgrow(details, Priority.ALWAYS);

        String dateText = log.getLoggedAt() != null ? DT_FMT.format(log.getLoggedAt()) : "—";
        Label date = fixedCell(dateText, TIME_COLUMN_WIDTH, "admin-activity-time");

        row.getChildren().addAll(action, details, date);
        return row;
    }

    // Fixed-width cell; widths must match the header labels in Dashboard.fxml
    private Label fixedCell(String text, double width, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setMinWidth(width);
        label.setPrefWidth(width);
        label.setMaxWidth(width);
        label.setTooltip(new Tooltip(text));
        return label;
    }

    // Login outcomes read as SUCCESSFUL/FAILED; other actions drop the underscores
    private String formatActivityAction(String action) {
        if (AccessLogAction.LOGIN_SUCCESS.equals(action)) {
            return "SUCCESSFUL";
        }
        if (AccessLogAction.LOGIN_FAILED.equals(action)) {
            return "FAILED";
        }
        return action.replace('_', ' ');
    }

    // Utility
    private void setText(Label label, String text) {
        if (label != null) label.setText(text);
    }
}
