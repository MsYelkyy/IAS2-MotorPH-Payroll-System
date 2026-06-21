package motorph.ui.it;

import motorph.application.dto.user.UserAccountRowDTO;
import motorph.application.service.ITUserAccountService;
import motorph.application.service.UserAccountService;
import motorph.domain.value.AccountStatus;
import motorph.domain.value.Role;
import motorph.ui.factory.ITUIServiceFactory;
import motorph.ui.util.UiMessageUtil;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles the IT User Accounts screen.
 * Includes account search, role assignment, and lock controls.
 */
public class ITUserAccountController {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");

    private static final String STATUS_ALL = "All";
    private static final String STATUS_ACTIVE = "Active";
    private static final String STATUS_LOCKED = "Locked";
    private static final String STATUS_DISABLED = "Disabled";

    private static final String TEXT_ACTIVE = "ACTIVE";
    private static final String TEXT_LOCKED = "LOCKED";
    private static final String TEXT_DISABLED = "DISABLED";

    private ITUserAccountService itUserAccountService;
    private UserAccountService userAccountService;

    // State
    private List<UserAccountRowDTO> allRows = new ArrayList<>();
    private UserAccountRowDTO selectedAccount;
    private final ObservableList<UserAccountRowDTO> accountItems = FXCollections.observableArrayList();
    private FilteredList<UserAccountRowDTO> filteredAccounts;

    // Toolbar
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterCombo;
    @FXML private Label moduleMessageLabel;

    // List
    @FXML private TableView<UserAccountRowDTO> accountTable;
    @FXML private TableColumn<UserAccountRowDTO, String> accountIdColumn;
    @FXML private TableColumn<UserAccountRowDTO, String> employeeIdColumn;
    @FXML private TableColumn<UserAccountRowDTO, String> emailColumn;
    @FXML private TableColumn<UserAccountRowDTO, String> rolesColumn;
    @FXML private TableColumn<UserAccountRowDTO, String> statusColumn;
    @FXML private TableColumn<UserAccountRowDTO, Void> actionColumn;

    // Detail panel
    @FXML private VBox detailPanel;
    @FXML private Label detailEmailLabel;
    @FXML private Label detailEmployeeIdLabel;
    @FXML private Label detailStatusLabel;
    @FXML private Label detailLockedLabel;
    @FXML private Label detailLastLoginLabel;
    @FXML private Label detailRolesLabel;

    @FXML private CheckBox roleAdminCheck;
    @FXML private CheckBox roleHrCheck;
    @FXML private CheckBox roleFinanceCheck;
    @FXML private CheckBox roleItCheck;
    @FXML private CheckBox roleEmployeeCheck;

    @FXML private VBox roleAssignmentSection;
    @FXML private Button saveRolesButton;
    @FXML private Button lockButton;
    @FXML private Button enableButton;

    @FXML
    private void initialize() {
        ITUIServiceFactory factory = ITUIServiceFactory.getInstance();
        itUserAccountService = factory.getITUserAccountService();
        userAccountService = factory.getUserAccountService();

        filterCombo.getItems().setAll(STATUS_ALL, STATUS_ACTIVE, STATUS_LOCKED, STATUS_DISABLED);
        filterCombo.setValue(STATUS_ALL);
        filterCombo.setOnAction(e -> applyFilter());
        searchField.textProperty().addListener((obs, old, text) -> applyFilter());

        setupAccountTable();
        loadAccounts();
    }

    /** Called by ITMainController each time the User Management nav item is activated. */
    public void refresh() {
        loadAccounts();
        closeDetail();
        UiMessageUtil.clearMessage(moduleMessageLabel);
    }

    // Data loading
    private void loadAccounts() {
        try {
            allRows = itUserAccountService.getUserAccountRows();
            accountItems.setAll(allRows);
            applyFilter();
            UiMessageUtil.setSuccess(moduleMessageLabel, "User accounts refreshed.");
        } catch (Exception e) {
            allRows = new ArrayList<>();
            accountItems.clear();
            applyFilter();
            UiMessageUtil.setError(moduleMessageLabel, UiMessageUtil.toUserMessage(e));
        }
    }

    private void applyFilter() {
        if (filteredAccounts == null) return;
        String keyword = normal(searchField.getText());
        String filter = filterCombo.getValue();
        filteredAccounts.setPredicate(row ->
                matchesFilter(row, filter) && matchesSearch(row, keyword));
    }

    private boolean matchesFilter(UserAccountRowDTO row, String filter) {
        if (filter == null || STATUS_ALL.equals(filter)) return true;
        return switch (filter) {
            case STATUS_ACTIVE -> row.isActive() && !isLocked(row);
            case STATUS_LOCKED -> row.isActive() && isLocked(row);
            case STATUS_DISABLED -> !row.isActive();
            default -> true;
        };
    }

