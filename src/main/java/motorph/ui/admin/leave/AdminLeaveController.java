package motorph.ui.admin.leave;

import motorph.application.dto.leave.LeaveCreditRowDTO;
import motorph.application.dto.leave.LeaveRequestRowDTO;
import motorph.application.service.LeaveService;
import motorph.application.service.UserAccountService;
import motorph.domain.model.UserAccount;
import motorph.domain.value.RequestStatus;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.util.UiMessageUtil;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Manages the Admin Leave & Overtime module: request list, detail view, and employee credits panel. */
public class AdminLeaveController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    private LeaveService leaveService;
    private UserAccountService userAccountService;

    private List<LeaveRequestRowDTO> allRows = new ArrayList<>();
    private final ObservableList<LeaveRequestRowDTO> leaveItems = FXCollections.observableArrayList();
    private final ObservableList<LeaveCreditRowDTO> creditItems = FXCollections.observableArrayList();
    private FilteredList<LeaveRequestRowDTO> filteredLeaveItems;
    private LeaveRequestRowDTO selectedRow;

    @FXML private Button backButton;
    @FXML private Label moduleMessageLabel;

    @FXML private VBox listPanel;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private ComboBox<String> leaveTypeFilterCombo;
    @FXML private TextField employeeSearchField;
    @FXML private TableView<LeaveRequestRowDTO> leaveTable;
    @FXML private TableColumn<LeaveRequestRowDTO, Number> leaveIdColumn;
    @FXML private TableColumn<LeaveRequestRowDTO, String> leaveEmployeeColumn;
    @FXML private TableColumn<LeaveRequestRowDTO, String> leaveTypeColumn;
    @FXML private TableColumn<LeaveRequestRowDTO, LocalDate> leavePeriodColumn;
    @FXML private TableColumn<LeaveRequestRowDTO, Number> leaveHoursColumn;
    @FXML private TableColumn<LeaveRequestRowDTO, String> leaveStatusColumn;
    @FXML private TableColumn<LeaveRequestRowDTO, LocalDateTime> leaveSubmittedColumn;
    @FXML private TableColumn<LeaveRequestRowDTO, Void> leaveActionColumn;

    @FXML private VBox detailPanel;
    @FXML private Label detailIdLabel;
    @FXML private Label detailEmployeeLabel;
    @FXML private Label detailLeaveTypeLabel;
    @FXML private Label detailStatusLabel;
    @FXML private Label detailPeriodLabel;
    @FXML private Label detailHoursLabel;
    @FXML private Label detailReasonLabel;
    @FXML private Label detailSubmittedLabel;
    @FXML private Label detailApproverLabel;
    @FXML private Label detailReviewedLabel;
    @FXML private Label detailRemarksLabel;
    @FXML private Label detailSelfNoteLabel;
    @FXML private Button approveButton;
    @FXML private Button rejectButton;
    @FXML private Button reverseToRejectButton;
    @FXML private Button reverseToCancelButton;
    @FXML private Button viewCreditsButton;

    @FXML private VBox creditsPanel;
    @FXML private Label creditsHeaderLabel;
    @FXML private TableView<LeaveCreditRowDTO> creditTable;
    @FXML private TableColumn<LeaveCreditRowDTO, String> creditTypeColumn;
    @FXML private TableColumn<LeaveCreditRowDTO, Number> creditTotalColumn;
    @FXML private TableColumn<LeaveCreditRowDTO, Number> creditUsedColumn;
    @FXML private TableColumn<LeaveCreditRowDTO, Number> creditRemainingColumn;

    @FXML
    private void initialize() {
        leaveService = EmployeeUIServiceFactory.getInstance().getLeaveService();
        userAccountService = EmployeeUIServiceFactory.getInstance().getUserAccountService();
        setupFilters();
        setupLeaveTable();
        setupCreditTable();
        showList();
    }

    /** Refreshes the leave request list when the nav button is clicked. */
    public void refresh() {
        loadRows();
        applyFilters();
    }

    @FXML
    private void handleBack() {
        if (creditsPanel != null && creditsPanel.isVisible()) {
            showDetail(selectedRow);
        } else {
            showList();
        }
    }

    private void showList() {
        setPanelVisible(listPanel, true);
        setPanelVisible(detailPanel, false);
        setPanelVisible(creditsPanel, false);
        setNavVisible(false);
        setFilterVisible(true);
        loadRows();
        applyFilters();
    }

    private void showDetail(LeaveRequestRowDTO row) {
        if (row == null) {
            showList();
            return;
        }

        selectedRow = row;
        setPanelVisible(listPanel, false);
        setPanelVisible(detailPanel, true);
        setPanelVisible(creditsPanel, false);
        setNavVisible(true);
        setFilterVisible(false);
        populateDetail(row);
    }

    private void showCredits(LeaveRequestRowDTO row) {
        selectedRow = row;
        setPanelVisible(listPanel, false);
        setPanelVisible(detailPanel, false);
        setPanelVisible(creditsPanel, true);
        setNavVisible(true);
        setFilterVisible(false);
        populateCredits(row);
    }

    @FXML
    private void handleApprove() {
        if (selectedRow == null) return;
        try {
            UserAccount actor = resolveActorAccount();
            leaveService.approveLeaveRequest(actor, selectedRow.getLeaveRequestId());
            UiMessageUtil.setSuccess(moduleMessageLabel,
                    "Leave approved for " + selectedRow.getEmployeeFullName() + ".");
            loadRows();
            showList();
        } catch (Exception e) {
            UiMessageUtil.setError(moduleMessageLabel, userMessage(e));
        }
    }

    @FXML
    private void handleReject() {
        if (selectedRow == null) return;
        try {
            UserAccount actor = resolveActorAccount();
            leaveService.rejectLeaveRequest(actor, selectedRow.getLeaveRequestId());
            UiMessageUtil.setSuccess(moduleMessageLabel,
                    "Leave rejected for " + selectedRow.getEmployeeFullName() + ".");
            loadRows();
            showList();
        } catch (Exception e) {
            UiMessageUtil.setError(moduleMessageLabel, userMessage(e));
        }
    }

    @FXML
    private void handleReverseToReject() {
        if (selectedRow == null) return;
        try {
            UserAccount actor = resolveActorAccount();
            leaveService.reverseApprovedLeave(actor, selectedRow.getLeaveRequestId(), RequestStatus.REJECTED);
            UiMessageUtil.setSuccess(moduleMessageLabel,
                    "Approved leave reversed to rejected. Credits restored.");
            loadRows();
            showList();
        } catch (Exception e) {
            UiMessageUtil.setError(moduleMessageLabel, userMessage(e));
        }
    }

    @FXML
    private void handleReverseToCancel() {
        if (selectedRow == null) return;
        try {
            UserAccount actor = resolveActorAccount();
            leaveService.reverseApprovedLeave(actor, selectedRow.getLeaveRequestId(), RequestStatus.CANCELLED);
            UiMessageUtil.setSuccess(moduleMessageLabel,
                    "Approved leave reversed to cancelled. Credits restored.");
            loadRows();
            showList();
        } catch (Exception e) {
            UiMessageUtil.setError(moduleMessageLabel, userMessage(e));
        }
    }

    @FXML
    private void handleViewCredits() {
        if (selectedRow != null) showCredits(selectedRow);
    }

    private void loadRows() {
        try {
            allRows = leaveService.getLeaveRequestRows();
        } catch (Exception e) {
            allRows = new ArrayList<>();
            UiMessageUtil.setError(moduleMessageLabel, userMessage(e));
        }
        leaveItems.setAll(allRows);
    }

    private void setupFilters() {
        if (statusFilterCombo != null) {
            statusFilterCombo.getItems().setAll(
                    "All Statuses", "PENDING", "APPROVED", "REJECTED", "CANCELLED");
            statusFilterCombo.setValue("All Statuses");
            statusFilterCombo.setOnAction(e -> applyFilters());
        }
        if (leaveTypeFilterCombo != null) {
            leaveTypeFilterCombo.getItems().setAll(
                    "All Types", "Vacation Leave", "Sick Leave", "Emergency Leave",
                    "Maternity Leave", "Paternity Leave", "Solo Parent Leave", "Unpaid Leave");
            leaveTypeFilterCombo.setValue("All Types");
            leaveTypeFilterCombo.setOnAction(e -> applyFilters());
        }
        if (employeeSearchField != null) {
            employeeSearchField.textProperty().addListener((obs, old, value) -> applyFilters());
        }
    }

    private void applyFilters() {
        if (filteredLeaveItems == null) return;

        String statusVal = statusFilterCombo != null ? statusFilterCombo.getValue() : "All Statuses";
        String typeVal = leaveTypeFilterCombo != null ? leaveTypeFilterCombo.getValue() : "All Types";
        String search = employeeSearchField != null
                ? employeeSearchField.getText().trim().toLowerCase()
                : "";

        filteredLeaveItems.setPredicate(row -> {
            boolean statusMatches = "All Statuses".equals(statusVal)
                    || row.getStatus().name().equals(statusVal);
            boolean typeMatches = "All Types".equals(typeVal)
                    || typeVal.equals(row.getLeaveTypeName());
            boolean employeeMatches = search.isEmpty()
                    || row.getEmployeeFullName().toLowerCase().contains(search)
                    || String.valueOf(row.getEmployeeId()).contains(search);

            return statusMatches && typeMatches && employeeMatches;
        });
    }

    private void populateDetail(LeaveRequestRowDTO row) {
        setText(detailIdLabel, "#" + row.getLeaveRequestId());
        setText(detailEmployeeLabel, row.getEmployeeFullName() + " (ID: " + row.getEmployeeId() + ")");
        setText(detailLeaveTypeLabel, row.getLeaveTypeName());
        setText(detailStatusLabel, row.getStatus().name());
        setText(detailPeriodLabel, row.getStartDate().format(DATE_FMT) + " - " + row.getEndDate().format(DATE_FMT));
        setText(detailHoursLabel, String.format("%.1f hrs", row.getLeaveHours()));
        setText(detailReasonLabel, row.getReason());
        setText(detailSubmittedLabel, row.getDateSubmitted() != null
                ? row.getDateSubmitted().format(DATETIME_FMT)
                : "-");
        setText(detailApproverLabel, row.getApproverAccountId() != null
                ? "Account #" + row.getApproverAccountId()
                : "-");
        setText(detailReviewedLabel, row.getDateReviewed() != null
                ? row.getDateReviewed().format(DATETIME_FMT)
                : "-");
        setText(detailRemarksLabel, row.getRemarks() != null && !row.getRemarks().isBlank()
                ? row.getRemarks()
                : "-");

        boolean isSelf = CurrentUserSession.getEmployeeId() == row.getEmployeeId();
        RequestStatus status = row.getStatus();

        setLabelVisible(detailSelfNoteLabel, isSelf);
        setButtonVisible(approveButton, status == RequestStatus.PENDING && !isSelf);
        setButtonVisible(rejectButton, status == RequestStatus.PENDING && !isSelf);
        setButtonVisible(reverseToRejectButton, status == RequestStatus.APPROVED && !isSelf);
        setButtonVisible(reverseToCancelButton, status == RequestStatus.APPROVED && !isSelf);
        setButtonVisible(viewCreditsButton, true);
    }

    private void populateCredits(LeaveRequestRowDTO row) {
        if (creditsHeaderLabel != null) {
            creditsHeaderLabel.setText("Leave Credits - " + row.getEmployeeFullName()
                    + " (" + LocalDate.now().getYear() + ")");
        }

        try {
            creditItems.setAll(leaveService.getLeaveCreditsForEmployee(row.getEmployeeId()));
        } catch (Exception e) {
            creditItems.clear();
            UiMessageUtil.setError(moduleMessageLabel, userMessage(e));
        }
    }

    private void setupLeaveTable() {
        leaveTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        leaveIdColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getLeaveRequestId()));
        leaveEmployeeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEmployeeFullName()));
        leaveTypeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getLeaveTypeName()));
        leavePeriodColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getStartDate()));
        leavePeriodColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate value, boolean empty) {
                super.updateItem(value, empty);
                LeaveRequestRowDTO row = empty ? null : getTableRow().getItem();
                setText(row == null ? null : row.getStartDate().format(DATE_FMT)
                        + " - " + row.getEndDate().format(DATE_FMT));
            }
        });
        leaveHoursColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getLeaveHours()));
        leaveHoursColumn.setCellFactory(column -> hoursCell());
        leaveStatusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStatus().name()));
        leaveStatusColumn.setCellFactory(column -> statusCell());
        leaveSubmittedColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getDateSubmitted()));
        leaveSubmittedColumn.setCellFactory(column -> dateTimeCell());
        leaveActionColumn.setSortable(false);
        leaveActionColumn.setCellFactory(column -> new TableCell<>() {
            private final Button viewButton = new Button("View");

            {
                viewButton.getStyleClass().add("table-action-button");
                viewButton.setOnAction(event -> {
                    LeaveRequestRowDTO row = getTableRow().getItem();
                    if (row != null) {
                        showDetail(row);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER_LEFT);
                setGraphic(empty ? null : viewButton);
            }
        });

        leaveTable.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(LeaveRequestRowDTO row, boolean empty) {
                super.updateItem(row, empty);
                boolean selected = !empty && row != null && row == selectedRow;
                setStyle(selected ? "-fx-background-color: rgba(59,130,246,0.10);" : "");
            }
        });

        filteredLeaveItems = new FilteredList<>(leaveItems, row -> true);
        SortedList<LeaveRequestRowDTO> sortedRows = new SortedList<>(filteredLeaveItems);
        sortedRows.comparatorProperty().bind(leaveTable.comparatorProperty());
        leaveTable.setItems(sortedRows);
    }

    private void setupCreditTable() {
        creditTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        creditTypeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getLeaveTypeName()));
        creditTotalColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getTotalCredits()));
        creditTotalColumn.setCellFactory(column -> daysCell());
        creditUsedColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getUsedCredits()));
        creditUsedColumn.setCellFactory(column -> daysCell());
        creditRemainingColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getRemainingCredits()));
        creditRemainingColumn.setCellFactory(column -> daysCell());
        creditTable.setItems(creditItems);
    }

    private TableCell<LeaveRequestRowDTO, String> statusCell() {
        return new TableCell<>() {
            private final Label badge = new Label();

            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setGraphic(null);
                    return;
                }

                badge.setText(value);
                badge.getStyleClass().removeAll("status-badge", "status-badge-inactive");
                badge.getStyleClass().add("APPROVED".equals(value) || "PENDING".equals(value)
                        ? "status-badge"
                        : "status-badge-inactive");
                setAlignment(Pos.CENTER_LEFT);
                setGraphic(badge);
            }
        };
    }

    private TableCell<LeaveRequestRowDTO, Number> hoursCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                setAlignment(Pos.CENTER_RIGHT);
                setText(empty || value == null ? null : String.format("%.1f", value.doubleValue()));
            }
        };
    }

    private TableCell<LeaveCreditRowDTO, Number> daysCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                setAlignment(Pos.CENTER_RIGHT);
                setText(empty || value == null ? null : String.format("%.1f days", value.doubleValue()));
            }
        };
    }

    private TableCell<LeaveRequestRowDTO, LocalDateTime> dateTimeCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : value.format(DATE_FMT));
            }
        };
    }

    private UserAccount resolveActorAccount() {
        int accountId = CurrentUserSession.getUserAccountId();
        if (accountId <= 0) {
            throw new IllegalStateException("No active session found. Please log in again.");
        }
        return userAccountService.getById(accountId);
    }

    private void setText(Label label, String text) {
        if (label != null) label.setText(text != null ? text : "-");
    }

    private void setPanelVisible(VBox panel, boolean visible) {
        if (panel != null) {
            panel.setVisible(visible);
            panel.setManaged(visible);
        }
    }

    private void setLabelVisible(Label label, boolean visible) {
        if (label != null) {
            label.setVisible(visible);
            label.setManaged(visible);
        }
    }

    private void setButtonVisible(Button button, boolean visible) {
        if (button != null) {
            button.setVisible(visible);
            button.setManaged(visible);
        }
    }

    private void setNavVisible(boolean showBack) {
        if (backButton != null) {
            backButton.setVisible(showBack);
            backButton.setManaged(showBack);
        }
    }

    private void setFilterVisible(boolean visible) {
        setNodeVisible(employeeSearchField, visible);
        setNodeVisible(statusFilterCombo, visible);
        setNodeVisible(leaveTypeFilterCombo, visible);
    }

    private void setNodeVisible(javafx.scene.Node node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    private String userMessage(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) return "Unable to complete the request. Please try again.";
        if (isDatabaseUnavailable(e)) return "Database connection unavailable. Start MySQL and try again.";
        return msg;
    }

    private boolean isDatabaseUnavailable(Exception e) {
        Throwable t = e;
        while (t != null) {
            String msg = t.getMessage();
            if (msg != null && (msg.contains("Failed to connect to the database")
                    || msg.contains("Communications link failure")
                    || msg.contains("Connection refused"))) return true;
            t = t.getCause();
        }
        return false;
    }
}
