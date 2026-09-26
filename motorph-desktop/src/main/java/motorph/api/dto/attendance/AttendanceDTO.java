package motorph.api.dto.attendance;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import motorph.domain.value.AttendanceStatus;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Client-side copy of the backend's {@code Attendance}. {@link #getHoursWorked()} and
 * {@link #isComplete()} are recomputed here because they depend only on the time-in, time-out,
 * and status fields.
 */
public class AttendanceDTO {

    private final Integer attendanceId;
    private final int employeeId;
    private final LocalDate workDate;
    private final LocalTime timeIn;
    private final LocalTime timeOut;
    private final AttendanceStatus attendanceStatus;

    @JsonCreator
    public AttendanceDTO(@JsonProperty("attendanceId") Integer attendanceId,
                         @JsonProperty("employeeId") int employeeId,
                         @JsonProperty("workDate") LocalDate workDate,
                         @JsonProperty("timeIn") LocalTime timeIn,
                         @JsonProperty("timeOut") LocalTime timeOut,
                         @JsonProperty("attendanceStatus") AttendanceStatus attendanceStatus) {
        this.attendanceId = attendanceId;
        this.employeeId = employeeId;
        this.workDate = workDate;
        this.timeIn = timeIn;
        this.timeOut = timeOut;
        this.attendanceStatus = attendanceStatus;
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
        return Duration.between(timeIn, timeOut).toMinutes() / 60.0;
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
}
