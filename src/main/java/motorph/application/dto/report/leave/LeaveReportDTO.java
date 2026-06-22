package motorph.application.dto.report.leave;

import java.time.LocalDate;

/**
 * Stores leave report row data.
 */
public class LeaveReportDTO {

    private int leaveRequestId;
    private int employeeId;
    private String employeeName;
    private String departmentName;
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private double leaveHours;
    private String status;

    // constructors
    public LeaveReportDTO() {
    }

    public LeaveReportDTO(int leaveRequestId,
                          int employeeId,
                          String employeeName,
                          String departmentName,
                          String leaveType,
                          LocalDate startDate,
                          LocalDate endDate,
                          double leaveHours,
                          String status) {

        this.leaveRequestId = leaveRequestId;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.departmentName = departmentName;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.leaveHours = leaveHours;
        this.status = status;
    }

    // getters/setters
    public int getLeaveRequestId() {
        return leaveRequestId;
    }

    public void setLeaveRequestId(int leaveRequestId) {
        this.leaveRequestId = leaveRequestId;
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

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(String leaveType) {
        this.leaveType = leaveType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public double getLeaveHours() {
        return leaveHours;
    }

    public void setLeaveHours(double leaveHours) {
        this.leaveHours = leaveHours;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}