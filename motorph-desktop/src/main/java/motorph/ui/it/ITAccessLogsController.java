package motorph.ui.it;

import motorph.api.AccessLogApiClient;
import motorph.api.ApiClientFactory;
import motorph.api.dto.accesslog.AccessLogAction;
import motorph.api.dto.accesslog.UserAccessLogDTO;
import motorph.ui.common.EmptyState;
import motorph.ui.util.UiMessageUtil;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Handles the IT User Access Logs screen.
 * Includes login metrics, filters, and the access log table.
 */
public class ITAccessLogsController {

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss");

    private static final int PAGE_SIZE = 7;

    private AccessLogApiClient accessLogApiClient;

    private final ObservableList<UserAccessLogDTO> allLogs      = FXCollections.observableArrayList();
    private final ObservableList<UserAccessLogDTO> filteredLogs = FXCollections.observableArrayList();
    private final ObservableList<UserAccessLogDTO> pagedLogs    = FXCollections.observableArrayList();

    private int currentPage = 1;

    // Metric cards
    @FXML private Label totalLogsLabel;
    @FXML private Label loginSuccessLabel;
    @FXML private Label loginFailedLabel;
    @FXML private Label otherActionsLabel;

    // Filter toolbar
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> actionFilterCombo;
    @FXML private Label            messageLabel;

    // Table
    @FXML private TableView<UserAccessLogDTO>           logsTable;
    @FXML private TableColumn<UserAccessLogDTO, String> userIdColumn;
    @FXML private TableColumn<UserAccessLogDTO, String> actionColumn;
    @FXML private TableColumn<UserAccessLogDTO, String> detailsColumn;
    @FXML private TableColumn<UserAccessLogDTO, String> timestampColumn;

    @FXML private Button logsPrevButton;
    @FXML private Button logsNextButton;
    @FXML private Label  logsPageLabel;
    @FXML private Label  logsRecordInfoLabel;

    @FXML
    private void initialize() {
        accessLogApiClient = ApiClientFactory.getInstance().getAccessLogApiClient();
        configureFilters();
        configureTable();
        refresh();
    }

    @FXML
    private void handleRefresh() {
        refresh();
    }

    /** Called by ITMainController each time the User Access Logs nav item is activated. */
    public void refresh() {
        try {
            // the endpoint already returns newest-first
            List<UserAccessLogDTO> logs = new ArrayList<>(accessLogApiClient.getAllLogs());
            allLogs.setAll(logs);
            updateMetrics();
            applyFilters();
            UiMessageUtil.setSuccess(messageLabel, "Access logs refreshed.");
        } catch (Exception e) {
            allLogs.clear();
            filteredLogs.clear();
            updateMetrics();
            currentPage = 1;
            renderPage();
            UiMessageUtil.setError(messageLabel, UiMessageUtil.toUserMessage(e));
        }
    }

    // Setup
    private void configureFilters() {
        actionFilterCombo.getItems().setAll(
                "All Actions",
                AccessLogAction.LOGIN_SUCCESS,
                AccessLogAction.LOGIN_FAILED,
                AccessLogAction.LOGOUT,
                AccessLogAction.PASSWORD_RESET,
                AccessLogAction.PASSWORD_RESET_COMPLETED,
                AccessLogAction.TOTP_ENROLLMENT_STARTED,
                AccessLogAction.TOTP_ENROLLMENT_COMPLETED,
                AccessLogAction.TOTP_VERIFICATION_SUCCEEDED,
                AccessLogAction.TOTP_VERIFICATION_FAILED,
                AccessLogAction.TOTP_RESET_BY_ADMIN,
                AccessLogAction.ACCOUNT_STATUS_CHANGED,
                AccessLogAction.DATABASE_BACKUP_CREATED,
                AccessLogAction.DATABASE_RESTORE,
                AccessLogAction.DATABASE_BACKUP_DOWNLOADED,
                AccessLogAction.DATABASE_BACKUP_DELETED,
                AccessLogAction.PASSWORD_CHANGED,
                AccessLogAction.PASSWORD_RESET_CANCELLED,
                AccessLogAction.USER_ROLES_UPDATED,
                AccessLogAction.ROLE_PERMISSIONS_UPDATED,
                AccessLogAction.EMPLOYEE_CREATED,
                AccessLogAction.EMPLOYEE_UPDATED,
                AccessLogAction.EMPLOYEE_DEACTIVATED,
                AccessLogAction.SALARY_RECORD_ADDED,
                AccessLogAction.SALARY_RECORD_UPDATED,
                AccessLogAction.BENEFIT_ENDED,
                AccessLogAction.LEAVE_APPROVED,
                AccessLogAction.LEAVE_REJECTED,
                AccessLogAction.OVERTIME_APPROVED,
                AccessLogAction.OVERTIME_REJECTED,
                AccessLogAction.PAYRUN_CREATED,
                AccessLogAction.PAYROLL_GENERATED,
                AccessLogAction.PAYSLIP_EXPORTED,
                AccessLogAction.REPORT_EXPORTED
        );
        actionFilterCombo.setValue("All Actions");
        searchField.textProperty().addListener((obs, old, val) -> applyFilters());
        actionFilterCombo.valueProperty().addListener((obs, old, val) -> applyFilters());
    }

