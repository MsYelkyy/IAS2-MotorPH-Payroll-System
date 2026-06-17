package motorph.ui.admin.useraccount;

import motorph.application.service.UserAccountService;
import motorph.domain.model.UserAccount;
import motorph.domain.value.Role;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.util.UiMessageUtil;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Manages the User Account Management sub-view: list, search, status controls, and role assignment. */
public class AdminUserAccountController {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    private UserAccountService userAccountService;

    // ── State ─────────────────────────────────────────────────────────────────
    private List<UserAccount> allAccounts = new ArrayList<>();
    private UserAccount selectedAccount;
    private final ObservableList<UserAccount> accountItems = FXCollections.observableArrayList();
    private FilteredList<UserAccount> filteredAccounts;

    // ── Toolbar ───────────────────────────────────────────────────────────────
    @FXML private TextField    searchField;
    @FXML private ComboBox<String> filterCombo;
    @FXML private Label        moduleMessageLabel;

    // ── List ──────────────────────────────────────────────────────────────────
    @FXML private TableView<UserAccount> accountTable;
    @FXML private TableColumn<UserAccount, Number> accountIdColumn;
    @FXML private TableColumn<UserAccount, Number> employeeIdColumn;
    @FXML private TableColumn<UserAccount, String> emailColumn;
    @FXML private TableColumn<UserAccount, String> rolesColumn;
    @FXML private TableColumn<UserAccount, String> statusColumn;
    @FXML private TableColumn<UserAccount, Void> actionColumn;

    // ── Detail panel ──────────────────────────────────────────────────────────
    @FXML private VBox         detailPanel;
    @FXML private Label        detailEmailLabel;
    @FXML private Label        detailEmployeeIdLabel;
    @FXML private Label        detailStatusLabel;
    @FXML private Label        detailLockedLabel;
    @FXML private Label        detailLastLoginLabel;
    @FXML private Label        detailRolesLabel;

    // role checkboxes
    @FXML private CheckBox     roleAdminCheck;
    @FXML private CheckBox     roleHrCheck;
    @FXML private CheckBox     roleFinanceCheck;
    @FXML private CheckBox     roleItCheck;
    @FXML private CheckBox     roleEmployeeCheck;

    // action buttons
    @FXML private Button       lockButton;
    @FXML private Button       enableButton;

    @FXML
    private void initialize() {
        userAccountService = EmployeeUIServiceFactory.getInstance().getUserAccountService();

        filterCombo.getItems().setAll("All", "Active", "Locked", "Disabled");
        filterCombo.setValue("All");
        filterCombo.setOnAction(e -> applyFilter());

        searchField.textProperty().addListener((obs, old, text) -> applyFilter());

        setupAccountTable();
        loadAccounts();
    }

