package motorph.backend.dto.report.attendance;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * One row of the attendance report. Field/getter names must match what
 * attendance_report.jrxml binds against.
 */
public class AttendanceReportDTO {

    private int employeeId;
    private String employeeName;
    private String positionName;
    private String departmentName;
    private LocalDate workDate;
    private LocalTime timeIn;
    private LocalTime timeOut;
    private double totalHoursWorked;
    private String attendanceStatus;

    public AttendanceReportDTO() {
    }

    public AttendanceReportDTO(int employeeId, String employeeName, String positionName, String departmentName,
                               LocalDate workDate, LocalTime timeIn, LocalTime timeOut,
                               double totalHoursWorked, String attendanceStatus) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.positionName = positionName;
        this.departmentName = departmentName;
        this.workDate = workDate;
        this.timeIn = timeIn;
        this.timeOut = timeOut;
        this.totalHoursWorked = totalHoursWorked;
        this.attendanceStatus = attendanceStatus;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }

    public LocalTime getTimeIn() {
        return timeIn;
    }

    public void setTimeIn(LocalTime timeIn) {
        this.timeIn = timeIn;
    }

    public LocalTime getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(LocalTime timeOut) {
        this.timeOut = timeOut;
    }

    public double getTotalHoursWorked() {
        return totalHoursWorked;
    }

    public void setTotalHoursWorked(double totalHoursWorked) {
        this.totalHoursWorked = totalHoursWorked;
    }

    public String getAttendanceStatus() {
        return attendanceStatus;
    }

    public void setAttendanceStatus(String attendanceStatus) {
        this.attendanceStatus = attendanceStatus;
    }
}
