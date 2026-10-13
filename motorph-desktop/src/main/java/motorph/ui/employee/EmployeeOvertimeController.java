package motorph.ui.employee;

import motorph.domain.value.RequestStatus;
import motorph.api.ApiClientFactory;
import motorph.api.OvertimeApiClient;
import motorph.api.dto.attendance.AttendanceDTO;
import motorph.api.dto.overtime.OvertimeRequestDTO;
import motorph.api.dto.overtime.OvertimeSubmitRequest;
import motorph.ui.common.EmptyState;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.util.UiMessageUtil;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Handles overtime request submission, cancellation, and history rendering for the employee. */
public class EmployeeOvertimeController {

    private static final int REASON_LIMIT = 255;
    private static final int PAGE_SIZE = 10;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy hh:mm a");

    private OvertimeApiClient overtimeApiClient;

    private final Map<String, Integer> overtimeTypeIds = new LinkedHashMap<>();
    private final Map<Integer, String> overtimeTypeNames = new LinkedHashMap<>();
    private final Map<String, EligibleAttendance> attendanceOptions = new LinkedHashMap<>();
    private final List<OvertimeHistoryRow> allHistoryRows = new ArrayList<>();
    private final List<OvertimeHistoryRow> filteredHistoryRows = new ArrayList<>();
    private int currentPage = 1;

    @FXML private StackPane toastRoot;
    @FXML private Label emptyStateLabel;
    @FXML private ComboBox<String> attendanceComboBox;
    @FXML private ComboBox<String> overtimeTypeComboBox;
    @FXML private TextField hoursField;
    @FXML private TextArea reasonArea;
    @FXML private Label attendanceErrorLabel;
    @FXML private Label overtimeTypeErrorLabel;
    @FXML private Label hoursErrorLabel;
    @FXML private Label reasonErrorLabel;
    @FXML private Label reasonCounterLabel;
    @FXML private Button submitButton;

    @FXML private Label overtimeHistoryLabel;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private TableView<OvertimeHistoryRow> overtimeHistoryTable;
    @FXML private TableColumn<OvertimeHistoryRow, String> colRequestId;
    @FXML private TableColumn<OvertimeHistoryRow, String> colDate;
    @FXML private TableColumn<OvertimeHistoryRow, String> colOvertimeType;
    @FXML private TableColumn<OvertimeHistoryRow, String> colHours;
    @FXML private TableColumn<OvertimeHistoryRow, String> colStatus;
    @FXML private TableColumn<OvertimeHistoryRow, String> colDateRequested;
    @FXML private TableColumn<OvertimeHistoryRow, Void> colAction;
    @FXML private Label recordInfoLabel;
    @FXML private Label pageLabel;
    @FXML private Button prevButton;
    @FXML private Button nextButton;

    @FXML
    private void initialize() {
        overtimeApiClient = ApiClientFactory.getInstance().getOvertimeApiClient();

        loadOvertimeTypes();
        overtimeTypeComboBox.getItems().setAll(overtimeTypeIds.keySet());
        overtimeTypeComboBox.setOnAction(e ->
                clearFieldError(overtimeTypeErrorLabel, overtimeTypeComboBox));

        attendanceComboBox.setOnAction(e ->
                clearFieldError(attendanceErrorLabel, attendanceComboBox));

        configureReasonField();
        configureHistoryTable();

        statusFilterComboBox.getItems().setAll("All Status", "Pending", "Approved", "Rejected", "Cancelled");
        statusFilterComboBox.setValue("All Status");
        statusFilterComboBox.setOnAction(e -> {
            currentPage = 1;
            renderHistoryRows();
        });
    }

    // reloads eligible attendance and request history when the view becomes active
    public void refresh() {
        loadEligibleAttendance();
        refreshOvertimeHistory();
    }

    // clears the overtime request form and validation state
    public void resetForm() {
        attendanceComboBox.getSelectionModel().clearSelection();
        overtimeTypeComboBox.getSelectionModel().clearSelection();
        hoursField.clear();
        reasonArea.clear();
        updateReasonCounter();
        clearAllFieldErrors();

        if (statusFilterComboBox != null) {
            statusFilterComboBox.setValue("All Status");
        }
    }

    @FXML
    private void handleClearOvertimeRequest() {
        resetForm();
    }

