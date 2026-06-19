package motorph.ui.it;

import motorph.application.service.UserAccessLogService;
import motorph.domain.model.UserAccessLog;
import motorph.ui.factory.ITUIServiceFactory;
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

    private UserAccessLogService userAccessLogService;

    private final ObservableList<UserAccessLog> allLogs      = FXCollections.observableArrayList();
    private final ObservableList<UserAccessLog> filteredLogs = FXCollections.observableArrayList();
    private final ObservableList<UserAccessLog> pagedLogs    = FXCollections.observableArrayList();

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
    @FXML private TableView<UserAccessLog>           logsTable;
    @FXML private TableColumn<UserAccessLog, String> userIdColumn;
    @FXML private TableColumn<UserAccessLog, String> actionColumn;
    @FXML private TableColumn<UserAccessLog, String> detailsColumn;
    @FXML private TableColumn<UserAccessLog, String> timestampColumn;

    @FXML private Button logsPrevButton;
    @FXML private Button logsNextButton;
    @FXML private Label  logsPageLabel;
    @FXML private Label  logsRecordInfoLabel;

    @FXML
    private void initialize() {
        userAccessLogService = ITUIServiceFactory.getInstance().getUserAccessLogService();
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
            List<UserAccessLog> logs = new ArrayList<>(userAccessLogService.getAllLogs());
            java.util.Collections.reverse(logs); // most recent first
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
                UserAccessLogService.ACTION_LOGIN_SUCCESS,
                UserAccessLogService.ACTION_LOGIN_FAILED,
                UserAccessLogService.ACTION_OPEN_VIEW,
                UserAccessLogService.ACTION_PASSWORD_RESET,
                UserAccessLogService.ACTION_ACCOUNT_STATUS_CHANGED,
                UserAccessLogService.ACTION_GENERATED_REPORT,
                UserAccessLogService.ACTION_DATABASE_BACKUP_DOWNLOADED
        );
        actionFilterCombo.setValue("All Actions");
        searchField.textProperty().addListener((obs, old, val) -> applyFilters());
        actionFilterCombo.valueProperty().addListener((obs, old, val) -> applyFilters());
    }

    private void configureTable() {
        logsTable.setItems(pagedLogs);
        logsTable.setPlaceholder(new Label("No access log entries found."));
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
                if (UserAccessLogService.ACTION_LOGIN_FAILED.equals(action)) {
                    setStyle("-fx-text-fill: #ef4444;");
                } else if (UserAccessLogService.ACTION_LOGIN_SUCCESS.equals(action)) {
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

        List<UserAccessLog> result = allLogs.stream()
                .filter(l -> matchesAction(l, action))
                .filter(l -> matchesSearch(l, query))
                .collect(Collectors.toList());

        filteredLogs.setAll(result);
        currentPage = 1;
        renderPage();
    }

    private boolean matchesAction(UserAccessLog l, String filter) {
        if (filter == null || "All Actions".equals(filter)) return true;
        return filter.equals(l.getAction());
    }

    private boolean matchesSearch(UserAccessLog l, String query) {
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
                .filter(l -> UserAccessLogService.ACTION_LOGIN_SUCCESS.equals(l.getAction())).count();
        long failed  = allLogs.stream()
                .filter(l -> UserAccessLogService.ACTION_LOGIN_FAILED.equals(l.getAction())).count();
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
        logsRecordInfoLabel.setText(total == 0
                ? "No entries found."
                : "Showing " + (from + 1) + " to " + to + " of " + total + " entries");
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
