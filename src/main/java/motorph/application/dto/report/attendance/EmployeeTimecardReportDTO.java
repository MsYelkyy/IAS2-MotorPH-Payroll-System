package motorph.application.dto.report.attendance;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores employee timecard report header data.
 */
public class EmployeeTimecardReportDTO {

    private int employeeId;
    private String employeeName;
    private String positionName;
    private String departmentName;
    private LocalDate periodEndDate;
    private List<EmployeeTimecardEntryDTO> entries;

    // constructors
    public EmployeeTimecardReportDTO() {
        this.entries = new ArrayList<>();
    }

    public EmployeeTimecardReportDTO(int employeeId,
                                     String employeeName,
                                     String positionName,
                                     String departmentName,
                                     LocalDate periodEndDate,
                                     List<EmployeeTimecardEntryDTO> entries) {

        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.positionName = positionName;
        this.departmentName = departmentName;
        this.periodEndDate = periodEndDate;
        this.entries = entries == null ? new ArrayList<>() : entries;
    }

    // getters/setters
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

    public LocalDate getPeriodEndDate() {
        return periodEndDate;
    }

    public void setPeriodEndDate(LocalDate periodEndDate) {
        this.periodEndDate = periodEndDate;
    }

    public List<EmployeeTimecardEntryDTO> getEntries() {
        return entries;
    }

    public void setEntries(List<EmployeeTimecardEntryDTO> entries) {
        this.entries = entries == null ? new ArrayList<>() : entries;
    }
}