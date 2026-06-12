package motorph.ui.employee.leave;

import motorph.application.service.LeaveService;
import motorph.domain.model.LeaveRequest;
import motorph.domain.model.Employee;
import motorph.domain.value.Gender;
import motorph.domain.value.RequestStatus;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.util.UiMessageUtil;
import motorph.ui.util.UiValidationUtil;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Handles leave request submission, cancellation, and credit display. */
public class EmployeeLeaveController {

    private LeaveService leaveService;
    private Consumer<String> onLeaveCreditsUpdated;
    private Integer latestPendingLeaveRequestId;

    private final Map<String, Integer> leaveTypeIds = new LinkedHashMap<>();

    @FXML
    private ComboBox<String> leaveTypeComboBox;

    @FXML
    private TextField leaveCreditsField;

    @FXML
    private DatePicker leaveStartDatePicker;;

    @FXML
    private DatePicker leaveEndDatePicker;

    @FXML
    private TextField leaveHoursField;

    @FXML
    private TextField leaveReasonField;

    @FXML
    private Label leaveHistoryLabel;

    @FXML
    private Label leaveMessageLabel;

    @FXML
    private void initialize() {
        System.out.println("EmployeeLeaveController initialize running");

        leaveService = EmployeeUIServiceFactory.getInstance().getLeaveService();

        loadLeaveTypesForCurrentEmployee();
        leaveTypeComboBox.getItems().setAll(leaveTypeIds.keySet());
        leaveTypeComboBox.setOnAction(e -> refreshLeaveCredits());

        leaveHoursField.setEditable(false);
        leaveStartDatePicker.setOnAction(e -> updateLeaveHours());
        leaveEndDatePicker.setOnAction(e -> updateLeaveHours());

        System.out.println("Leave types loaded: " + leaveTypeComboBox.getItems());
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

    // stores a callback invoked when leave credits change (updates dashboard label)
    public void setOnLeaveCreditsUpdated(Consumer<String> callback) {
        this.onLeaveCreditsUpdated = callback;
    }

    // updates leave hours using service computation
    private void updateLeaveHours() {
        LocalDate startDate = leaveStartDatePicker.getValue();
        LocalDate endDate = leaveEndDatePicker.getValue();

        if (startDate == null || endDate == null) {
            leaveHoursField.setText("");
            return;
        }

        try {
            double hours = leaveService.computeLeaveHours(startDate, endDate);
            leaveHoursField.setText(String.format("%.1f", hours));
        } catch (Exception e) {
            leaveHoursField.setText("");
            UiMessageUtil.setError(leaveMessageLabel, userMessage(e));
        }
    }

    // refreshes leave credits and history for the current employee
    public void refresh() {
        refreshLeaveCredits();
        refreshLeaveHistory();
    }

    @FXML
    private void handleSubmitLeaveRequest() {
        int employeeId = CurrentUserSession.getEmployeeId();
        if (employeeId <= 0) {
            UiMessageUtil.setError(leaveMessageLabel, "Please log in before using this action.");
            return;
        }

        clearLeaveInvalid();

        try {
            Integer leaveTypeId = leaveTypeIds.get(leaveTypeComboBox.getValue());
            if (leaveTypeId == null) {
                throw new IllegalArgumentException("Select a leave type.");
            }

            LocalDate startDate = leaveStartDatePicker.getValue();
            LocalDate endDate = leaveEndDatePicker.getValue();

            if (startDate == null) {
                throw new IllegalArgumentException("Start date is required.");
            }

            if (endDate == null) {
                throw new IllegalArgumentException("End date is required.");
            }

            double hours = UiValidationUtil.parsePositiveDouble(
                    leaveHoursField,
                    "Leave hours must be a positive number."
            );

            leaveService.submitLeaveRequest(
                    employeeId,
                    leaveTypeId,
                    startDate,
                    endDate,
                    hours,
                    UiValidationUtil.getText(leaveReasonField)
            );

            UiMessageUtil.setSuccess(leaveMessageLabel, "Leave request submitted.");
            refresh();

        } catch (Exception e) {
            UiMessageUtil.setError(leaveMessageLabel, userMessage(e));
        }
    }

    @FXML
    private void handleCancelLatestLeaveRequest() {
        int employeeId = CurrentUserSession.getEmployeeId();
        if (employeeId <= 0) {
            UiMessageUtil.setError(leaveMessageLabel, "Please log in before using this action.");
            return;
        }
        if (latestPendingLeaveRequestId == null) {
            UiMessageUtil.setError(leaveMessageLabel, "No pending leave request to cancel.");
            return;
        }
        try {
            leaveService.cancelLeaveRequest(employeeId, latestPendingLeaveRequestId);
            latestPendingLeaveRequestId = null;
            UiMessageUtil.setSuccess(leaveMessageLabel, "Pending leave request cancelled.");
            refresh();
        } catch (Exception e) {
            UiMessageUtil.setError(leaveMessageLabel, userMessage(e));
        }
    }

    // loads and displays remaining credits for the selected leave type
    private void refreshLeaveCredits() {
        String selected = leaveTypeComboBox.getValue();
        Integer typeId = leaveTypeIds.get(selected);

        if (typeId == null) {
            if (leaveCreditsField != null) leaveCreditsField.setText("Select a type");
            return;
        }

        if (typeId == 7) {
            if (leaveCreditsField != null) leaveCreditsField.setText("N/A");
            if (onLeaveCreditsUpdated != null) onLeaveCreditsUpdated.accept("0");
            return;
        }

        int employeeId = CurrentUserSession.getEmployeeId();
        if (employeeId <= 0) {
            if (leaveCreditsField != null) leaveCreditsField.setText("Login required");
            return;
        }

        try {
            double credits = leaveService.getRemainingLeaveCredits(employeeId, typeId, LocalDate.now().getYear());
            String formatted = String.format("%.1f days", credits);
            if (leaveCreditsField != null) leaveCreditsField.setText(formatted);
            if (onLeaveCreditsUpdated != null) onLeaveCreditsUpdated.accept(String.format("%.0f", credits));
        } catch (Exception e) {
            if (leaveCreditsField != null) leaveCreditsField.setText("Unavailable");
        }
    }

    // loads and summarizes the most recent leave request
    private void refreshLeaveHistory() {
        int employeeId = CurrentUserSession.getEmployeeId();
        if (employeeId <= 0) return;

        try {
            List<LeaveRequest> requests = leaveService.getLeaveRequestsByEmployeeId(employeeId);
            latestPendingLeaveRequestId = requests.stream()
                    .filter(r -> r.getStatus() == RequestStatus.PENDING)
                    .max(Comparator.comparing(LeaveRequest::getRequestDate))
                    .map(LeaveRequest::getRequestId)
                    .orElse(null);

            if (requests.isEmpty()) {
                if (leaveHistoryLabel != null) leaveHistoryLabel.setText("No leave requests found.");
                return;
            }
            LeaveRequest latest = requests.stream()
                    .max(Comparator.comparing(LeaveRequest::getRequestDate))
                    .orElseThrow();
            if (leaveHistoryLabel != null) {
                leaveHistoryLabel.setText(
                        "Latest: " + latest.getStatus().name()
                                + " | " + latest.getStartDate()
                                + " to " + latest.getEndDate()
                                + " | " + latest.getLeaveHours() + " hours"
                );
            }
        } catch (Exception e) {
            latestPendingLeaveRequestId = null;
            if (leaveHistoryLabel != null) leaveHistoryLabel.setText(userMessage(e));
        }
    }

    // clears validation marks from all leave form fields
    private void clearLeaveInvalid() {
        UiValidationUtil.clearInvalid(leaveHoursField);
        UiValidationUtil.clearInvalid(leaveReasonField);
    }

    // extracts a user-friendly message from an exception
    private String userMessage(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) return "Unable to complete the request. Please try again.";
        if (isDatabaseUnavailable(e)) return "Database connection unavailable. Start MySQL and try again.";
        return msg;
    }

    // checks whether the exception signals a database connectivity failure
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
}
