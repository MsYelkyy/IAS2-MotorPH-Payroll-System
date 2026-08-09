package motorph.backend.domain;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

import motorph.backend.domain.value.AttendanceStatus;

/**
 * Represents an employee attendance record and maps to the attendance table.
 */
public class Attendance {

    private final Integer attendanceId;
    private final int employeeId;
    private final LocalDate workDate;
    private final LocalTime timeIn;
    private final LocalTime timeOut;
    private final AttendanceStatus attendanceStatus;

    public Attendance(
            Integer attendanceId,
            int employeeId,
            LocalDate workDate,
            LocalTime timeIn,
            LocalTime timeOut,
            AttendanceStatus attendanceStatus) {

        this.attendanceId = validateNullableId(attendanceId, "Attendance ID must be positive.");
        this.employeeId = validatePositive(employeeId, "Employee ID must be positive.");
        this.workDate = requireNonNull(workDate, "Work date is required.");
        this.timeIn = timeIn;
        this.timeOut = timeOut;
        this.attendanceStatus = requireNonNull(attendanceStatus, "Attendance status is required.");
    }

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

    public AttendanceStatus getAttendanceStatus() {
        return attendanceStatus;
    }

    public double getHoursWorked() {
        if (timeIn == null || timeOut == null) {
            return 0.0;
        }

        long minutesWorked = Duration.between(timeIn, timeOut).toMinutes();
        return minutesWorked / 60.0;
    }

    public boolean isComplete() {
        return timeIn != null && timeOut != null;
    }

    public boolean isPresent() {
        return attendanceStatus == AttendanceStatus.PRESENT;
    }

    public boolean isLate() {
        return attendanceStatus == AttendanceStatus.LATE;
    }

    public boolean isAbsent() {
        return attendanceStatus == AttendanceStatus.ABSENT;
    }

    private int validatePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private Integer validateNullableId(Integer value, String message) {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private <T> T requireNonNull(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
