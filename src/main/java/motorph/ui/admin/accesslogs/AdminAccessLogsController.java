package motorph.ui.admin.accesslogs;

import motorph.application.service.UserAccessLogService;
import motorph.domain.model.UserAccessLog;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.util.UiMessageUtil;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Manages the User Access Logs sub-view: searchable, filterable audit log table. */
public class AdminAccessLogsController {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss");

    private UserAccessLogService userAccessLogService;

    // ── State ─────────────────────────────────────────────────────────────────
    private List<UserAccessLog> allLogs = new ArrayList<>();

    // ── Toolbar ───────────────────────────────────────────────────────────────
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> actionFilterCombo;
    @FXML private Label            moduleMessageLabel;
    @FXML private Label            logCountLabel;

    // ── Table rows ────────────────────────────────────────────────────────────
    @FXML private VBox logRows;

    @FXML
    private void initialize() {
        userAccessLogService = EmployeeUIServiceFactory.getInstance().getUserAccessLogService();

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
        actionFilterCombo.setOnAction(e -> applyFilter());
        searchField.textProperty().addListener((obs, old, text) -> applyFilter());

        loadLogs();
    }

    /** Called by AdminMainController each time access logs nav is activated. */
    public void refresh() {
        loadLogs();
        UiMessageUtil.clearMessage(moduleMessageLabel);
    }

    @FXML
    private void handleRefresh() {
        loadLogs();
        UiMessageUtil.setSuccess(moduleMessageLabel, "Logs refreshed.");
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadLogs() {
        try {
            allLogs = userAccessLogService.getAllLogs();
            applyFilter();
        } catch (Exception e) {
            UiMessageUtil.setError(moduleMessageLabel, UiMessageUtil.toUserMessage(e));
        }
    }

    private void applyFilter() {
        String keyword      = searchField.getText();
        String actionFilter = actionFilterCombo.getValue();

        List<UserAccessLog> filtered = allLogs.stream()
                .filter(log -> matchesAction(log, actionFilter))
                .filter(log -> matchesSearch(log, keyword))
                .collect(Collectors.toList());

        renderRows(filtered);
        if (logCountLabel != null) {
            logCountLabel.setText(filtered.size() + " of " + allLogs.size() + " entries");
        }
    }

    private boolean matchesAction(UserAccessLog log, String filter) {
        if (filter == null || filter.equals("All Actions")) return true;
        return filter.equals(log.getAction());
    }

    private boolean matchesSearch(UserAccessLog log, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        String lower = keyword.toLowerCase();
        return (log.getAction() != null && log.getAction().toLowerCase().contains(lower))
                || (log.getDetails() != null && log.getDetails().toLowerCase().contains(lower))
                || (log.getUserAccountId() != null && String.valueOf(log.getUserAccountId()).contains(lower));
    }

    // ── Row rendering ─────────────────────────────────────────────────────────

    private void renderRows(List<UserAccessLog> list) {
        logRows.getChildren().clear();
        // Show most recent first (list from DB is ascending by default)
        List<UserAccessLog> reversed = new ArrayList<>(list);
        java.util.Collections.reverse(reversed);
        reversed.forEach(log -> logRows.getChildren().add(buildRow(log)));
    }

    private HBox buildRow(UserAccessLog log) {
        HBox row = new HBox(0);
        row.getStyleClass().add("table-data-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);

        String accountId = log.getUserAccountId() != null ? String.valueOf(log.getUserAccountId()) : "—";
        Label acctLbl    = col(accountId, 60);

        Label actionLbl = new Label(log.getAction());
        actionLbl.getStyleClass().addAll("data-line", "table-cell");
        actionLbl.setMinWidth(190);
        actionLbl.setPrefWidth(190);

        // Color-code login failures
        if (UserAccessLogService.ACTION_LOGIN_FAILED.equals(log.getAction())) {
            actionLbl.setStyle("-fx-text-fill: #ef4444;");
        } else if (UserAccessLogService.ACTION_LOGIN_SUCCESS.equals(log.getAction())) {
            actionLbl.setStyle("-fx-text-fill: #22c55e;");
        }

        Label detailsLbl = new Label(log.getDetails() != null ? log.getDetails() : "—");
        detailsLbl.getStyleClass().addAll("data-line", "table-cell");
        detailsLbl.setWrapText(false);
        HBox.setHgrow(detailsLbl, Priority.ALWAYS);

        String timestamp = log.getLoggedAt() != null ? DT_FMT.format(log.getLoggedAt()) : "—";
        Label timeLbl = col(timestamp, 175);

        row.getChildren().addAll(acctLbl, actionLbl, detailsLbl, timeLbl);
        return row;
    }

    private Label col(String text, double width) {
        Label lbl = new Label(text);
        lbl.getStyleClass().addAll("data-line", "table-cell");
        lbl.setMinWidth(width);
        lbl.setPrefWidth(width);
        return lbl;
    }
}
