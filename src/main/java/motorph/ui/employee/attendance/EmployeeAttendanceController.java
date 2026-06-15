package motorph.ui.employee.attendance;

import motorph.application.service.AttendanceService;
import motorph.domain.model.Attendance;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.util.UiMessageUtil;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Handles time-in/time-out actions and displays today's attendance status. */
public class EmployeeAttendanceController {

    private static final int PAGE_SIZE = 10;
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM d, yyyy (EEEE)", Locale.ENGLISH);
    private static final DateTimeFormatter TABLE_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM d, yyyy (EEE)", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    private AttendanceService attendanceService;
    private final List<AttendanceRowItem> allAttendanceRows = new ArrayList<>();
    private int currentAttendancePage = 1;

    @FXML private Label todayDateLabel;
    @FXML private Label todayStatusLabel;
    @FXML private Label todayTimeInLabel;
    @FXML private Label todayTimeOutLabel;
    @FXML private Label attendanceMessageLabel;

    @FXML private TableView<AttendanceRowItem> attendanceTable;
    @FXML private TableColumn<AttendanceRowItem, String> colAttendanceDate;
    @FXML private TableColumn<AttendanceRowItem, String> colAttendanceTimeIn;
    @FXML private TableColumn<AttendanceRowItem, String> colAttendanceTimeOut;
    @FXML private TableColumn<AttendanceRowItem, String> colAttendanceHours;
    @FXML private TableColumn<AttendanceRowItem, String> colAttendanceStatus;
    @FXML private Label attendanceRecordInfoLabel;
    @FXML private Label attendancePageLabel;
    @FXML private Button attendancePrevButton;
    @FXML private Button attendanceNextButton;

    @FXML
    private void initialize() {
        attendanceService = EmployeeUIServiceFactory.getInstance().getAttendanceService();
        attendanceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setupAttendanceColumns();
        updateTodayDate();
    }

