package motorph.application.dto.employee;

import motorph.domain.value.EmploymentStatus;

/**
 * Carries basic employee details for table and list display.
 */
public class EmployeeSummaryDTO {

    private final Integer employeeId;
    private final String fullName;
    private final EmploymentStatus employmentStatus;
    private final int positionId;
    private final int departmentId;
    private final boolean active;

    // constructors
    public EmployeeSummaryDTO(Integer employeeId,
                              String fullName,
                              EmploymentStatus employmentStatus,
                              int positionId,
                              int departmentId,
                              boolean active) {
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.employmentStatus = employmentStatus;
        this.positionId = positionId;
        this.departmentId = departmentId;
        this.active = active;
    }

    // getters
    public Integer getEmployeeId() {
        return employeeId;
    }

    // getters
    public String getFullName() {
        return fullName;
    }

    // getters
    public EmploymentStatus getEmploymentStatus() {
        return employmentStatus;
    }

    // getters
    public int getPositionId() {
        return positionId;
    }

    // getters
    public int getDepartmentId() {
        return departmentId;
    }

    // getters
    public boolean isActive() {
        return active;
    }
}