package motorph.api.dto.employee;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import motorph.domain.value.EmploymentStatus;

/**
 * Wire copy of the backend's EmployeeSummaryDTO, mirroring the old
 * motorph.application.dto.employee.EmployeeSummaryDTO's getter names.
 */
public class EmployeeSummaryDTO {

    private final Integer employeeId;
    private final String fullName;
    private final EmploymentStatus employmentStatus;
    private final int positionId;
    private final int departmentId;
    private final boolean active;

    @JsonCreator
    public EmployeeSummaryDTO(@JsonProperty("employeeId") Integer employeeId,
                              @JsonProperty("fullName") String fullName,
                              @JsonProperty("employmentStatus") EmploymentStatus employmentStatus,
                              @JsonProperty("positionId") int positionId,
                              @JsonProperty("departmentId") int departmentId,
                              @JsonProperty("active") boolean active) {
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