    // refreshes today's attendance record and history for the current employee
    public void refresh() {
        UiMessageUtil.clearMessage(attendanceMessageLabel);
        updateTodayDate();
        loadAttendanceStatus();
        loadAttendanceHistory();
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
            loadAttendanceHistory();
            UiMessageUtil.setSuccess(attendanceMessageLabel, "Time in recorded.");
        } catch (Exception e) {
            UiMessageUtil.setError(attendanceMessageLabel, attendanceActionMessage(e));
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
            loadAttendanceHistory();
            UiMessageUtil.setSuccess(attendanceMessageLabel, "Time out recorded.");
        } catch (Exception e) {
            UiMessageUtil.setError(attendanceMessageLabel, attendanceActionMessage(e));
        }
    }

    // queries and displays today's attendance record
    private void loadAttendanceStatus() {
        int employeeId = CurrentUserSession.getEmployeeId();
        if (employeeId <= 0) return;
        try {
            Attendance record = attendanceService.getAttendanceForDate(employeeId, LocalDate.now());
            if (todayTimeInLabel != null) todayTimeInLabel.setText(formatTime(record.getTimeIn()));
            if (todayTimeOutLabel != null) todayTimeOutLabel.setText(formatTime(record.getTimeOut()));
            if (todayStatusLabel != null) todayStatusLabel.setText(record.getAttendanceStatus().name());
        } catch (Exception e) {
            if (todayTimeInLabel != null) todayTimeInLabel.setText("-");
            if (todayTimeOutLabel != null) todayTimeOutLabel.setText("-");
            if (todayStatusLabel != null) todayStatusLabel.setText("Pending");
            String msg = userMessage(e);
            if (!msg.contains("Attendance record not found")) {
                UiMessageUtil.setError(attendanceMessageLabel, msg);
            }
        }
    }

    // loads all attendance records and renders the first paginated table page
    private void loadAttendanceHistory() {
        int employeeId = CurrentUserSession.getEmployeeId();
        if (employeeId <= 0) {
            allAttendanceRows.clear();
            renderAttendancePage();
            return;
        }

        allAttendanceRows.clear();
        try {
            List<Attendance> records = attendanceService.getAttendanceByEmployeeId(employeeId);
            records.sort(Comparator.comparing(Attendance::getWorkDate).reversed());
            for (Attendance record : records) {
                allAttendanceRows.add(new AttendanceRowItem(record));
            }
        } catch (Exception e) {
            String msg = userMessage(e);
            if (!msg.contains("Attendance record not found")) {
                UiMessageUtil.setError(attendanceMessageLabel, msg);
            }
        }

        currentAttendancePage = 1;
        renderAttendancePage();
    }

    // refreshes the attendance table and pagination footer for the current page
    private void renderAttendancePage() {
        int total = allAttendanceRows.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        currentAttendancePage = Math.max(1, Math.min(currentAttendancePage, totalPages));

        int from = (currentAttendancePage - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, total);

        attendanceTable.getItems().setAll(allAttendanceRows.subList(from, to));
        attendancePageLabel.setText("Page " + currentAttendancePage + " of " + totalPages);
        attendanceRecordInfoLabel.setText(total == 0
                ? "No attendance records found."
                : "Showing " + (from + 1) + " to " + to + " of " + total + " records");
        attendancePrevButton.setDisable(currentAttendancePage <= 1);
        attendanceNextButton.setDisable(currentAttendancePage >= totalPages);
    }

    @FXML
    private void handleAttendancePrevPage() {
        if (currentAttendancePage > 1) {
            currentAttendancePage--;
            renderAttendancePage();
        }
    }

    @FXML
    private void handleAttendanceNextPage() {
        int totalPages = Math.max(1, (int) Math.ceil((double) allAttendanceRows.size() / PAGE_SIZE));
        if (currentAttendancePage < totalPages) {
            currentAttendancePage++;
            renderAttendancePage();
        }
    }

    // wires table columns to display-only attendance row values
    private void setupAttendanceColumns() {
        colAttendanceDate.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDate()));
        colAttendanceTimeIn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getTimeIn()));
        colAttendanceTimeOut.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getTimeOut()));
        colAttendanceHours.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getHoursWorked()));
        colAttendanceStatus.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStatus()));
    }

    private void updateTodayDate() {
        if (todayDateLabel != null) {
            todayDateLabel.setText(LocalDate.now().format(DATE_FORMATTER));
        }
    }

    private static String formatTime(LocalTime time) {
        return time == null ? "-" : time.format(TIME_FORMATTER);
    }

    private static String formatHours(Attendance attendance) {
        return attendance == null || !attendance.isComplete()
                ? "-"
                : String.format(Locale.ENGLISH, "%.2f", attendance.getHoursWorked());
    }

    private String attendanceActionMessage(Exception e) {
        String msg = userMessage(e);
        String normalized = msg.toLowerCase(Locale.ENGLISH);
        if (normalized.contains("already timed in")) {
            return "Already timed in.";
        }
        if (normalized.contains("no attendance record")
                || normalized.contains("missing time in")
                || normalized.contains("time in")) {
            return "Time in first.";
        }
        return msg;
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

    private static class AttendanceRowItem {
        private final String date;
        private final String timeIn;
        private final String timeOut;
        private final String hoursWorked;
        private final String status;

        private AttendanceRowItem(Attendance attendance) {
            this.date = attendance.getWorkDate().format(TABLE_DATE_FORMATTER);
            this.timeIn = formatTime(attendance.getTimeIn());
            this.timeOut = formatTime(attendance.getTimeOut());
            this.hoursWorked = formatHours(attendance);
            this.status = attendance.getAttendanceStatus().name();
        }

        private String getDate() {
            return date;
        }

        private String getTimeIn() {
            return timeIn;
        }

        private String getTimeOut() {
            return timeOut;
        }

        private String getHoursWorked() {
            return hoursWorked;
        }

        private String getStatus() {
            return status;
        }
    }
}
