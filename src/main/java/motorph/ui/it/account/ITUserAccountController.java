package motorph.ui.it.account;

import motorph.application.dto.user.UserAccountRowDTO;
import motorph.application.service.ITUserAccountService;
import motorph.application.service.UserAccountService;
import motorph.domain.value.AccountStatus;
import motorph.domain.value.Role;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.util.UiMessageUtil;

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
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/** Controller for the IT User Account Management screen. */
public class ITUserAccountController {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");

    private static final int PAGE_SIZE = 7;

    private static final String STATUS_ALL = "All";
    private static final String STATUS_ACTIVE = "Active";
    private static final String STATUS_LOCKED = "Locked";
    private static final String STATUS_DISABLED = "Disabled";

    private ITUserAccountService itUserAccountService;
    private UserAccountService userAccountService;

    private final ObservableList<UserAccountRowDTO> allRows      = FXCollections.observableArrayList();
    private final ObservableList<UserAccountRowDTO> filteredRows = FXCollections.observableArrayList();
    private final ObservableList<UserAccountRowDTO> pagedRows    = FXCollections.observableArrayList();

    private int currentPage = 1;

    @FXML private Label totalAccountsLabel;
    @FXML private Label activeAccountsLabel;
    @FXML private Label lockedAccountsLabel;
    @FXML private Label disabledAccountsLabel;
    @FXML private Label messageLabel;

    @FXML private Button accountsPrevButton;
    @FXML private Button accountsNextButton;
    @FXML private Label  accountsPageLabel;
    @FXML private Label  accountsRecordInfoLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterCombo;

    @FXML private TableView<UserAccountRowDTO> accountsTable;
    @FXML private TableColumn<UserAccountRowDTO, String> employeeIdColumn;
    @FXML private TableColumn<UserAccountRowDTO, String> fullNameColumn;
    @FXML private TableColumn<UserAccountRowDTO, String> companyEmailColumn;
    @FXML private TableColumn<UserAccountRowDTO, String> rolesColumn;
    @FXML private TableColumn<UserAccountRowDTO, String> currentRoleColumn;
    @FXML private TableColumn<UserAccountRowDTO, String> statusColumn;
    @FXML private TableColumn<UserAccountRowDTO, String> lastLoginColumn;
    @FXML private TableColumn<UserAccountRowDTO, UserAccountRowDTO> actionsColumn;

