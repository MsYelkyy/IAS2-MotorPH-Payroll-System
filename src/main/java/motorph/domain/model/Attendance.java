package motorph.domain.model;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import motorph.domain.value.AttendanceStatus;

/**
 * Represents an employee attendance record and maps to the attendance table.
 */
public class Attendance {

    private final Integer attendanceId;
    private final int employeeId;
    private final LocalDate workDate;
    private final LocalTime timeIn;
    private final LocalTime timeOut;
    private final double overtimeHours;
    private final AttendanceStatus attendanceStatus;

    // constructors
    public Attendance(
            Integer attendanceId,
            int employeeId,
            LocalDate workDate,
            LocalTime timeIn,
            LocalTime timeOut,
            double overtimeHours,
            AttendanceStatus attendanceStatus) {

        this.attendanceId = validateNullableId(attendanceId, "Attendance ID must be positive.");
        this.employeeId = validatePositive(employeeId, "Employee ID must be positive.");
        this.workDate = requireNonNull(workDate, "Work date is required.");
        this.timeIn = timeIn;
        this.timeOut = timeOut;
        this.overtimeHours = validateNonNegative(overtimeHours, "Overtime hours cannot be negative.");
        this.attendanceStatus = requireNonNull(attendanceStatus, "Attendance status is required.");
    }

    // getters
    public Integer getAttendanceId() {
        return attendanceId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public LocalTime getTimeIn() {
        return timeIn;
    }

    public LocalTime getTimeOut() {
        return timeOut;
    }

    public double getOvertimeHours() {
        return overtimeHours;
    }

    public AttendanceStatus getAttendanceStatus() {
        return attendanceStatus;
    }

    // returns the total worked hours
    public double getHoursWorked() {
        if (timeIn == null || timeOut == null) {
            return 0.0;
        }

        long minutesWorked = Duration.between(timeIn, timeOut).toMinutes();
        return minutesWorked / 60.0;
    }

    // checks if the attendance is complete
    public boolean isComplete() {
        return timeIn != null && timeOut != null;
    }

    // checks if the employee is present
    public boolean isPresent() {
        return attendanceStatus == AttendanceStatus.PRESENT;
    }

    // checks if the employee is late
    public boolean isLate() {
        return attendanceStatus == AttendanceStatus.LATE;
    }

    // checks if the employee is absent
    public boolean isAbsent() {
        return attendanceStatus == AttendanceStatus.ABSENT;
    }

    // validates a required positive number
    private int validatePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    // validates an optional positive number
    private Integer validateNullableId(Integer value, String message) {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    // validates a non-negative number
    private double validateNonNegative(double value, String message) {
        if (value < 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    // validates a required object
    private <T> T requireNonNull(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}