    private boolean matchesSearch(UserAccountRowDTO row, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        String haystack = String.join(" ",
                String.valueOf(row.getEmployeeId()),
                nullSafe(row.getFullName()),
                nullSafe(row.getCompanyEmail()),
                rolesText(row),
                statusText(row));
        return normal(haystack).contains(keyword);
    }

    // Table rendering
    private void setupAccountTable() {
        accountTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        accountIdColumn.setCellValueFactory(d ->
                new SimpleStringProperty(String.valueOf(d.getValue().getUserAccountId())));
        employeeIdColumn.setCellValueFactory(d ->
                new SimpleStringProperty(String.valueOf(d.getValue().getEmployeeId())));
        emailColumn.setCellValueFactory(d ->
                new SimpleStringProperty(nullSafe(d.getValue().getCompanyEmail())));
        rolesColumn.setCellValueFactory(d ->
                new SimpleStringProperty(rolesText(d.getValue())));
        statusColumn.setCellValueFactory(d ->
                new SimpleStringProperty(statusText(d.getValue())));
        statusColumn.setCellFactory(column -> statusCell());

        actionColumn.setSortable(false);
        actionColumn.setCellFactory(column -> new TableCell<>() {
            private final Button manageButton = new Button("Manage");

            {
                manageButton.getStyleClass().add("table-action-button");
                manageButton.setOnAction(event -> {
                    UserAccountRowDTO row = getTableRow() == null ? null : getTableRow().getItem();
                    if (row != null) {
                        selectAccount(row);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER_LEFT);
                setGraphic(empty ? null : manageButton);
            }
        });

        accountTable.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(UserAccountRowDTO row, boolean empty) {
                super.updateItem(row, empty);
                boolean selected = !empty
                        && row != null
                        && selectedAccount != null
                        && row.getUserAccountId() == selectedAccount.getUserAccountId();
                setStyle(selected ? "-fx-background-color: rgba(59,130,246,0.10);" : "");
            }
        });

        filteredAccounts = new FilteredList<>(accountItems, row -> true);
        SortedList<UserAccountRowDTO> sorted = new SortedList<>(filteredAccounts);
        sorted.comparatorProperty().bind(accountTable.comparatorProperty());
        accountTable.setItems(sorted);
    }

