package motorph.ui.hr;

import motorph.api.UserAccountApiClient;
import motorph.domain.value.RequestStatus;
import motorph.api.ApiClientFactory;
import motorph.api.EmployeeApiClient;
import motorph.api.LeaveApiClient;
import motorph.api.OvertimeApiClient;
import motorph.api.dto.employee.EmployeeProfileDTO;
import motorph.api.dto.leave.LeaveCreditRowDTO;
import motorph.api.dto.leave.LeaveRequestRowDTO;
import motorph.api.dto.overtime.OvertimeRequestDTO;
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
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Manages the HR Leave & Overtime module: request list, detail view, and employee credits panel. */
public class HrLeaveController {

    private static final Logger logger = Logger.getLogger(HrLeaveController.class.getName());

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    private LeaveApiClient leaveApiClient;
    private OvertimeApiClient overtimeApiClient;
    private UserAccountApiClient userAccountApiClient;
    private EmployeeApiClient employeeApiClient;

    private List<LeaveRequestRowDTO> allRows = new ArrayList<>();
    private final ObservableList<LeaveRequestRowDTO> leaveItems = FXCollections.observableArrayList();
    private final ObservableList<LeaveCreditRowDTO> creditItems = FXCollections.observableArrayList();
    private FilteredList<LeaveRequestRowDTO> filteredLeaveItems;
    private LeaveRequestRowDTO selectedRow;

    private final ObservableList<OvertimeRow> overtimeItems = FXCollections.observableArrayList();
    private FilteredList<OvertimeRow> filteredOvertimeItems;
    private OvertimeRow selectedOvertimeRow;
    private boolean overtimeSegmentActive;

    @FXML private Button backButton;
    @FXML private Label moduleMessageLabel;
    @FXML private StackPane toastRoot;
    @FXML private TabPane segmentTabPane;
    @FXML private Tab leaveTab;
    @FXML private Tab overtimeTab;

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
    @FXML private Label detailSelfNoteLabel;
    @FXML private Button approveButton;
    @FXML private Button rejectButton;
    @FXML private Button viewCreditsButton;

    @FXML private VBox creditsPanel;
    @FXML private Label creditsHeaderLabel;
    @FXML private TableView<LeaveCreditRowDTO> creditTable;
    @FXML private TableColumn<LeaveCreditRowDTO, String> creditTypeColumn;
    @FXML private TableColumn<LeaveCreditRowDTO, Number> creditTotalColumn;
    @FXML private TableColumn<LeaveCreditRowDTO, Number> creditUsedColumn;
    @FXML private TableColumn<LeaveCreditRowDTO, Number> creditRemainingColumn;

    @FXML private VBox overtimeListPanel;
    @FXML private TableView<OvertimeRow> overtimeTable;
    @FXML private TableColumn<OvertimeRow, Number> otIdColumn;
    @FXML private TableColumn<OvertimeRow, String> otEmployeeColumn;
    @FXML private TableColumn<OvertimeRow, String> otDepartmentColumn;
    @FXML private TableColumn<OvertimeRow, LocalDate> otDateColumn;
    @FXML private TableColumn<OvertimeRow, String> otTypeColumn;
    @FXML private TableColumn<OvertimeRow, Number> otHoursColumn;
    @FXML private TableColumn<OvertimeRow, String> otStatusColumn;
    @FXML private TableColumn<OvertimeRow, Void> otActionColumn;

    @FXML private VBox overtimeDetailPanel;
    @FXML private Label otDetailIdLabel;
    @FXML private Label otDetailEmployeeLabel;
    @FXML private Label otDetailDepartmentLabel;
    @FXML private Label otDetailTypeLabel;
    @FXML private Label otDetailStatusLabel;
    @FXML private Label otDetailDateLabel;
    @FXML private Label otDetailHoursLabel;
    @FXML private Label otDetailReasonLabel;
    @FXML private Label otDetailSubmittedLabel;
    @FXML private Label otDetailApproverLabel;
    @FXML private Label otDetailReviewedLabel;
    @FXML private Label otDetailSelfNoteLabel;
    @FXML private Button otApproveButton;
    @FXML private Button otRejectButton;

    @FXML
    private void initialize() {
        leaveApiClient = ApiClientFactory.getInstance().getLeaveApiClient();
        overtimeApiClient = ApiClientFactory.getInstance().getOvertimeApiClient();
        userAccountApiClient = ApiClientFactory.getInstance().getUserAccountApiClient();
        employeeApiClient = ApiClientFactory.getInstance().getEmployeeApiClient();
        setupFilters();
        setupLeaveTable();
        setupCreditTable();
        setupOvertimeTable();
        setupSegmentTabs();
        showLeaveSegment();
    }

