package motorph.ui.employee.leave;

import motorph.application.service.LeaveService;
import motorph.domain.model.Employee;
import motorph.domain.model.LeaveRequest;
import motorph.domain.value.Gender;
import motorph.domain.value.RequestStatus;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.util.UiMessageUtil;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Handles leave request submission, cancellation, credit display, and history rendering. */
public class EmployeeLeaveController {

    private static final int REASON_LIMIT = 255;
    private static final int PAGE_SIZE = 10;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy hh:mm a");

    private LeaveService leaveService;
    private Consumer<String> onLeaveCreditsUpdated;

    private final Map<String, Integer> leaveTypeIds = new LinkedHashMap<>();
    private final List<LeaveHistoryRow> allHistoryRows = new ArrayList<>();
    private final List<LeaveHistoryRow> filteredHistoryRows = new ArrayList<>();
    private int currentLeavePage = 1;

    @FXML private HBox leaveCreditsBox;
    @FXML private ComboBox<String> leaveTypeComboBox;
    @FXML private DatePicker leaveStartDatePicker;
    @FXML private DatePicker leaveEndDatePicker;
    @FXML private TextField leaveHoursField;
    @FXML private TextArea leaveReasonArea;
    @FXML private Label leaveTypeErrorLabel;
    @FXML private Label fromDateErrorLabel;
    @FXML private Label toDateErrorLabel;
    @FXML private Label leaveHoursErrorLabel;
    @FXML private Label reasonErrorLabel;
    @FXML private Label reasonCounterLabel;
    @FXML private Label leaveHistoryLabel;
    @FXML private Label leaveMessageLabel;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private TableView<LeaveHistoryRow> leaveHistoryTable;
    @FXML private TableColumn<LeaveHistoryRow, String> colRequestId;
    @FXML private TableColumn<LeaveHistoryRow, String> colLeaveType;
    @FXML private TableColumn<LeaveHistoryRow, String> colFromDate;
    @FXML private TableColumn<LeaveHistoryRow, String> colToDate;
    @FXML private TableColumn<LeaveHistoryRow, String> colTotalHours;
    @FXML private TableColumn<LeaveHistoryRow, String> colStatus;
    @FXML private TableColumn<LeaveHistoryRow, String> colDateRequested;
    @FXML private TableColumn<LeaveHistoryRow, Void> colAction;
    @FXML private Label leaveRecordInfoLabel;
    @FXML private Label leavePageLabel;
    @FXML private Button leavePrevButton;
    @FXML private Button leaveNextButton;

    @FXML
    private void initialize() {
        leaveService = EmployeeUIServiceFactory.getInstance().getLeaveService();

        loadLeaveTypesForCurrentEmployee();
        leaveTypeComboBox.getItems().setAll(leaveTypeIds.keySet());
        leaveTypeComboBox.setOnAction(e -> {
            clearFieldError(leaveTypeErrorLabel, leaveTypeComboBox);
            UiMessageUtil.clearMessage(leaveMessageLabel);
            renderLeaveCredits();
        });

        leaveHoursField.setEditable(false);
        configureDatePickers();
        configureReasonField();
        configureHistoryTable();

        statusFilterComboBox.getItems().setAll("All Status", "Pending", "Approved", "Rejected", "Cancelled");
        statusFilterComboBox.setValue("All Status");
        statusFilterComboBox.setOnAction(e -> {
            currentLeavePage = 1;
            renderHistoryRows();
        });

        renderLeaveCredits();
    }

    // reloads credits and history when the view becomes active
    public void refresh() {
        renderLeaveCredits();
        refreshLeaveHistory();
    }

    // clears the leave request form and validation state
    public void resetForm() {
        leaveTypeComboBox.getSelectionModel().clearSelection();
        leaveStartDatePicker.setValue(null);
        leaveEndDatePicker.setValue(null);
        leaveHoursField.clear();
        leaveReasonArea.clear();
        updateReasonCounter();
        clearAllFieldErrors();
        UiMessageUtil.clearMessage(leaveMessageLabel);

        if (statusFilterComboBox != null) {
            statusFilterComboBox.setValue("All Status");
        }
    }

