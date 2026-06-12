package motorph.ui.employee.attendance;

import motorph.application.service.AttendanceService;
import motorph.domain.model.Attendance;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.util.UiMessageUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.time.LocalDate;

/** Handles time-in/time-out actions and displays today's attendance status. */
public class EmployeeAttendanceController {

    private AttendanceService attendanceService;

    @FXML
    private Label attendanceDateLabel;

    @FXML
    private Label attendanceTimeInLabel;

    @FXML
    private Label attendanceTimeOutLabel;

    @FXML
    private Label attendanceHoursLabel;

    @FXML
    private Label attendanceStatusLabel;

    @FXML
    private Label attendanceMessageLabel;

    @FXML
    private void initialize() {
        attendanceService = EmployeeUIServiceFactory.getInstance().getAttendanceService();
    }

    // refreshes today's attendance record for the current employee
    public void refresh() {
        if (attendanceDateLabel != null) attendanceDateLabel.setText(LocalDate.now().toString());
        loadAttendanceStatus();
    }

    @FXML
    private void handleTimeIn() {
        int employeeId = CurrentUserSession.getEmployeeId();
        if (employeeId <= 0) {
            UiMessageUtil.setError(attendanceMessageLabel, "Please log in before using this action.");
            return;
        }
        try {
            attendanceService.timeIn(employeeId);
            loadAttendanceStatus();
            UiMessageUtil.setSuccess(attendanceMessageLabel, "Time in recorded.");
        } catch (Exception e) {
            UiMessageUtil.setError(attendanceMessageLabel, userMessage(e));
        }
    }

    @FXML
    private void handleTimeOut() {
        int employeeId = CurrentUserSession.getEmployeeId();
        if (employeeId <= 0) {
            UiMessageUtil.setError(attendanceMessageLabel, "Please log in before using this action.");
            return;
        }
        try {
            attendanceService.timeOut(employeeId);
            loadAttendanceStatus();
            UiMessageUtil.setSuccess(attendanceMessageLabel, "Time out recorded.");
        } catch (Exception e) {
            UiMessageUtil.setError(attendanceMessageLabel, userMessage(e));
        }
    }

    // queries and displays today's attendance record
    private void loadAttendanceStatus() {
        int employeeId = CurrentUserSession.getEmployeeId();
        if (employeeId <= 0) return;
        try {
            Attendance record = attendanceService.getAttendanceForDate(employeeId, LocalDate.now());
            if (attendanceTimeInLabel  != null) attendanceTimeInLabel.setText(record.getTimeIn()  == null ? "—" : record.getTimeIn().toString());
            if (attendanceTimeOutLabel != null) attendanceTimeOutLabel.setText(record.getTimeOut() == null ? "—" : record.getTimeOut().toString());
            if (attendanceHoursLabel   != null) attendanceHoursLabel.setText(String.format("%.2f", record.getHoursWorked()));
            if (attendanceStatusLabel  != null) attendanceStatusLabel.setText(record.getAttendanceStatus().name());
        } catch (Exception e) {
            if (attendanceTimeInLabel  != null) attendanceTimeInLabel.setText("—");
            if (attendanceTimeOutLabel != null) attendanceTimeOutLabel.setText("—");
            if (attendanceHoursLabel   != null) attendanceHoursLabel.setText("—");
            if (attendanceStatusLabel  != null) attendanceStatusLabel.setText("Pending");
            String msg = userMessage(e);
            if (!msg.contains("Attendance record not found")) {
                UiMessageUtil.setError(attendanceMessageLabel, msg);
            }
        }
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