    @FXML
    private void initialize() {
        EmployeeUIServiceFactory factory = EmployeeUIServiceFactory.getInstance();
        itUserAccountService = factory.getITUserAccountService();
        userAccountService = factory.getUserAccountService();

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
            List<UserAccountRowDTO> rows = itUserAccountService.getUserAccountRows();
            allRows.setAll(rows);
            updateSummaryCards();
            applyFilters();
            UiMessageUtil.setSuccess(messageLabel, "User accounts refreshed.");
        } catch (Exception e) {
            allRows.clear();
            filteredRows.clear();
            updateSummaryCards();
            UiMessageUtil.setError(messageLabel, UiMessageUtil.toUserMessage(e));
        }
    }

    private void configureFilters() {
        statusFilterCombo.getItems().setAll(STATUS_ALL, STATUS_ACTIVE, STATUS_LOCKED, STATUS_DISABLED);
        statusFilterCombo.setValue(STATUS_ALL);

        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        statusFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
    }

    private void configureTable() {
        accountsTable.setItems(pagedRows);
        accountsTable.setPlaceholder(new Label("No user accounts found."));
        accountsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        employeeIdColumn.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().getEmployeeId())));
        fullNameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(nullSafe(data.getValue().getFullName())));
        companyEmailColumn.setCellValueFactory(data ->
                new SimpleStringProperty(nullSafe(data.getValue().getCompanyEmail())));
        rolesColumn.setCellValueFactory(data ->
                new SimpleStringProperty(formatRoles(data.getValue().getRoles())));
        currentRoleColumn.setCellValueFactory(data ->
                new SimpleStringProperty(formatRole(data.getValue().getActiveRole())));
        statusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(displayStatus(data.getValue())));
        lastLoginColumn.setCellValueFactory(data ->
                new SimpleStringProperty(formatDate(data.getValue().getLastLoginAt())));

        actionsColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));
        actionsColumn.setCellFactory(column -> new ActionCell());
    }

    private void applyFilters() {
        String query = normal(searchField.getText());
        String status = statusFilterCombo.getValue();

        filteredRows.setAll(allRows.stream()
                .filter(row -> matchesStatus(row, status))
                .filter(row -> matchesQuery(row, query))
                .collect(Collectors.toList()));

        currentPage = 1;
        renderPage();
    }

    private boolean matchesStatus(UserAccountRowDTO row, String status) {
        if (status == null || STATUS_ALL.equals(status)) {
            return true;
        }
        return switch (status) {
            case STATUS_ACTIVE -> isActiveUnlocked(row);
            case STATUS_LOCKED -> isLocked(row);
            case STATUS_DISABLED -> isDisabled(row);
            default -> true;
        };
    }

    private boolean matchesQuery(UserAccountRowDTO row, String query) {
        if (query.isBlank()) {
            return true;
        }

        String searchable = String.join(" ",
                String.valueOf(row.getEmployeeId()),
                nullSafe(row.getFullName()),
                nullSafe(row.getCompanyEmail()),
                formatRoles(row.getRoles()),
                formatRole(row.getActiveRole()),
                displayStatus(row));

        return normal(searchable).contains(query);
    }

    private void renderPage() {
        int total      = filteredRows.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        currentPage    = Math.max(1, Math.min(currentPage, totalPages));

        int from = (currentPage - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, total);

        pagedRows.setAll(filteredRows.subList(from, to));
        accountsPageLabel.setText("Page " + currentPage + " of " + totalPages);
        accountsRecordInfoLabel.setText(total == 0
                ? "No accounts found."
                : "Showing " + (from + 1) + " to " + to + " of " + total + " accounts");
        accountsPrevButton.setDisable(currentPage <= 1);
        accountsNextButton.setDisable(currentPage >= totalPages);
    }

    @FXML
    private void handleAccountsPrevPage() {
        if (currentPage > 1) {
            currentPage--;
            renderPage();
        }
    }

    @FXML
    private void handleAccountsNextPage() {
        int totalPages = Math.max(1, (int) Math.ceil((double) filteredRows.size() / PAGE_SIZE));
        if (currentPage < totalPages) {
            currentPage++;
            renderPage();
        }
    }

    private void updateSummaryCards() {
        long active = allRows.stream().filter(this::isActiveUnlocked).count();
        long locked = allRows.stream().filter(this::isLocked).count();
        long disabled = allRows.stream().filter(this::isDisabled).count();

        totalAccountsLabel.setText(String.valueOf(allRows.size()));
        activeAccountsLabel.setText(String.valueOf(active));
        lockedAccountsLabel.setText(String.valueOf(locked));
        disabledAccountsLabel.setText(String.valueOf(disabled));
    }

    private void lockAccount(UserAccountRowDTO row) {
        performAccountAction(row, "Lock Account",
                "Lock the account for " + row.getFullName() + "?",
                () -> userAccountService.lockAccount(row.getUserAccountId()));
    }

    private void unlockAccount(UserAccountRowDTO row) {
        performAccountAction(row, "Unlock Account",
                "Unlock the account for " + row.getFullName() + "?",
                () -> userAccountService.unlockAccount(row.getUserAccountId()));
    }

    private void disableAccount(UserAccountRowDTO row) {
        performAccountAction(row, "Disable Account",
                "Disable the account for " + row.getFullName() + "?",
                () -> userAccountService.disableAccount(row.getUserAccountId()));
    }

    private void enableAccount(UserAccountRowDTO row) {
        performAccountAction(row, "Enable Account",
                "Enable the account for " + row.getFullName() + "?",
                () -> userAccountService.enableAccount(row.getUserAccountId()));
    }

    private void performAccountAction(UserAccountRowDTO row, String title, String message, Runnable action) {
        if (!confirm(title, message)) {
            return;
        }

        try {
            action.run();
            refresh();
            UiMessageUtil.setSuccess(messageLabel, title + " completed for " + row.getFullName() + ".");
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
        if (accountsTable.getScene() != null) {
            alert.initOwner(accountsTable.getScene().getWindow());
        }
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == confirm;
    }

    private boolean isActiveUnlocked(UserAccountRowDTO row) {
        return row.isActive() && !row.isLocked() && row.getAccountStatus() == AccountStatus.ACTIVE;
    }

    private boolean isLocked(UserAccountRowDTO row) {
        return row.isLocked() || row.getAccountStatus() == AccountStatus.LOCKED;
    }

    private boolean isDisabled(UserAccountRowDTO row) {
        return !row.isActive() || row.getAccountStatus() == AccountStatus.DISABLED;
    }

    private String displayStatus(UserAccountRowDTO row) {
        if (isDisabled(row)) {
            return STATUS_DISABLED;
        }
        if (isLocked(row)) {
            return STATUS_LOCKED;
        }
        return STATUS_ACTIVE;
    }

    private String formatRoles(List<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return "-";
        }
        return roles.stream()
                .map(this::formatRole)
                .collect(Collectors.joining(", "));
    }

    private String formatRole(Role role) {
        return role != null ? role.name() : "-";
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

    private final class ActionCell extends TableCell<UserAccountRowDTO, UserAccountRowDTO> {
        private final HBox actions = new HBox(8);

        private ActionCell() {
            actions.setAlignment(Pos.CENTER_LEFT);
        }

        @Override
        protected void updateItem(UserAccountRowDTO row, boolean empty) {
            super.updateItem(row, empty);

            if (empty || row == null) {
                setGraphic(null);
                return;
            }

            actions.getChildren().clear();
            if (ITUserAccountController.this.isDisabled(row)) {
                actions.getChildren().add(actionButton("Enable", () -> enableAccount(row), "small-pill-button"));
            } else if (ITUserAccountController.this.isLocked(row)) {
                actions.getChildren().add(actionButton("Unlock", () -> unlockAccount(row), "small-pill-button"));
            } else {
                actions.getChildren().add(actionButton("Lock", () -> lockAccount(row), "small-pill-button"));
                actions.getChildren().add(actionButton("Disable", () -> disableAccount(row), "danger-outline-button"));
            }

            setGraphic(actions);
        }

        private Button actionButton(String text, Runnable action, String styleClass) {
            Button button = new Button(text);
            button.getStyleClass().add(styleClass);
            button.setOnAction(event -> action.run());
            return button;
        }
    }
}
