package motorph.ui.it;

import motorph.api.ApiClientFactory;
import motorph.api.PasswordResetApiClient;
import motorph.api.dto.user.PasswordResetCountsDTO;
import motorph.api.dto.user.PasswordResetRequestRowDTO;
import motorph.domain.value.PasswordResetStatus;
import motorph.ui.common.EmptyState;
import motorph.ui.util.UiMessageUtil;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Handles the IT Password Reset Requests screen.
 * Includes request review, approval, and cancellation actions.
 */
public class PasswordResetRequestController {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");

    private static final int PAGE_SIZE = 7;

    private static final String STATUS_ALL = "All";
    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_COMPLETED = "Completed";
    private static final String STATUS_CANCELLED = "Cancelled";

    private PasswordResetApiClient passwordResetApiClient;

    private final ObservableList<PasswordResetRequestRowDTO> allRows      = FXCollections.observableArrayList();
    private final ObservableList<PasswordResetRequestRowDTO> filteredRows = FXCollections.observableArrayList();
    private final ObservableList<PasswordResetRequestRowDTO> pagedRows    = FXCollections.observableArrayList();

    private int currentPage = 1;

    @FXML private Label pendingRequestsLabel;
    @FXML private Label completedRequestsLabel;
    @FXML private Label cancelledRequestsLabel;
    @FXML private Label totalRequestsLabel;
    @FXML private Label messageLabel;

    @FXML private Button resetsPrevButton;
    @FXML private Button resetsNextButton;
    @FXML private Label  resetsPageLabel;
    @FXML private Label  resetsRecordInfoLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterCombo;

    @FXML private TableView<PasswordResetRequestRowDTO> requestsTable;
    @FXML private TableColumn<PasswordResetRequestRowDTO, String> requestIdColumn;
    @FXML private TableColumn<PasswordResetRequestRowDTO, String> employeeColumn;
    @FXML private TableColumn<PasswordResetRequestRowDTO, String> companyEmailColumn;
    @FXML private TableColumn<PasswordResetRequestRowDTO, PasswordResetStatus> statusColumn;
    @FXML private TableColumn<PasswordResetRequestRowDTO, String> requestedAtColumn;
    @FXML private TableColumn<PasswordResetRequestRowDTO, String> resolvedByColumn;
    @FXML private TableColumn<PasswordResetRequestRowDTO, String> completedAtColumn;
    @FXML private TableColumn<PasswordResetRequestRowDTO, PasswordResetRequestRowDTO> actionsColumn;

    @FXML
    private void initialize() {
        passwordResetApiClient = ApiClientFactory.getInstance().getPasswordResetApiClient();

        configureFilters();
        configureTable();
        refresh();
    }

    @FXML
    private void handleRefresh() {
        refresh();
    }

    public void refresh() {
        try {
            allRows.setAll(passwordResetApiClient.getRequests());
            updateSummaryCards();
            applyFilters();
            UiMessageUtil.setSuccess(messageLabel, "Password reset requests refreshed.");
        } catch (Exception e) {
            allRows.clear();
            filteredRows.clear();
            clearSummaryCards();
            currentPage = 1;
            renderPage();
            UiMessageUtil.setError(messageLabel, UiMessageUtil.toUserMessage(e));
        }
    }

    private void configureFilters() {
        statusFilterCombo.getItems().setAll(STATUS_ALL, STATUS_PENDING, STATUS_COMPLETED, STATUS_CANCELLED);
        statusFilterCombo.setValue(STATUS_ALL);

        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        statusFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
    }