    @FXML
    private void handleSubmitOvertimeRequest() {
        int employeeId = CurrentUserSession.getEmployeeId();
        if (employeeId <= 0) {
            UiMessageUtil.showErrorToastTop(toastRoot, "Please log in before using this action.");
            return;
        }

        if (!validateForm()) {
            UiMessageUtil.showErrorToast(toastRoot, "Some required fields are missing or invalid. Please check the highlighted fields.");
            return;
        }

        try {
            EligibleAttendance attendance = attendanceOptions.get(attendanceComboBox.getValue());
            int overtimeTypeId = overtimeTypeIds.get(overtimeTypeComboBox.getValue());
            double hours = Double.parseDouble(hoursField.getText().trim());

            overtimeApiClient.submitOvertimeRequest(new OvertimeSubmitRequest(
                    attendance.attendanceId(),
                    overtimeTypeId,
                    attendance.workDate(),
                    hours,
                    getReasonText()
            ));

            resetForm();
            refresh();
            UiMessageUtil.showToast(toastRoot, "Overtime request submitted.");
        } catch (Exception e) {
            showServiceError(e);
        }
    }

    // loads the fixed overtime type options
    private void loadOvertimeTypes() {
        overtimeTypeIds.clear();
        overtimeTypeNames.clear();

        overtimeTypeIds.put("Regular OT", 1);
        overtimeTypeIds.put("Rest Day OT", 2);
        overtimeTypeIds.put("Special Holiday OT", 3);
        overtimeTypeIds.put("Regular Holiday OT", 4);
        overtimeTypeIds.put("Night Differential OT", 5);

        for (Map.Entry<String, Integer> entry : overtimeTypeIds.entrySet()) {
            overtimeTypeNames.put(entry.getValue(), entry.getKey());
        }
    }

    // loads recent completed attendance eligible for overtime and toggles the empty state
    private void loadEligibleAttendance() {
        attendanceOptions.clear();
        attendanceComboBox.getItems().clear();

        int employeeId = CurrentUserSession.getEmployeeId();
        if (employeeId <= 0) {
            setEmptyState(true);
            return;
        }

        try {
            List<AttendanceDTO> eligible = overtimeApiClient.getEligibleAttendance(LocalDate.now());
            for (AttendanceDTO attendance : eligible) {
                String label = attendance.getWorkDate().format(DATE_FORMAT)
                        + String.format(" (%.1f hrs)", attendance.getHoursWorked());
                attendanceOptions.put(label, new EligibleAttendance(
                        attendance.getAttendanceId(), attendance.getWorkDate()));
                attendanceComboBox.getItems().add(label);
            }
            setEmptyState(eligible.isEmpty());
        } catch (Exception e) {
            setEmptyState(true);
            UiMessageUtil.showErrorToastTop(toastRoot, userMessage(e));
        }
    }

    // shows the empty-state message and disables submit when no eligible attendance exists
    private void setEmptyState(boolean empty) {
        setNodeVisible(emptyStateLabel, empty);
        if (submitButton != null) {
            submitButton.setDisable(empty);
        }
    }