    private void configureTable() {
        logsTable.setItems(pagedLogs);
        logsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        userIdColumn.setCellValueFactory(data -> {
            Integer id = data.getValue().getUserAccountId();
            return new SimpleStringProperty(id != null ? String.valueOf(id) : "—");
        });

        actionColumn.setCellValueFactory(data -> {
            String action = data.getValue().getAction();
            return new SimpleStringProperty(action != null ? action : "—");
        });
        actionColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String action, boolean empty) {
                super.updateItem(action, empty);
                if (empty || action == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(action);
                if (AccessLogAction.LOGIN_FAILED.equals(action)) {
                    setStyle("-fx-text-fill: #ef4444;");
                } else if (AccessLogAction.LOGIN_SUCCESS.equals(action)) {
                    setStyle("-fx-text-fill: #22c55e;");
                } else {
                    setStyle("");
                }
            }
        });

        detailsColumn.setCellValueFactory(data -> {
            String details = data.getValue().getDetails();
            return new SimpleStringProperty(details != null ? details : "—");
        });

        timestampColumn.setCellValueFactory(data -> {
            var ts = data.getValue().getLoggedAt();
            return new SimpleStringProperty(ts != null ? DT_FMT.format(ts) : "—");
        });
    }

    // Filtering
    private void applyFilters() {
        String query  = norm(searchField.getText());
        String action = actionFilterCombo.getValue();

        List<UserAccessLogDTO> result = allLogs.stream()
                .filter(l -> matchesAction(l, action))
                .filter(l -> matchesSearch(l, query))
                .collect(Collectors.toList());

        filteredLogs.setAll(result);
        currentPage = 1;
        renderPage();
    }

    private boolean matchesAction(UserAccessLogDTO l, String filter) {
        if (filter == null || "All Actions".equals(filter)) return true;
        return filter.equals(l.getAction());
    }

    private boolean matchesSearch(UserAccessLogDTO l, String query) {
        if (query.isBlank()) return true;
        String text = String.join(" ",
                l.getUserAccountId() != null ? String.valueOf(l.getUserAccountId()) : "",
                l.getAction()  != null ? l.getAction()  : "",
                l.getDetails() != null ? l.getDetails() : "");
        return norm(text).contains(query);
    }

    // Metrics
    private void updateMetrics() {
        long success = allLogs.stream()
                .filter(l -> AccessLogAction.LOGIN_SUCCESS.equals(l.getAction())).count();
        long failed  = allLogs.stream()
                .filter(l -> AccessLogAction.LOGIN_FAILED.equals(l.getAction())).count();
        long other   = allLogs.size() - success - failed;

        totalLogsLabel.setText(String.valueOf(allLogs.size()));
        loginSuccessLabel.setText(String.valueOf(success));
        loginFailedLabel.setText(String.valueOf(failed));
        otherActionsLabel.setText(String.valueOf(other));
    }

    private void renderPage() {
        int total      = filteredLogs.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        currentPage    = Math.max(1, Math.min(currentPage, totalPages));

        int from = (currentPage - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, total);

        pagedLogs.setAll(filteredLogs.subList(from, to));
        logsPageLabel.setText("Page " + currentPage + " of " + totalPages);
        EmptyState.apply(logsTable, !allLogs.isEmpty(), "log entries", "User activity will appear here.");
        logsRecordInfoLabel.setText(EmptyState.recordInfo(from, to, total, allLogs.size(), "entries"));
        logsPrevButton.setDisable(currentPage <= 1);
        logsNextButton.setDisable(currentPage >= totalPages);
    }

    @FXML
    private void handleLogsPrevPage() {
        if (currentPage > 1) {
            currentPage--;
            renderPage();
        }
    }

    @FXML
    private void handleLogsNextPage() {
        int totalPages = Math.max(1, (int) Math.ceil((double) filteredLogs.size() / PAGE_SIZE));
        if (currentPage < totalPages) {
            currentPage++;
            renderPage();
        }
    }

    private String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}