    private void configureTable() {
        requestsTable.setItems(pagedRows);
        requestsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        requestIdColumn.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().getResetRequestId())));
        employeeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(nullSafe(data.getValue().getEmployeeName())));
        companyEmailColumn.setCellValueFactory(data ->
                new SimpleStringProperty(nullSafe(data.getValue().getCompanyEmail())));
        statusColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getStatus()));
        requestedAtColumn.setCellValueFactory(data ->
                new SimpleStringProperty(formatDate(data.getValue().getRequestedAt())));
        resolvedByColumn.setCellValueFactory(data ->
                new SimpleStringProperty(nullSafe(data.getValue().getResolvedBy())));
        completedAtColumn.setCellValueFactory(data ->
                new SimpleStringProperty(formatDate(data.getValue().getCompletedAt())));
        actionsColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue()));

        statusColumn.setCellFactory(column -> new StatusCell());
        actionsColumn.setCellFactory(column -> new ActionCell());
    }

    private void applyFilters() {
        String query = normal(searchField.getText());
        PasswordResetStatus status = selectedStatus();

        filteredRows.setAll(allRows.stream()
                .filter(row -> status == null || row.getStatus() == status)
                .filter(row -> matchesQuery(row, query))
                .collect(Collectors.toList()));

        currentPage = 1;
        renderPage();
    }

    private boolean matchesQuery(PasswordResetRequestRowDTO row, String query) {
        if (query.isBlank()) {
            return true;
        }

        String searchable = String.join(" ",
                String.valueOf(row.getResetRequestId()),
                nullSafe(row.getEmployeeName()),
                nullSafe(row.getCompanyEmail()));

        return normal(searchable).contains(query);
    }

    private PasswordResetStatus selectedStatus() {
        String selected = statusFilterCombo.getValue();
        if (selected == null || STATUS_ALL.equals(selected)) {
            return null;
        }
        return switch (selected) {
            case STATUS_PENDING -> PasswordResetStatus.PENDING;
            case STATUS_COMPLETED -> PasswordResetStatus.COMPLETED;
            case STATUS_CANCELLED -> PasswordResetStatus.CANCELLED;
            default -> null;
        };
    }

    private void updateSummaryCards() {
        PasswordResetCountsDTO counts = passwordResetApiClient.getCounts();

        pendingRequestsLabel.setText(String.valueOf(counts.pendingCount()));
        completedRequestsLabel.setText(String.valueOf(counts.completedCount()));
        cancelledRequestsLabel.setText(String.valueOf(counts.cancelledCount()));
        totalRequestsLabel.setText(String.valueOf(counts.totalCount()));
    }

    private void clearSummaryCards() {
        pendingRequestsLabel.setText("0");
        completedRequestsLabel.setText("0");
        cancelledRequestsLabel.setText("0");
        totalRequestsLabel.setText("0");
    }

    private void renderPage() {
        int total      = filteredRows.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        currentPage    = Math.max(1, Math.min(currentPage, totalPages));

        int from = (currentPage - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, total);

        pagedRows.setAll(filteredRows.subList(from, to));
        resetsPageLabel.setText("Page " + currentPage + " of " + totalPages);
        EmptyState.apply(requestsTable, !allRows.isEmpty(), "requests", "Password reset requests will appear here.");
        resetsRecordInfoLabel.setText(EmptyState.recordInfo(from, to, total, allRows.size(), "requests"));
        resetsPrevButton.setDisable(currentPage <= 1);
        resetsNextButton.setDisable(currentPage >= totalPages);
    }

    @FXML
    private void handleResetsPrevPage() {
        if (currentPage > 1) {
            currentPage--;
            renderPage();
        }
    }

    @FXML
    private void handleResetsNextPage() {
        int totalPages = Math.max(1, (int) Math.ceil((double) filteredRows.size() / PAGE_SIZE));
        if (currentPage < totalPages) {
            currentPage++;
            renderPage();
        }
    }

    private void processRequest(PasswordResetRequestRowDTO row) {
        performRequestAction(row,
                "Process Password Reset",
                "Process password reset request #" + row.getResetRequestId() + " for " + row.getEmployeeName() + "?",
                () -> passwordResetApiClient.processRequest(row.getResetRequestId()),
                "Password reset processed for " + row.getEmployeeName() + ".");
    }

    private void cancelRequest(PasswordResetRequestRowDTO row) {
        performRequestAction(row,
                "Cancel Password Reset",
                "Cancel password reset request #" + row.getResetRequestId() + " for " + row.getEmployeeName() + "?",
                () -> passwordResetApiClient.cancelRequest(row.getResetRequestId()),
                "Password reset request cancelled for " + row.getEmployeeName() + ".");
    }

    private void performRequestAction(PasswordResetRequestRowDTO row,
                                      String title,
                                      String message,
                                      Runnable action,
                                      String successMessage) {
        if (row == null || !confirm(title, message)) {
            return;
        }

        try {
            action.run();
            refresh();
            UiMessageUtil.setSuccess(messageLabel, successMessage);
        } catch (Exception e) {
            UiMessageUtil.setError(messageLabel, UiMessageUtil.toUserMessage(e));
        }
    }

    private boolean confirm(String title, String message) {
        ButtonType confirm = new ButtonType(title, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, confirm, cancel);
        alert.setTitle(title);
        alert.setHeaderText(null);
        if (requestsTable.getScene() != null) {
            alert.initOwner(requestsTable.getScene().getWindow());
        }
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == confirm;
    }

    private String displayStatus(PasswordResetStatus status) {
        if (status == null) {
            return "-";
        }
        String text = status.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }

    private String formatDate(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_FMT) : "-";
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    private String normal(String value) {
        return nullSafe(value).trim().toLowerCase(Locale.ROOT);
    }

    private final class StatusCell extends TableCell<PasswordResetRequestRowDTO, PasswordResetStatus> {
        @Override
        protected void updateItem(PasswordResetStatus status, boolean empty) {
            super.updateItem(status, empty);

            if (empty || status == null) {
                setGraphic(null);
                return;
            }

            Label badge = new Label(displayStatus(status).toUpperCase(Locale.ROOT));
            badge.getStyleClass().add("status-badge");
            switch (status) {
                case PENDING -> badge.getStyleClass().add("status-badge-warning");
                case COMPLETED -> badge.getStyleClass().add("status-badge-success");
                case CANCELLED -> badge.getStyleClass().add("status-badge-danger");
                default -> {
                }
            }
            setGraphic(badge);
        }
    }

    private final class ActionCell extends TableCell<PasswordResetRequestRowDTO, PasswordResetRequestRowDTO> {
        private final HBox actions = new HBox(8);

        private ActionCell() {
            actions.setAlignment(Pos.CENTER_LEFT);
        }

        @Override
        protected void updateItem(PasswordResetRequestRowDTO row, boolean empty) {
            super.updateItem(row, empty);

            if (empty || row == null) {
                setGraphic(null);
                return;
            }

            actions.getChildren().clear();
            if (row.getStatus() == PasswordResetStatus.PENDING) {
                actions.getChildren().add(actionButton("Process", () -> processRequest(row), "small-pill-button", false));
                actions.getChildren().add(actionButton("Cancel", () -> cancelRequest(row), "danger-outline-button", false));
            } else if (row.getStatus() == PasswordResetStatus.COMPLETED) {
                actions.getChildren().add(actionButton("Completed", () -> { }, "small-pill-button", true));
            } else if (row.getStatus() == PasswordResetStatus.CANCELLED) {
                actions.getChildren().add(actionButton("Cancelled", () -> { }, "danger-outline-button", true));
            }

            setGraphic(actions);
        }

        private Button actionButton(String text, Runnable action, String styleClass, boolean disabled) {
            Button button = new Button(text);
            button.getStyleClass().add(styleClass);
            button.setDisable(disabled);
            button.setOnAction(event -> action.run());
            return button;
        }
    }
}