    @FXML
    private void handleClearLeaveRequest() {
        resetForm();
        renderLeaveCredits();
    }

    @FXML
    private void handleSubmitLeaveRequest() {
        int employeeId = CurrentUserSession.getEmployeeId();
        if (employeeId <= 0) {
            UiMessageUtil.setError(leaveMessageLabel, "Please log in before using this action.");
            return;
        }

        UiMessageUtil.clearMessage(leaveMessageLabel);
        if (!validateForm()) {
            return;
        }

        try {
            int leaveTypeId = leaveTypeIds.get(leaveTypeComboBox.getValue());
            LocalDate startDate = leaveStartDatePicker.getValue();
            LocalDate endDate = leaveEndDatePicker.getValue();
            double hours = Double.parseDouble(leaveHoursField.getText().trim());

            leaveService.submitLeaveRequest(
                    employeeId,
                    leaveTypeId,
                    startDate,
                    endDate,
                    hours,
                    getReasonText()
            );

            clearFormFields();
            clearAllFieldErrors();
            UiMessageUtil.setSuccess(leaveMessageLabel, "Leave request submitted.");
            refresh();
        } catch (Exception e) {
            showServiceError(e);
        }
    }

    // stores a callback invoked when leave credits change, used by the dashboard.
    public void setOnLeaveCreditsUpdated(Consumer<String> callback) {
        this.onLeaveCreditsUpdated = callback;
    }