    private TableCell<UserAccountRowDTO, String> statusCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                getStyleClass().removeAll("status-badge", "status-badge-warning", "status-badge-neutral");
                if (empty || status == null) {
                    setText(null);
                    return;
                }
                setText(status);
                getStyleClass().add(switch (status) {
                    case TEXT_DISABLED -> "status-badge-neutral";
                    case TEXT_LOCKED -> "status-badge-warning";
                    default -> "status-badge";
                });
            }
        };
    }

    private String statusText(UserAccountRowDTO row) {
        if (isDisabled(row)) return TEXT_DISABLED;
        if (isLocked(row)) return TEXT_LOCKED;
        return TEXT_ACTIVE;
    }

    private String rolesText(UserAccountRowDTO row) {
        List<Role> roles = row.getRoles();
        if (roles == null || roles.isEmpty()) return "—";
        return roles.stream().map(Role::name).collect(Collectors.joining(", "));
    }

    // Detail panel
    private void selectAccount(UserAccountRowDTO row) {
        selectedAccount = row;
        accountTable.refresh();
        loadDetailPanel(row);
        setVisible(detailPanel, true);
    }

    private void loadDetailPanel(UserAccountRowDTO row) {
        detailEmailLabel.setText(nullSafe(row.getCompanyEmail()));
        detailEmployeeIdLabel.setText(String.valueOf(row.getEmployeeId()));
        detailStatusLabel.setText(statusText(row));
        detailLockedLabel.setText(isLocked(row) ? "Yes" : "No");
        detailLastLoginLabel.setText(formatDate(row.getLastLoginAt()));
        detailRolesLabel.setText(rolesText(row));

        Set<Role> current = row.getRoles() != null ? Set.copyOf(row.getRoles()) : Set.of();
        roleAdminCheck.setSelected(current.contains(Role.ADMIN));
        roleHrCheck.setSelected(current.contains(Role.HR));
        roleFinanceCheck.setSelected(current.contains(Role.FINANCE));
        roleItCheck.setSelected(current.contains(Role.IT));
        roleEmployeeCheck.setSelected(current.contains(Role.EMPLOYEE));

        boolean disabled = isDisabled(row);
        setVisible(roleAssignmentSection, !disabled);
        setVisible(saveRolesButton, !disabled);
        setVisible(lockButton, !disabled);
        setVisible(enableButton, !disabled);

        boolean locked = isLocked(row);
        lockButton.setText(locked ? "Unlock Account" : "Lock Account");
        lockButton.getStyleClass().removeAll("danger-outline-button", "secondary-button");
        lockButton.getStyleClass().add(locked ? "secondary-button" : "danger-outline-button");

        enableButton.setText(disabled ? "Enable Account" : "Disable Account");
        enableButton.getStyleClass().removeAll("danger-outline-button", "secondary-button");
        enableButton.getStyleClass().add(disabled ? "secondary-button" : "danger-outline-button");
    }

    @FXML
    private void handleToggleLock() {
        if (selectedAccount == null || isDisabled(selectedAccount)) return;
        int id = selectedAccount.getUserAccountId();
        try {
            if (isLocked(selectedAccount)) {
                userAccountService.unlockAccount(id);
                UiMessageUtil.setSuccess(moduleMessageLabel, "Account unlocked.");
            } else {
                userAccountService.lockAccount(id);
                UiMessageUtil.setSuccess(moduleMessageLabel, "Account locked.");
            }
            reloadAndReselect(id);
        } catch (Exception e) {
            UiMessageUtil.setError(moduleMessageLabel, UiMessageUtil.toUserMessage(e));
        }
    }

    @FXML
    private void handleToggleEnable() {
        if (selectedAccount == null || isDisabled(selectedAccount)) return;
        int id = selectedAccount.getUserAccountId();
        boolean disabled = isDisabled(selectedAccount);
        String action = disabled ? "enable" : "disable";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm");
        confirm.setHeaderText("Are you sure you want to " + action + " this account?");
        confirm.setContentText(nullSafe(selectedAccount.getCompanyEmail()));
        if (accountTable.getScene() != null) {
            confirm.initOwner(accountTable.getScene().getWindow());
        }
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    if (disabled) {
                        userAccountService.enableAccount(id);
                        UiMessageUtil.setSuccess(moduleMessageLabel, "Account enabled.");
                    } else {
                        userAccountService.disableAccount(id);
                        UiMessageUtil.setSuccess(moduleMessageLabel, "Account disabled.");
                    }
                    reloadAndReselect(id);
                } catch (Exception e) {
                    UiMessageUtil.setError(moduleMessageLabel, UiMessageUtil.toUserMessage(e));
                }
            }
        });
    }

    @FXML
    private void handleSaveRoles() {
        if (selectedAccount == null || isDisabled(selectedAccount)) return;

        List<Role> roles = new ArrayList<>();
        if (roleAdminCheck.isSelected())    roles.add(Role.ADMIN);
        if (roleHrCheck.isSelected())       roles.add(Role.HR);
        if (roleFinanceCheck.isSelected())  roles.add(Role.FINANCE);
        if (roleItCheck.isSelected())       roles.add(Role.IT);
        if (roleEmployeeCheck.isSelected()) roles.add(Role.EMPLOYEE);

        if (roles.isEmpty()) {
            UiMessageUtil.setError(moduleMessageLabel, "At least one role must be assigned.");
            return;
        }

        int id = selectedAccount.getUserAccountId();
        try {
            userAccountService.updateRoles(id, roles);
            UiMessageUtil.setSuccess(moduleMessageLabel,
                    "Roles updated for " + nullSafe(selectedAccount.getCompanyEmail()));
            reloadAndReselect(id);
        } catch (Exception e) {
            UiMessageUtil.setError(moduleMessageLabel, UiMessageUtil.toUserMessage(e));
        }
    }

    @FXML
    private void handleCloseDetail() {
        closeDetail();
    }

    private void closeDetail() {
        selectedAccount = null;
        if (accountTable != null) {
            accountTable.refresh();
        }
        setVisible(detailPanel, false);
    }

    /** Reload list after a command and re-open the detail panel on the same account. */
    private void reloadAndReselect(int userAccountId) {
        loadAccounts();
        allRows.stream()
                .filter(r -> r.getUserAccountId() == userAccountId)
                .findFirst()
                .ifPresentOrElse(this::selectAccount, this::closeDetail);
    }

    // Status helpers
    private boolean isLocked(UserAccountRowDTO row) {
        return row.isLocked() || row.getAccountStatus() == AccountStatus.LOCKED;
    }

    private boolean isDisabled(UserAccountRowDTO row) {
        return !row.isActive();
    }

    private String formatDate(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_FMT) : "Never";
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    private String normal(String value) {
        return nullSafe(value).trim().toLowerCase(Locale.ROOT);
    }

    private void setVisible(Node node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }
}
