package motorph.backend.dto.employee;

import motorph.backend.domain.value.EmploymentStatus;

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

    public EmployeeSummaryDTO(Integer employeeId, String fullName, EmploymentStatus employmentStatus,
                              int positionId, int departmentId, boolean active) {
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.employmentStatus = employmentStatus;
        this.positionId = positionId;
        this.departmentId = departmentId;
        this.active = active;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public String getFullName() {
        return fullName;
    }

    public EmploymentStatus getEmploymentStatus() {
        return employmentStatus;
    }

    public int getPositionId() {
        return positionId;
    }

    public int getDepartmentId() {
        return departmentId;
    }

    public boolean isActive() {
        return active;
    }
}
