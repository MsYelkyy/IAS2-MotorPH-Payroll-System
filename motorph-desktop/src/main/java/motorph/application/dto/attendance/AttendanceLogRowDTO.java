package motorph.application.dto.attendance;

import motorph.domain.value.AttendanceStatus;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Carries one attendance log row with employee details for HR display.
 */
public class AttendanceLogRowDTO {

    private final int employeeId;
    private final String employeeName;
    private final String department;
    private final LocalDate workDate;
    private final LocalTime timeIn;
    private final LocalTime timeOut;
    private final double hoursWorked;
    private final AttendanceStatus status;

    // constructors
    public AttendanceLogRowDTO(int employeeId,
                               String employeeName,
                               String department,
                               LocalDate workDate,
                               LocalTime timeIn,
                               LocalTime timeOut,
                               double hoursWorked,
                               AttendanceStatus status) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.department = department;
        this.workDate = workDate;
        this.timeIn = timeIn;
        this.timeOut = timeOut;
        this.hoursWorked = hoursWorked;
        this.status = status;
    }

    // getters
    public int getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public String getDepartment() {
        return department;
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

    public double getHoursWorked() {
        return hoursWorked;
    }

    public AttendanceStatus getStatus() {
        return status;
    }
}