    private void configureDatePickers() {
        leaveStartDatePicker.setEditable(false);
        leaveEndDatePicker.setEditable(false);
        leaveStartDatePicker.addEventFilter(KeyEvent.KEY_TYPED, KeyEvent::consume);
        leaveEndDatePicker.addEventFilter(KeyEvent.KEY_TYPED, KeyEvent::consume);

        leaveStartDatePicker.setDayCellFactory(dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(!empty && date.isBefore(LocalDate.now()));
            }
        });

        leaveEndDatePicker.setDayCellFactory(dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                LocalDate startDate = leaveStartDatePicker.getValue();
                setDisable(!empty && (date.isBefore(LocalDate.now())
                        || (startDate != null && date.isBefore(startDate))));
            }
        });

        leaveStartDatePicker.setOnAction(e -> {
            clearFieldError(fromDateErrorLabel, leaveStartDatePicker);
            updateLeaveHours();
            UiMessageUtil.clearMessage(leaveMessageLabel);
        });

        leaveEndDatePicker.setOnAction(e -> {
            clearFieldError(toDateErrorLabel, leaveEndDatePicker);
            updateLeaveHours();
            UiMessageUtil.clearMessage(leaveMessageLabel);
        });
    }

    private void configureReasonField() {
        leaveReasonArea.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && newValue.length() > REASON_LIMIT) {
                leaveReasonArea.setText(oldValue);
                return;
            }
            clearFieldError(reasonErrorLabel, leaveReasonArea);
            UiMessageUtil.clearMessage(leaveMessageLabel);
            updateReasonCounter();
        });
        updateReasonCounter();
    }

    private void configureHistoryTable() {
        leaveHistoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colRequestId.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().requestId()));
        colLeaveType.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().leaveType()));
        colFromDate.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().fromDate()));
        colToDate.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().toDate()));
        colTotalHours.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().totalHours()));
        colStatus.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().statusText()));
        colDateRequested.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().dateRequested()));

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                getStyleClass().removeAll("leave-status-pending", "leave-status-approved",
                        "leave-status-rejected", "leave-status-cancelled");
                if (empty || status == null) {
                    setText(null);
                } else {
                    setText(status);
                    getStyleClass().add("leave-status-" + status.toLowerCase());
                }
            }
        });

        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button cancelButton = new Button("Cancel");
            {
                cancelButton.getStyleClass().add("leave-cancel-button");
                cancelButton.setOnAction(event -> {
                    LeaveHistoryRow row = getTableView().getItems().get(getIndex());
                    handleCancelLeaveRequest(row);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER_LEFT);
                LeaveHistoryRow row = empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()
                        ? null
                        : getTableView().getItems().get(getIndex());
                setGraphic(row != null && row.status() == RequestStatus.PENDING ? cancelButton : null);
                setText(row != null && row.status() != RequestStatus.PENDING ? "-" : null);
            }
        });
    }

    // loads leave types based on employee gender and employment status
    private void loadLeaveTypesForCurrentEmployee() {
        leaveTypeIds.clear();

        int employeeId = CurrentUserSession.getEmployeeId();
        if (employeeId <= 0) {
            leaveTypeIds.put("Unpaid Leave", 7);
            return;
        }

        Employee employee = leaveService.getEmployeeById(employeeId);
        if (employee.isProbationary()) {
            leaveTypeIds.put("Unpaid Leave", 7);
            return;
        }

        leaveTypeIds.put("Vacation Leave", 1);
        leaveTypeIds.put("Sick Leave", 2);
        leaveTypeIds.put("Emergency Leave", 3);

        if (employee.getGender() == Gender.FEMALE) {
            leaveTypeIds.put("Maternity Leave", 4);
        }
        if (employee.getGender() == Gender.MALE) {
            leaveTypeIds.put("Paternity Leave", 5);
        }

        leaveTypeIds.put("Solo Parent Leave", 6);
        leaveTypeIds.put("Unpaid Leave", 7);
    }

    // updates leave hours using the existing service computation
    private void updateLeaveHours() {
        LocalDate startDate = leaveStartDatePicker.getValue();
        LocalDate endDate = leaveEndDatePicker.getValue();

        if (startDate == null || endDate == null) {
            leaveHoursField.clear();
            return;
        }

        try {
            double hours = leaveService.computeLeaveHours(startDate, endDate);
            leaveHoursField.setText(String.format("%.1f", hours));
            clearFieldError(leaveHoursErrorLabel, leaveHoursField);
        } catch (Exception e) {
            leaveHoursField.clear();
            showFieldError(leaveHoursErrorLabel, leaveHoursField, userMessage(e));
        }
    }

    private boolean validateForm() {
        clearAllFieldErrors();

        boolean valid = true;

        if (leaveTypeIds.get(leaveTypeComboBox.getValue()) == null) {
            showFieldError(leaveTypeErrorLabel, leaveTypeComboBox, "Required");
            valid = false;
        }

        LocalDate startDate = leaveStartDatePicker.getValue();
        LocalDate endDate = leaveEndDatePicker.getValue();
        if (startDate == null) {
            showFieldError(fromDateErrorLabel, leaveStartDatePicker, "Required");
            valid = false;
        }
        if (endDate == null) {
            showFieldError(toDateErrorLabel, leaveEndDatePicker, "Required");
            valid = false;
        }
        if (startDate != null && endDate != null) {
            updateLeaveHours();
            if (endDate.isBefore(startDate)) {
                showFieldError(toDateErrorLabel, leaveEndDatePicker, "Must be after From Date");
                valid = false;
            }
        }

        double hours = parseLeaveHours();
        if (hours <= 0) {
            showFieldError(leaveHoursErrorLabel, leaveHoursField, "Invalid hours");
            valid = false;
        }

        if (getReasonText().isBlank()) {
            showFieldError(reasonErrorLabel, leaveReasonArea, "Required");
            valid = false;
        }

        return valid;
    }

    private double parseLeaveHours() {
        try {
            return Double.parseDouble(leaveHoursField.getText() == null ? "" : leaveHoursField.getText().trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private void renderLeaveCredits() {
        if (leaveCreditsBox == null) return;
        leaveCreditsBox.getChildren().clear();

        int employeeId = CurrentUserSession.getEmployeeId();
        int year = LocalDate.now().getYear();
        int index = 0;

        for (Map.Entry<String, Integer> entry : leaveTypeIds.entrySet()) {
            String name = entry.getKey();
            int typeId = entry.getValue();
            String value = "N/A";

            if (employeeId > 0 && typeId != 7) {
                try {
                    double credits = leaveService.getRemainingLeaveCredits(employeeId, typeId, year);
                    value = String.format("%.2f", credits);
                    if (typeId == 1 && onLeaveCreditsUpdated != null) {
                        onLeaveCreditsUpdated.accept(String.format("%.0f", credits));
                    }
                } catch (Exception e) {
                    value = "Unavailable";
                }
            }

            VBox tile = new VBox(4);
            tile.getStyleClass().add("leave-credit-tile");
            tile.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(tile, Priority.ALWAYS);
            Label typeLabel = new Label(name);
            typeLabel.getStyleClass().add("metric-label");
            Label valueLabel = new Label(value);
            valueLabel.getStyleClass().add("leave-credit-value");
            Label unitLabel = new Label(typeId == 7 ? "unpaid" : "credits available");
            unitLabel.getStyleClass().add("note-text");
            tile.getChildren().setAll(typeLabel, valueLabel, unitLabel);
            leaveCreditsBox.getChildren().add(tile);

            index++;
            if (index < leaveTypeIds.size()) {
                Region divider = new Region();
                divider.getStyleClass().add("leave-credit-divider");
                leaveCreditsBox.getChildren().add(divider);
            }
        }
    }

    private void refreshLeaveHistory() {
        int employeeId = CurrentUserSession.getEmployeeId();
        allHistoryRows.clear();
        if (employeeId <= 0) {
            leaveHistoryLabel.setText("No leave requests found.");
            currentLeavePage = 1;
            renderHistoryRows();
            return;
        }

        try {
            List<LeaveRequest> requests = leaveService.getLeaveRequestsByEmployeeId(employeeId);
            requests.sort(Comparator.comparing(LeaveRequest::getRequestDate).reversed());
            for (LeaveRequest request : requests) {
                allHistoryRows.add(new LeaveHistoryRow(request, resolveLeaveTypeName(request.getLeaveTypeId())));
            }

            leaveHistoryLabel.setText(requests.isEmpty()
                    ? "No leave requests found."
                    : "Pending requests can be cancelled. Approved requests consume leave credits.");
        } catch (Exception e) {
            leaveHistoryLabel.setText(userMessage(e));
        }

        currentLeavePage = 1;
        renderHistoryRows();
    }

    private void renderHistoryRows() {
        if (leaveHistoryTable == null) return;

        String filter = statusFilterComboBox == null ? "All Status" : statusFilterComboBox.getValue();
        filteredHistoryRows.clear();
        filteredHistoryRows.addAll(allHistoryRows.stream()
                .filter(row -> filter == null
                        || "All Status".equals(filter)
                        || row.statusText().equalsIgnoreCase(filter))
                .toList());
        renderHistoryPage();
    }

    private void renderHistoryPage() {
        int total = filteredHistoryRows.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        currentLeavePage = Math.max(1, Math.min(currentLeavePage, totalPages));

        int from = (currentLeavePage - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, total);

        leaveHistoryTable.getItems().setAll(filteredHistoryRows.subList(from, to));
        leavePageLabel.setText("Page " + currentLeavePage + " of " + totalPages);
        leaveRecordInfoLabel.setText(total == 0
                ? "No leave requests found."
                : "Showing " + (from + 1) + " to " + to + " of " + total + " records");
        leavePrevButton.setDisable(currentLeavePage <= 1);
        leaveNextButton.setDisable(currentLeavePage >= totalPages);
    }

    @FXML
    private void handleLeavePrevPage() {
        if (currentLeavePage > 1) {
            currentLeavePage--;
            renderHistoryPage();
        }
    }

    @FXML
    private void handleLeaveNextPage() {
        int totalPages = Math.max(1, (int) Math.ceil((double) filteredHistoryRows.size() / PAGE_SIZE));
        if (currentLeavePage < totalPages) {
            currentLeavePage++;
            renderHistoryPage();
        }
    }

    private void handleCancelLeaveRequest(LeaveHistoryRow row) {
        if (row == null || row.request().getRequestId() == null) return;

        int employeeId = CurrentUserSession.getEmployeeId();
        if (employeeId <= 0) {
            UiMessageUtil.setError(leaveMessageLabel, "Please log in before using this action.");
            return;
        }

        try {
            leaveService.cancelLeaveRequest(employeeId, row.request().getRequestId());
            UiMessageUtil.setSuccess(leaveMessageLabel, "Pending leave request cancelled.");
            refresh();
        } catch (Exception e) {
            UiMessageUtil.setError(leaveMessageLabel, userMessage(e));
        }
    }

    private String resolveLeaveTypeName(int leaveTypeId) {
        return leaveTypeIds.entrySet().stream()
                .filter(entry -> entry.getValue() == leaveTypeId)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("Leave Type " + leaveTypeId);
    }

    private void clearFormFields() {
        leaveTypeComboBox.getSelectionModel().clearSelection();
        leaveStartDatePicker.setValue(null);
        leaveEndDatePicker.setValue(null);
        leaveHoursField.clear();
        leaveReasonArea.clear();
        updateReasonCounter();
    }

    private void clearAllFieldErrors() {
        clearFieldError(leaveTypeErrorLabel, leaveTypeComboBox);
        clearFieldError(fromDateErrorLabel, leaveStartDatePicker);
        clearFieldError(toDateErrorLabel, leaveEndDatePicker);
        clearFieldError(leaveHoursErrorLabel, leaveHoursField);
        clearFieldError(reasonErrorLabel, leaveReasonArea);
    }

    private void showFieldError(Label label, Node field, String message) {
        label.setText(message);
        if (!field.getStyleClass().contains("invalid-field")) {
            field.getStyleClass().add("invalid-field");
        }
    }

    private void clearFieldError(Label label, Node field) {
        if (label != null) label.setText("");
        if (field != null) field.getStyleClass().remove("invalid-field");
    }

    private void showServiceError(Exception e) {
        String message = userMessage(e);
        String lower = message.toLowerCase();
        if (lower.contains("reason")) {
            showFieldError(reasonErrorLabel, leaveReasonArea, message);
        } else if (lower.contains("hour")) {
            showFieldError(leaveHoursErrorLabel, leaveHoursField, message);
        } else if (lower.contains("start") || lower.contains("from")) {
            showFieldError(fromDateErrorLabel, leaveStartDatePicker, message);
        } else if (lower.contains("end") || lower.contains("to date")) {
            showFieldError(toDateErrorLabel, leaveEndDatePicker, message);
        } else if (lower.contains("leave type")) {
            showFieldError(leaveTypeErrorLabel, leaveTypeComboBox, message);
        } else {
            UiMessageUtil.setError(leaveMessageLabel, message);
        }
    }

    private void updateReasonCounter() {
        if (reasonCounterLabel == null) return;
        int count = leaveReasonArea == null || leaveReasonArea.getText() == null
                ? 0
                : leaveReasonArea.getText().length();
        reasonCounterLabel.setText(count + " / " + REASON_LIMIT);
    }

    private String getReasonText() {
        return leaveReasonArea.getText() == null ? "" : leaveReasonArea.getText().trim();
    }

    private String userMessage(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) return "Unable to complete the request. Please try again.";
        if (isDatabaseUnavailable(e)) return "Database connection unavailable. Start MySQL and try again.";
        return msg;
    }

    private boolean isDatabaseUnavailable(Exception e) {
        Throwable current = e;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null && (msg.contains("Failed to connect to the database")
                    || msg.contains("Communications link failure")
                    || msg.contains("Connection refused"))) return true;
            current = current.getCause();
        }
        return false;
    }

    private record LeaveHistoryRow(LeaveRequest request, String leaveType) {
        String requestId() {
            return request.getRequestId() == null ? "-" : String.format("LR-%04d", request.getRequestId());
        }

        String fromDate() {
            return request.getStartDate().format(DATE_FORMAT);
        }

        String toDate() {
            return request.getEndDate().format(DATE_FORMAT);
        }

        String totalHours() {
            return String.format("%.2f", request.getLeaveHours());
        }

        RequestStatus status() {
            return request.getStatus();
        }

        String statusText() {
            return request.getStatus().name();
        }

        String dateRequested() {
            return request.getRequestDate().format(DATE_TIME_FORMAT);
        }
    }
}