    // keeps the tab header as a selector only and routes selection to the matching segment
    private void setupSegmentTabs() {
        if (segmentTabPane == null) return;

        // tab content lives in the StackPane below, so cap the pane to its header height
        segmentTabPane.setMaxHeight(42);

        segmentTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == overtimeTab) {
                showOvertimeSegment();
            } else {
                showLeaveSegment();
            }
        });
    }

    /** Refreshes the active segment's request list when the nav button is clicked. */
    public void refresh() {
        if (overtimeSegmentActive) {
            loadOvertimeRows();
            applyFilters();
        } else {
            loadRows();
            applyFilters();
        }
    }

    // switches to the leave requests segment
    private void showLeaveSegment() {
        overtimeSegmentActive = false;
        setOvertimePanelVisible(overtimeListPanel, false);
        setOvertimePanelVisible(overtimeDetailPanel, false);
        setNodeVisible(leaveTypeFilterCombo, true);
        showList();
    }

    // switches to the overtime requests segment
    private void showOvertimeSegment() {
        overtimeSegmentActive = true;
        showOvertimeList();
    }

    @FXML
    private void handleBack() {
        if (overtimeSegmentActive) {
            showOvertimeList();
        } else if (creditsPanel != null && creditsPanel.isVisible()) {
            showDetail(selectedRow);
        } else {
            showList();
        }
    }

    private void showList() {
        setPanelVisible(listPanel, true);
        setPanelVisible(detailPanel, false);
        setPanelVisible(creditsPanel, false);
        setOvertimePanelVisible(overtimeListPanel, false);
        setOvertimePanelVisible(overtimeDetailPanel, false);
        setNavVisible(false);
        setFilterVisible(true);
        setNodeVisible(leaveTypeFilterCombo, true);
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
            leaveApiClient.approveLeaveRequest(selectedRow.getLeaveRequestId());
            loadRows();
            showList();
            UiMessageUtil.showSuccessToastTop(toastRoot, "Leave request approved.");
        } catch (Exception e) {
            UiMessageUtil.showErrorToastTop(toastRoot, userMessage(e));
        }
    }

    @FXML
    private void handleReject() {
        if (selectedRow == null) return;
        try {
            leaveApiClient.rejectLeaveRequest(selectedRow.getLeaveRequestId());
            loadRows();
            showList();
            UiMessageUtil.showSuccessToastTop(toastRoot, "Leave request rejected.");
        } catch (Exception e) {
            UiMessageUtil.showErrorToastTop(toastRoot, userMessage(e));
        }
    }

    @FXML
    private void handleViewCredits() {
        if (selectedRow != null) showCredits(selectedRow);
    }

    private void loadRows() {
        try {
            allRows = leaveApiClient.getAllLeaveRequests();
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
        if (overtimeSegmentActive) {
            applyOvertimeFilters();
            return;
        }

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

    // filters the overtime list by status and employee search (leave type does not apply)
    private void applyOvertimeFilters() {
        if (filteredOvertimeItems == null) return;

        String statusVal = statusFilterCombo != null ? statusFilterCombo.getValue() : "All Statuses";
        String search = employeeSearchField != null
                ? employeeSearchField.getText().trim().toLowerCase()
                : "";

        filteredOvertimeItems.setPredicate(row -> {
            boolean statusMatches = "All Statuses".equals(statusVal)
                    || row.statusName().equals(statusVal);
            boolean employeeMatches = search.isEmpty()
                    || row.employeeName().toLowerCase().contains(search)
                    || String.valueOf(row.employeeId()).contains(search);

            return statusMatches && employeeMatches;
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
        setText(detailApproverLabel, resolveReviewerName(row.getApproverAccountId()));
        setText(detailReviewedLabel, row.getDateReviewed() != null
                ? row.getDateReviewed().format(DATETIME_FMT)
                : "-");

        boolean isSelf = CurrentUserSession.getEmployeeId() == row.getEmployeeId();
        RequestStatus status = row.getStatus();

        // only pending requests are actionable; reviewed requests are read-only
        boolean actionable = status == RequestStatus.PENDING && !isSelf;
        setLabelVisible(detailSelfNoteLabel, isSelf && status == RequestStatus.PENDING);
        setButtonVisible(approveButton, actionable);
        setButtonVisible(rejectButton, actionable);
        setButtonVisible(viewCreditsButton, true);
    }

    // resolves the reviewer's full name from the approver account id, or "-" when not reviewed
    private String resolveReviewerName(Integer approverAccountId) {
        if (approverAccountId == null) return "-";
        try {
            return userAccountApiClient.getAccountSummary(approverAccountId).fullName();
        } catch (Exception e) {
            return "Account #" + approverAccountId;
        }
    }

    private void populateCredits(LeaveRequestRowDTO row) {
        if (creditsHeaderLabel != null) {
            creditsHeaderLabel.setText("Leave Credits - " + row.getEmployeeFullName()
                    + " (" + LocalDate.now().getYear() + ")");
        }

        try {
            creditItems.setAll(leaveApiClient.getLeaveCredits(row.getEmployeeId()));
        } catch (Exception e) {
            creditItems.clear();
            UiMessageUtil.setError(moduleMessageLabel, userMessage(e));
        }
    }

    // shows the overtime list and hides the leave and overtime detail panels
    private void showOvertimeList() {
        setPanelVisible(listPanel, false);
        setPanelVisible(detailPanel, false);
        setPanelVisible(creditsPanel, false);
        setOvertimePanelVisible(overtimeListPanel, true);
        setOvertimePanelVisible(overtimeDetailPanel, false);
        setNavVisible(false);
        setFilterVisible(true);
        setNodeVisible(leaveTypeFilterCombo, false);
        loadOvertimeRows();
        applyFilters();
    }

    // shows the overtime detail panel for the selected request
    private void showOvertimeDetail(OvertimeRow row) {
        if (row == null) {
            showOvertimeList();
            return;
        }

        selectedOvertimeRow = row;
        setPanelVisible(listPanel, false);
        setPanelVisible(detailPanel, false);
        setPanelVisible(creditsPanel, false);
        setOvertimePanelVisible(overtimeListPanel, false);
        setOvertimePanelVisible(overtimeDetailPanel, true);
        setNavVisible(true);
        setFilterVisible(false);
        setNodeVisible(leaveTypeFilterCombo, false);
        populateOvertimeDetail(row);
    }

    // loads all overtime requests and enriches each with employee name and department
    private void loadOvertimeRows() {
        List<OvertimeRow> rows = new ArrayList<>();
        try {
            for (OvertimeRequestDTO request : overtimeApiClient.getAllOvertimeRequests()) {
                String employeeName = "Employee #" + request.getEmployeeId();
                String department = "-";
                try {
                    EmployeeProfileDTO profile = employeeApiClient.getEmployee(request.getEmployeeId());
                    if (profile != null) {
                        employeeName = profile.getFullName();
                        department = profile.getDepartmentName() != null ? profile.getDepartmentName() : "-";
                    }
                } catch (Exception e) {
                    // falls back to placeholders when the profile cannot be resolved
                    logger.log(Level.FINE, "Could not resolve employee profile for overtime row", e);
                }
                rows.add(new OvertimeRow(request, employeeName, department,
                        resolveOvertimeTypeName(request.getOvertimeTypeId())));
            }
        } catch (Exception e) {
            UiMessageUtil.setError(moduleMessageLabel, userMessage(e));
        }
        overtimeItems.setAll(rows);
    }

    @FXML
    private void handleApproveOvertime() {
        if (selectedOvertimeRow == null) return;
        try {
            overtimeApiClient.approveOvertimeRequest(selectedOvertimeRow.requestId());
            loadOvertimeRows();
            showOvertimeList();
            UiMessageUtil.showSuccessToastTop(toastRoot, "Overtime request approved.");
        } catch (Exception e) {
            UiMessageUtil.showErrorToastTop(toastRoot, userMessage(e));
        }
    }

    @FXML
    private void handleRejectOvertime() {
        if (selectedOvertimeRow == null) return;
        try {
            overtimeApiClient.rejectOvertimeRequest(selectedOvertimeRow.requestId());
            loadOvertimeRows();
            showOvertimeList();
            UiMessageUtil.showSuccessToastTop(toastRoot, "Overtime request rejected.");
        } catch (Exception e) {
            UiMessageUtil.showErrorToastTop(toastRoot, userMessage(e));
        }
    }

    private void populateOvertimeDetail(OvertimeRow row) {
        setText(otDetailIdLabel, "#" + row.requestId());
        setText(otDetailEmployeeLabel, row.employeeName() + " (ID: " + row.employeeId() + ")");
        setText(otDetailDepartmentLabel, row.department());
        setText(otDetailTypeLabel, row.overtimeTypeName());
        setText(otDetailStatusLabel, row.statusName());
        setText(otDetailDateLabel, row.workDate().format(DATE_FMT));
        setText(otDetailHoursLabel, String.format("%.1f hrs", row.requestedHours()));
        setText(otDetailReasonLabel, row.reason());
        setText(otDetailSubmittedLabel, row.dateSubmitted() != null
                ? row.dateSubmitted().format(DATETIME_FMT)
                : "-");
        setText(otDetailApproverLabel, resolveReviewerName(row.approverAccountId()));
        setText(otDetailReviewedLabel, row.dateReviewed() != null
                ? row.dateReviewed().format(DATETIME_FMT)
                : "-");

        boolean isSelf = CurrentUserSession.getEmployeeId() == row.employeeId();
        RequestStatus status = row.status();

        // only pending requests are actionable; reviewed requests are read-only
        boolean actionable = status == RequestStatus.PENDING && !isSelf;
        setLabelVisible(otDetailSelfNoteLabel, isSelf && status == RequestStatus.PENDING);
        setButtonVisible(otApproveButton, actionable);
        setButtonVisible(otRejectButton, actionable);
    }

    private void setupOvertimeTable() {
        overtimeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        otIdColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().requestId()));
        otEmployeeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().employeeName()));
        otDepartmentColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().department()));
        otDateColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().workDate()));
        otDateColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : value.format(DATE_FMT));
            }
        });
        otTypeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().overtimeTypeName()));
        otHoursColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().requestedHours()));
        otHoursColumn.setCellFactory(column -> overtimeHoursCell());
        otStatusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().statusName()));
        otStatusColumn.setCellFactory(column -> overtimeStatusCell());
        otActionColumn.setSortable(false);
        otActionColumn.setCellFactory(column -> new TableCell<>() {
            private final Button viewButton = new Button("View");

            {
                viewButton.getStyleClass().add("table-action-button");
                viewButton.setOnAction(event -> {
                    OvertimeRow row = getTableRow().getItem();
                    if (row != null) {
                        showOvertimeDetail(row);
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

        overtimeTable.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(OvertimeRow row, boolean empty) {
                super.updateItem(row, empty);
                boolean selected = !empty && row != null && row == selectedOvertimeRow;
                setStyle(selected ? "-fx-background-color: rgba(59,130,246,0.10);" : "");
            }
        });

        filteredOvertimeItems = new FilteredList<>(overtimeItems, row -> true);
        SortedList<OvertimeRow> sortedRows = new SortedList<>(filteredOvertimeItems);
        sortedRows.comparatorProperty().bind(overtimeTable.comparatorProperty());
        overtimeTable.setItems(sortedRows);
    }

    private TableCell<OvertimeRow, String> overtimeStatusCell() {
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

    private TableCell<OvertimeRow, Number> overtimeHoursCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                setAlignment(Pos.CENTER_RIGHT);
                setText(empty || value == null ? null : String.format("%.1f", value.doubleValue()));
            }
        };
    }

    // toggles an overtime panel's visibility
    private void setOvertimePanelVisible(VBox panel, boolean visible) {
        if (panel != null) {
            panel.setVisible(visible);
            panel.setManaged(visible);
        }
    }

    // resolves the overtime type name from the seeded overtime type id
    private String resolveOvertimeTypeName(int overtimeTypeId) {
        return switch (overtimeTypeId) {
            case 1 -> "Regular OT";
            case 2 -> "Rest Day OT";
            case 3 -> "Special Holiday OT";
            case 4 -> "Regular Holiday OT";
            case 5 -> "Night Differential OT";
            default -> "Overtime Type " + overtimeTypeId;
        };
    }

    private void setupLeaveTable() {
        leaveTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
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
        creditTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
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

    // holds an overtime request enriched with employee name, department, and type name for display
    private record OvertimeRow(OvertimeRequestDTO request, String employeeName, String department, String overtimeTypeName) {

        // getters/setters
        int requestId() {
            return request.getRequestId();
        }

        int employeeId() {
            return request.getEmployeeId();
        }

        LocalDate workDate() {
            return request.getOvertimeDate();
        }

        double requestedHours() {
            return request.getRequestedHours();
        }

        String reason() {
            return request.getReason();
        }

        RequestStatus status() {
            return request.getStatus();
        }

        String statusName() {
            return request.getStatus().name();
        }

        java.time.LocalDateTime dateSubmitted() {
            return request.getRequestDate();
        }

        java.time.LocalDateTime dateReviewed() {
            return request.getDateReviewed();
        }

        Integer approverAccountId() {
            return request.getApproverAccountId();
        }
    }
}