    /** Called by AdminMainController each time user management nav is activated. */
    public void refresh() {
        loadAccounts();
        closeDetail();
        UiMessageUtil.clearMessage(moduleMessageLabel);
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadAccounts() {
        try {
            allAccounts = userAccountService.findAll();
            accountItems.setAll(allAccounts);
            applyFilter();
        } catch (Exception e) {
            UiMessageUtil.setError(moduleMessageLabel, UiMessageUtil.toUserMessage(e));
        }
    }

    private void applyFilter() {
        if (filteredAccounts == null) return;

        String keyword = searchField.getText();
        String filter  = filterCombo.getValue();
        filteredAccounts.setPredicate(account ->
                matchesFilter(account, filter) && matchesSearch(account, keyword));
    }

    private boolean matchesFilter(UserAccount a, String filter) {
        if (filter == null || filter.equals("All")) return true;
        return switch (filter) {
            case "Active"   -> a.isActive() && !a.isLocked();
            case "Locked"   -> a.isLocked();
            case "Disabled" -> !a.isActive();
            default         -> true;
        };
    }

    private boolean matchesSearch(UserAccount a, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        String lower = keyword.toLowerCase();
        return a.getCompanyEmail().toLowerCase().contains(lower)
                || String.valueOf(a.getEmployeeId()).contains(lower);
    }

    // ── Row rendering ─────────────────────────────────────────────────────────

    private void setupAccountTable() {
        accountTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        accountIdColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getUserAccountId()));
        employeeIdColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getEmployeeId()));
        emailColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCompanyEmail()));
        rolesColumn.setCellValueFactory(data ->
                new SimpleStringProperty(rolesText(data.getValue())));
        statusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(statusText(data.getValue())));
        statusColumn.setCellFactory(column -> statusCell());
        actionColumn.setSortable(false);
        actionColumn.setCellFactory(column -> new TableCell<>() {
            private final Button manageButton = new Button("Manage");

            {
                manageButton.getStyleClass().add("table-action-button");
                manageButton.setOnAction(event -> {
                    UserAccount account = getTableRow().getItem();
                    if (account != null) {
                        selectAccount(account);
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
            protected void updateItem(UserAccount account, boolean empty) {
                super.updateItem(account, empty);
                boolean selected = !empty
                        && account != null
                        && selectedAccount != null
                        && account.getUserAccountId() != null
                        && account.getUserAccountId().equals(selectedAccount.getUserAccountId());
                setStyle(selected ? "-fx-background-color: rgba(59,130,246,0.10);" : "");
            }
        });

        filteredAccounts = new FilteredList<>(accountItems, account -> true);
        SortedList<UserAccount> sortedAccounts = new SortedList<>(filteredAccounts);
        sortedAccounts.comparatorProperty().bind(accountTable.comparatorProperty());
        accountTable.setItems(sortedAccounts);
    }

    private TableCell<UserAccount, String> statusCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                getStyleClass().removeAll("status-badge", "status-badge-warning", "status-badge-inactive");
                if (empty || status == null) {
                    setText(null);
                    return;
                }
                setText(status);
                getStyleClass().add(switch (status) {
                    case "DISABLED" -> "status-badge-inactive";
                    case "LOCKED" -> "status-badge-warning";
                    default -> "status-badge";
                });
            }
        };
    }

    private String statusText(UserAccount account) {
        return !account.isActive() ? "DISABLED" : account.isLocked() ? "LOCKED" : "ACTIVE";
    }

    private String rolesText(UserAccount account) {
        return account.getRoles() == null || account.getRoles().isEmpty() ? "â€”"
                : account.getRoles().stream().map(Role::name).collect(Collectors.joining(", "));
    }

    private HBox buildRow(UserAccount a) {
        HBox row = new HBox(0);
        row.getStyleClass().add("table-data-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);

        Label idLbl    = col(String.valueOf(a.getUserAccountId() != null ? a.getUserAccountId() : "—"), 50);
        Label empLbl   = col(String.valueOf(a.getEmployeeId()), 70);
        Label emailLbl = col(a.getCompanyEmail(), 230);

        String rolesText = a.getRoles() == null || a.getRoles().isEmpty() ? "—"
                : a.getRoles().stream().map(Role::name).collect(Collectors.joining(", "));
        Label rolesLbl = col(rolesText, 160);

        String statusText = !a.isActive() ? "DISABLED" : a.isLocked() ? "LOCKED" : "ACTIVE";
        Label statusLbl = new Label(statusText);
        statusLbl.getStyleClass().add(!a.isActive() ? "status-badge-inactive"
                : a.isLocked() ? "status-badge-warning" : "status-badge");
        statusLbl.setMinWidth(72);
        statusLbl.setPrefWidth(72);
        HBox.setMargin(statusLbl, new Insets(0, 0, 0, 12));

        Label spacer = new Label();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        boolean isSelected = selectedAccount != null
                && a.getUserAccountId() != null
                && a.getUserAccountId().equals(selectedAccount.getUserAccountId());
        if (isSelected) row.setStyle("-fx-background-color: rgba(59,130,246,0.10);");

        Button viewBtn = new Button("Manage");
        viewBtn.getStyleClass().add("table-action-button");
        viewBtn.setOnAction(ev -> selectAccount(a));

        HBox actions = new HBox(6, viewBtn);
        actions.setMinWidth(90);
        actions.setPrefWidth(90);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setStyle("-fx-padding: 0 0 0 12;");

        row.getChildren().addAll(idLbl, empLbl, emailLbl, rolesLbl, statusLbl, spacer, actions);
        return row;
    }

    private Label col(String text, double width) {
        Label lbl = new Label(text);
        lbl.getStyleClass().addAll("data-line", "table-cell");
        lbl.setMinWidth(width);
        lbl.setPrefWidth(width);
        return lbl;
    }

    // ── Detail panel ──────────────────────────────────────────────────────────

    private void selectAccount(UserAccount account) {
        selectedAccount = account;
        applyFilter();
        accountTable.refresh();
        loadDetailPanel(account);
        setVisible(detailPanel, true);
    }

    private void loadDetailPanel(UserAccount a) {
        detailEmailLabel.setText(a.getCompanyEmail());
        detailEmployeeIdLabel.setText(String.valueOf(a.getEmployeeId()));

        String status = !a.isActive() ? "DISABLED" : a.isLocked() ? "LOCKED" : "ACTIVE";
        detailStatusLabel.setText(status);

        detailLockedLabel.setText(a.isLocked() ? "Yes" : "No");
        detailLastLoginLabel.setText(a.getLastLoginAt() != null ? DT_FMT.format(a.getLastLoginAt()) : "Never");

        String rolesText = a.getRoles() == null || a.getRoles().isEmpty() ? "—"
                : a.getRoles().stream().map(Role::name).collect(Collectors.joining(", "));
        detailRolesLabel.setText(rolesText);

        // Populate role checkboxes
        Set<Role> currentRoles = a.getRoles() != null ? Set.copyOf(a.getRoles()) : Set.of();
        roleAdminCheck.setSelected(currentRoles.contains(Role.ADMIN));
        roleHrCheck.setSelected(currentRoles.contains(Role.HR));
        roleFinanceCheck.setSelected(currentRoles.contains(Role.FINANCE));
        roleItCheck.setSelected(currentRoles.contains(Role.IT));
        roleEmployeeCheck.setSelected(currentRoles.contains(Role.EMPLOYEE));

        // Update lock button label
        lockButton.setText(a.isLocked() ? "Unlock Account" : "Lock Account");
        lockButton.getStyleClass().removeAll("danger-outline-button", "secondary-button");
        lockButton.getStyleClass().add(a.isLocked() ? "secondary-button" : "danger-outline-button");

        // Update enable/disable button label
        enableButton.setText(a.isActive() ? "Disable Account" : "Enable Account");
        enableButton.getStyleClass().removeAll("danger-outline-button", "secondary-button");
        enableButton.getStyleClass().add(a.isActive() ? "danger-outline-button" : "secondary-button");
    }

    @FXML
    private void handleToggleLock() {
        if (selectedAccount == null) return;
        try {
            if (selectedAccount.isLocked()) {
                userAccountService.unlockAccount(selectedAccount.getUserAccountId());
                UiMessageUtil.setSuccess(moduleMessageLabel, "Account unlocked.");
            } else {
                userAccountService.lockAccount(selectedAccount.getUserAccountId());
                UiMessageUtil.setSuccess(moduleMessageLabel, "Account locked.");
            }
            loadAccounts();
            // Re-select to refresh detail
            allAccounts.stream()
                    .filter(a -> a.getUserAccountId().equals(selectedAccount.getUserAccountId()))
                    .findFirst()
                    .ifPresent(this::selectAccount);
        } catch (Exception e) {
            UiMessageUtil.setError(moduleMessageLabel, UiMessageUtil.toUserMessage(e));
        }
    }

    @FXML
    private void handleToggleEnable() {
        if (selectedAccount == null) return;
        String action = selectedAccount.isActive() ? "disable" : "enable";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm");
        confirm.setHeaderText("Are you sure you want to " + action + " this account?");
        confirm.setContentText(selectedAccount.getCompanyEmail());
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    if (selectedAccount.isActive()) {
                        userAccountService.disableAccount(selectedAccount.getUserAccountId());
                        UiMessageUtil.setSuccess(moduleMessageLabel, "Account disabled.");
                    } else {
                        userAccountService.enableAccount(selectedAccount.getUserAccountId());
                        UiMessageUtil.setSuccess(moduleMessageLabel, "Account enabled.");
                    }
                    loadAccounts();
                    allAccounts.stream()
                            .filter(a -> a.getUserAccountId().equals(selectedAccount.getUserAccountId()))
                            .findFirst()
                            .ifPresent(this::selectAccount);
                } catch (Exception e) {
                    UiMessageUtil.setError(moduleMessageLabel, UiMessageUtil.toUserMessage(e));
                }
            }
        });
    }

    @FXML
    private void handleSaveRoles() {
        if (selectedAccount == null) return;
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

        try {
            userAccountService.updateRoles(selectedAccount.getUserAccountId(), roles);
            UiMessageUtil.setSuccess(moduleMessageLabel, "Roles updated for " + selectedAccount.getCompanyEmail());
            loadAccounts();
            allAccounts.stream()
                    .filter(a -> a.getUserAccountId().equals(selectedAccount.getUserAccountId()))
                    .findFirst()
                    .ifPresent(this::selectAccount);
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

    private void setVisible(javafx.scene.Node node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }
}