    private void configureReasonField() {
        reasonArea.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && newValue.length() > REASON_LIMIT) {
                reasonArea.setText(oldValue);
                return;
            }
            clearFieldError(reasonErrorLabel, reasonArea);
            updateReasonCounter();
        });
        updateReasonCounter();
    }

    private void configureHistoryTable() {
        overtimeHistoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        colRequestId.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().requestId()));
        colDate.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().overtimeDate()));
        colOvertimeType.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().overtimeType()));
        colHours.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().requestedHours()));
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
                    OvertimeHistoryRow row = getTableView().getItems().get(getIndex());
                    handleCancelOvertimeRequest(row);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER_LEFT);
                OvertimeHistoryRow row = empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()
                        ? null
                        : getTableView().getItems().get(getIndex());
                setGraphic(row != null && row.status() == RequestStatus.PENDING ? cancelButton : null);
                setText(row != null && row.status() != RequestStatus.PENDING ? "-" : null);
            }
        });
    }

    private void refreshOvertimeHistory() {
        int employeeId = CurrentUserSession.getEmployeeId();
        allHistoryRows.clear();
        overtimeHistoryLabel.setText("View and track your overtime requests.");
        if (employeeId <= 0) {
            currentPage = 1;
            renderHistoryRows();
            return;
        }

        try {
            List<OvertimeRequestDTO> requests = overtimeApiClient.getOwnOvertimeRequests();
            requests.sort(Comparator.comparing(OvertimeRequestDTO::getRequestDate).reversed());
            for (OvertimeRequestDTO request : requests) {
                allHistoryRows.add(new OvertimeHistoryRow(request, resolveOvertimeTypeName(request.getOvertimeTypeId())));
            }

        } catch (Exception e) {
            overtimeHistoryLabel.setText(userMessage(e));
        }

        currentPage = 1;
        renderHistoryRows();
    }

    private void renderHistoryRows() {
        if (overtimeHistoryTable == null) return;

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
        currentPage = Math.max(1, Math.min(currentPage, totalPages));

        int from = (currentPage - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, total);

        overtimeHistoryTable.getItems().setAll(filteredHistoryRows.subList(from, to));
        pageLabel.setText("Page " + currentPage + " of " + totalPages);
        EmptyState.apply(overtimeHistoryTable, !allHistoryRows.isEmpty(), "overtime requests", "Submitted overtime requests will appear here.");
        recordInfoLabel.setText(EmptyState.recordInfo(from, to, total, allHistoryRows.size(), "requests"));
        prevButton.setDisable(currentPage <= 1);
        nextButton.setDisable(currentPage >= totalPages);
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 1) {
            currentPage--;
            renderHistoryPage();
        }
    }

    @FXML
    private void handleNextPage() {
        int totalPages = Math.max(1, (int) Math.ceil((double) filteredHistoryRows.size() / PAGE_SIZE));
        if (currentPage < totalPages) {
            currentPage++;
            renderHistoryPage();
        }
    }

    private void handleCancelOvertimeRequest(OvertimeHistoryRow row) {
        if (row == null || row.request().getRequestId() == null) return;

        int employeeId = CurrentUserSession.getEmployeeId();
        if (employeeId <= 0) {
            UiMessageUtil.showErrorToastTop(toastRoot, "Please log in before using this action.");
            return;
        }

        try {
            overtimeApiClient.cancelOvertimeRequest(row.request().getRequestId());
            refresh();
            UiMessageUtil.showToast(toastRoot, "Pending overtime request cancelled.");
        } catch (Exception e) {
            UiMessageUtil.showErrorToastTop(toastRoot, userMessage(e));
        }
    }

    private boolean validateForm() {
        clearAllFieldErrors();

        boolean valid = true;

        if (attendanceOptions.get(attendanceComboBox.getValue()) == null) {
            showFieldError(attendanceErrorLabel, attendanceComboBox, "Required");
            valid = false;
        }

        if (overtimeTypeIds.get(overtimeTypeComboBox.getValue()) == null) {
            showFieldError(overtimeTypeErrorLabel, overtimeTypeComboBox, "Required");
            valid = false;
        }

        if (parseHours() <= 0) {
            showFieldError(hoursErrorLabel, hoursField, "Invalid hours");
            valid = false;
        }

        if (getReasonText().isBlank()) {
            showFieldError(reasonErrorLabel, reasonArea, "Required");
            valid = false;
        }

        return valid;
    }

    private double parseHours() {
        try {
            return Double.parseDouble(hoursField.getText() == null ? "" : hoursField.getText().trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private String resolveOvertimeTypeName(int overtimeTypeId) {
        return overtimeTypeNames.getOrDefault(overtimeTypeId, "Overtime Type " + overtimeTypeId);
    }

    private void clearAllFieldErrors() {
        clearFieldError(attendanceErrorLabel, attendanceComboBox);
        clearFieldError(overtimeTypeErrorLabel, overtimeTypeComboBox);
        clearFieldError(hoursErrorLabel, hoursField);
        clearFieldError(reasonErrorLabel, reasonArea);
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
            showFieldError(reasonErrorLabel, reasonArea, message);
        } else if (lower.contains("hour")) {
            showFieldError(hoursErrorLabel, hoursField, message);
        } else if (lower.contains("overtime type")) {
            showFieldError(overtimeTypeErrorLabel, overtimeTypeComboBox, message);
        } else if (lower.contains("attendance") || lower.contains("within the last")) {
            showFieldError(attendanceErrorLabel, attendanceComboBox, message);
        } else {
            UiMessageUtil.showErrorToastTop(toastRoot, message);
        }
    }

    private void updateReasonCounter() {
        if (reasonCounterLabel == null) return;
        int count = reasonArea == null || reasonArea.getText() == null
                ? 0
                : reasonArea.getText().length();
        reasonCounterLabel.setText(count + " / " + REASON_LIMIT);
    }

    private String getReasonText() {
        return reasonArea.getText() == null ? "" : reasonArea.getText().trim();
    }

    private void setNodeVisible(Node node, boolean visible) {
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

    // holds an eligible attendance option for the form dropdown
    private record EligibleAttendance(int attendanceId, LocalDate workDate) {
        // getters/setters
    }

    // holds a single overtime request row for the history table
    private record OvertimeHistoryRow(OvertimeRequestDTO request, String overtimeType) {
        String requestId() {
            return request.getRequestId() == null ? "-" : String.format("OT-%04d", request.getRequestId());
        }

        String overtimeDate() {
            return request.getOvertimeDate().format(DATE_FORMAT);
        }

        String requestedHours() {
            return String.format("%.2f", request.getRequestedHours());
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
